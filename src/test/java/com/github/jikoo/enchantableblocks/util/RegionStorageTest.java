package com.github.jikoo.enchantableblocks.util;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import java.io.IOException;
import java.nio.file.Files;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DisplayName("Feature: Store data in YAML files by region")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionStorageTest {

    private JavaPlugin plugin;
    private World world;

    @BeforeAll
    void beforeAll() throws NoSuchFieldException, IllegalAccessException {
        MockBukkit.mock();
        plugin = MockBukkit.load(EnchantableBlocksPlugin.class);
        PluginHelper.setDataDir(plugin);
        world = MockBukkit.getMock().addSimpleWorld("world");
    }

    @Test
    void testSave() throws IOException, InvalidConfigurationException {
        RegionStorage storage = new RegionStorage(plugin, world, 0, 0);
        if (storage.getDataFile().exists()) {
            Files.delete(storage.getDataFile().toPath());
        }
        storage.load();
        storage.set("test.path", "sample text");
        storage.save();
        assertThat("Region storage must be written.", storage.getDataFile().exists(), is(true));
        Files.delete(storage.getDataFile().toPath());
    }

    @Test
    void testLoad() throws IOException, InvalidConfigurationException {
        RegionStorage storage = new RegionStorage(plugin, world, 1, 1);
        String path = "sandwich.bread";
        String areYouAwareOfMyMonstrosity = "hot dog bun";
        storage.set(path, areYouAwareOfMyMonstrosity);
        storage.save();
        RegionStorage stored = new RegionStorage(plugin, storage.getWorld(), storage.getRegionX(), storage.getRegionZ());
        stored.load();
        assertThat("Stored value must equal expected value.", stored.get(path), is(areYouAwareOfMyMonstrosity));
    }

    @AfterAll
    void afterAll() {
        MockBukkit.unmock();
    }

}
