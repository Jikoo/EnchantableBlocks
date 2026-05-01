package com.github.jikoo.enchantableblocks.block.impl.furnace;

import com.github.jikoo.planarwrappers.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Feature: Configuration for furnace-specific details.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableFurnaceConfigTest {

  private static final String INVALID_WORLD = "%invalid_world%";
  private static final String ORE_WORLD = "mining_dimension";
  private static final String VANILLA_WORLD = "lame_vanilla_world";

  private MockedStatic<Bukkit> bukkit;
  private EnchantableFurnaceConfig config;

  @BeforeAll
  void beforeAll() {
    bukkit = mockStatic();
    bukkit.when(() -> Bukkit.getRegistry(any())).thenAnswer(invocation -> {
      Registry<?> registry = mock(Registry.class);
      if (Enchantment.class.isAssignableFrom(invocation.getArgument(0))) {
        doAnswer(invocation1 -> mock(Enchantment.class)).when(registry).getOrThrow(any());
      }
      if (Material.class.isAssignableFrom(invocation.getArgument(0))) {
        doAnswer(invocation1 -> Material.matchMaterial(invocation1.getArgument(0, NamespacedKey.class).getKey()))
            .when(registry).get(any());
      }
      return registry;
    });

    File configFile = Path.of(".", "src", "test", "resources", "furnace_config.yml").toFile();
    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
    this.config = new EnchantableFurnaceConfig(configuration);
  }

  @AfterAll
  void tearDown() {
    bukkit.close();
  }

  @DisplayName("Fortune list should be customizable per-world.")
  @Test
  void testFortuneList() {
    Setting<Set<Material>> fortuneList = config.fortuneList();
    Collection<Material> value = Set.of(Material.WET_SPONGE, Material.STONE_BRICKS);
    assertThat("Materials should be set in default settings",
        fortuneList.get(INVALID_WORLD),
        both(everyItem(is(in(value)))).and(containsInAnyOrder(value.toArray())));

    // Ensure cache gets hit.
    fortuneList.get(INVALID_WORLD);

    value = Set.of(Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE,
        Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE,
        Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE);
    assertThat("Materials should be overridden", fortuneList.get(ORE_WORLD),
        both(everyItem(is(in(value)))).and(containsInAnyOrder(value.toArray())));
  }

  @DisplayName("Fortune list mode should be customizable per-world.")
  @Test
  void testEnabled() {
    Setting<Boolean> isBlacklist = config.fortuneListIsBlacklist();

    assertThat("Default uses blacklist",
        isBlacklist.get(INVALID_WORLD));
    assertThat("Mining world uses whitelist",
        isBlacklist.get(ORE_WORLD), is(false));
    assertThat("Worlds without overrides use blacklist",
        isBlacklist.get(VANILLA_WORLD));
  }

}
