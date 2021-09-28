package com.github.jikoo.enchantableblocks.config;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.config.impl.EnchantableFurnaceConfig;
import com.github.jikoo.enchantableblocks.util.PluginHelper;
import com.github.jikoo.enchantableblocks.util.enchant.Enchantability;
import com.github.jikoo.planarwrappers.config.Mapping;
import com.github.jikoo.planarwrappers.config.Setting;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Configuration should be interpretable in a simple fashion.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigTest {

  private static final String INVALID_WORLD = "%invalid_world%";
  private static final String ORE_WORLD = "mining_dimension";
  private static final String VANILLA_WORLD = "lame_vanilla_world";
  private static final String POWER_WORLD = "busted_endgame_bullhonkey_world";

  private EnchantableFurnaceConfig config;

  @BeforeAll
  void beforeAll() throws NoSuchFieldException, IllegalAccessException {
    MockBukkit.mock();
    EnchantableBlocksPlugin plugin = MockBukkit.load(EnchantableBlocksPlugin.class);
    PluginHelper.setDataDir(plugin);
    plugin.getBlockManager().reload();
    EnchantableBlockConfig config = Objects.requireNonNull(plugin.getBlockManager().getRegistry().get(Material.FURNACE)).getConfig();
    this.config = (EnchantableFurnaceConfig) config;
  }

  @DisplayName("Nonexistent configuration sections should be handled gracefully.")
  @Test
  void testNonexistantSection() {
    assertDoesNotThrow(() -> new EnchantableBlockConfig(new YamlConfiguration()) {
    });
  }

  @DisplayName("Blocks should be able to be disabled per-world.")
  @Test
  void testFurnaceEnabled() {
    Setting<Boolean> enabled = config.enabled;

    assertThat("Furnaces should be enabled in default settings",
        enabled.get(INVALID_WORLD));
    assertThat("Furnaces should be disabled in lame_vanilla_world",
        enabled.get(VANILLA_WORLD), is(false));
    assertThat("Furnaces should be enabled in worlds without overrides",
        enabled.get(ORE_WORLD));
  }

  @DisplayName("Fortune list should be customizable per-world.")
  @Test
  void testFortuneList() {
    Setting<Set<Material>> fortuneList = config.fortuneList;
    Collection<Material> value = EnumSet.of(Material.WET_SPONGE, Material.STONE_BRICKS);
    assertThat("Materials should be set in default settings",
        fortuneList.get(INVALID_WORLD),
        both(everyItem(is(in(value)))).and(containsInAnyOrder(value.toArray())));

    // Ensure cache gets hit.
    fortuneList.get(INVALID_WORLD);

    value = EnumSet.of(Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE,
        Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE,
        Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE);
    assertThat("Materials should be overridden", fortuneList.get(ORE_WORLD),
        both(everyItem(is(in(value)))).and(containsInAnyOrder(value.toArray())));
  }

  @DisplayName("Block enchantablity should be customizable per-world.")
  @Test
  void testFurnaceEnchantability() {
    Setting<Enchantability> enchantability = config.tableEnchantability;

    assertThat("Enchantability should be STONE by default",
        enchantability.get(INVALID_WORLD), is(Enchantability.STONE));
    assertThat("Enchantability should be overridden to GOLD_ARMOR",
        enchantability.get(POWER_WORLD), is(Enchantability.GOLD_ARMOR));
  }

  @DisplayName("Block disabled enchantments should be customizable per-world.")
  @Test
  void testDisabledEnchants() {
    Setting<Set<Enchantment>> disabledEnchants = config.tableDisabledEnchants;

    assertThat("Conflicts should default to empty set",
        disabledEnchants.get(INVALID_WORLD).isEmpty());
    Collection<Enchantment> value = Collections.singleton(Enchantment.DURABILITY);
    assertThat("Conflicts should be overridden properly", disabledEnchants.get(VANILLA_WORLD),
        both(everyItem(is(in(value)))).and(containsInAnyOrder(value.toArray())));
  }

  @DisplayName("Block enchantment conflicts should be customizable per-world.")
  @Test
  void testEnchantConflicts() {
    Setting<Multimap<Enchantment, Enchantment>> conflicts = config.tableEnchantmentConflicts;

    Multimap<Enchantment, Enchantment> defaultConflicts = conflicts.get(INVALID_WORLD);
    assertThat("Conflicts should default to a single entry", defaultConflicts.size(), is(1));
    assertThat("Conflict should be silk touch and fortune",
        defaultConflicts.containsEntry(Enchantment.SILK_TOUCH, Enchantment.LOOT_BONUS_BLOCKS));

    assertThat("Conflicts should be overridden properly",
        conflicts.get(POWER_WORLD).isEmpty());
    assertThat("Invalid overrides should still override",
        config.anvilEnchantmentConflicts.get(POWER_WORLD).isEmpty());
  }

  @DisplayName("Maximum level enchantments combine to should be customizable per-world.")
  @Test
  void testEnchantMax() {
    Mapping<Enchantment, Integer> enchantmentMax = config.anvilEnchantmentMax;

    Enchantment enchantment = Enchantment.SILK_TOUCH;
    int actual = enchantmentMax.get(INVALID_WORLD, enchantment);
    assertThat("Max level should default to enchantment max level",
        actual, is(enchantment.getMaxLevel()));
    assertThat("Cached value should equal previous return value",
        enchantmentMax.get(INVALID_WORLD, enchantment), is(actual));
    assertThat("Worlds with invalid keys should fall through to default",
        enchantmentMax.get(VANILLA_WORLD, enchantment), is(actual));

    enchantment = Enchantment.DIG_SPEED;
    assertThat("Max level should use specified defaults",
        enchantmentMax.get(INVALID_WORLD, enchantment), is(4));
    assertThat("World overrides should be provided",
        enchantmentMax.get(POWER_WORLD, enchantment), is(10));

    enchantment = Enchantment.LOOT_BONUS_BLOCKS;
    assertThat("Invalid values should fall through to defaults",
        enchantmentMax.get(INVALID_WORLD, enchantment), is(enchantment.getMaxLevel()));

  }

  @AfterAll
  void afterAll() {
    MockBukkit.unmock();
  }

}
