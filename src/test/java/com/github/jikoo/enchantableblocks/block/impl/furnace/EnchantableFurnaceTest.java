package com.github.jikoo.enchantableblocks.block.impl.furnace;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.InventoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.github.jikoo.planarwrappers.util.StringConverters;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.jikoo.enchantableblocks.mock.matcher.ItemMatcher.isSimilar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Feature: Enchantable furnaces.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableFurnaceTest {

  private FurnaceRecipe recipe;
  private EnchantableFurnaceRegistration reg;
  private Block block;
  private ItemStack itemStack;
  private ConfigurationSection storage;

  @BeforeAll
  void beforeAll() {
    var server = ServerMocks.mockServer();

    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);
    var pluginManager = mock(PluginManager.class);
    when(server.getPluginManager()).thenReturn(pluginManager);

    recipe = new FurnaceRecipe(
        Objects.requireNonNull(StringConverters.toNamespacedKey("sample:text")),
        ItemType.COARSE_DIRT.createItemStack(), Material.DIRT, 0, 200);
  }

  @BeforeEach
  void beforeEach() {
    reg = mock(EnchantableFurnaceRegistration.class);
    block = mock(Block.class);
    itemStack = ItemType.FURNACE.createItemStack();
    storage = mock(ConfigurationSection.class);

    // Set up matching recipe
    when(reg.getFurnaceRecipe(any())).thenAnswer(invocation -> {
      FurnaceInventory inventory = invocation.getArgument(0);
      if (recipe.getInput().isSimilar(inventory.getSmelting())) {
        return recipe;
      }
      return null;
    });
  }

  private @NotNull Furnace setUpTile() {
    var tile = mock(Furnace.class);
    when(block.getState()).thenReturn(tile);

    var inventory = InventoryMocks.newFurnaceMock();
    when(tile.getInventory()).thenReturn(inventory);
    when(inventory.getHolder()).thenReturn(tile);

    AtomicInteger burnTime = new AtomicInteger();
    when(tile.getBurnTime()).thenAnswer(invocation -> (short) burnTime.get());
    doAnswer(invocation -> {
      burnTime.set(invocation.getArgument(0, Short.class));
      return null;
    }).when(tile).setBurnTime(anyShort());

    AtomicInteger cookTimeTotal = new AtomicInteger();
    when(tile.getCookTimeTotal()).thenAnswer(invocation -> cookTimeTotal.get());
    doAnswer(invocation -> {
      cookTimeTotal.set(invocation.getArgument(0));
      return null;
    }).when(tile).setCookTimeTotal(anyInt());

    return tile;
  }

  @DisplayName("Legacy data is updated")
  @Test
  void testConstructorLegacyData() {
    short legacyFrozenTicks = 200;
    itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, legacyFrozenTicks);

    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Data needs saving", enchantableFurnace.isDirty());
    assertThat("Legacy frozen ticks are preserved", enchantableFurnace.getFrozenTicks(), is(legacyFrozenTicks));
    verify(enchantableFurnace.getItemStack()).addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
  }

  @DisplayName("New data is created")
  @Test
  void testConstructorNewData() {
    itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);

    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Data needs saving", enchantableFurnace.isDirty());
    assertThat("Silk touch can pause", enchantableFurnace.canPause());
    assertThat("No free frozen tick", enchantableFurnace.getFrozenTicks(), is((short) 0));
    verify(enchantableFurnace.getItemStack(), times(0)).addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
  }

  @DisplayName("New data does not fetch silk level")
  @Test
  void testConstructorNewNonSilk() {
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Data needs saving", enchantableFurnace.isDirty());
    assertThat("Non-silk cannot pause", enchantableFurnace.canPause(), is(false));
    verify(enchantableFurnace.getItemStack(), times(0)).getEnchantmentLevel(Enchantment.SILK_TOUCH);
  }

  @DisplayName("Existing data is used")
  @Test
  void testConstructorExistingData() {
    storage = new YamlConfiguration();
    storage.set("silk.enabled", true);
    short frozenTicks = 200;
    storage.set("silk.ticks", frozenTicks);

    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Data needs saving", enchantableFurnace.isDirty());
    assertThat("Can pause", enchantableFurnace.canPause());
    assertThat("No free frozen tick", enchantableFurnace.getFrozenTicks(), is(frozenTicks));
    verify(enchantableFurnace.getItemStack(), times(0)).addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
  }

  @DisplayName("Block provides initializing registration.")
  @Test
  void testGetRegistration() {
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Furnace must supply initializing registration",
        enchantableFurnace.getRegistration(), is(reg));
  }

  @DisplayName("Block provides initializing registration's configuration.")
  @Test
  void testGetConfig() {
    var config = mock(EnchantableFurnaceConfig.class);
    when(reg.getConfig()).thenReturn(config);

    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Furnace must supply registration config",
        enchantableFurnace.getConfig(), is(config));
  }

  @DisplayName("In-world tile is not provided if invalid.")
  @Test
  void testGetFurnaceTileInvalid() {
    var tile = mock(Chest.class);
    when(block.getState()).thenReturn(tile);

    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Invalid tile is not provided", enchantableFurnace.getFurnaceTile(), is(nullValue()));
  }

  @DisplayName("In-world tile is provided if valid.")
  @Test
  void testGetFurnaceTileValid() {
    var tile = mock(Furnace.class);
    when(block.getState()).thenReturn(tile);

    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Valid tile is provided", enchantableFurnace.getFurnaceTile(), is(tile));
  }

  @DisplayName("Furnace modifiers are set by enchantments.")
  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 3 })
  void testModifier(int modifier) {
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);

    assertThat("Base modifier must be 0", enchantableFurnace.getCookModifier(), is(0));
    assertThat("Base modifier must be 0", enchantableFurnace.getBurnModifier(), is(0));
    assertThat("Base modifier must be 0", enchantableFurnace.getFortune(), is(0));

    itemStack.addUnsafeEnchantment(Enchantment.EFFICIENCY, modifier);
    enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    itemStack.removeEnchantment(Enchantment.EFFICIENCY);
    assertThat("Modifier must be set", enchantableFurnace.getCookModifier(), is(modifier));
    assertThat("Base modifier must be 0", enchantableFurnace.getBurnModifier(), is(0));
    assertThat("Base modifier must be 0", enchantableFurnace.getFortune(), is(0));

    itemStack.addUnsafeEnchantment(Enchantment.UNBREAKING, modifier);
    enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    itemStack.removeEnchantment(Enchantment.UNBREAKING);
    assertThat("Base modifier must be 0", enchantableFurnace.getCookModifier(), is(0));
    assertThat("Modifier must be set", enchantableFurnace.getBurnModifier(), is(modifier));
    assertThat("Base modifier must be 0", enchantableFurnace.getFortune(), is(0));

    itemStack.addUnsafeEnchantment(Enchantment.FORTUNE, modifier);
    enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    itemStack.removeEnchantment(Enchantment.FORTUNE);
    assertThat("Base modifier must be 0", enchantableFurnace.getCookModifier(), is(0));
    assertThat("Base modifier must be 0", enchantableFurnace.getBurnModifier(), is(0));
    assertThat("Modifier must be set", enchantableFurnace.getFortune(), is(modifier));
  }

  @DisplayName("Silk touch not present does not allow pausing.")
  @Test
  void testCannotPause() {
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Non-silk item cannot pause", enchantableFurnace.canPause(), is(false));
    assertThat("Unpauseable furnace is not paused", enchantableFurnace.isPaused(), is(false));
    enchantableFurnace.setFrozenTicks((short) 10);
    assertThat(
        "Unpauseable furnace is not paused with frozen ticks",
        enchantableFurnace.isPaused(),
        is(false));
  }

  @DisplayName("Silk touch allows pausing.")
  @Test
  void testPauseFreeze() {
    itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Silk item can pause", enchantableFurnace.canPause());
    short ticks = 0;
    assertThat("Furnace has no frozen ticks", enchantableFurnace.getFrozenTicks(), is(ticks));
    assertThat(
        "Pauseable furnace is not paused with no frozen ticks",
        enchantableFurnace.isPaused(),
        is(false));
    ticks = 10;
    enchantableFurnace.setFrozenTicks(ticks);
    assertThat("Pauseable furnace is paused with frozen ticks", enchantableFurnace.isPaused());
    assertThat("Furnace has frozen ticks", enchantableFurnace.getFrozenTicks(), is(ticks));
  }

  @DisplayName("Furnace that cannot pause should not pause.")
  @Test
  void testShouldPauseCannotPause() {
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat(
        "Furnace that cannot pause should not pause",
        enchantableFurnace.shouldPause(null),
        is(false));
  }

  @DisplayName("Furnace with null tile should not pause.")
  @Test
  void testShouldPauseNullTile() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    assertThat(
        "Furnace with null tile should not pause",
        enchantableFurnace.shouldPause(null),
        is(false));
  }

  @DisplayName("Furnace that cannot pause should not pause.")
  @Test
  void testShouldPauseInternal() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true, false);
    setUpTile();
    assertThat(
        "Furnace that cannot pause should not pause",
        enchantableFurnace.shouldPause(null),
        is(false));
  }

  @DisplayName("Furnace that is already paused should not pause.")
  @Test
  void testShouldPauseHasFrozenTicks() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    when(enchantableFurnace.getFrozenTicks()).thenReturn((short) 10);
    setUpTile();
    assertThat(
        "Furnace that is already paused should not pause",
        enchantableFurnace.shouldPause(null),
        is(false));
  }

  @DisplayName("Furnace with no input should pause.")
  @Test
  void testShouldPauseNoInput() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    setUpTile();
    assertThat("Furnace with no input should pause", enchantableFurnace.shouldPause(null));
  }

  @DisplayName("Furnace with full result should pause.")
  @Test
  void testShouldPauseFullResult() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    var tile = setUpTile();
    var inv = tile.getInventory();
    inv.setSmelting(recipe.getInput());
    var result = recipe.getResult();
    result.setAmount(result.getType().getMaxStackSize());
    inv.setResult(result);

    assertThat("Furnace with no input should pause", enchantableFurnace.shouldPause(null));
  }

  @DisplayName("Furnace with no matching recipe should pause.")
  @Test
  void testShouldPauseNonmatching() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    var tile = setUpTile();
    var inv = tile.getInventory();
    ItemStack input = new ItemStack(Material.FURNACE);
    assertThat("Input is not similar to recipe input", input, not(isSimilar(recipe.getInput())));
    inv.setSmelting(input);
    inv.setResult(recipe.getResult());

    assertThat("Furnace with no matching recipe should pause", enchantableFurnace.shouldPause(null));
  }

  @DisplayName("Furnace with input not matching recipe input should pause")
  @Test
  void testShouldPauseInputNotYieldResult() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);

    var mockRecipe = mock(FurnaceRecipe.class);
    var choice = mock(RecipeChoice.class);
    when(mockRecipe.getInputChoice()).thenReturn(choice);
    doAnswer(invocation -> mockRecipe).when(reg).getFurnaceRecipe(any());

    var tile = setUpTile();
    var inv = tile.getInventory();
    inv.setSmelting(recipe.getInput());
    inv.setResult(recipe.getResult());

    assertThat("Furnace with input not matching recipe input should pause", enchantableFurnace.shouldPause(null));
  }

  @DisplayName("Furnace with null result should not pause")
  @Test
  void testShouldPauseNullResult() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    var tile = setUpTile();
    var inv = tile.getInventory();
    inv.setSmelting(recipe.getInput());

    assertThat(
        "Furnace with null result should not pause",
        enchantableFurnace.shouldPause(null),
        is(false));
  }

  @DisplayName("Furnace with air result should not pause")
  @Test
  void testShouldPauseAirResult() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    var tile = setUpTile();
    var inv = tile.getInventory();
    inv.setSmelting(recipe.getInput());
    inv.setResult(new ItemStack(Material.AIR));

    assertThat(
        "Furnace with air result should not pause",
        enchantableFurnace.shouldPause(null),
        is(false));
  }

  @DisplayName("Furnace with similar result should not pause")
  @Test
  void testShouldPauseSimilarResult() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    var tile = setUpTile();
    var inv = tile.getInventory();
    inv.setSmelting(recipe.getInput());
    inv.setResult(recipe.getResult());

    assertThat(
        "Furnace with similar result should not pause",
        enchantableFurnace.shouldPause(null),
        is(false));
  }

  @DisplayName("Furnace with dissimilar result should pause")
  @Test
  void testShouldPauseDissimilarResult() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    var tile = setUpTile();
    var inv = tile.getInventory();
    inv.setSmelting(recipe.getInput());
    ItemStack result = new ItemStack(Material.DIAMOND);
    assertThat("Result must be dissimilar", result, not(isSimilar(recipe.getResult())));
    inv.setResult(result);

    assertThat("Furnace with dissimilar result should pause", enchantableFurnace.shouldPause(null));
  }

  @DisplayName("FurnaceSmeltEvents are handled as if event has completed.")
  @Test
  void testShouldPausePostSmelt() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);

    var furnace = setUpTile();
    var inv = furnace.getInventory();
    var smelting = recipe.getInput();
    inv.setSmelting(smelting);
    inv.setResult(null);
    var event = new FurnaceSmeltEvent(block, smelting, recipe.getResult());

    assertThat(
        "Situation does not result in pausing without event context",
        enchantableFurnace.shouldPause(null),
        is(false));
    assertThat(
        "Events that cause inventory modification are handled as if modification occurred",
        enchantableFurnace.shouldPause(event));
  }

  @DisplayName("Furnace that cannot pause will not pause.")
  @Test
  void testPauseCannotPause() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    enchantableFurnace.pause();
    assertThat("Furnace is not paused", enchantableFurnace.isPaused(), is(false));
    verify(enchantableFurnace, times(0)).setFrozenTicks(anyShort());
  }

  @DisplayName("Furnace that has frozen ticks will not pause.")
  @Test
  void testPausePreFrozen() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    when(enchantableFurnace.getFrozenTicks()).thenReturn((short) 10);

    enchantableFurnace.pause();

    assertThat("Furnace is not paused", enchantableFurnace.isPaused(), is(false));
    verify(enchantableFurnace, times(0)).setFrozenTicks(anyShort());
  }

  @DisplayName("Furnace that has no tile will not pause.")
  @Test
  void testPauseNoTile() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);

    enchantableFurnace.pause();

    assertThat("Furnace is not paused", enchantableFurnace.isPaused(), is(false));
    verify(enchantableFurnace, times(0)).setFrozenTicks(anyShort());
  }

  @DisplayName("Furnace freezes ticks when pausing.")
  @Test
  void testPause() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    when(enchantableFurnace.isPaused()).thenAnswer(invocation -> enchantableFurnace.getFrozenTicks() > 0);
    var tile = setUpTile();
    short frozenTime = 200;
    tile.setBurnTime(frozenTime);

    enchantableFurnace.pause();

    assertThat("Furnace is paused", enchantableFurnace.isPaused());
    verify(enchantableFurnace).setFrozenTicks(anyShort());
    verify(tile).setBurnTime((short) 0);
    verify(tile).update(true);
    assertThat("Furnace has frozen ticks", enchantableFurnace.getFrozenTicks(), is(frozenTime));
  }

  @DisplayName("Furnace does not resume when not frozen.")
  @Test
  void testResumeNotFrozen() {
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
    assertThat("Furnace does not resume when not frozen", enchantableFurnace.resume(), is(false));
  }

  @DisplayName("Furnace does not resume without a tile.")
  @Test
  void testResumeNullTile() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.isPaused()).thenReturn(true);
    assertThat("Furnace does not resume without a tile", enchantableFurnace.resume(), is(false));
  }

  @DisplayName("Furnace does not resume freezable tile.")
  @Test
  void testResumeFreezableTile() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.isPaused()).thenReturn(true);
    setUpTile();

    assertThat("Furnace does not resume freezable tile", enchantableFurnace.resume(), is(false));
  }

  @DisplayName("Furnace forcibly resumes freezable tile when ignoring state.")
  @Test
  void testResumeForceFreezableTile() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.isPaused()).thenReturn(true);
    setUpTile();

    assertThat("Furnace forcibly resumes freezable tile", enchantableFurnace.resume(false));
  }

  @DisplayName("Furnace resumes as needed.")
  @Test
  void testResume() {
    var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
    when(enchantableFurnace.canPause()).thenReturn(true);
    when(enchantableFurnace.isPaused()).thenAnswer(invocation -> enchantableFurnace.getFrozenTicks() > 0);
    short frozenTicks = 200;
    enchantableFurnace.setFrozenTicks(frozenTicks);
    var tile = setUpTile();
    tile.getInventory().setSmelting(recipe.getInput());

    assertThat("Furnace resumes as needed", enchantableFurnace.resume());
    assertThat("Tile is burning", tile.getBurnTime(), is(frozenTicks));
    verify(tile).update(true);
    assertThat("Furnace is no longer frozen", enchantableFurnace.getFrozenTicks(), is((short) 0));
  }

  @DisplayName("Cook time modifiers apply correctly to cook and burn time.")
  @ParameterizedTest
  @CsvSource({
      "0,200,200",
      "1,200,133", "2,200,100", "3,200,80", "4,200,66", "5,200,57",
      "6,200,50", "7,200,44", "8,200,39", "9,200,36", "10,200,33",
      "-1,200,266", "-2,200,300", "-3,200,320", "-4,200,333", "-5,200,342",
      "-6,200,350", "-7,200,355", "-8,200,360", "-9,200,363", "-10,200,366"
  })
  void testApplyCookTimeModifiers(int level, int ticks, short expectedTicks) {
    itemStack.addUnsafeEnchantment(Enchantment.EFFICIENCY, level);
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);

    assertThat("Cook time must be modified as expected",
        enchantableFurnace.applyCookTimeModifiers(ticks), is(expectedTicks));
    assertThat("Burn time must be modified as expected",
        enchantableFurnace.applyBurnTimeModifiers(ticks), is(expectedTicks));
  }

  @DisplayName("Burn time modifiers apply correctly to burn time.")
  @ParameterizedTest
  @CsvSource({
      "0,1600,1600",
      "1,1600,2000", "2,1600,2240", "3,1600,2400", "4,1600,2514", "5,1600,2600",
      "-1,1600,1200", "-2,1600,960", "-3,1600,800", "-4,1600,685", "-5,200,75",
  })
  void testApplyBurnTimeModifiers(int level, int ticks, short expectedTicks) {
    itemStack.addUnsafeEnchantment(Enchantment.UNBREAKING, level);
    var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);

    assertThat("Calculated value must be equal to expectation",
        enchantableFurnace.applyBurnTimeModifiers(ticks), is(expectedTicks));
  }

  @DisplayName("Furnaces must update as expected")
  @Nested
  class UpdateTests {

    private Plugin plugin;
    private ArgumentCaptor<Runnable> taskCaptor;

    @BeforeAll
    static void beforeAll() {
      var server = Bukkit.getServer();
      var scheduler = mock(BukkitScheduler.class);
      when(server.getScheduler()).thenReturn(scheduler);
    }

    @AfterAll
    static void afterAll() {
      var server = Bukkit.getServer();
      when(server.getScheduler()).thenReturn(null);
    }

    @BeforeEach
    void beforeEach() {
      plugin = mock(Plugin.class);
      var server = Bukkit.getServer();
      doReturn(server).when(plugin).getServer();

      taskCaptor = ArgumentCaptor.forClass(Runnable.class);
      var scheduler = server.getScheduler();
      doReturn(null).when(scheduler).runTask(any(), taskCaptor.capture());
    }

    @DisplayName("Furnaces must have tiles to update.")
    @Test
    void testUpdateNoTile() {
      var manager = mock(EnchantableBlockManager.class);
      var inventory = mock(FurnaceInventory.class);

      EnchantableFurnace.update(plugin, manager, inventory);
      verify(manager, times(0)).getBlock(any());
    }

    @DisplayName("Tile must be linked to an EnchantableFurnace to update.")
    @Test
    void testUpdateNullEnchantableBlock() {
      var manager = mock(EnchantableBlockManager.class);
      var inventory = setUpTile().getInventory();

      EnchantableFurnace.update(plugin, manager, inventory);
      verify(manager).getBlock(any());
      verify(plugin, times(0)).getServer();

      manager = mock(EnchantableBlockManager.class);
      var enchantableBlock = mock(EnchantableBlock.class);
      doReturn(enchantableBlock).when(manager).getBlock(any());

      EnchantableFurnace.update(plugin, manager, inventory);
      verify(manager).getBlock(any());
      verify(plugin, times(0)).getServer();
    }

    @DisplayName("Furnace must be able to pause to update.")
    @Test
    void testUpdateNoPause() {
      var manager = mock(EnchantableBlockManager.class);
      var enchantableFurnace = new EnchantableFurnace(reg, block, itemStack, storage);
      doReturn(enchantableFurnace).when(manager).getBlock(any());
      var inventory = setUpTile().getInventory();

      EnchantableFurnace.update(plugin, manager, inventory);
      verify(manager).getBlock(any());
      verify(plugin, times(0)).getServer();
    }

    @DisplayName("Multiple updates do not trigger multiple tasks.")
    @Test
    void testUpdateRepeat() {
      var manager = mock(EnchantableBlockManager.class);
      var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
      when(enchantableFurnace.canPause()).thenReturn(true);
      doReturn(enchantableFurnace).when(manager).getBlock(any());
      var inventory = setUpTile().getInventory();

      EnchantableFurnace.update(plugin, manager, inventory);
      verify(plugin).getServer();
      EnchantableFurnace.update(plugin, manager, inventory);
      verify(plugin).getServer();

      // Run task, allowing another to be scheduled
      taskCaptor.getValue().run();

      EnchantableFurnace.update(plugin, manager, inventory);
      verify(plugin, times(2)).getServer();
    }

    @DisplayName("Matching pause state does nothing")
    @Test
    void testUpdateNoPauseChange() {
      var manager = mock(EnchantableBlockManager.class);
      var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
      when(enchantableFurnace.canPause()).thenReturn(true);
      doReturn(enchantableFurnace).when(manager).getBlock(any());
      var inventory = setUpTile().getInventory();
      EnchantableFurnace.update(plugin, manager, inventory);
      Runnable task = taskCaptor.getValue();

      when(enchantableFurnace.isPaused()).thenReturn(true);
      task.run();
      verify(enchantableFurnace, times(0)).pause();
      verify(enchantableFurnace, times(0)).resume();

      when(enchantableFurnace.isPaused()).thenReturn(false);
      inventory.setSmelting(recipe.getInput());
      task.run();
      verify(enchantableFurnace, times(0)).pause();
      verify(enchantableFurnace, times(0)).resume();
    }

    @DisplayName("Paused but resumable resumes")
    @Test
    void testUpdateResume() {
      var manager = mock(EnchantableBlockManager.class);
      var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
      when(enchantableFurnace.canPause()).thenReturn(true);
      doReturn(enchantableFurnace).when(manager).getBlock(any());
      var inventory = setUpTile().getInventory();
      EnchantableFurnace.update(plugin, manager, inventory);
      Runnable task = taskCaptor.getValue();

      when(enchantableFurnace.isPaused()).thenReturn(true);
      inventory.setSmelting(recipe.getInput());
      task.run();
      verify(enchantableFurnace, times(0)).pause();
      verify(enchantableFurnace).resume();
    }

    @DisplayName("Running but pauseable pauses")
    @Test
    void testUpdatePause() {
      var manager = mock(EnchantableBlockManager.class);
      var enchantableFurnace = spy(new EnchantableFurnace(reg, block, itemStack, storage));
      when(enchantableFurnace.canPause()).thenReturn(true);
      doReturn(enchantableFurnace).when(manager).getBlock(any());
      var inventory = setUpTile().getInventory();
      EnchantableFurnace.update(plugin, manager, inventory);
      Runnable task = taskCaptor.getValue();

      task.run();
      verify(enchantableFurnace).pause();
      verify(enchantableFurnace, times(0)).resume();
    }

  }

}