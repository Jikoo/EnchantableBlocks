package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager.RegionStorageData;
import com.github.jikoo.enchantableblocks.util.PluginHelper;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
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

  private Plugin plugin;
  private EnchantableBlockManager manager;
  private RegionInUseCheck inUseCheck;

  @BeforeAll
  void beforeAll() {
    ServerMock server = MockBukkit.mock();
    server.addWorld(new LoadedStateWorld(true));
    server.addWorld(new LoadedStateWorld(false));
  }

  @AfterAll
  void afterAll() {
    MockBukkit.unmock();
  }

  @BeforeEach
  void setUp() throws NoSuchFieldException, IllegalAccessException {
    MockPlugin fakePlugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    PluginHelper.setDataDir(fakePlugin);
    plugin = fakePlugin;
    manager = new EnchantableBlockManager(plugin);
    inUseCheck = new RegionInUseCheck(plugin.getLogger());
  }

  @DisplayName("Null value is never in use.")
  @ParameterizedTest
  @MethodSource("getWorlds")
  void testNullRegion(LoadedStateWorld world) {
    Region key = new Region(world.getName(), 0, 0);

    assertThat("Null value is never in use", inUseCheck.test(key, null), is(false));
  }

  @DisplayName("Clean regions do nothing.")
  @ParameterizedTest
  @MethodSource("getWorlds")
  void testCleanRegion(LoadedStateWorld world) {
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
  void testDirtyEmptyRegion(LoadedStateWorld world) throws IOException {
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
  void testDirtyRegion(LoadedStateWorld world) throws IOException {
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

  static @NotNull Stream<World> getWorlds() {
    return Stream.of(Bukkit.getWorld("loaded"), Bukkit.getWorld("unloaded"));
  }

  private static class LoadedStateWorld extends WorldMock {
    private final boolean loaded;

    private LoadedStateWorld(boolean loaded) {
      super();
      this.loaded = loaded;
      setName(this.loaded ? "loaded" : "unloaded");
    }

    @Override
    public boolean isChunkLoaded(@NotNull Chunk chunk) {
      return isChunkLoaded(chunk.getX(), chunk.getZ());
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
      return this.loaded;
    }

    public boolean getLoadedState() {
      return this.loaded;
    }
  }

}