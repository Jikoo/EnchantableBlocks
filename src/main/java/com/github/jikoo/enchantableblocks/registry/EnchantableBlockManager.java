package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.util.Cache;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import com.github.jikoo.planarwrappers.collections.BlockMap;
import com.github.jikoo.planarwrappers.util.Coords;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * A management system for {@link EnchantableBlock EnchantableBlocks}.
 */
public class EnchantableBlockManager {

  private final @NotNull Logger logger;
  private final @NotNull EnchantableBlockRegistry blockRegistry;
  private final @NotNull BlockMap<EnchantableBlock> blockMap;
  private final @NotNull Cache<Region, RegionStorageData> saveFileCache;

  /**
   * Construct a new {@code EnchantableBlockManager} for the given {@link Plugin}.
   *
   * @param plugin the {@code Plugin}
   */
  public EnchantableBlockManager(@NotNull Plugin plugin) {
    this(
        new EnchantableBlockRegistry(plugin),
        new Cache.CacheBuilder<>(),
        plugin.getConfig().getInt("autosave", 5),
        plugin.getDataFolder().toPath().resolve("data"),
        plugin.getLogger());
  }

  @VisibleForTesting
  EnchantableBlockManager(
      @NotNull EnchantableBlockRegistry registry,
      @NotNull Cache.CacheBuilder<Region, RegionStorageData> cacheBuilder,
      int autoSave,
      @NotNull Path dataDir,
      @NotNull Logger logger) {
    this.blockMap = new BlockMap<>();
    this.logger = logger;
    this.blockRegistry = registry;
    this.saveFileCache = cacheBuilder
        .withRetention(Math.max(autoSave * 60_000L, 60_000L))
        .withInUseCheck(new RegionInUseCheck(logger))
        .withLoadFunction(new RegionLoadFunction(this, dataDir, logger)).build();
  }

  /**
   * Get the {@link EnchantableBlockRegistry} belonging to the manager.
   *
   * @return the registry
   */
  public @NotNull EnchantableBlockRegistry getRegistry() {
    return this.blockRegistry;
  }

  /**
   * Get an {@link EnchantableBlock} by {@link Block}.
   *
   * @param block the {@code Block}
   * @return the {@code EnchantableBlock} or {@code null} the {@code Block} is not stored
   */
  public @Nullable EnchantableBlock getBlock(@NotNull final Block block) {

    EnchantableBlock enchantableBlock = this.blockMap.get(block);
    if (enchantableBlock != null
        && enchantableBlock.getConfig().enabled.get(block.getWorld().getName())) {
      return enchantableBlock;
    }

    return null;

  }

  /**
   * Create an {@link EnchantableBlock} for a {@link Block} from an {@link ItemStack}.
   *
   * <p>Note that this will override existing {@link EnchantableBlock EnchantableBlocks} without
   * warning.
   *
   * @param block the {@code Block}
   * @param itemStack the {@code ItemStack}
   * @return the {@code EnchantableBlock} or {@code null} if not created
   */
  public @Nullable EnchantableBlock createBlock(
      @NotNull final Block block,
      @Nullable final ItemStack itemStack) {

    if (isInvalidBlock(itemStack)) {
      return null;
    }

    final EnchantableBlock enchantableBlock = this.newBlock(block, itemStack);

    if (enchantableBlock == null) {
      return null;
    }

    this.blockMap.put(block, enchantableBlock);

    return enchantableBlock;
  }

  /**
   * Helper method for ensuring an {@link ItemStack} can be used to create an
   * {@link EnchantableBlock}.
   *
   * @param itemStack the {@code ItemStack}
   * @return true if the {@code ItemStack} is an invalid type
   */
  @Contract("null -> true")
  private boolean isInvalidBlock(@Nullable ItemStack itemStack) {
    return itemStack == null
        || itemStack.getType().isAir()
        || !itemStack.getType().isBlock()
        || itemStack.getEnchantments().isEmpty();
  }

  /**
   * Create an {@link EnchantableBlock} for a {@link Block} from an {@link ItemStack}.
   *
   * @param block the {@code Block}
   * @param itemStack the {@code ItemStack}
   * @return the {@code EnchantableBlock} or {@code null} if no registration matches
   */
  private @Nullable EnchantableBlock newBlock(
      @NotNull Block block,
      @NotNull ItemStack itemStack) {
    var registration = blockRegistry.get(itemStack.getType());

    if (registration == null) {
      return null;
    }

    if (!registration.getConfig().enabled.get(block.getWorld().getName())) {
      return null;
    }

    return registration.newBlock(block, itemStack, getBlockStorage(block));
  }

