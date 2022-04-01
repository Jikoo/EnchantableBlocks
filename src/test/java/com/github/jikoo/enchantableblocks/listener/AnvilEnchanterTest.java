package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantmentHelper;
import com.github.jikoo.enchantableblocks.util.mock.AnvilInventoryMock;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Enchant and combine blocks in anvils.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnvilEnchanterTest {

  private static final Enchantment ENCHANTMENT = Enchantment.DIG_SPEED;
  public static final Material MATERIAL = Material.COAL_ORE;

  private ServerMock server;
  private Plugin plugin;
  private EnchantableBlockRegistry registry;
  private Player player;
  private AnvilEnchanter enchanter;
  private ItemStack itemStack;

  @BeforeAll
  void setUpAll() {
    server = MockBukkit.mock();
    EnchantmentHelper.setupToolEnchants();
    plugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    registry = new EnchantableBlockRegistry(plugin);
    var registration = new DummyEnchantableRegistration(
        plugin, Set.of(ENCHANTMENT), Set.of(MATERIAL));
    registry.register(registration);
  }

  @BeforeEach
  void setUp() {
    enchanter = new AnvilEnchanter(plugin, registry);
    itemStack = new ItemStack(MATERIAL);
  }

  @AfterAll
  void tearDownAll() {
    plugin.getServer().getScheduler().cancelTasks(plugin);
    MockBukkit.unmock();
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
  void testStackedAdditionItemsInvalid() {
    var base = itemStack.clone();
    var addition = itemStack.clone();
    addition.setAmount(64);
    assertThat("Items are invalid", enchanter.areItemsInvalid(base, addition));
  }

  @Test
  void testItemsValid() {
    var base = itemStack.clone();
    var addition = itemStack.clone();
    assertThat("Items are valid", !enchanter.areItemsInvalid(base, addition));
  }

  private void prepareEvent(boolean hasPermission) {
    player = new PlayerMock(server, "sample text") {
      @Override
      public boolean hasPermission(String name) {
        return hasPermission;
      }

      @Override
      public boolean setWindowProperty(@NotNull InventoryView.Property prop, int value) {
        // Sends packet to client, ignore.
        return true;
      }
    };
    var inventory = new AnvilInventoryMock(player);
    inventory.setItem(0, itemStack.clone());
    var additionItem = new ItemStack(Material.ENCHANTED_BOOK);
    var additionMeta = additionItem.getItemMeta();
    if (additionMeta instanceof EnchantmentStorageMeta storageMeta) {
      storageMeta.addStoredEnchant(ENCHANTMENT, ENCHANTMENT.getMaxLevel(), true);
    }
    additionItem.setItemMeta(additionMeta);
    inventory.setItem(1, additionItem);
    player.openInventory(inventory);
  }

  @Test
  void testCreativePrepareAnvil() {
    prepareEvent(true);
    player.setGameMode(GameMode.CREATIVE);
    var view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    assertThat(
        "Result must be unchanged for creative player",
        event.getResult(), is(nullValue()));
  }

  @Test
  void testInvalidItemPrepareAnvil() {
    prepareEvent(true);
    var view = player.getOpenInventory();
    view.setItem(0, null);
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    assertThat(
        "Result must be unchanged for invalid items",
        event.getResult(), is(nullValue()));
  }

  @Test
  void testUnregisteredPrepareAnvil() {
    prepareEvent(true);
    var view = player.getOpenInventory();
    view.setItem(0, new ItemStack(Material.REDSTONE_ORE));
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    assertThat(
        "Result must be unchanged for unregistered items",
        event.getResult(), is(nullValue()));
  }

  @Test
  void testNoPermissionPrepareAnvil() {
    prepareEvent(false);
    var view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertThat(
        "Result must be unchanged for disallowed player",
        event.getResult(), is(nullValue()));
  }

  @Test
  void testChangeBasePrepareAnvil() {
    prepareEvent(true);
    InventoryView view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    var successItem = itemStack.clone();
    successItem.addUnsafeEnchantment(ENCHANTMENT, ENCHANTMENT.getMaxLevel());
    assertThat("Result must be success item", event.getResult(), is(successItem));
    view.setItem(0, null);
    assertDoesNotThrow(() -> server.getScheduler().performOneTick());
    assertThat("Inventory result must not be set", view.getItem(2), is(nullValue()));
  }

  @Test
  void testChangeAdditionPrepareAnvil() {
    prepareEvent(true);
    InventoryView view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    var successItem = itemStack.clone();
    successItem.addUnsafeEnchantment(ENCHANTMENT, ENCHANTMENT.getMaxLevel());
    assertThat("Result must be success item", event.getResult(), is(successItem));
    view.setItem(1, null);
    assertDoesNotThrow(() -> server.getScheduler().performOneTick());
    assertThat("Inventory result must not be set", view.getItem(2), is(nullValue()));
  }

  @Test
  void testSuccessPrepareAnvil() {
    prepareEvent(true);
    InventoryView view = player.getOpenInventory();
    var event = new PrepareAnvilEvent(view, null);
    assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
    var successItem = itemStack.clone();
    successItem.addUnsafeEnchantment(ENCHANTMENT, ENCHANTMENT.getMaxLevel());
    assertThat("Result must be success item", event.getResult(), is(successItem));
    assertDoesNotThrow(() -> server.getScheduler().performOneTick());
    assertThat(
        "Inventory result must be success item",
        view.getTopInventory().getItem(2), is(successItem));
  }

}