package com.github.jikoo.enchantableblocks.config;

import static com.github.jikoo.enchantableblocks.mock.matcher.EnchantMatchers.enchant;
import static com.github.jikoo.enchantableblocks.mock.matcher.EnchantMatchers.enchantSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.enchantments.EnchantmentMocks;
import com.github.jikoo.planarenchanting.table.Enchantability;
import com.github.jikoo.planarwrappers.config.Mapping;
import com.github.jikoo.planarwrappers.config.Setting;
import com.google.common.collect.Multimap;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Parse complex settings into simple configuration.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlockConfigTest {
  // These tests are more of a failsafe for PlanarWrappers issues;
  // EnchantableBlockConfig instances are just containers for parsed data.

  private static final String INVALID_WORLD = "%invalid_world%";
  private static final String ORE_WORLD = "mining_dimension";
  private static final String VANILLA_WORLD = "lame_vanilla_world";
  private static final String POWER_WORLD = "busted_endgame_bullhonkey_world";

  private EnchantableBlockConfig config;

  @BeforeAll
  void beforeAll() {
    ServerMocks.mockServer();
    EnchantmentMocks.init();

    File configFile = Path.of(".", "src", "test", "resources", "generic_config.yml").toFile();
    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
    this.config = new EnchantableBlockConfig(configuration) {};
  }

  @DisplayName("Nonexistent configuration sections should be handled gracefully.")
  @Test
  void testLoadNonexistent() {
    assertDoesNotThrow(() -> new EnchantableBlockConfig(new YamlConfiguration()) {});
  }

  @DisplayName("Blocks should be able to be disabled per-world.")
  @Test
  void testEnabled() {
    Setting<Boolean> enabled = config.enabled();

    assertThat("Blocks should be enabled in default settings",
        enabled.get(INVALID_WORLD));
    assertThat("Blocks should be disabled in lame_vanilla_world",
        enabled.get(VANILLA_WORLD), is(false));
    assertThat("Blocks should be enabled in worlds without overrides",
        enabled.get(ORE_WORLD));
  }

  @DisplayName("Enchantablity should be customizable per-world.")
  @Test
  void testTableEnchantability() {
    Setting<Enchantability> enchantability = config.tableEnchantability();

    assertThat("Enchantability should be STONE by default",
        enchantability.get(INVALID_WORLD), is(Enchantability.STONE));
    assertThat("Enchantability should be overridden to GOLD_ARMOR",
        enchantability.get(POWER_WORLD), is(Enchantability.GOLD_ARMOR));
  }

  @DisplayName("Disabled enchantments should be customizable per-world.")
  @Test
  void testDisabledEnchants() {
    Setting<Set<Enchantment>> disabledEnchants = config.tableDisabledEnchants();

    assertThat("Conflicts should default to empty set",
        disabledEnchants.get(INVALID_WORLD).isEmpty());
    Set<Enchantment> value = Set.of(Enchantment.UNBREAKING);
    assertThat("Conflicts should be overridden properly",
        disabledEnchants.get(VANILLA_WORLD),
        is(enchantSet(value)));
  }

  @DisplayName("Enchantment conflicts should be customizable per-world.")
  @Test
  void testEnchantmentConflicts() {
    Setting<Multimap<Enchantment, Enchantment>> conflicts = config.tableEnchantmentConflicts();

    Multimap<Enchantment, Enchantment> defaultConflicts = conflicts.get(INVALID_WORLD);
    assertThat("Conflicts should default to a single entry", defaultConflicts.size(), is(1));
    Optional<Entry<Enchantment, Collection<Enchantment>>> entryOptional = defaultConflicts.asMap().entrySet().stream().findFirst();
    assertThat("Conflict entry must exist", entryOptional.isPresent());
    Entry<Enchantment, Collection<Enchantment>> entry = entryOptional.get();
    assertThat("Conflict must contain silk touch", entry.getKey(), is(enchant(Enchantment.SILK_TOUCH)));
    assertThat("Conflict must contain fortune", entry.getValue(), contains(enchant(Enchantment.FORTUNE)));

    assertThat("Conflicts should be overridden properly",
        conflicts.get(POWER_WORLD).isEmpty());
    assertThat("Invalid overrides should still override",
        config.anvilEnchantmentConflicts().get(POWER_WORLD).isEmpty());
  }

  @DisplayName("Maximum level enchantments combine to should be customizable per-world.")
  @Test
  void testAnvilEnchantMax() {
    Mapping<Enchantment, Integer> enchantmentMax = config.anvilEnchantmentMax();

    Enchantment enchantment = Enchantment.SILK_TOUCH;
    int actual = enchantmentMax.get(INVALID_WORLD, enchantment);
    assertThat("Max level should default to enchantment max level",
        actual, is(enchantment.getMaxLevel()));
    assertThat("Cached value should equal previous return value",
        enchantmentMax.get(INVALID_WORLD, enchantment), is(actual));
    assertThat("Worlds with invalid keys should fall through to default",
        enchantmentMax.get(VANILLA_WORLD, enchantment), is(actual));

    enchantment = Enchantment.EFFICIENCY;
    assertThat("Max level should use specified defaults",
        enchantmentMax.get(INVALID_WORLD, enchantment), is(4));
    assertThat("World overrides should be provided",
        enchantmentMax.get(POWER_WORLD, enchantment), is(10));

    enchantment = Enchantment.FORTUNE;
    assertThat("Invalid values should fall through to defaults",
        enchantmentMax.get(INVALID_WORLD, enchantment), is(enchantment.getMaxLevel()));
  }

}