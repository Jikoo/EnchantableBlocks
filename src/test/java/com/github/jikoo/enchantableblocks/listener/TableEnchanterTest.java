package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantOperation;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantmentHelper;
import com.github.jikoo.planarwrappers.util.StringConverters;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
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
    // Default item
    itemStack = new ItemStack(ENCHANTABLE_MATERIAL);
  }

  @AfterAll
  void tearDownAll() {
    MockBukkit.unmock();
  }

  @Test
  void testEnchantedUnableToEnchant() {
    itemStack.addUnsafeEnchantment(VALID_ENCHANT, 10);
    assertThat("Enchanted item cannot be enchanted", listener.isUnableToEnchant(itemStack, player));
  }

  @Test
  void testStackUnableToEnchant() {
    itemStack.setAmount(2);
    assertThat("Stacked item cannot be enchanted", listener.isUnableToEnchant(itemStack, player));
  }

  @Test
  void testUnregisteredUnableToEnchant() {
    itemStack.setType(UNREGISTERED_MATERIAL);
    assertThat("Unregistered material cannot be enchanted", listener.isUnableToEnchant(itemStack, player));
  }

  @Test
  void testNoEnchantsUnableToEnchant() {
    itemStack.setType(UNENCHANTABLE_MATERIAL);
    assertThat("Material with no enchants cannot be enchanted", listener.isUnableToEnchant(itemStack, player));
  }

  @Test
  void testMainPermUnableToEnchant() {
    player = new PlayerMock(server, "sampletext") {
      @Override
      public boolean hasPermission(String name) {
        return "enchantableblocks.enchant.table".equals(name);
      }
    };
    assertThat("Player with main permission can enchant", !listener.isUnableToEnchant(itemStack, player));
  }

  @Test
  void testSpecificPermUnableToEnchant() {
    player = new PlayerMock(server, "sampletext") {
      @Override
      public boolean hasPermission(String name) {
        return "enchantableblocks.enchant.table.dummyenchantableblock".equals(name);
      }
    };
    assertThat("Player with main permission can enchant", !listener.isUnableToEnchant(itemStack, player));
  }

  @Test
  void testUnregisteredGetOperation() {
    itemStack.setType(UNREGISTERED_MATERIAL);
    assertThat(
        "Unregistered material yields null operation",
        listener.getOperation(itemStack, player),
        is(nullValue()));
  }

  @Test
  void testNoEnchantsGetOperation() {
    itemStack.setType(UNENCHANTABLE_MATERIAL);
    assertThat(
        "Unenchantable material yields null operation",
        listener.getOperation(itemStack, player),
        is(nullValue()));
  }

  @Test
  void testGetOperation() {
    assertThat(
        "Enchantable material yields operation",
        listener.getOperation(itemStack, player),
        is(notNullValue()));
  }

  @Test
  void testLowLevelGetOffer() {
    var operation = new EnchantOperation(Set.of());
    var offer = listener.getOffer(operation, player, 0, 0);
    assertThat("Invalid level yields no offer", offer, is(nullValue()));
  }

  @Test
  void testEmptyGetOffer() {
    var operation = new EnchantOperation(Set.of());
    var offer = listener.getOffer(operation, player, 0, 30);
    assertThat("No enchantments yields no offer", offer, is(nullValue()));
  }

  @Test
  void testGetOffer() {
    var operation = new EnchantOperation(Set.of(VALID_ENCHANT));
    operation.setSeed(0);
    var offer = listener.getOffer(operation, player, 0, 30);
    assertThat("Seed yielding results yields offer", offer, is(notNullValue()));
  }

  @Test
  void testOwnSeed() {
    var value = 10;
    var expected = value - 1;
    var ownSeed1 = listener.getOwnSeed(player, () -> expected);
    assertThat("Own seed must be from supplier", ownSeed1, is((long) expected));
    var ownSeed2 = listener.getOwnSeed(player, () -> value);
    assertThat("Own seed must be from cache", ownSeed2, is((long) expected));
  }

  @Test
  void testResetSeed() {
    var value1 = 10;
    var ownSeed1 = listener.getOwnSeed(player, () -> value1);
    assertThat("Own seed must be from supplier", ownSeed1, is((long) value1));
    listener.resetSeed(player);
    var value2 = value1 - 1;
    var ownSeed2 = listener.getOwnSeed(player, () -> value2);
    assertThat("Own seed must be from supplier", ownSeed2, is((long) value2));
  }

  @Test
  void testInternalSeed() {
    var value = 10;
    var expected = value - 1;
    player = new PlayerMock(server,"sampletext") {
      @SuppressWarnings("unused")
      public Object getHandle() {
        return new Object() {
          public int eG() {
            return expected;
          }
        };
      }
    };
    var seed = listener.getEnchantmentSeed(player, () -> value);
    assertThat("Seed must be from player", seed, is((long) expected));
  }

  @Test
  void testBrokenInternalSeed() {
    var value = 10;
    var seed = listener.getEnchantmentSeed(player, () -> value);
    assertThat("Seed must fall through", seed, is((long) value));
    // Again to hit fallthrough more quickly
    seed = listener.getEnchantmentSeed(player, () -> value);
    assertThat("Seed must fall through", seed, is((long) value));
  }

  @Test
  void testUnablePrepareItemEnchant() {
    itemStack.setType(UNREGISTERED_MATERIAL);
    var event = new PrepareItemEnchantEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, new EnchantmentOffer[3], 0);
    assertDoesNotThrow(() -> listener.onPrepareItemEnchant(event));
  }

  @Test
  void testNoEnchantsPrepareItemEnchant() {
    plugin.getConfig().set("blocks.DummyEnchantableBlock.tableDisabledEnchantments",
        Arrays.asList("efficiency", "unbreaking"));
    registry.reload();

    var event = new PrepareItemEnchantEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
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
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, new EnchantmentOffer[3], 30);
    player.getPersistentDataContainer().set(
        Objects.requireNonNull(StringConverters.toNamespacedKey("enchantableblocks:enchant_seed")),
        PersistentDataType.INTEGER, 0
    );
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
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, 30, new HashMap<>(), 0);
    assertDoesNotThrow(() -> listener.onEnchantItem(event));
    assertThat("Enchantments must be empty", event.getEnchantsToAdd().isEmpty());
  }

  @Test
  void testEnchantItem() {
    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, 30, new HashMap<>(), 2);
    player.getPersistentDataContainer().set(
        Objects.requireNonNull(StringConverters.toNamespacedKey("enchantableblocks:enchant_seed")),
        PersistentDataType.INTEGER, 0
    );
    assertDoesNotThrow(() -> listener.onEnchantItem(event));
    assertThat("Enchantments must not be empty", !event.getEnchantsToAdd().isEmpty());
  }


  @Test
  void testEnchantItemSucceed() {
    var event = new EnchantItemEvent(
        player, player.getOpenInventory(), player.getLocation().getBlock(),
        itemStack, 30, Map.of(), 2);
    assertDoesNotThrow(() -> listener.onEnchantItemSucceed(event));
  }

}