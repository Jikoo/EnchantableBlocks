package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager.RegionStorageData;
import com.github.jikoo.enchantableblocks.util.Region;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Load data on demand per region.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionLoadFunctionTest {

  private Plugin plugin;
  private RegionLoadFunction loadFunction;

  @BeforeEach
  void setUp() {
    plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    var dataFolder = Path.of(".", "src", "test", "resources", plugin.getName()).toFile();
    when(plugin.getDataFolder()).thenReturn(dataFolder);
    var config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
    when(plugin.getConfig()).thenReturn(config);
    var logger = mock(Logger.class);
    when(plugin.getLogger()).thenReturn(logger);
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

    Files.deleteIfExists(storageData.getStorage().getDataFile().toPath());
  }

  @DisplayName("Invalid data should be handled gracefully.")
  @Test
  void testLoadInvalid() {
    var logger = plugin.getLogger();
    Region region = new Region("world", -1, -1);

    RegionStorageData storageData = loadFunction.apply(region, true);
    assertThat("Invalid data should load blank file", storageData, is(notNullValue()));
    verify(logger).log(any(Level.class), any(Throwable.class), any());

    storageData = loadFunction.apply(region, false);
    assertThat("Invalid data should still load if not creating", storageData, is(notNullValue()));
    verify(logger, times(2)).log(any(Level.class), any(Throwable.class), any());
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