package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager.RegionStorageData;
import com.github.jikoo.enchantableblocks.util.PluginHelper;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.logging.PatternCountHandler;
import java.io.IOException;
import java.nio.file.Files;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Load data on demand per region.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionLoadFunctionTest {

  private Plugin plugin;
  private RegionLoadFunction loadFunction;

  @BeforeAll
  void beforeAll() {
    MockBukkit.mock();
  }

  @AfterAll
  void afterAll() {
    plugin.getServer().getScheduler().cancelTasks(plugin);
    MockBukkit.unmock();
  }

  @BeforeEach
  void setUp() throws NoSuchFieldException, IllegalAccessException {
    MockPlugin fakePlugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    PluginHelper.setDataDir(fakePlugin);
    plugin = fakePlugin;
    EnchantableBlockManager manager = new EnchantableBlockManager(plugin);
    loadFunction = new RegionLoadFunction(plugin, manager);
  }

  @DisplayName("Nonexistent data should be handled gracefully.")
  @Test
  void testLoadNotExists() throws IOException {
    Region region = new Region("world", 0, 0);
    RegionStorageData storageData = loadFunction.apply(region, false);
    // Another test probably created file, delete.
    if (storageData != null) {
      Files.deleteIfExists(storageData.getStorage().getDataFile().toPath());
      storageData = loadFunction.apply(region, false);
    }
    assertThat("Nonexistent data must not be created if not set to", storageData, is(nullValue()));
    storageData = loadFunction.apply(region, true);
    assertThat("Nonexistent data must be created if set to", storageData, is(notNullValue()));
  }

  @DisplayName("Invalid data should be handled gracefully.")
  @Test
  void testLoadInvalid() {
    PatternCountHandler handler = new PatternCountHandler("while scanning for the next token");
    plugin.getLogger().addHandler(handler);
    Region region = new Region("world", -1, -1);

    RegionStorageData storageData = loadFunction.apply(region, true);
    assertThat("Invalid data should load blank file", storageData, is(notNullValue()));

    storageData = loadFunction.apply(region, false);
    assertThat("Invalid data should still load if not creating", storageData, is(notNullValue()));

    assertThat("Expected 2 attempts to load invalid yaml", handler.getMatches(), is(2));
  }

  @DisplayName("Valid data should always load.")
  @Test
  void testLoadValid() {
    Region region = new Region("world", 1, 1);
    RegionStorageData storageData = loadFunction.apply(region, true);
    assertThat("Data must be loaded if existent", storageData, is(notNullValue()));
    storageData = loadFunction.apply(region, false);
    assertThat(
        "Data must be loaded if existent even if not creating",
        storageData,
        is(notNullValue()));
  }

}