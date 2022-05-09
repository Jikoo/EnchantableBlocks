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

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantmentHelper;
import com.github.jikoo.planarwrappers.util.StringConverters;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

  private ServerMock server;
  private Plugin plugin;
  private EnchantableBlockRegistry registry;
  private Player player;
  private TableEnchanter listener;
  private ItemStack itemStack;

  @BeforeAll
  void setUpAll() {
    server = MockBukkit.mock();
    server.addSimpleWorld("world");
    EnchantmentHelper.setupToolEnchants();
  }

  @BeforeEach
  void setUp() {
    player = new PlayerMock(server, "sampletext") {
      @Override
      public boolean hasPermission(String name) {
        return true;
      }
    };
    plugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    registry = new EnchantableBlockRegistry(plugin);

    // Add dummy registrations for valid and invalid materials.
    var registration = new DummyEnchantableRegistration(
        plugin,
        Set.of(VALID_ENCHANT, Enchantment.DURABILITY),
        EnumSet.of(ENCHANTABLE_MATERIAL)
    );
    registry.register(registration);
    registration = new DummyEnchantableRegistration(plugin, Set.of(), EnumSet.of(
        UNENCHANTABLE_MATERIAL));
    registry.register(registration);

    listener = new TableEnchanter(plugin, registry);

    server.getPluginManager().registerEvents(listener, plugin);

    // Default item
    itemStack = new ItemStack(ENCHANTABLE_MATERIAL);
  }

  @AfterEach
  void afterEach() {
    server.getPluginManager().clearPlugins();
  }

  @AfterAll
  void tearDownAll() {
    MockBukkit.unmock();
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
  void testMainPermUnableToEnchant() {
    player = new PlayerMock(server, "sampletext") {
      @Override
      public boolean hasPermission(String name) {
        return "enchantableblocks.enchant.table".equals(name);
      }
    };
    assertThat("Player with main permission can enchant", listener.getTable(player, itemStack), is(notNullValue()));
  }

  @Test
  void testSpecificPermUnableToEnchant() {
    player = new PlayerMock(server, "sampletext") {
      @Override
      public boolean hasPermission(String name) {
        return "enchantableblocks.enchant.table.dummyenchantableblock".equals(name);
      }
    };
    assertThat("Player with main permission can enchant", listener.getTable(player, itemStack), is(notNullValue()));
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
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, new EnchantmentOffer[3], 0);
    assertDoesNotThrow(() -> plugin.getServer().getPluginManager().callEvent(event));
  }

  @Test
  void testNoEnchantsPrepareItemEnchant() {
    plugin.getConfig().set("blocks.DummyEnchantableBlock.tableDisabledEnchantments",
        Arrays.asList("efficiency", "unbreaking"));
    registry.reload();

    var event = new PrepareItemEnchantEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, new EnchantmentOffer[3], 0);
    assertDoesNotThrow(() -> plugin.getServer().getPluginManager().callEvent(event));
    assertThat(
        "Offers must be unset",
        Arrays.asList(event.getOffers()),
        everyItem(is(nullValue())));
  }

  @Test
  void testPrepareItemEnchant() {
    var event = new PrepareItemEnchantEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, new EnchantmentOffer[3], 30);
    player.getPersistentDataContainer().set(
        Objects.requireNonNull(StringConverters.toNamespacedKey("enchantableblocks:enchanting_table_seed")),
        PersistentDataType.LONG, 0L
    );
    assertDoesNotThrow(() -> plugin.getServer().getPluginManager().callEvent(event));
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
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, 30, new HashMap<>(), 0);
    assertDoesNotThrow(() -> plugin.getServer().getPluginManager().callEvent(event));
    assertThat("Enchantments must be empty", event.getEnchantsToAdd(), is(anEmptyMap()));
  }

  @Test
  void testEnchantItem() {
    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, 30, new HashMap<>(), 2);
    player.getPersistentDataContainer().set(
        Objects.requireNonNull(StringConverters.toNamespacedKey("enchantableblocks:enchanting_table_seed")),
        PersistentDataType.LONG, 0L
    );
    assertDoesNotThrow(() -> plugin.getServer().getPluginManager().callEvent(event));
    assertThat("Enchantments must not be empty", event.getEnchantsToAdd(), is(aMapWithSize(greaterThan(0))));
  }

  @Test
  void testEnchantedUnableToEnchant() {
    itemStack.addUnsafeEnchantment(VALID_ENCHANT, 10);
    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, 30, new HashMap<>(), 2);
    player.getPersistentDataContainer().set(
        Objects.requireNonNull(StringConverters.toNamespacedKey("enchantableblocks:enchanting_table_seed")),
        PersistentDataType.LONG, 0L
    );
    assertDoesNotThrow(() -> plugin.getServer().getPluginManager().callEvent(event));
    assertThat("Enchanted item cannot be enchanted", event.getEnchantsToAdd(), is(anEmptyMap()));
  }

  @Test
  void testStackUnableToEnchant() {
    itemStack.setAmount(2);
    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, 30, new HashMap<>(), 2);
    player.getPersistentDataContainer().set(
        Objects.requireNonNull(StringConverters.toNamespacedKey("enchantableblocks:enchanting_table_seed")),
        PersistentDataType.LONG, 0L
    );
    assertDoesNotThrow(() -> plugin.getServer().getPluginManager().callEvent(event));
    assertThat("Stacked item cannot be enchanted", event.getEnchantsToAdd(), is(anEmptyMap()));
  }

}