  /**
   * Helper method for getting a non-null {@link ConfigurationSection} for a {@link Block}, creating
   * as needed.
   *
   * @param block the {@code Block}
   * @return the {@code ConfigurationSection}
   */
  private @NotNull ConfigurationSection getBlockStorage(@NotNull Block block) {
    var chunkStorage = this.getChunkStorage(block);
    var blockPath = getBlockPath(block);

    if (chunkStorage.isConfigurationSection(blockPath)) {
      return Objects.requireNonNull(chunkStorage.getConfigurationSection(blockPath));
    }

    return chunkStorage.createSection(blockPath);
  }

  /**
   * Helper method for getting a non-null {@link ConfigurationSection} for a {@link Chunk}, creating
   * as needed.
   *
   * @param block a {@link Block} in the {@link Chunk}
   * @return the {@code ConfigurationSection}
   */
  private @NotNull ConfigurationSection getChunkStorage(@NotNull Block block) {
    var storagePair = saveFileCache.get(new Region(block));
    var regionStorage = Objects.requireNonNull(storagePair).getStorage();
    var chunkPath = getChunkPath(block);

    if (regionStorage.isConfigurationSection(chunkPath)) {
      return Objects.requireNonNull(regionStorage.getConfigurationSection(chunkPath));
    }

    return regionStorage.createSection(chunkPath);
  }

  /**
   * Load an {@link EnchantableBlock} from storage.
   *
   * @param block the {@link Block}
   * @param storage the {@link ConfigurationSection} to load the {@code EnchantableBlock} from
   * @return the {@code EnchantableBlock} or {@code null} if invalid
   */
  private @Nullable EnchantableBlock loadEnchantableBlock(
      @NotNull final Block block,
      @NotNull final ConfigurationSection storage) {
    ItemStack itemStack = storage.getItemStack("itemstack");

    if (isInvalidBlock(itemStack)) {
      return null;
    }

    EnchantableBlock enchantableBlock = this.newBlock(block, itemStack);

    if (enchantableBlock == null || !enchantableBlock.isCorrectBlockType()
        || !enchantableBlock.getConfig().enabled.get(
        block.getWorld().getName())) {
      return null;
    }

    return enchantableBlock;
  }

  /**
   * Remove an {@link EnchantableBlock}.
   *
   * @param block the {@link Block} representing an {@code EnchantableBlock}
   * @return the {@link ItemStack} representation or {@code null} if not valid
   */
  public @Nullable ItemStack destroyBlock(@NotNull final Block block) {
    EnchantableBlock enchantableBlock = this.blockMap.remove(block);

    if (enchantableBlock == null) {
      return null;
    }

    var saveData = this.saveFileCache.get(new Region(block));

    if (saveData == null) {
      return null;
    }

    var chunkPath = getChunkPath(block);

    ItemStack itemStack = enchantableBlock.getItemStack();

    if (!saveData.getStorage().isConfigurationSection(chunkPath)) {
      saveData.getStorage().set(chunkPath, null);
      saveData.setDirty();

      if (!enchantableBlock.isCorrectType(block.getType())) {
        return null;
      }

      return itemStack;
    }

    var chunkSection = saveData.getStorage().getConfigurationSection(chunkPath);
    var blockPath = getBlockPath(block.getX(), block.getY(), block.getZ());

    if (chunkSection != null) {
      // Delete block data.
      chunkSection.set(blockPath, null);

      // If chunk section is now empty, also delete chunk section.
      if (chunkSection.getKeys(false).isEmpty()) {
        saveData.getStorage().set(chunkPath, null);
      }
    }

    saveData.setDirty();

    if (!enchantableBlock.isCorrectType(block.getType())) {
      return null;
    }

    return itemStack;
  }

  /**
   * Load all stored {@link EnchantableBlock EnchantableBlocks} for a {@link Chunk}.
   *
   * @param chunk the {@code Chunk}
   */
  public void loadChunkBlocks(@NotNull final Chunk chunk) {

    RegionStorageData saveData = this.saveFileCache.get(new Region(chunk), false);

    if (saveData == null) {
      return;
    }

    String path = getChunkPath(chunk);
    ConfigurationSection chunkStorage = saveData.getStorage().getConfigurationSection(path);

    if (chunkStorage == null) {
      return;
    }

    for (String xyz : chunkStorage.getKeys(false)) {
      if (!chunkStorage.isConfigurationSection(xyz)) {
        chunkStorage.set(path, null);
        saveData.setDirty();
        this.logger.warning(() -> String.format(
            "Invalid ConfigurationSection %s: %s",
            xyz,
            chunkStorage.get(xyz)));
        continue;
      }

      String itemPath = xyz + ".itemstack";
      String[] split = xyz.split("_");
      Block block;

      if (split.length != 3) {
        chunkStorage.set(xyz, null);
        saveData.setDirty();
        this.logger.warning(() -> String.format(
            "Unparseable coordinates in %s: %s representing %s",
            chunk.getWorld().getName(),
            xyz,
            chunkStorage.getItemStack(itemPath)));
        continue;
      }

      try {
        block = chunk.getWorld()
            .getBlockAt(
                Integer.parseInt(split[0]),
                Integer.parseInt(split[1]),
                Integer.parseInt(split[2]));
      } catch (@NotNull NumberFormatException e) {
        chunkStorage.set(xyz, null);
        saveData.setDirty();
        this.logger.warning(() -> String.format(
            "Unparseable coordinates in %s: %s representing %s",
            chunk.getWorld().getName(),
            xyz,
            chunkStorage.getItemStack(itemPath)));
        continue;
      }

      var enchantableBlock = this.loadEnchantableBlock(
          block,
          Objects.requireNonNull(chunkStorage.getConfigurationSection(xyz)));

      if (enchantableBlock == null) {
        // Invalid EnchantableBlock, could not load.
        chunkStorage.set(xyz, null);
        saveData.setDirty();
        this.logger.warning(() -> String.format(
            "Removed invalid save in %s at %s: %s",
            chunk.getWorld().getName(),
            block.getLocation().toVector(),
            chunkStorage.getItemStack(itemPath)));
        continue;
      }

      this.blockMap.put(block, enchantableBlock);
    }
  }

