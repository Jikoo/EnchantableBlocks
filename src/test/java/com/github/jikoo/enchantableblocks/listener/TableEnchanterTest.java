package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.enchantments.EnchantmentMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Enchant blocks in enchanting tables.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TableEnchanterTest {

  private static final Material ENCHANTABLE_MATERIAL = Material.COAL_ORE;
  private static final Material UNENCHANTABLE_MATERIAL = Material.DIRT;
  private static final Material UNREGISTERED_MATERIAL = Material.ACACIA_BOAT;
  private static final Enchantment VALID_ENCHANT = Enchantment.DIG_SPEED;

  private Plugin plugin;
  private EnchantableBlockRegistry registry;
  private Player player;
  private TableEnchanter listener;
  private ItemStack itemStack;
  private Block block;

  @BeforeAll
  void setUpAll() {
    EnchantmentMocks.init();

    var server = BukkitServer.newServer();
    Bukkit.setServer(server);

    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);
    var scheduler = mock(BukkitScheduler.class);
    when(server.getScheduler()).thenReturn(scheduler);
  }

  @BeforeEach
  void setUp() {
    player = mock(Player.class);
    when(player.hasPermission(any(String.class))).thenReturn(true);
    var world = WorldMocks.newWorld("world");
    when(player.getWorld()).thenReturn(world);
    when(player.getEnchantmentSeed()).thenReturn(0);
    var pdc = mock(PersistentDataContainer.class);
    when(player.getPersistentDataContainer()).thenReturn(pdc);

    plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    when(plugin.getConfig()).thenReturn(new YamlConfiguration());
    registry = new EnchantableBlockRegistry(plugin);

    // Add dummy registrations for valid and invalid materials.
    var registration = new DummyEnchantableRegistration(
        plugin,
        Set.of(VALID_ENCHANT, Enchantment.DURABILITY),
        Set.of(ENCHANTABLE_MATERIAL)
    );
    registry.register(registration);
    registration = new DummyEnchantableRegistration(plugin, Set.of(), Set.of(UNENCHANTABLE_MATERIAL));
    registry.register(registration);

    listener = new TableEnchanter(plugin, registry);

    // Default item
    itemStack = new ItemStack(ENCHANTABLE_MATERIAL);

    block = WorldMocks.newWorld("world").getBlockAt(0, 0, 0);
  }

  @Test
  void testUnregisteredUnableToEnchant() {
    itemStack.setType(UNREGISTERED_MATERIAL);
    assertThat("Unregistered material cannot be enchanted", listener.getTable(player, itemStack), is(nullValue()));
  }

  @Test
  void testNoEnchantsUnableToEnchant() {
    itemStack.setType(UNENCHANTABLE_MATERIAL);
    assertThat("Material with no enchants cannot be enchanted", listener.getTable(player, itemStack), is(nullValue()));
  }

  @Test
  void testMainPermAbleToEnchant() {
    when(player.hasPermission("enchantableblocks.enchant.table")).thenReturn(true);
    assertThat("Player with main permission can enchant", listener.getTable(player, itemStack), is(notNullValue()));
  }

  @Test
  void testSpecificPermAbleToEnchant() {
    when(player.hasPermission("enchantableblocks.enchant.table.dummyenchantableblock")).thenReturn(true);
    assertThat("Player with specific permission can enchant", listener.getTable(player, itemStack), is(notNullValue()));
  }

  @Test
  void testUnregisteredGetOperation() {
    itemStack.setType(UNREGISTERED_MATERIAL);
    assertThat(
        "Unregistered material yields null operation",
        listener.getTable(player, itemStack),
        is(nullValue()));
  }

  @Test
  void testNoEnchantsGetOperation() {
    itemStack.setType(UNENCHANTABLE_MATERIAL);
    assertThat(
        "Unenchantable material yields null operation",
        listener.getTable(player, itemStack),
        is(nullValue()));
  }

  @Test
  void testGetOperation() {
    assertThat(
        "Enchantable material yields operation",
        listener.getTable(player, itemStack),
        is(notNullValue()));
  }

  @Test
  void testUnablePrepareItemEnchant() {
    itemStack.setType(UNREGISTERED_MATERIAL);
    var event = new PrepareItemEnchantEvent(
        player, player.getOpenInventory(), block,
        itemStack, new EnchantmentOffer[3], 0);
    assertDoesNotThrow(() -> listener.onPrepareItemEnchant(event));
    assertThat(
        "Offers must be unset",
        Arrays.asList(event.getOffers()),
        everyItem(is(nullValue())));
  }

  @Test
  void testNoEnchantsPrepareItemEnchant() {
    plugin.getConfig().set("blocks.DummyEnchantableBlock.tableDisabledEnchantments",
        Arrays.asList("efficiency", "unbreaking"));
    registry.reload();

    var event = new PrepareItemEnchantEvent(
        player, player.getOpenInventory(), block,
        itemStack, new EnchantmentOffer[3], 0);
    assertDoesNotThrow(() -> listener.onPrepareItemEnchant(event));
    assertThat(
        "Offers must be unset",
        Arrays.asList(event.getOffers()),
        everyItem(is(nullValue())));
  }

  @Test
  void testPrepareItemEnchant() {
    var event = new PrepareItemEnchantEvent(
        player, player.getOpenInventory(), block,
        itemStack, new EnchantmentOffer[3], 30);
    assertDoesNotThrow(() -> listener.onPrepareItemEnchant(event));
    assertThat(
        "Offers must be set",
        Arrays.asList(event.getOffers()),
        hasItem(notNullValue()));
  }

  @Test
  void testNoEnchantsEnchantItem() {
    plugin.getConfig().set("blocks.DummyEnchantableBlock.tableDisabledEnchantments",
        Arrays.asList("efficiency", "unbreaking"));
    registry.reload();

    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), block,
        itemStack, 30, new HashMap<>(), 0);
    assertDoesNotThrow(() -> listener.onEnchantItem(event));
    assertThat("Enchantments must be empty", event.getEnchantsToAdd(), is(anEmptyMap()));
  }

  @Test
  void testEnchantItem() {
    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), block,
        itemStack, 30, new HashMap<>(), 2);
    assertDoesNotThrow(() -> listener.onEnchantItem(event));
    assertThat("Enchantments must not be empty", event.getEnchantsToAdd(), is(aMapWithSize(greaterThan(0))));
  }

  @Test
  void testEnchantedUnableToEnchant() {
    itemStack.addUnsafeEnchantment(VALID_ENCHANT, 10);
    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), block,
        itemStack, 30, new HashMap<>(), 2);
    assertDoesNotThrow(() -> listener.onEnchantItem(event));
    assertThat("Enchanted item cannot be enchanted", event.getEnchantsToAdd(), is(anEmptyMap()));
  }

  @Test
  void testStackUnableToEnchant() {
    itemStack.setAmount(2);
    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), block,
        itemStack, 30, new HashMap<>(), 2);
    assertDoesNotThrow(() -> listener.onEnchantItem(event));
    assertThat("Stacked item cannot be enchanted", event.getEnchantsToAdd(), is(anEmptyMap()));
  }

}