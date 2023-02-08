package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.enchantments.EnchantmentMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.InventoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemMetaHelper;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;

@DisplayName("Feature: Enchant and combine blocks in anvils.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnvilEnchanterTest {

  private static final Enchantment ENCHANTMENT = Enchantment.DIG_SPEED;
  public static final Material MATERIAL = Material.COAL_ORE;

  private AnvilEnchanter enchanter;
  private ItemStack itemStack;
  private ArgumentCaptor<Runnable> runnableCaptor;

  @BeforeAll
  void setUpAll() {
    EnchantmentMocks.init();

    var server = BukkitServer.newServer();

    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);

    Bukkit.setServer(server);
  }

  @BeforeEach
  void setUp() {
    var server = Bukkit.getServer();

    var scheduler = mock(BukkitScheduler.class);
    runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(scheduler.runTask(any(Plugin.class), runnableCaptor.capture())).thenReturn(null);
    when(server.getScheduler()).thenReturn(scheduler);

    var plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    when(plugin.getServer()).thenReturn(server);
    when(plugin.getConfig()).thenReturn(new YamlConfiguration());

    EnchantableBlockRegistry registry = new EnchantableBlockRegistry(plugin);
    var registration = new DummyEnchantableRegistration(
        plugin, Set.of(ENCHANTMENT), Set.of(MATERIAL));
    registry.register(registration);

    enchanter = new AnvilEnchanter(plugin, registry);
    itemStack = new ItemStack(MATERIAL);
  }

  @Test
  void testNullBaseItemsInvalid() {
    assertThat("Items are invalid", enchanter.areItemsInvalid(null, null));
  }

  @Test
  void testNullAdditionItemsInvalid() {
    assertThat("Items are invalid", enchanter.areItemsInvalid(itemStack, null));
  }

  @Test
  void testStackedBaseItemsInvalid() {
    var base = itemStack.clone();
    base.setAmount(64);
    var addition = itemStack.clone();
    assertThat("Items are invalid", enchanter.areItemsInvalid(base, addition));
  }

  @Test
  void testStackedAdditionItemsValid() {
    var base = itemStack.clone();
    var addition = itemStack.clone();
    addition.setAmount(64);
    assertThat("Items are valid", enchanter.areItemsInvalid(base, addition), is(false));
  }

  @Test
  void testItemsValid() {
    var base = itemStack.clone();
    var addition = itemStack.clone();
    assertThat("Items are valid", !enchanter.areItemsInvalid(base, addition));
  }

  private @NotNull Player prepareEventPlayer(boolean hasPermission) {
    var player = mock(Player.class);
    when(player.hasPermission(any(String.class))).thenReturn(hasPermission);
    when(player.setWindowProperty(any(), anyInt())).thenReturn(true);
    var world = WorldMocks.newWorld("world");
    when(player.getWorld()).thenReturn(world);

    var inventory = InventoryMocks.newAnvilMock();
    inventory.setItem(0, itemStack.clone());
    var additionItem = new ItemStack(Material.ENCHANTED_BOOK);
    var additionMeta = additionItem.getItemMeta();
    if (additionMeta instanceof EnchantmentStorageMeta storageMeta) {
      storageMeta.addStoredEnchant(ENCHANTMENT, ENCHANTMENT.getMaxLevel(), true);
    }
    additionItem.setItemMeta(additionMeta);
    inventory.setItem(1, additionItem);

    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(inventory);
    when(view.getPlayer()).thenReturn(player);

    when(player.getOpenInventory()).thenReturn(view);

    return player;
  }

  @Test
  void testInvalidItemPrepareAnvil() {
    var player = prepareEventPlayer(true);
    var view = player.getOpenInventory();
    view.getTopInventory().setItem(0, null);
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    assertThat(
        "Result must be unchanged for invalid items",
        event.getResult(), is(nullValue()));
  }

  @Test
  void testUnregisteredPrepareAnvil() {
    var player = prepareEventPlayer(true);
    var view = player.getOpenInventory();
    view.getTopInventory().setItem(0, new ItemStack(Material.REDSTONE_ORE));
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    assertThat(
        "Result must be unchanged for unregistered items",
        event.getResult(), is(nullValue()));
  }

  @Test
  void testNoPermissionPrepareAnvil() {
    var player = prepareEventPlayer(false);
    var view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertThat(
        "Result must be unchanged for disallowed player",
        event.getResult(), is(nullValue()));
  }

  @Test
  void testChangeBasePrepareAnvil() {
    var player = prepareEventPlayer(true);
    var view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    var successItem = itemStack.clone();
    ItemMetaHelper itemMeta = (ItemMetaHelper) successItem.getItemMeta();
    assertThat("Meta is not null", itemMeta, is(notNullValue()));
    itemMeta.setRepairCost(1);
    itemMeta.addEnchant(ENCHANTMENT, ENCHANTMENT.getMaxLevel(), true);
    successItem.setItemMeta(itemMeta);
    assertThat("Result must be success item", event.getResult(), is(successItem));
    view.getTopInventory().setItem(0, null);
    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    assertThat("Inventory result must not be set", view.getTopInventory().getItem(2), is(nullValue()));
  }

  @Test
  void testChangeAdditionPrepareAnvil() {
    var player = prepareEventPlayer(true);
    var view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    var successItem = itemStack.clone();
    ItemMetaHelper itemMeta = (ItemMetaHelper) successItem.getItemMeta();
    assertThat("Meta is not null", itemMeta, is(notNullValue()));
    itemMeta.setRepairCost(1);
    itemMeta.addEnchant(ENCHANTMENT, ENCHANTMENT.getMaxLevel(), true);
    successItem.setItemMeta(itemMeta);
    assertThat("Result must be success item", event.getResult(), is(successItem));
    view.getTopInventory().setItem(1, null);
    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    assertThat("Inventory result must not be set", view.getTopInventory().getItem(2), is(nullValue()));
  }

  @Test
  void testSuccessPrepareAnvil() {
    var player = prepareEventPlayer(true);
    var view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    var successItem = itemStack.clone();
    ItemMetaHelper itemMeta = (ItemMetaHelper) successItem.getItemMeta();
    assertThat("Meta is not null", itemMeta, is(notNullValue()));
    itemMeta.setRepairCost(1);
    itemMeta.addEnchant(ENCHANTMENT, ENCHANTMENT.getMaxLevel(), true);
    successItem.setItemMeta(itemMeta);
    assertThat("Result must be success item", event.getResult(), is(successItem));
    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    assertThat(
        "Inventory result must be success item",
        view.getTopInventory().getItem(2), is(successItem));
  }

}