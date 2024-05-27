package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.enchantments.EnchantmentMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Enchant blocks in enchanting tables.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TableEnchanterTest {

  private @Nullable EnchantableRegistration registration;
  private Player player;
  private TableEnchanter listener;
  private ItemStack itemStack;

  @BeforeAll
  void setUpAll() {
    var server = ServerMocks.mockServer();
    EnchantmentMocks.init(server);

    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);
    var scheduler = mock(BukkitScheduler.class);
    when(server.getScheduler()).thenReturn(scheduler);
  }

  @BeforeEach
  void setUp() {
    // Set up default registration.
    registration = mock(EnchantableRegistration.class);
    doReturn(true).when(registration).hasEnchantPermission(notNull(), anyString());
    doReturn(Set.of(Enchantment.EFFICIENCY)).when(registration).getEnchants();
    doReturn(new EnchantableBlockConfig(new YamlConfiguration()) {}).when(registration).getConfig();

    Plugin plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    var registry = mock(EnchantableBlockRegistry.class);
    // Use doAnswer so that we can test with a null registration despite instantiating by default.
    doAnswer(invocation -> registration).when(registry).get(any());

    listener = new TableEnchanter(plugin, registry);

    player = mock(Player.class);
    var world = mock(World.class);
    doReturn("world").when(world).getName();
    doReturn(world).when(player).getWorld();

    // Set up type.
    Material material = Material.COAL_ORE;
    doReturn(Set.of(material)).when(registration).getMaterials();
    itemStack = new ItemStack(Material.COAL_ORE);
  }

  @DisplayName("Materials with no corresponding registration cannot enchant.")
  @Test
  void testUnregistered() {
    registration = null;
    assertThat("Never ineligible", listener.isIneligible(player, itemStack), is(false));
    assertThat(
        "Unregistered material cannot be enchanted",
        listener.getTable(player, itemStack),
        is(nullValue()));
  }

  @DisplayName("Registrations with no enchantments cannot enchant.")
  @Test
  void testNoEnchantsRegistration() {
    doReturn(Set.of()).when(registration).getEnchants();
    assertThat("Never ineligible", listener.isIneligible(player, itemStack), is(false));
    assertThat(
        "Material with no enchants cannot be enchanted",
        listener.getTable(player, itemStack),
        is(nullValue()));
  }

  @DisplayName("Registrations with all enchantments blacklisted cannot enchant.")
  @Test
  void testEnchantsBlacklist() {
    doReturn(Set.of(Enchantment.EFFICIENCY))
        .doReturn(Set.of())
        .when(registration).getEnchants();
    verify(registration, times(0)).getConfig();
    assertThat("Never ineligible", listener.isIneligible(player, itemStack), is(false));
    assertThat(
        "Material with no enchants cannot be enchanted",
        listener.getTable(player, itemStack),
        is(nullValue()));
    // Ensure that config is obtained for blacklist.
    verify(registration).getConfig();
  }

  @DisplayName("Permission is required to enchant.")
  @Test
  void testNoPerm() {
    doReturn(false).when(registration).hasEnchantPermission(notNull(), anyString());
    assertThat("Never ineligible", listener.isIneligible(player, itemStack), is(false));
    assertThat(
        "Player without permission cannot enchant",
        listener.getTable(player, itemStack),
        is(nullValue()));
  }

  @DisplayName("Valid setups can enchant.")
  @Test
  void testValid() {
    assertThat("Never ineligible", listener.isIneligible(player, itemStack), is(false));
    assertThat("Can enchant", listener.getTable(player, itemStack), is(notNullValue()));
  }

  @DisplayName("Incompatibilities are respected, including inverses.")
  @Test
  void testIncompatibility() {
    Enchantment enchant1 = Enchantment.EFFICIENCY;
    Enchantment enchant2 = Enchantment.UNBREAKING;
    Multimap<Enchantment, Enchantment> conflicts = HashMultimap.create();
    conflicts.put(enchant1, enchant2);

    var hasConflict = listener.hasConflict(conflicts);
    assertThat("Enchantments conflict", hasConflict.test(enchant1, enchant2));
    assertThat("Inverse conflicts", hasConflict.test(enchant2, enchant1));
    assertThat("Other enchants do not conflict", hasConflict.test(enchant1, enchant1), is(false));
  }

  @DisplayName("Listener performs as expected.")
  @Test
  void testListener() {
    var pdc = mock(PersistentDataContainer.class);
    doReturn(pdc).when(player).getPersistentDataContainer();

    var view = mock(InventoryView.class);
    var table = mock(Block.class);

    var prepareEvent = new PrepareItemEnchantEvent(player, view, table, itemStack, new EnchantmentOffer[3], 15);
    assertDoesNotThrow(() -> listener.onPrepareItemEnchant(prepareEvent));
    assertThat("Offers are set", prepareEvent.getOffers(), hasItemInArray(notNullValue()));

    var enchantEvent = new EnchantItemEvent(player, view, table, itemStack, 30, Map.of(), Enchantment.UNBREAKING, 30, 15);
    assertDoesNotThrow(() -> listener.onEnchantItem(enchantEvent));
    assertThat(
        "Enchantments are added",
        enchantEvent.getEnchantsToAdd(),
        is(aMapWithSize(greaterThan(0))));
  }

}