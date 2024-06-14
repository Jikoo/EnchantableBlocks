package com.github.jikoo.enchantableblocks.registry;

import static com.github.jikoo.enchantableblocks.mock.matcher.IsSimilarMatcher.similar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.answer.SpiedAnswer;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager.RegionStorageData;
import com.github.jikoo.enchantableblocks.util.Cache;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import com.github.jikoo.planarwrappers.util.Coords;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("Feature: Manage enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlockManagerTest {

  private static final String NORMAL_WORLD_NAME = "world";
  private static final String DISABLED_WORLD_NAME = "lame_vanilla_world";
  private static final String DISABLED_WORLD_PATH = "overrides." + DISABLED_WORLD_NAME + ".enabled";
  private Material goodMat;
  private Material badMat;
  private Enchantment goodEnchant;

  @Mock EnchantableBlockRegistry registry;
  Path dataDir;
  @Mock Logger logger;
  Cache<Region, RegionStorageData> cache;
  EnchantableBlockManager manager;
  private YamlConfiguration backingConfig;
  private Block block;
  private Block blockDisabledWorld;
  private Chunk chunk;
  private Chunk chunkBad;
  @Captor ArgumentCaptor<Supplier<String>> loggerCaptor;
  AutoCloseable mockAnnotations;

  @BeforeAll
  void beforeAll() {
    var server = ServerMocks.mockServer();
    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);

    goodMat = Material.COAL_ORE;
    badMat = Material.DIRT;
    goodEnchant = Enchantment.EFFICIENCY;

    block = WorldMocks.newWorld(NORMAL_WORLD_NAME).getBlockAt(0, 0, 0);
    blockDisabledWorld = WorldMocks.newWorld(DISABLED_WORLD_NAME).getBlockAt(0, 0, 0);
  }

  @BeforeEach
  void beforeEach() {
    mockAnnotations = MockitoAnnotations.openMocks(this);

    // Grab finalized cache from builder during creation.
    var builder = spy(new Cache.CacheBuilder<Region, RegionStorageData>());
    doAnswer(new SpiedAnswer<Cache<Region, RegionStorageData>>() {
      @Override
      public Cache<Region, RegionStorageData> accept(Cache<Region, RegionStorageData> answer) {
        cache = super.accept(answer);
        return cache;
      }
    }).when(builder).build();
    // Set up data folder.
    dataDir = Path.of(".", "src", "test", "resources", getClass().getSimpleName(), "data");

    manager = new EnchantableBlockManager(registry, builder, 5, dataDir, logger);

    // Set up registration.
    var registration = mock(EnchantableRegistration.class);
    doAnswer(invocation ->
        new EnchantableBlock(registration, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)) {})
        .when(registration).newBlock(any(), any(), any());
    doReturn(registration).when(registry).get(goodMat);
    doReturn(Set.of(goodMat)).when(registration).getMaterials();

    // Set up config for disabled world.
    backingConfig = new YamlConfiguration();
    backingConfig.set(DISABLED_WORLD_PATH, false);
    var config = new EnchantableBlockConfig(backingConfig) {};
    doReturn(config).when(registration).getConfig();

    // Reset block types
    block.setType(goodMat);
    blockDisabledWorld.setType(goodMat);
  }

  @AfterEach
  void afterEach() throws Exception {
    mockAnnotations.close();
  }

  @AfterAll
  void afterAll() {
    ServerMocks.unsetBukkitServer();
  }

  @DisplayName("Registry is obtainable for type registration.")
  @Test
  void testGetRegistry() {
    assertThat("Registry is same instance", manager.getRegistry(), is(registry));
  }

  @DisplayName("Tests for block creation and retrieval.")
  @Nested
  class BlockRetrieveTest {

    @DisplayName("Null items do not create blocks.")
    @Test
    void testCreateItemNull() {
      assertThat(
          "Manager must not create block",
          manager.createBlock(block, null),
          is(nullValue()));
      verify(registry, times(0)).get(any());
    }

    @DisplayName("Empty items do not create blocks.")
    @Test
    void testCreateItemAir() {
      var item = new ItemStack(Material.AIR);
      assertThat(
          "Manager must not create block",
          manager.createBlock(block, item),
          is(nullValue()));
      verify(registry, times(0)).get(any());
    }

    @DisplayName("Items that do not have a corresponding block do not create blocks.")
    @Test
    void testCreateItemNotBlock() {
      var item = new ItemStack(Material.DIAMOND);
      assertThat(
          "Manager must not create block",
          manager.createBlock(block, item),
          is(nullValue()));
      verify(registry, times(0)).get(any());
    }

    @DisplayName("Unenchanted items do not create blocks.")
    @Test
    void testCreateItemUnenchanted() {
      var item = new ItemStack(goodMat);
      assertThat(
          "Manager must not create block",
          manager.createBlock(block, item),
          is(nullValue()));
      verify(registry, times(0)).get(any());
    }

    @DisplayName("Items with no registration do not create blocks.")
    @Test
    void testCreateNoRegistration() {
      var item = getValidItem();
      doReturn(null).when(registry).get(goodMat);

      assertThat(
          "Manager must not create block",
          manager.createBlock(block, item),
          is(nullValue()));
      verify(registry).get(any());
    }

    @DisplayName("Blocks cannot be created in disabled worlds.")
    @Test
    void testCreateDisabledWorld() {
      var item = getValidItem();

      assertThat(
          "Manager must not create block",
          manager.createBlock(blockDisabledWorld, item),
          is(nullValue()));
      verify(registry).get(any());
    }

    @DisplayName("Valid items must create blocks.")
    @Test
    void testCreate() {
      var item = getValidItem();
      EnchantableBlock enchantableBlock = manager.createBlock(block, item);
      assertThat("Manager must create block", enchantableBlock, is(notNullValue()));
      assertThat(
          "Item should match creation stack",
          enchantableBlock.getItemStack(),
          similar(item));
    }

    @DisplayName("Unset block returns null.")
    @Test
    void testGetUnset() {
      assertThat("Unset block is null", manager.getBlock(block), is(nullValue()));
    }

    @DisplayName("Valid block is returned.")
    @Test
    void testGet() {
      ItemStack stack = getValidItem();
      EnchantableBlock enchantableBlock = manager.createBlock(block, stack);

      assertThat("Manager must create block", enchantableBlock, is(notNullValue()));
      assertThat(
          "Enabled valid block must be retrievable",
          manager.getBlock(block),
          is(enchantableBlock));
    }

    @DisplayName("Valid block in disabled world returns null.")
    @Test
    void testGetDisabled() {
      var item = getValidItem();
      backingConfig.set(DISABLED_WORLD_PATH, true);
      manager.getRegistry().reload();
      var enchantableBlock = manager.createBlock(blockDisabledWorld, item);
      assertThat("Manager must create block", enchantableBlock, is(notNullValue()));
      assertThat(
          "Enabled valid block must be retrievable",
          manager.getBlock(blockDisabledWorld),
          is(enchantableBlock));

      backingConfig.set(DISABLED_WORLD_PATH, false);
      manager.getRegistry().reload();
      assertThat(
          "Valid block in disabled world must return null",
          manager.getBlock(blockDisabledWorld),
          is(nullValue()));
    }

  }

  @DisplayName("Tests for block data management and destruction.")
  @Nested
  class DataManagementTest {

    private static Field cacheField;
    private Cache<Region, RegionStorageData> saveFileCache;

    @BeforeAll
    static void beforeAll() throws NoSuchFieldException {
      // This is hacky, but I'd argue that it's better practice than a VisibleForTesting annotation.
      cacheField = EnchantableBlockManager.class.getDeclaredField("saveFileCache");
      cacheField.setAccessible(true);
    }

    @BeforeEach
    void beforeEach() throws IllegalAccessException {
      saveFileCache = (Cache<Region, RegionStorageData>) cacheField.get(manager);
    }

    @DisplayName("Destroying undefined blocks returns null.")
    @Test
    void testBlockUndefined() {
      assertThat(
          "Undefined block should return null",
          manager.destroyBlock(block),
          nullValue());
    }

    @DisplayName("Destroying blocks with null save data returns null.")
    @Test
    void testNullData() {
      ItemStack stack = getValidItem();
      EnchantableBlock enchantableBlock = manager.createBlock(block, stack);
      assertThat("Manager must create block", enchantableBlock, is(notNullValue()));

      // Inject null data. This should never be possible, but we should handle it gracefully.
      saveFileCache.put(new Region(block), null);

      assertThat(
          "Null save data should return null",
          manager.destroyBlock(block),
          nullValue());
    }

    @DisplayName("Destroying blocks with invalid type returns null.")
    @Test
    void testBlockWrongType() {
      ItemStack stack = getValidItem();
      EnchantableBlock enchantableBlock = manager.createBlock(block, stack);
      assertThat("Manager must create block", enchantableBlock, is(notNullValue()));

      block.setType(badMat);

      assertThat(
          "Invalid save data should return null for invalid block type",
          manager.destroyBlock(block),
          nullValue());
    }

    @DisplayName("Destroying blocks with invalid data should return in-memory item if possible.")
    @Test
    void testInvalidData() {
      ItemStack stack = getValidItem();
      EnchantableBlock enchantableBlock = manager.createBlock(block, stack);
      assertThat("Manager must create block", enchantableBlock, is(notNullValue()));

      RegionStorageData storageData = saveFileCache.get(new Region(block));

      assertThat("Storage data must be present", storageData, is(notNullValue()));

      String chunkPath = EnchantableBlockManager.getChunkPath(block);
      storageData.getStorage().set(chunkPath, "not a section");

      assertThat(
          "Invalid save data should still return in-memory item if available",
          manager.destroyBlock(block),
          similar(stack));
    }

    @DisplayName("Destroying valid blocks should return creation item.")
    @Test
    void testValidBlock() {
      ItemStack stack = getValidItem();
      var enchantableBlock = manager.createBlock(block, stack);
      assertThat("Manager must create block", enchantableBlock, is(notNullValue()));

      assertThat(
          "Valid block should return creation item",
          manager.destroyBlock(block),
          similar(stack));
    }

    private void setUpChunks() {
      chunk = block.getChunk();
      Region region = new Region(chunk);
      RegionStorage storage = Objects.requireNonNull(saveFileCache.get(region))
          .getStorage();
      chunkBad = block.getWorld().getChunkAt(chunk.getX() + 1, chunk.getZ() + 1);
      var blockBad = block.getWorld().getBlockAt(Coords.chunkToBlock(chunkBad.getX()), 0,
          Coords.chunkToBlock(chunkBad.getZ()));
      storage.set(EnchantableBlockManager.getChunkPath(blockBad), null);
      var section = storage.getConfigurationSection(EnchantableBlockManager.getChunkPath(block));
      if (section == null) {
        section = storage.createSection(EnchantableBlockManager.getChunkPath(block));
      }
      section.set("badpath", "not a config section");
      section.set("bad block path.stuff", "value");
      section.set("bad_block_path.stuff", "extreme value");
      section.set("1_1_1", "bad value");
      section.set("1_2_1.itemstack", "bad value");
      ItemStack stack = getValidItem();
      section.set(EnchantableBlockManager.getBlockPath(block) + ".itemstack", stack);
      block.setType(stack.getType());
      section.set("0_1_0.itemstack", stack);
    }

    @DisplayName("Invalid data is handled gracefully and logged when loading.")
    @Test
    void testLoadChunkBlocks() {
      setUpChunks();

      doNothing().when(logger).warning(loggerCaptor.capture());

      manager.loadChunkBlocks(chunk);
      verify(logger, times(6)).warning(ArgumentMatchers.<Supplier<String>>any());

      List<String> warnings = loggerCaptor.getAllValues().stream().map(Supplier::get).toList();
      assertThat(
          "Expected 2 non-ConfigurationSetting values",
          warnings.stream().filter(value -> value.startsWith("Invalid ConfigurationSection"))
              .count(),
          is(2L));
      assertThat(
          "Expected 2 unparseable coordinates",
          warnings.stream().filter(value -> value.startsWith("Unparseable coordinates")).count(),
          is(2L));
      assertThat(
          "Expected 2 invalid items or blocks",
          warnings.stream().filter(value -> value.startsWith("Removed invalid save")).count(),
          is(2L));

      var enchantableBlock = manager.getBlock(block);
      assertThat("Block must be loaded", enchantableBlock, is(notNullValue()));
      assertDoesNotThrow(() -> manager.loadChunkBlocks(chunkBad));
    }

    @DisplayName("Invalid data is handled gracefully when chunks are unloaded.")
    @Test
    void testUnloadChunkBlocks() {
      setUpChunks();

      ItemStack stack = getValidItem();
      manager.createBlock(block, stack);

      assertDoesNotThrow(() -> manager.unloadChunkBlocks(chunk));
      assertThat("Block must be unloaded", manager.getBlock(block), is(nullValue()));
      assertDoesNotThrow(() -> manager.unloadChunkBlocks(chunkBad));
    }

    @DisplayName("Data is removed from cache when expired.")
    @Test
    void testExpireCache() {
      // Don't bother fetching via block so that chunks are guaranteed unloaded.
      Region key = new Region("not_a_world", 0, 0);
      RegionStorageData storage = saveFileCache.get(key);

      assertThat("Cached value must not be null", storage, is(notNullValue()));

      manager.expireCache();
      storage = saveFileCache.get(key, false);

      assertThat("Cache must be cleaned after values expire", storage, is(nullValue()));
    }

    @DisplayName("Regional data holder manages dirty state for blocks.")
    @Test
    void testDataHolder() {
      var regionStorage = spy(new RegionStorage(dataDir, new Region(block)));
      var data = manager.new RegionStorageData(regionStorage);
      assertThat("New data should not be dirty", data.isDirty(), is(false));
      verify(regionStorage, times(2)).getRegion();

      data.setDirty();
      assertThat("Data must be dirty once set", data.isDirty());
      // Verify that once dirty state is set, isDirty uses set state.
      verify(regionStorage, times(2)).getRegion();

      data.clean();
      assertThat("Data must not be dirty after clean", data.isDirty(), is(false));

      data = manager.new RegionStorageData(new RegionStorage(dataDir, new Region(block)));
      block.setType(goodMat);
      ItemStack stack = getValidItem();
      var enchantableBlock = manager.createBlock(block, stack);
      assertThat("EnchantableBlock must not be null", enchantableBlock, notNullValue());
      enchantableBlock.setDirty(true);
      assertThat("Block must be dirty", enchantableBlock.isDirty());
      assertThat("Data must be dirty if blocks are dirty", data.isDirty());
      data.clean();
      assertThat("Data must not be dirty after clean", data.isDirty(), is(false));
      assertThat("Block must be dirty", enchantableBlock.isDirty(), is(false));
    }
  }

  private @NotNull ItemStack getValidItem() {
    ItemStack itemStack = new ItemStack(goodMat);
    itemStack.addUnsafeEnchantment(goodEnchant, 1);
    return itemStack;
  }

}