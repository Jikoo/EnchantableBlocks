package com.github.jikoo.enchantableblocks.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Store data in YAML files by region.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionStorageTest {

  private final Path dataDir = Path.of(".", "src", "test", "resources", getClass().getSimpleName());
  private final String world = "world";

  @AfterAll
  void afterAll() throws IOException {
    try (Stream<Path> files = Files.walk(dataDir)) {
      files.sorted(Comparator.reverseOrder()).forEach(file -> {
        try {
          Files.delete(file);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  @DisplayName("Getter returns corresponding Region.")
  @Test
  void testGetRegion() {
    var region = new Region(world, 0, 0);
    var storage = new RegionStorage(dataDir, region);

    assertThat("Region must match.", storage.getRegion(), is(region));
  }

  @DisplayName("Saving should write to disk.")
  @Test
  void testSave() throws IOException, InvalidConfigurationException {
    RegionStorage storage = new RegionStorage(dataDir, new Region(world, 0, 0));
    Files.deleteIfExists(storage.getDataFile().toPath());
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
    RegionStorage storage = new RegionStorage(dataDir, region);
    String path = "sandwich.bread";
    String areYouAwareOfMyMonstrosity = "hot dog bun";
    storage.set(path, areYouAwareOfMyMonstrosity);
    storage.save();
    RegionStorage stored = new RegionStorage(dataDir, region);
    stored.load();
    assertThat("Stored value must equal expected value.", stored.get(path),
        is(areYouAwareOfMyMonstrosity));
  }

}
