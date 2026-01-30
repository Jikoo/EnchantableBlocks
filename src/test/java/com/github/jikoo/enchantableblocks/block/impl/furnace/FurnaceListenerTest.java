package com.github.jikoo.enchantableblocks.block.impl.furnace;

import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.InventoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.BlockMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.planarwrappers.util.StringConverters;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;

import static com.github.jikoo.enchantableblocks.mock.matcher.ItemMatcher.isSimilar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Feature: Event handlers for enchantable furnaces.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FurnaceListenerTest {

  private CookingRecipe<?> recipe;

  @BeforeAll
  void beforeAll() {
    var server = ServerMocks.mockServer();

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
    var scheduler = mock(BukkitScheduler.class);
    when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
      invocation.getArgument(1, Runnable.class).run();
      return null;
    });
    when(server.getScheduler()).thenReturn(scheduler);
  }

  @DisplayName("Furnace-specific events")
  @Nested
  class FurnaceEventsTest {
    private static final short BURN_TIME = 1600;
    private EnchantableFurnace enchantableFurnace;
    private EnchantableBlockManager manager;
    private FurnaceListener listener;
    private Block block;

    @BeforeAll
    static void beforeAll() {
    }

    @BeforeEach
    void beforeEach() {
      var server = Bukkit.getServer();

      var plugin = mock(Plugin.class);
      when(plugin.getServer()).thenReturn(server);
      when(plugin.getName()).thenReturn(getClass().getSimpleName());
      manager = mock(EnchantableBlockManager.class);
      listener = new FurnaceListener(plugin, manager);

      // Set up world and block
      var world = WorldMocks.newWorld("world");
      block = world.getBlockAt(0, 0, 0);
      BlockMocks.mockType(block);
      block.setType(Material.FURNACE);

      // Set up state and inventory
      var tile = mock(Furnace.class);
      when(block.getState()).thenReturn(tile);
      when(tile.getBlock()).thenReturn(block);
      when(tile.getWorld()).thenReturn(world);
      FurnaceInventory inventory = InventoryMocks.newFurnaceMock();
      when(tile.getInventory()).thenReturn(inventory);

      // Set up manager and registry
      var registry = mock(EnchantableBlockRegistry.class);
      when(manager.getRegistry()).thenReturn(registry);
      EnchantableFurnaceRegistration registration = mock(EnchantableFurnaceRegistration.class);
      when(registry.get(any(Material.class))).thenReturn(registration);
      when(registration.getFurnaceRecipe(any())).thenAnswer(invocation -> recipe);

      // Set up enchantable block
      enchantableFurnace = mock(EnchantableFurnace.class);
      var itemStack = new ItemStack(Material.FURNACE);
      when(enchantableFurnace.getItemStack()).thenReturn(itemStack);
      when(enchantableFurnace.getFurnaceTile()).thenReturn(tile);

      when(manager.getBlock(block)).thenReturn(enchantableFurnace);

      // Add a player for events
      var player = mock(Player.class);
      var viewMock = mock(InventoryView.class);
      when(player.getOpenInventory()).thenReturn(viewMock);

      var playerInventory = InventoryMocks.newMock(PlayerInventory.class, InventoryType.PLAYER, 41);
      ItemStack air = new ItemStack(Material.AIR);
      when(playerInventory.getItemInMainHand()).thenReturn(air);
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
    void testFurnaceBurnInvalidFurnace() {
      when(manager.getBlock(block)).thenReturn(null);
      var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
      assertDoesNotThrow(() -> listener.onFurnaceBurn(event));
      assertThat("Invalid block must be ignored", !event.isCancelled());
      assertThat("Burn time must be unmodified", event.getBurnTime(), is((int) BURN_TIME));
    }

    @DisplayName("Paused furnaces cancel FurnaceBurnEvents and force resume.")
    @Test
    void testFurnaceBurnPausedFurnace() {
      when(enchantableFurnace.canPause()).thenReturn(true);
      doReturn(true).when(enchantableFurnace).resume(anyBoolean());
      var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
      assertDoesNotThrow(() -> listener.onFurnaceBurn(event));
      verify(enchantableFurnace).resume(false);
      assertThat("Forcibly resuming must cancel event", event.isCancelled());
    }

    @DisplayName("Unbreaking furnaces modify fuel life.")
    @Test
    void testFurnaceBurnUnbreaking() {
      when(enchantableFurnace.getBurnModifier()).thenReturn(10);
      var event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), BURN_TIME);
      assertDoesNotThrow(() -> listener.onFurnaceBurn(event));
      assertThat("Non-resume event is not cancelled", !event.isCancelled());
      assertThat("Burn time must be modified", event.getBurnTime(), is(not(BURN_TIME)));
    }

    @DisplayName("Invalid furnaces do not modify FurnaceStartSmeltEvents.")
    @Test
    void testFurnaceStartSmeltInvalid() {
      when(manager.getBlock(block)).thenReturn(null);
      var event = new FurnaceStartSmeltEvent(block, recipe.getInput(), recipe);
      assertDoesNotThrow(() -> listener.onFurnaceStartSmelt(event));
      assertThat(
          "Cook time must not be modified",
          event.getTotalCookTime(),
          is(recipe.getCookingTime()));
    }

    @DisplayName("Furnaces apply cook modifier.")
    @Test
    void testFurnaceStartSmeltModifier() {
      when(enchantableFurnace.applyCookTimeModifiers(anyDouble())).thenAnswer(invocation -> (short) (invocation.getArgument(0, Double.class) + 10));
      var event = new FurnaceStartSmeltEvent(block, recipe.getInput(), recipe);
      assertDoesNotThrow(() -> listener.onFurnaceStartSmelt(event));
      assertThat(
          "Cook time must be modified",
          event.getTotalCookTime(),
          is(recipe.getCookingTime() + 10));
    }

    @DisplayName("Invalid furnaces do not modify FurnaceSmeltEvents.")
    @Test
    void testFurnaceSmeltInvalid() {
      when(manager.getBlock(block)).thenReturn(null);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
      assertThat("Event is never cancelled", !event.isCancelled());
      assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
      verify(enchantableFurnace, times(0)).getFortune();
    }

    @DisplayName("Invalid furnace tiles do not modify FurnaceSmeltEvents.")
    @Test
    void testFurnaceSmeltInvalidTile() {
      when(enchantableFurnace.getFurnaceTile()).thenReturn(null);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
      assertThat("Event is never cancelled", !event.isCancelled());
      assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
      verify(enchantableFurnace, times(0)).getFortune();
    }

    @DisplayName("Efficiency and unbreaking furnaces do not modify FurnaceSmeltEvents.")
    @Test
    void testFurnaceSmeltUnbreakingEfficiency() {
      when(enchantableFurnace.getCookModifier()).thenReturn(10);
      when(enchantableFurnace.getBurnModifier()).thenReturn(10);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
      assertThat("Event is never cancelled", !event.isCancelled());
      assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
    }

    @DisplayName("Fortune result is not calculated if there is no space.")
    @Test
    void testApplyFortuneFull() {
      var result = recipe.getResult();
      result.setAmount(result.getType().getMaxStackSize());
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), result);
      var supplier = mock(IntSupplier.class);
      listener.applyFortune(event, supplier);
      verify(supplier, times(0)).getAsInt();
      assertThat("Full result must not be modified", event.getResult(), is(result));
    }

    @DisplayName("Fortune result is ignored if less than one.")
    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void testApplyFortuneBelowOne(int value) {
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      listener.applyFortune(event, () -> value);
      assertThat("Result must not be modified", event.getResult(), is(recipe.getResult()));
    }

    @DisplayName("Fortune result is included if there is space.")
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void testApplyFortunePositive(int value) {
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      listener.applyFortune(event, () -> value);
      assertThat("Result must be similar", event.getResult(), isSimilar(recipe.getResult()));
      assertThat("Result must be modified", event.getResult(), is(not(recipe.getResult())));
      assertThat("Result amount must be increased as expected", event.getResult().getAmount(),
          is(value + 1));
    }

    @DisplayName("Fortune result does not exceed maximum stack size.")
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void testApplyFortuneReducedSpace(int value) {
      // Set up event for stack with 1 free slot.
      var result = recipe.getResult();
      result.setAmount(result.getType().getMaxStackSize() - 1);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), result);

      listener.applyFortune(event, () -> value);

      // Expect max stack.
      var expected = recipe.getResult();
      expected.setAmount(result.getType().getMaxStackSize());
      assertThat("Result must be full", event.getResult(), is(expected));
    }

    @DisplayName("Furnace smelt applies fortune if not in blacklist.")
    @Test
    void testFurnaceSmeltFortuneNotBlacklist() {
      var config = new EnchantableFurnaceConfig(new YamlConfiguration());
      when(enchantableFurnace.getConfig()).thenReturn(config);
      when(enchantableFurnace.getFortune()).thenReturn(10);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      listener = spy(listener);

      listener.onFurnaceSmelt(event);

      verify(listener).applyFortune(any(), any());
      assertThat("Event is never cancelled", !event.isCancelled());
    }

    @DisplayName("Furnace smelt does not apply fortune if in blacklist.")
    @Test
    void testFurnaceSmeltFortuneBlacklist() {
      var yaml = new YamlConfiguration();
      yaml.set("fortuneList", List.of(recipe.getInput().getType().name()));
      var config = new EnchantableFurnaceConfig(yaml);
      when(enchantableFurnace.getConfig()).thenReturn(config);
      when(enchantableFurnace.getFortune()).thenReturn(10);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      listener = spy(listener);

      listener.onFurnaceSmelt(event);

      verify(listener, times(0)).applyFortune(any(), any());
      assertThat("Event is never cancelled", !event.isCancelled());
    }

    @DisplayName("Furnace smelt applies fortune if in whitelist.")
    @Test
    void testFurnaceSmeltFortuneWhitelisted() {
      var yaml = new YamlConfiguration();
      yaml.set("fortuneListIsBlacklist", false);
      yaml.set("fortuneList", List.of(recipe.getInput().getType().name()));
      var config = new EnchantableFurnaceConfig(yaml);
      when(enchantableFurnace.getConfig()).thenReturn(config);
      when(enchantableFurnace.getFortune()).thenReturn(10);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      listener = spy(listener);

      listener.onFurnaceSmelt(event);

      verify(listener).applyFortune(any(), any());
      assertThat("Event is never cancelled", !event.isCancelled());
    }

    @DisplayName("Furnace smelt does not apply fortune if not in whitelist.")
    @Test
    void testFurnaceSmeltNotWhitelistedFortune() {
      var yaml = new YamlConfiguration();
      yaml.set("fortuneListIsBlacklist", false);
      var config = new EnchantableFurnaceConfig(yaml);
      when(enchantableFurnace.getConfig()).thenReturn(config);
      when(enchantableFurnace.getFortune()).thenReturn(10);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      listener = spy(listener);

      listener.onFurnaceSmelt(event);

      verify(listener, times(0)).applyFortune(any(), any());
      assertThat("Event is never cancelled", !event.isCancelled());
    }

    @DisplayName("Furnaces that cannot pause do not attempt to.")
    @Test
    void testFurnaceSmeltNoPause() {
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
      verify(enchantableFurnace, times(0)).shouldPause(any());
      assertThat("Event is never cancelled", !event.isCancelled());
    }

    @DisplayName("Furnaces that can pause attempt to.")
    @Test
    void testFurnaceSmeltTryPause() {
      when(enchantableFurnace.canPause()).thenReturn(true);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
      verify(enchantableFurnace).shouldPause(any());
      verify(enchantableFurnace, times(0)).pause();
      assertThat("Event is never cancelled", !event.isCancelled());
    }

    @DisplayName("Furnaces that should pause do pause.")
    @Test
    void testFurnaceSmeltDoPause() {
      when(enchantableFurnace.canPause()).thenReturn(true);
      when(enchantableFurnace.shouldPause(any())).thenReturn(true);
      var event = new FurnaceSmeltEvent(block, recipe.getInput(), recipe.getResult());
      assertDoesNotThrow(() -> listener.onFurnaceSmelt(event));
      verify(enchantableFurnace).pause();
      assertThat("Event is never cancelled", !event.isCancelled());
    }

  }

  @Nested
  class ClickEventsTest {

    private FurnaceListener listener;

    @BeforeEach
    void beforeEach() {
      var plugin = mock(Plugin.class);
      var manager = mock(EnchantableBlockManager.class);
      listener = new FurnaceListener(plugin, manager);
    }

    @DisplayName("Clicks not affecting a furnace are ignored.")
    @Test
    void testInventoryClickNonFurnace() {
      var view = mock(InventoryView.class);
      var inventory = mock(Inventory.class);
      when(view.getTopInventory()).thenReturn(inventory);
      var event = new InventoryClickEvent(view, SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
      assertDoesNotThrow(() -> listener.onInventoryClick(event));
      verify(inventory, times(0)).getHolder();
    }

    @DisplayName("Clicks affecting a furnace cause updates.")
    @Test
    void testInventoryClickFurnace() {
      var view = mock(InventoryView.class);
      var inventory = mock(FurnaceInventory.class);
      when(view.getTopInventory()).thenReturn(inventory);
      var event = new InventoryClickEvent(view, SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
      assertDoesNotThrow(() -> listener.onInventoryClick(event));
      verify(inventory).getHolder();
    }

    @DisplayName("Item movements not involving a furnace are ignored.")
    @Test
    void testInventoryMoveItemNonFurnace() {
      var inventory = mock(Inventory.class);
      var event = new InventoryMoveItemEvent(inventory, new ItemStack(Material.DIRT), inventory, true);
      assertDoesNotThrow(() -> listener.onInventoryMoveItem(event));
      verify(inventory, times(0)).getHolder();
    }

    @DisplayName("Item movements from a furnace cause updates.")
    @Test
    void testInventoryMoveItemFromFurnace() {
      var fromInv = mock(FurnaceInventory.class);
      var toInv = mock(Inventory.class);
      var event = new InventoryMoveItemEvent(fromInv, new ItemStack(Material.DIRT), toInv, true);
      assertDoesNotThrow(() -> listener.onInventoryMoveItem(event));
      verify(fromInv).getHolder();
    }

    @DisplayName("Item movements to a furnace cause updates.")
    @Test
    void testInventoryMoveItemToFurnace() {
      var fromInv = mock(Inventory.class);
      var toInv = mock(FurnaceInventory.class);
      var event = new InventoryMoveItemEvent(fromInv, new ItemStack(Material.DIRT), toInv, true);
      assertDoesNotThrow(() -> listener.onInventoryMoveItem(event));
      verify(toInv).getHolder();
    }

    @DisplayName("Drags not affecting a furnace are ignored.")
    @Test
    void testInventoryDragNonFurnace() {
      var view = mock(InventoryView.class);
      var inventory = mock(Inventory.class);
      when(view.getTopInventory()).thenReturn(inventory);
      var event = new InventoryDragEvent(view, new ItemStack(Material.AIR), new ItemStack(Material.DIRT), false, Map.of(0, new ItemStack(Material.DIRT)));
      assertDoesNotThrow(() -> listener.onInventoryDrag(event));
      verify(inventory, times(0)).getHolder();
    }

    @DisplayName("Drags affecting furnaces cause updates.")
    @Test
    void testInventoryDragFurnace() {
      var view = mock(InventoryView.class);
      var inventory = mock(FurnaceInventory.class);
      when(view.getTopInventory()).thenReturn(inventory);
      var event = new InventoryDragEvent(view, new ItemStack(Material.AIR), new ItemStack(Material.DIRT), false, Map.of(0, new ItemStack(Material.DIRT)));
      assertDoesNotThrow(() -> listener.onInventoryDrag(event));
      verify(inventory).getHolder();
    }

  }

}