  /**
   * Unload all stored {@link EnchantableBlock EnchantableBlocks} for a {@link Chunk}.
   *
   * @param chunk the {@code Chunk}
   */
  public void unloadChunkBlocks(@NotNull final Chunk chunk) {
    // Clear out and clean up loaded EnchantableBlocks.
    this.blockMap.remove(chunk);
  }

  /**
   * Expire all values in the save file cache.
   */
  public void expireCache() {
    saveFileCache.expireAll();
  }

  /**
   * Get the path for a {@link Chunk Chunk's} {@link ConfigurationSection} from a {@link Block}.
   *
   * @param block the {@code Block}
   * @return the path
   */
  @VisibleForTesting
  static @NotNull String getChunkPath(@NotNull Block block) {
    return getChunkPath(Coords.blockToChunk(block.getX()), Coords.blockToChunk(block.getZ()));
  }

  /**
   * Get the path for a {@link Chunk Chunk's} {@link ConfigurationSection}.
   *
   * @param chunk the {@code Block}
   * @return the path
   */
  private static @NotNull String getChunkPath(@NotNull Chunk chunk) {
    return getChunkPath(chunk.getX(), chunk.getZ());
  }

  /**
   * Get the path for a {@link Chunk Chunk's} {@link ConfigurationSection} from chunk coordinates.
   *
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   * @return the path
   */
  private static @NotNull String getChunkPath(int chunkX, int chunkZ) {
    return chunkX + "_" + chunkZ;
  }

  /**
   * Get the path for a {@link Block Block's} {@link ConfigurationSection}.
   *
   * @param block the {@code Block}
   * @return the path
   */
  @VisibleForTesting
  static @NotNull String getBlockPath(@NotNull Block block) {
    return getBlockPath(block.getX(), block.getY(), block.getZ());
  }

  /**
   * Get the path for a {@link Block Block's} {@link ConfigurationSection} from coordinates.
   *
   * @param x the X coordinate
   * @param y the Y coordinate
   * @param z the Z coordinate
   * @return the path
   */
  @VisibleForTesting
  static @NotNull String getBlockPath(int x, int y, int z) {
    return x + "_" + y + "_" + z;
  }

  /**
   * Container for ensuring that {@link RegionStorage} files are saved as necessary.
   */
  class RegionStorageData {

    private final @NotNull RegionStorage storage;
    private boolean dirty = false;

    /**
     * Construct a new {@code RegionStorageData}.
     *
     * @param storage the {@link RegionStorage}
     */
    RegionStorageData(@NotNull RegionStorage storage) {
      this.storage = storage;
    }

    /**
     * Get the {@link RegionStorage} stored.
     *
     * @return the {@code RegionStorage}
     */
    public @NotNull RegionStorage getStorage() {
      return storage;
    }

    /**
     * Check if the {@link RegionStorage} has unsaved changes.
     *
     * @return true if the {@code RegionStorage} needs to be saved
     */
    boolean isDirty() {
      if (dirty) {
        return true;
      }
      final String worldName = storage.getRegion().worldName();
      dirty = storage.getRegion().anyChunkMatch((chunkX, chunkZ) ->
          blockMap.get(worldName, chunkX, chunkZ).stream()
              .anyMatch(EnchantableBlock::isDirty));
      return dirty;
    }

    /**
     * Flag the {@link RegionStorage} as having unsaved changes.
     */
    public void setDirty() {
      this.dirty = true;
    }

    /**
     * Mark the {@link RegionStorage} and all contained {@link EnchantableBlock EnchantableBlocks}
     * as having been saved since last modification.
     */
    void clean() {
      this.dirty = false;
      final String worldName = storage.getRegion().worldName();
      this.storage.getRegion().forEachChunk((chunkX, chunkZ) ->
          blockMap.get(worldName, chunkX, chunkZ)
              .forEach(enchantableBlock -> enchantableBlock.setDirty(false)));
    }
  }

}
