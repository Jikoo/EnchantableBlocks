package com.github.jikoo.enchantableblocks.block.impl.furnace;

import static com.github.jikoo.enchantableblocks.mock.matcher.IsSimilarMatcher.isSimilar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.inventory.InventoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.BlockMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.planarwrappers.util.StringConverters;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
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

  private EnchantableFurnace enchantableFurnace;
  private EnchantableBlockManager manager;
  private FurnaceListener listener;
  private CookingRecipe<?> recipe;
  private Player player;
  private Block block;
  private Furnace tile;
  private ItemStack itemStack;

  @BeforeAll
  void setUpAll() {
    Server server = BukkitServer.newServer();
    Bukkit.setServer(server);

    // Set up item factory for ItemMeta generation and comparison.
    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);

    // Add furnace recipe dirt -> coarse dirt
    recipe = new FurnaceRecipe(
        Objects.requireNonNull(StringConverters.toNamespacedKey("sample:text")),
        new ItemStack(Material.COARSE_DIRT), Material.DIRT, 0, 200);
    // Set up recipe iterator
    when(server.recipeIterator()).thenAnswer(invocation -> Set.of((Recipe) recipe).iterator());

    // Set up scheduler to run tasks immediately.
    BukkitScheduler scheduler = mock(BukkitScheduler.class);
    when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
      invocation.getArgument(1, Runnable.class).run();
      return null;
    });
    when(server.getScheduler()).thenReturn(scheduler);
  }

  @BeforeEach
  void setUp() {
    Server server = Bukkit.getServer();

    Plugin plugin = mock(Plugin.class);
    when(plugin.getServer()).thenReturn(server);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    manager = mock(EnchantableBlockManager.class);
    listener = new FurnaceListener(plugin, manager);

    // Set up world and block
    World world = WorldMocks.newWorld("world");
    block = world.getBlockAt(0, 0, 0);
    BlockMocks.mockType(block);
    block.setType(Material.FURNACE);

    // Set up state and inventory
    tile = mock(Furnace.class);
    when(block.getState()).thenReturn(tile);
    when(tile.getBlock()).thenReturn(block);
    when(tile.getWorld()).thenReturn(world);
    FurnaceInventory inventory = InventoryMocks.newFurnaceMock();
    when(tile.getInventory()).thenReturn(inventory);

    // Set up manager and registry
    EnchantableBlockRegistry registry = mock(EnchantableBlockRegistry.class);
    when(manager.getRegistry()).thenReturn(registry);
    EnchantableFurnaceRegistration registration = mock(EnchantableFurnaceRegistration.class);
    when(registry.get(any(Material.class))).thenReturn(registration);
    when(registration.getFurnaceRecipe(any())).thenAnswer(invocation -> recipe);
    var config = new EnchantableFurnaceConfig(new YamlConfiguration());
    when(registration.getConfig()).thenReturn(config);
    itemStack = new ItemStack(Material.FURNACE);
    enchantableFurnace = spy(new EnchantableFurnace(registration, block, itemStack, new YamlConfiguration()));
    // Input item is cloned during creation.
    itemStack = enchantableFurnace.getItemStack();
    when(manager.getBlock(block)).thenReturn(enchantableFurnace);

    // Add a player for events
    player = mock(Player.class);
    InventoryView viewMock = mock(InventoryView.class);
    when(player.getOpenInventory()).thenReturn(viewMock);

    PlayerInventory playerInventory = InventoryMocks.newMock(PlayerInventory.class, InventoryType.PLAYER, 41);
    when(playerInventory.getItemInMainHand()).thenReturn(new ItemStack(Material.AIR));
    when(viewMock.getTopInventory()).thenReturn(playerInventory);

    // Set up open/close for modifications.
    when(player.openInventory(any(Inventory.class))).thenAnswer(invocation -> {
      when(viewMock.getTopInventory()).thenReturn(playerInventory);
      return viewMock;
    });
    doAnswer(invocation -> {
      when(viewMock.getTopInventory()).thenReturn(playerInventory);
      return null;
    }).when(player).closeInventory();
  }

  @DisplayName("Invalid furnaces do not modify FurnaceBurnEvents.")
  @Test
  void testInvalidFurnaceConsumeFuel() {
    when(manager.getBlock(block)).thenReturn(null);
    var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
    assertDoesNotThrow(() -> listener.onFurnaceBurn(event));
    assertThat("Invalid block must be ignored", !event.isCancelled());
    assertThat("Burn time must be unmodified", event.getBurnTime(), is((int) BURN_TIME));
  }

  @DisplayName("Paused furnaces cancel FurnaceBurnEvents and resume.")
  @Test
  void testPausedFurnaceConsumeFuel() {
    when(enchantableFurnace.canPause()).thenReturn(true);
    enchantableFurnace.setFrozenTicks((short) 200);
    var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
    assertDoesNotThrow(() -> listener.onFurnaceBurn(event));
    assertThat("Resume must cancel event", event.isCancelled());
  }

  @DisplayName("Unbreaking furnaces modify fuel life.")
  @Test
  void testFurnaceConsumeFuel() {
    itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
    var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
    assertDoesNotThrow(() -> listener.onFurnaceBurn(event));
    assertThat("Non-resume event is not cancelled", !event.isCancelled());
    assertThat("Burn time must be modified", event.getBurnTime(), is(not(BURN_TIME)));
  }

  @DisplayName("Invalid furnaces do not modify FurnaceStartSmeltEvents.")
  @Test
  void testInvalidFurnaceStartSmelt() {
    var event = new FurnaceStartSmeltEvent(block, recipe.getInput(), recipe);
    assertDoesNotThrow(() -> listener.onFurnaceStartSmelt(event));
    assertThat(
        "Cook time must not be modified",
        event.getTotalCookTime(),
        is(recipe.getCookingTime()));
  }

  @DisplayName("Efficiency furnaces modify total cook time.")
  @Test
  void testValidFurnaceStartSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var event = new FurnaceStartSmeltEvent(block, recipe.getInput(), recipe);
    assertDoesNotThrow(() -> listener.onFurnaceStartSmelt(event));
    assertThat(
        "Cook time must be modified",
        event.getTotalCookTime(),
        is(not(recipe.getCookingTime())));
  }

  @DisplayName("Invalid furnaces do not modify FurnaceSmeltEvents.")
  @Test
  void testInvalidFurnaceSmelt() {
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
    assertThat("Event is never cancelled", !event.isCancelled());
    assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
  }

  @DisplayName("Invalid furnace tiles do not modify FurnaceSmeltEvents.")
  @Test
  void testInvalidTileFurnaceSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    block.setType(Material.DIRT);
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
    assertThat("Event is never cancelled", !event.isCancelled());
    assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
  }

  @DisplayName("Efficiency and unbreaking furnaces do not modify FurnaceSmeltEvents.")
  @Test
  void testBoringFurnaceSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
    assertThat("Event is never cancelled", !event.isCancelled());
    assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
  }

  @DisplayName("Fortune furnaces do not cancel FurnaceSmeltEvents.")
  @Test
  void testFortuneFurnaceSmelt() {
    itemStack.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 10);
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
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
    when(enchantableFurnace.canPause()).thenReturn(true);
    itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
    ItemStack input = recipe.getInput();
    input.setAmount(2);
    var event = new FurnaceSmeltEvent(block, input, recipe.getResult());
    assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
    assertThat("Event is never cancelled", !event.isCancelled());
  }

  @DisplayName("Silk touch furnaces pause during tick completion.")
  @Test
  void testPauseSilkFurnaceSmelt() {
    when(enchantableFurnace.canPause()).thenReturn(true);
    when(enchantableFurnace.shouldPause(any())).thenReturn(true);
    tile.setBurnTime(BURN_TIME);
    var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
    assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
    assertThat("Event is never cancelled", !event.isCancelled());
    verify(enchantableFurnace).pause();
  }

  @DisplayName("Non-furnace clicks are ignored.")
  @Test
  void onInventoryClick() {
    var event = new InventoryClickEvent(player.getOpenInventory(), SlotType.CONTAINER, 0,
        ClickType.LEFT, InventoryAction.PICKUP_ALL);
    assertDoesNotThrow(() -> listener.onInventoryClick(event));
  }

  @DisplayName("Furnace clicks are handled appropriately.")
  @Test
  void onFurnaceInventoryClick() {
    player.openInventory(tile.getInventory());
    var event = new InventoryClickEvent(player.getOpenInventory(), SlotType.CONTAINER, 0,
        ClickType.LEFT, InventoryAction.PICKUP_ALL);
    assertDoesNotThrow(() -> listener.onInventoryClick(event));
    // TODO this should call #verify on #update
  }

  @DisplayName("Non-furnace item movements are ignored.")
  @Test
  void onInventoryMoveItem() {
    var event = new InventoryMoveItemEvent(player.getInventory(), new ItemStack(Material.DIRT),
        player.getInventory(), true);
    assertDoesNotThrow(() -> listener.onInventoryMoveItem(event));
    // TODO this should call #verify on #update
  }

  @DisplayName("Item movements from a furnace are handled appropriately.")
  @Test
  void onFurnaceInventoryMoveItem() {
    player.openInventory(tile.getInventory());
    var event = new InventoryMoveItemEvent(tile.getInventory(), new ItemStack(Material.DIRT),
        player.getInventory(), false);
    assertDoesNotThrow(() -> listener.onInventoryMoveItem(event));
    // TODO this should call #verify on #update
  }

  @DisplayName("Item movements to a furnace are handled appropriately.")
  @Test
  void onFurnaceAcceptInventoryMoveItem() {
    var event = new InventoryMoveItemEvent(player.getInventory(), new ItemStack(Material.DIRT),
        tile.getInventory(), true);
    assertDoesNotThrow(() -> listener.onInventoryMoveItem(event));
    // TODO this should call #verify on #update
  }

  @DisplayName("Non-furnace drags are ignored.")
  @Test
  void onInventoryDrag() {
    var event = new InventoryDragEvent(player.getOpenInventory(), new ItemStack(Material.AIR),
        new ItemStack(Material.DIRT), false, Map.of(0, new ItemStack(Material.DIRT)));
    assertDoesNotThrow(() -> listener.onInventoryDrag(event));
    // TODO this should call #verify on #update
  }

  @DisplayName("Furnace drags are handled appropriately.")
  @Test
  void onFurnaceInventoryDrag() {
    player.openInventory(tile.getInventory());
    var event = new InventoryDragEvent(player.getOpenInventory(), new ItemStack(Material.AIR),
        new ItemStack(Material.DIRT), false, Map.of(0, new ItemStack(Material.DIRT)));
    assertDoesNotThrow(() -> listener.onInventoryDrag(event));
    // TODO this should call #verify on #update
  }

}