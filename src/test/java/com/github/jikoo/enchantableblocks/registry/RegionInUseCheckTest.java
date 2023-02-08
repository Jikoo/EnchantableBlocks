package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager.RegionStorageData;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Feature: Load data on demand per region.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionInUseCheckTest {

  private Collection<LoadedStateWorld> worlds;
  private Plugin plugin;
  private EnchantableBlockManager manager;
  private RegionInUseCheck inUseCheck;

  @BeforeAll
  void beforeAll() {
    var server = BukkitServer.newServer();

    worlds = new ArrayList<>();
    for (boolean loaded : new boolean[] { true, false }) {
      var world = mock(LoadedStateWorld.class);
      String name = "loaded_" + loaded;
      when(world.getName()).thenReturn(name);
      when(world.isChunkLoaded(any())).thenReturn(loaded);
      when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(loaded);
      when(world.getLoadedState()).thenReturn(loaded);

      when(server.getWorld(name)).thenReturn(world);

      worlds.add(world);
    }

    Bukkit.setServer(server);
  }

  @BeforeEach
  void setUp() {
    plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    File dataFolder = Path.of(".", "src", "test", "resources", plugin.getName()).toFile();
    when(plugin.getDataFolder()).thenReturn(dataFolder);
    when(plugin.getConfig()).thenReturn(new YamlConfiguration());
    manager = new EnchantableBlockManager(plugin);
    inUseCheck = new RegionInUseCheck(plugin.getLogger());
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

  @DisplayName("Null value is never in use.")
  @ParameterizedTest
  @MethodSource("getWorlds")
  void testNullRegion(@NotNull World world) {
    Region key = new Region(world.getName(), 0, 0);

    assertThat("Null value is never in use", inUseCheck.test(key, null), is(false));
  }

  @DisplayName("Clean regions do nothing.")
  @ParameterizedTest
  @MethodSource("getWorlds")
  void testCleanRegion(@NotNull LoadedStateWorld world) {
    Region key = new Region(world.getName(), 0, 0);
    RegionStorageData value = manager.new RegionStorageData(new RegionStorage(plugin, key));

    assertThat(
        "Value in-use state must match world state",
        inUseCheck.test(key, value),
        is(world.getLoadedState()));
  }

  @DisplayName("Empty data deletes from disk during check.")
  @ParameterizedTest
  @MethodSource("getWorlds")
  void testDirtyEmptyRegion(@NotNull LoadedStateWorld world) throws IOException {
    Region key = new Region(world.getName(), 0, 0);
    RegionStorageData value = manager.new RegionStorageData(new RegionStorage(plugin, key));

    // Dirty empty state
    value.setDirty();
    Path path = value.getStorage().getDataFile().toPath();
    Files.createDirectories(path.getParent());
    Files.createFile(path);

    assertThat(
        "Value in-use state must match world state",
        inUseCheck.test(key, value),
        is(world.getLoadedState()));
    assertThat("File must not exist after in use check on empty data", !Files.exists(path));
  }

  @DisplayName("Valid data writes to disk during check.")
  @ParameterizedTest
  @MethodSource("getWorlds")
  void testDirtyRegion(@NotNull LoadedStateWorld world) throws IOException {
    Region key = new Region(world.getName(), 0, 0);
    RegionStorageData value = manager.new RegionStorageData(new RegionStorage(plugin, key));

    // Non-empty dirty state
    value.setDirty();
    value.getStorage().set("path.to.value", "value");
    Path path = value.getStorage().getDataFile().toPath();
    Files.createDirectories(path.getParent());
    Files.createFile(path);

    assertThat(
        "Value in-use state must match world state",
        inUseCheck.test(key, value),
        is(world.getLoadedState()));
    assertThat("File must exist after in use check on data", Files.exists(path));

    // Clean up
    Files.deleteIfExists(path);
  }

  @NotNull Collection<LoadedStateWorld> getWorlds() {
    return worlds;
  }

  interface LoadedStateWorld extends World {

    boolean getLoadedState();

  }

}