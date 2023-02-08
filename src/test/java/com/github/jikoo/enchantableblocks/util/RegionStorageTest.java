package com.github.jikoo.enchantableblocks.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Store data in YAML files by region.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionStorageTest {

  private Plugin plugin;
  private final String world = "world";

  @BeforeAll
  void beforeAll() {
    plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    File dataFolder = Path.of(".", "src", "test", "resources", plugin.getName()).toFile();
    when(plugin.getDataFolder()).thenReturn(dataFolder);
  }

  @AfterAll
  void afterAll() throws IOException {
    try (Stream<Path> files = Files.walk(plugin.getDataFolder().toPath())) {
      files.sorted(Comparator.reverseOrder()).forEach(file -> {
        try {
          Files.delete(file);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  @DisplayName("Saving should write to disk.")
  @Test
  void testSave() throws IOException, InvalidConfigurationException {
    RegionStorage storage = new RegionStorage(plugin, new Region(world, 0, 0));
    if (storage.getDataFile().exists()) {
      Files.delete(storage.getDataFile().toPath());
    }
    storage.load();
    storage.set("test.path", "sample text");
    storage.save();
    assertThat("Region storage must be written.", storage.getDataFile().exists(), is(true));
    Files.delete(storage.getDataFile().toPath());
  }

  @DisplayName("Loading should read data from disk.")
  @Test
  void testLoad() throws IOException, InvalidConfigurationException {
    Region region = new Region(world, 1, 1);
    RegionStorage storage = new RegionStorage(plugin, region);
    String path = "sandwich.bread";
    String areYouAwareOfMyMonstrosity = "hot dog bun";
    storage.set(path, areYouAwareOfMyMonstrosity);
    storage.save();
    RegionStorage stored = new RegionStorage(plugin, region);
    stored.load();
    assertThat("Stored value must equal expected value.", stored.get(path),
        is(areYouAwareOfMyMonstrosity));
  }

}
