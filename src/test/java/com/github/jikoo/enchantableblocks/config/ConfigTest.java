package com.github.jikoo.enchantableblocks.config;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.config.data.WorldSetting;
import com.github.jikoo.enchantableblocks.util.PluginHelper;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;

@DisplayName("Feature: Configuration should be translatable in a simple fashion.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigTest {

    public static final String INVALID_WORLD = "%invalid_world%";
    public static final String DIMENSION1 = "mining_dimension";

    @BeforeAll
    void beforeAll() throws NoSuchFieldException, IllegalAccessException {
        MockBukkit.mock();
        EnchantableBlocksPlugin plugin = MockBukkit.load(EnchantableBlocksPlugin.class);
        PluginHelper.setDataDir(plugin);
        plugin.getRegistry().reload();
    }

    @DisplayName("Furnaces should be able to be disabled per-world.")
    @Test
    void testFurnaceEnabled() {
        WorldSetting<Boolean> enabled = EnchantableFurnace.getConfig().enabled;

        assertThat("Furnaces should be enabled in default settings", enabled.get(INVALID_WORLD), is(true));
        assertThat("Furnaces should be disabled in lame_vanilla_world", enabled.get("lame_vanilla_world"), is(false));
        assertThat("Furnaces should be enabled in worlds without overrides", enabled.get(DIMENSION1), is(true));
    }

    @DisplayName("Fortune list should be customizable per-world.")
    @Test
    void testFortuneList() {
        WorldSetting<Set<Material>> fortuneList = EnchantableFurnace.getConfig().fortuneList;
        Collection<Material> value = Arrays.asList(Material.WET_SPONGE, Material.STONE_BRICKS);
        assertThat("Materials should be set in default settings", fortuneList.get(INVALID_WORLD),
                both(everyItem(is(in(value)))).and(containsInAnyOrder(value.toArray())));

        // Ensure cache gets hit.
        fortuneList.get(INVALID_WORLD);

        value = Arrays.asList(Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE,
                Material.REDSTONE_ORE,Material.LAPIS_ORE, Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE);
        assertThat("Materials should be overridden", fortuneList.get(DIMENSION1),
                both(everyItem(is(in(value)))).and(containsInAnyOrder(value.toArray())));
    }

    @AfterAll
    void afterAll() {
        MockBukkit.unmock();
    }

}
