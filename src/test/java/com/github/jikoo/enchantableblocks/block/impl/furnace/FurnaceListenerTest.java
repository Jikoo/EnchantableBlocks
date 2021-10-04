package com.github.jikoo.enchantableblocks.block.impl.furnace;

import static com.github.jikoo.enchantableblocks.util.matcher.IsSimilarMatcher.isSimilar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.inventory.ChestInventoryMock;
import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.github.jikoo.enchantableblocks.util.mock.FurnaceMock;
import com.github.jikoo.planarwrappers.util.StringConverters;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Feature: Event handlers for enchantable furnaces.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FurnaceListenerTest {

  private static final short BURN_TIME = 1600;

  private PluginManager pluginManager;
  private EnchantableBlockManager manager;
  private FurnaceRecipe recipe;
  private Player player;
  private Block block;
  private FurnaceMock tile;
  private ItemStack itemStack;

  @BeforeAll
  void setUpAll() {
    var server = MockBukkit.mock();
    pluginManager = server.getPluginManager();

    // Add mock furnace recipe dirt -> coarse dirt
    recipe = new FurnaceRecipe(
        Objects.requireNonNull(StringConverters.toNamespacedKey("sample:text")),
        new ItemStack(Material.COARSE_DIRT), Material.DIRT, 0, 200);
    server.addRecipe(recipe);

    // Add a player for events
    player = MockBukkit.getMock().addPlayer("sampletext");

    var plugin = MockBukkit.load(EnchantableBlocksPlugin.class);
    manager = plugin.getBlockManager();
    server.getScheduler().performOneTick();
  }

  @AfterAll
  void tearDownAll() {
    MockBukkit.unmock();
  }

  @BeforeEach
  void setUp() {
    // Set up block and state
    var server = MockBukkit.getMock();
    var world = server.addSimpleWorld("world");
    var blockMock = world.getBlockAt(0, 0, 0);
    blockMock.setType(Material.FURNACE);
    tile = new FurnaceMock(blockMock);
    blockMock.setState(tile);
    block = blockMock;

    // Create default item and storage
    itemStack = new ItemStack(Material.FURNACE);
  }

  @AfterEach
  void tearDown() {
    manager.destroyBlock(block);
  }

  private @Nullable EnchantableBlock newBlock() {
    return manager.createBlock(block, itemStack);
  }

  @DisplayName("Invalid furnaces do not modify FurnaceBurnEvents.")
  @Test
  void testInvalidFurnaceConsumeFuel() {
    var enchantableBlock = newBlock();
    assertThat("Unenchanted item yields invalid block", enchantableBlock, is(nullValue()));
    var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Invalid block must be ignored", !event.isCancelled());
    assertThat("Burn time must be unmodified", event.getBurnTime(), is((int) BURN_TIME));
  }

  @DisplayName("Paused furnaces cancel FurnaceBurnEvents and resume.")
  @Test
  void testPausedFurnaceConsumeFuel() {
    itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 200);
    var enchantableBlock = newBlock();
    assertThat("Block must be valid", enchantableBlock, is(notNullValue()));
    var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Resume must cancel event", event.isCancelled());
  }

  @DisplayName("Unbreaking furnaces modify fuel life.")
  @Test
  void testFurnaceConsumeFuel() {
    itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
    var enchantableBlock = newBlock();
    assertThat("Block must be valid", enchantableBlock, is(notNullValue()));
    var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Non-resume event is not cancelled", !event.isCancelled());
    assertThat("Burn time must be modified", event.getBurnTime(), is(not(BURN_TIME)));
  }

  @DisplayName("Invalid furnaces do not modify FurnaceStartSmeltEvents.")
  @Test
  void testInvalidFurnaceStartSmelt() {
    var enchantableBlock = newBlock();
    assertThat("Unenchanted item yields invalid block", enchantableBlock, is(nullValue()));
    var event = new FurnaceStartSmeltEvent(block, recipe.getInput(), recipe);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat(
        "Cook time must not be modified",
        event.getTotalCookTime(),
        is(recipe.getCookingTime()));
  }

  @DisplayName("Efficiency furnaces modify total cook time.")
  @Test
  void testValidFurnaceStartSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = newBlock();
    assertThat("Block must be valid", enchantableBlock, is(notNullValue()));
    var event = new FurnaceStartSmeltEvent(block, recipe.getInput(), recipe);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat(
        "Cook time must be modified",
        event.getTotalCookTime(),
        is(not(recipe.getCookingTime())));
  }

  @DisplayName("Invalid furnaces do not modify FurnaceSmeltEvents.")
  @Test
  void testInvalidFurnaceSmelt() {
    var enchantableBlock = newBlock();
    assertThat("Unenchanted item yields invalid block", enchantableBlock, is(nullValue()));
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Event is never cancelled", !event.isCancelled());
    assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
  }

  @DisplayName("Invalid furnace tiles do not modify FurnaceSmeltEvents.")
  @Test
  void testInvalidTileFurnaceSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = newBlock();
    assertThat("Block must be valid", enchantableBlock, is(notNullValue()));
    block.setType(Material.DIRT);
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Event is never cancelled", !event.isCancelled());
    assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
  }

  @DisplayName("Efficiency and unbreaking furnaces do not modify FurnaceSmeltEvents.")
  @Test
  void testBoringFurnaceSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
    var enchantableBlock = newBlock();
    assertThat("Block must be valid", enchantableBlock, is(notNullValue()));
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Event is never cancelled", !event.isCancelled());
    assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
  }

  @DisplayName("Fortune furnaces do not cancel FurnaceSmeltEvents.")
  @Test
  void testFortuneFurnaceSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 10);
    var enchantableBlock = newBlock();
    assertThat("Block must be valid", enchantableBlock, is(notNullValue()));
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Event is never cancelled", !event.isCancelled());
  }

  @DisplayName("Fortune result is ignored if less than one.")
  @ParameterizedTest
  @ValueSource(ints = { -1, 0 })
  void testNegativeFortuneApplication(int value) {
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    FurnaceListener.applyFortune(event, () -> value);
    assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
  }

  @DisplayName("Fortune result is included if there is space.")
  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
  void testPositiveFortuneApplication(int value) {
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    FurnaceListener.applyFortune(event, () -> value);
    assertThat("Result must be similar", event.getResult(), isSimilar(recipe.getResult()));
    assertThat("Result must be modified", event.getResult(), is(not(recipe.getResult())));

    ItemStack result = recipe.getResult();
    result.setAmount(result.getType().getMaxStackSize());
    event = new FurnaceSmeltEvent(block, recipe.getInput(), result);
    FurnaceListener.applyFortune(event, () -> value);
    assertThat("Full result must not be modified", event.getResult(), is(result));
  }

  @DisplayName("Silk touch furnaces do not cancel FurnaceSmeltEvents.")
  @Test
  void testSilkFurnaceSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
    var enchantableBlock = newBlock();
    assertThat("Block must be valid", enchantableBlock, is(notNullValue()));
    ItemStack input = recipe.getInput();
    input.setAmount(2);
    var event = new FurnaceSmeltEvent(block, input, recipe.getResult());
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Event is never cancelled", !event.isCancelled());
  }

  @DisplayName("Silk touch furnaces pause during tick completion.")
  @Test
  void testPauseSilkFurnaceSmelt() {
    tile.setBurnTime(BURN_TIME);
    itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
    var enchantableBlock = newBlock();
    assertThat("Block must be valid", enchantableBlock, is(notNullValue()));
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertDoesNotThrow(() -> MockBukkit.getMock().getScheduler().performOneTick());
    assertThat("Event is never cancelled", !event.isCancelled());
  }

  @DisplayName("Non-furnace clicks are ignored.")
  @Test
  void onInventoryClick() {
    // Work around Mockbukkit issue - base InventoryView returns null improperly for getTopInventory
    player.openInventory(new ChestInventoryMock(null, 9));
    var event = new InventoryClickEvent(player.getOpenInventory(), SlotType.CONTAINER, 0,
        ClickType.LEFT, InventoryAction.PICKUP_ALL);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    player.closeInventory();
  }

  @DisplayName("Furnace clicks are handled appropriately.")
  @Test
  void onFurnaceInventoryClick() {
    player.openInventory(tile.getInventory());
    var event = new InventoryClickEvent(player.getOpenInventory(), SlotType.CONTAINER, 0,
        ClickType.LEFT, InventoryAction.PICKUP_ALL);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    player.closeInventory();
  }

  @DisplayName("Non-furnace item movements are ignored.")
  @Test
  void onInventoryMoveItem() {
    player.closeInventory();
    var event = new InventoryMoveItemEvent(player.getInventory(), new ItemStack(Material.DIRT),
        player.getInventory(), true);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
  }

  @DisplayName("Item movements from a furnace are handled appropriately.")
  @Test
  void onFurnaceInventoryMoveItem() {
    player.openInventory(tile.getInventory());
    var event = new InventoryMoveItemEvent(tile.getInventory(), new ItemStack(Material.DIRT),
        player.getInventory(), false);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    player.closeInventory();
  }

  @DisplayName("Item movements to a furnace are handled appropriately.")
  @Test
  void onFurnaceAcceptInventoryMoveItem() {
    player.closeInventory();
    var event = new InventoryMoveItemEvent(player.getInventory(), new ItemStack(Material.DIRT),
        tile.getInventory(), true);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
  }

  @DisplayName("Non-furnace drags are ignored.")
  @Test
  void onInventoryDrag() {
    // Work around Mockbukkit issue - base InventoryView returns null improperly for getTopInventory
    player.openInventory(new ChestInventoryMock(null, 9));
    var event = new InventoryDragEvent(player.getOpenInventory(), new ItemStack(Material.AIR),
        new ItemStack(Material.DIRT), false, Map.of(0, new ItemStack(Material.DIRT)));
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    player.closeInventory();
  }

  @DisplayName("Furnace drags are handled appropriately.")
  @Test
  void onFurnaceInventoryDrag() {
    player.openInventory(tile.getInventory());
    var event = new InventoryDragEvent(player.getOpenInventory(), new ItemStack(Material.AIR),
        new ItemStack(Material.DIRT), false, Map.of(0, new ItemStack(Material.DIRT)));
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    player.closeInventory();
  }

}