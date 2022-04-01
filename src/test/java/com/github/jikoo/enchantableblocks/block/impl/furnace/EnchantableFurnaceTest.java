package com.github.jikoo.enchantableblocks.block.impl.furnace;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.github.jikoo.enchantableblocks.util.mock.FurnaceMock;
import com.github.jikoo.planarwrappers.util.StringConverters;
import java.util.Objects;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Feature: Enchantable furnaces.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableFurnaceTest {

  private static final short PAUSED_TICKS = 200;

  private Plugin plugin;
  private EnchantableBlockManager manager;
  private EnchantableFurnaceRegistration registration;
  private Block block;
  private FurnaceMock tile;
  private ItemStack itemStack;
  private ConfigurationSection storage;

  @BeforeAll
  void setUpAll() {
    MockBukkit.mock();
  }

  @AfterAll
  void tearDownAll() {
    plugin.getServer().getScheduler().cancelTasks(plugin);
    MockBukkit.unmock();
  }

  @BeforeEach
  void setUp() {
    plugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    manager = new EnchantableBlockManager(plugin);
    registration = new EnchantableFurnaceRegistration(plugin, manager);
    manager.getRegistry().register(registration);
    var server = MockBukkit.getMock();
    server.getScheduler().performOneTick();

    // Add mock furnace recipe dirt -> coarse dirt
    server.addRecipe(new FurnaceRecipe(
        Objects.requireNonNull(StringConverters.toNamespacedKey("sample:text")),
        new ItemStack(Material.COARSE_DIRT), Material.DIRT, 0, 200));

    // Set up block and state
    var world = server.addSimpleWorld("world");
    var blockMock = world.getBlockAt(0, 0, 0);
    blockMock.setType(Material.FURNACE);
    tile = new FurnaceMock(blockMock);
    blockMock.setState(tile);
    block = blockMock;

    // Create default item and storage
    itemStack = new ItemStack(Material.FURNACE);
    storage = plugin.getConfig().createSection("going.to.the.store");
  }

  private @NotNull EnchantableFurnace newBlock() {
    return registration.newBlock(block, itemStack, storage);
  }

  @DisplayName("Block provides initializing registration.")
  @Test
  void testGetRegistration() {
    var enchantableFurnace = newBlock();
    assertThat("Furnace must supply initializing registration",
        enchantableFurnace.getRegistration(), is(registration));
  }

  @DisplayName("Block provides initializing registration's configuration.")
  @Test
  void testGetConfig() {
    var enchantableFurnace = newBlock();
    assertThat("Furnace must supply registration config",
        enchantableFurnace.getConfig(), is(registration.getConfig()));
  }

  @DisplayName("In-world tile is provided if correct.")
  @Test
  void testGetFurnaceTile() {
    var enchantableFurnace = newBlock();
    assertThat("Furnace must supply tile if block is furnace",
        enchantableFurnace.getFurnaceTile(), is(notNullValue()));

    block = block.getRelative(BlockFace.UP);
    enchantableFurnace = newBlock();
    assertThat("Furnace is null if block is not furnace",
        enchantableFurnace.getFurnaceTile(), is(nullValue()));
  }

  @DisplayName("Furnace modifiers are obtainable from enchantments.")
  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 3, 4, 5 })
  void testModifier(int modifier) {
    var enchantableFurnace = newBlock();

    assertThat("Base modifier must be 0", enchantableFurnace.getCookModifier(), is(0));
    assertThat("Base modifier must be 0", enchantableFurnace.getBurnModifier(), is(0));
    assertThat("Base modifier must be 0", enchantableFurnace.getFortune(), is(0));

    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, modifier);
    enchantableFurnace = newBlock();
    itemStack.removeEnchantment(Enchantment.DIG_SPEED);
    assertThat("Modifier must be set", enchantableFurnace.getCookModifier(), is(modifier));
    assertThat("Base modifier must be 0", enchantableFurnace.getBurnModifier(), is(0));
    assertThat("Base modifier must be 0", enchantableFurnace.getFortune(), is(0));

    itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, modifier);
    enchantableFurnace = newBlock();
    itemStack.removeEnchantment(Enchantment.DURABILITY);
    assertThat("Base modifier must be 0", enchantableFurnace.getCookModifier(), is(0));
    assertThat("Modifier must be set", enchantableFurnace.getBurnModifier(), is(modifier));
    assertThat("Base modifier must be 0", enchantableFurnace.getFortune(), is(0));

    itemStack.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, modifier);
    enchantableFurnace = newBlock();
    itemStack.removeEnchantment(Enchantment.LOOT_BONUS_BLOCKS);
    assertThat("Base modifier must be 0", enchantableFurnace.getCookModifier(), is(0));
    assertThat("Base modifier must be 0", enchantableFurnace.getBurnModifier(), is(0));
    assertThat("Modifier must be set", enchantableFurnace.getFortune(), is(modifier));
  }

  @DisplayName("Silk touch not present does not allow pausing.")
  @Test
  void testCannotPause() {
    var enchantableFurnace = newBlock();
    assertThat("Non-silk item cannot pause", enchantableFurnace.canPause(), is(false));
  }

  @DisplayName("Silk touch allows pausing.")
  @Test
  void testCanPause() {
    itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
    var enchantableFurnace = newBlock();
    assertThat("Silk item can pause", enchantableFurnace.canPause());
  }

  @DisplayName("Events that cause inventory modification are handled as if modification occurred.")
  @Test
  void testShouldPausePostSmelt() {
    PauseSituation pauseSituation = PAUSE_VALID_INPUT_VALID_OUTPUT;
    pauseSituation.setup().run();
    var enchantableFurnace = newBlock();
    var inventory = tile.getInventory();
    // Ensure single item just in case I'm dumb later
    Objects.requireNonNull(inventory.getSmelting()).setAmount(1);
    var event = new FurnaceSmeltEvent(block,
        Objects.requireNonNull(inventory.getSmelting()),
        Objects.requireNonNull(inventory.getResult()));

    assertThat(pauseSituation.reason(),
        enchantableFurnace.shouldPause(event), is(not(pauseSituation.pauseExpected())));
  }

  @DisplayName("Furnaces respond to cases where they should or should not pause appropriately.")
  @ParameterizedTest
  @MethodSource("getPauseCases")
  void testShouldPause(@NotNull EnchantableFurnaceTest.PauseSituation pauseSituation) {
    pauseSituation.setup().run();
    var enchantableFurnace = newBlock();
    assertThat(pauseSituation.reason(),
        enchantableFurnace.shouldPause(null), is(pauseSituation.pauseExpected()));
  }

  Stream<PauseSituation> getPauseCases() {
    return Stream.concat(getPauseTrueCases(), Stream.of(
        PAUSE_NON_SILK,
        PAUSE_INVALID_FURNACE,
        PAUSE_PRE_FROZEN,
        PAUSE_VALID_INPUT_NO_OUTPUT,
        PAUSE_VALID_INPUT_AIR_OUTPUT,
        PAUSE_VALID_INPUT_VALID_OUTPUT));
  }

  @DisplayName("Furnaces pause when appropriate.")
  @ParameterizedTest
  @MethodSource("getPauseTrueCases")
  void testPause(@NotNull EnchantableFurnaceTest.PauseSituation pauseSituation) {
    pauseSituation.setup().run();
    var enchantableFurnace = newBlock();
    enchantableFurnace.pause();
    assertThat(pauseSituation.reason(), enchantableFurnace.getFrozenTicks(), is(PAUSED_TICKS));
  }

  Stream<PauseSituation> getPauseTrueCases() {
    return Stream.of(PAUSE_VALID_NO_INPUT, PAUSE_VALID_OUTPUT_FULL, PAUSE_INVALID_INPUT);
  }

  @DisplayName("Furnaces resume when appropriate.")
  @ParameterizedTest
  @MethodSource("getResumeCases")
  void testResume(@NotNull EnchantableFurnaceTest.PauseSituation pauseSituation) {
    pauseSituation.setup().run();
    var enchantableFurnace = newBlock();

    int preResume = tile.getBurnTime();

    assertThat(pauseSituation.reason(), enchantableFurnace.resume(), is(not(pauseSituation.pauseExpected())));

    if (!pauseSituation.pauseExpected()) {
      // Reassign state - updating furnace assigns new state.
      tile = (FurnaceMock) block.getState();

      assertThat("Resumed furnace may not have frozen ticks",
          enchantableFurnace.getFrozenTicks(), is((short) 0));
      assertThat("Resumed time must be greater than pre-resume",
          (int) tile.getBurnTime(), is(greaterThan(preResume)));
    }
  }

  Stream<PauseSituation> getResumeCases() {
    return Stream.of(
        // Invalid furnace
        new PauseSituation(() -> block = block.getRelative(BlockFace.UP),
        "Invalid furnace cannot resume", true),
        // Non-silk
        new PauseSituation(() -> {}, "Non-silk cannot resume", true),
        // No frozen ticks
        new PauseSituation(() -> itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1),
            "Need frozen ticks to resume", true),
        // No input
        new PauseSituation(
            () -> itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, PAUSED_TICKS),
            "Furnace with no input cannot resume", true),
        // Air input
        new PauseSituation(
            () -> {
              itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, PAUSED_TICKS);
              var furnaceInventory = tile.getInventory();
              furnaceInventory.setSmelting(new ItemStack(Material.AIR));
            },"Furnace with air input cannot resume", true),
        // Full output
        new PauseSituation(
            () -> {
              itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, PAUSED_TICKS);
              var furnaceInventory = tile.getInventory();
              furnaceInventory.setSmelting(new ItemStack(Material.DIRT));
              furnaceInventory.setResult(
                  new ItemStack(Material.COARSE_DIRT, Material.COARSE_DIRT.getMaxStackSize()));
            },"Furnace with full output cannot resume", true),
        // Invalid input
        new PauseSituation(
            () -> {
              itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, PAUSED_TICKS);
              var furnaceInventory = tile.getInventory();
              furnaceInventory.setSmelting(new ItemStack(Material.FURNACE));
            },"Furnace with invalid input cannot resume", true),
        // Invalid output
        new PauseSituation(
            () -> {
              itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, PAUSED_TICKS);
              var furnaceInventory = tile.getInventory();
              furnaceInventory.setSmelting(new ItemStack(Material.DIRT));
              furnaceInventory.setResult(new ItemStack(Material.BLAST_FURNACE));
            },"Furnace with invalid output cannot resume", true
        ),

        // Resume expected
        // Valid input, no output
        new PauseSituation(
            () -> {
              itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, PAUSED_TICKS);
              var furnaceInventory = tile.getInventory();
              furnaceInventory.setSmelting(new ItemStack(Material.DIRT));
            },"Furnace with no output can resume", false
        ),
        // Valid input, air output
        new PauseSituation(
            () -> {
              itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, PAUSED_TICKS);
              var furnaceInventory = tile.getInventory();
              furnaceInventory.setSmelting(new ItemStack(Material.DIRT));
              furnaceInventory.setResult(new ItemStack(Material.AIR));
            },"Furnace with empty output can resume", false
        ),
        // Valid input, valid output
        new PauseSituation(
            () -> {
              itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, PAUSED_TICKS);
              var furnaceInventory = tile.getInventory();
              furnaceInventory.setSmelting(new ItemStack(Material.DIRT));
              furnaceInventory.setResult(new ItemStack(Material.COARSE_DIRT));
            },"Furnace with valid output can resume", false
        )
    );
  }

  @DisplayName("Furnaces that have been forcibly resumed are unpaused.")
  @ParameterizedTest
  @MethodSource("getResumeCases")
  void testForceResume(PauseSituation pauseSituation) {
    pauseSituation.setup().run();
    var enchantableFurnace = newBlock();

    boolean prePaused = enchantableFurnace.isPaused();

    assertThat(pauseSituation.reason(),
        enchantableFurnace.forceResume(),
        is(prePaused && enchantableFurnace.getFurnaceTile() != null));
  }

  @DisplayName("Cook time modifiers apply correctly to cook time.")
  @ParameterizedTest
  @CsvSource({
      "0,200,200",
      "1,200,133", "2,200,100", "3,200,80", "4,200,66", "5,200,57",
      "6,200,50", "7,200,44", "8,200,39", "9,200,36", "10,200,33",
      "-1,200,266", "-2,200,300", "-3,200,320", "-4,200,333", "-5,200,342",
      "-6,200,350", "-7,200,355", "-8,200,360", "-9,200,363", "-10,200,366"
  })
  void testApplyCookTimeModifiers(int level, int ticks, short expectedTicks) {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, level);
    var enchantableFurnace = newBlock();

    assertThat("Calculated value must be equal to expectation",
        enchantableFurnace.applyCookTimeModifiers(ticks), is(expectedTicks));
  }

  @DisplayName("Burn time modifiers apply correctly to burn time.")
  @ParameterizedTest
  @CsvSource({
      "0,1600,1600",
      "1,1600,2000", "2,1600,2240", "3,1600,2400", "4,1600,2514", "5,1600,2600",
      "-1,1600,1200", "-2,1600,960", "-3,1600,800", "-4,1600,686", "-5,200,75",
  })
  void testApplyBurnTimeModifiers(int level, int ticks, short expectedTicks) {
    itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, level);
    var enchantableFurnace = newBlock();

    assertThat("Calculated value must be equal to expectation",
        enchantableFurnace.applyBurnTimeModifiers(ticks), is(expectedTicks));
  }

  @DisplayName("Furnace updates do not throw errors and handle all possible situations.")
  @ParameterizedTest
  @MethodSource("getAllCases")
  void testUpdate(PauseSituation situation) {
    situation.setup().run();
    manager.createBlock(this.block, itemStack);
    var inventory = tile.getInventory();
    assertDoesNotThrow(() -> EnchantableFurnace.update(plugin, manager, inventory));
    var scheduler = MockBukkit.getMock().getScheduler();
    assertDoesNotThrow(scheduler::performOneTick);
    manager.destroyBlock(block);
  }

  Stream<PauseSituation> getAllCases() {
    return Stream.concat(getPauseCases(), getResumeCases());
  }

  private record PauseSituation(
      @NotNull Runnable setup,
      @NotNull String reason,
      boolean pauseExpected) {}

  // Pause not expected
  private final PauseSituation PAUSE_NON_SILK = new PauseSituation(
      () -> {}, "Non-silk cannot pause", false);
  private final PauseSituation PAUSE_INVALID_FURNACE = new PauseSituation(
      () -> {
        itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        block = block.getRelative(BlockFace.UP);
      }, "Invalid furnace cannot pause", false);
  private final PauseSituation PAUSE_PRE_FROZEN = new PauseSituation(
      () -> itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 2),
      "Furnace with existing frozen ticks cannot pause", false);
  private final PauseSituation PAUSE_VALID_INPUT_NO_OUTPUT = new PauseSituation(
      () -> {
        itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        var furnaceInventory = tile.getInventory();
        furnaceInventory.setSmelting(new ItemStack(Material.DIRT));
      },"Furnace with smeltable input and no output cannot pause", false);
  private final PauseSituation PAUSE_VALID_INPUT_AIR_OUTPUT = new PauseSituation(
      () -> {
        itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        var furnaceInventory = tile.getInventory();
        furnaceInventory.setSmelting(new ItemStack(Material.DIRT));
        furnaceInventory.setResult(new ItemStack(Material.AIR));
      },"Furnace with smeltable input and empty output cannot pause", false);
  private final PauseSituation PAUSE_VALID_INPUT_VALID_OUTPUT = new PauseSituation(
      () -> {
        itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        var furnaceInventory = tile.getInventory();
        furnaceInventory.setSmelting(new ItemStack(Material.DIRT));
        furnaceInventory.setResult(new ItemStack(Material.COARSE_DIRT));
      },"Furnace with smeltable input and valid output cannot pause", false);

  // Pause expected
  private final PauseSituation PAUSE_VALID_NO_INPUT = new PauseSituation(
      () -> {
        itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        tile.setBurnTime(PAUSED_TICKS);
      },"Furnace with no input can pause", true
  );
  private final PauseSituation PAUSE_VALID_OUTPUT_FULL = new PauseSituation(
      () -> {
        itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        tile.setBurnTime(PAUSED_TICKS);
        var furnaceInventory = tile.getInventory();
        furnaceInventory.setSmelting(new ItemStack(Material.FURNACE));
        furnaceInventory.setResult(new ItemStack(Material.BLAST_FURNACE, Material.BLAST_FURNACE.getMaxStackSize()));
      },"Furnace with full output can pause", true
  );
  private final PauseSituation PAUSE_INVALID_INPUT = new PauseSituation(
      () -> {
        itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        tile.setBurnTime(PAUSED_TICKS);
        var furnaceInventory = tile.getInventory();
        furnaceInventory.setSmelting(new ItemStack(Material.FURNACE, 0));
      },"Furnace with invalid input can pause", true
  );

}