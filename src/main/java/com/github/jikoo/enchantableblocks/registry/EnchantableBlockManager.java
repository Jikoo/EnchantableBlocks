package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.util.Cache;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import com.github.jikoo.planarwrappers.collections.BlockMap;
import com.github.jikoo.planarwrappers.util.Coords;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class EnchantableBlockManager {

  private final @NotNull Logger logger;
  private final @NotNull EnchantableBlockRegistry blockRegistry;
  private final @NotNull BlockMap<EnchantableBlock> blockMap;
  @VisibleForTesting
  final @NotNull Cache<Region, RegionStorageData> saveFileCache;

  public EnchantableBlockManager(@NotNull Plugin plugin) {
    this.logger = plugin.getLogger();
    blockRegistry = new EnchantableBlockRegistry(plugin);
    blockMap = new BlockMap<>();

    saveFileCache = new Cache.CacheBuilder<Region, RegionStorageData>()
        .withRetention(Math.max(plugin.getConfig().getInt("autosave", 5) * 60_000L, 60_000L))
        .withInUseCheck(new RegionInUseCheck(this.logger))
        .withLoadFunction(new RegionLoadFunction(plugin, this)).build();
  }

  public @NotNull EnchantableBlockRegistry getRegistry() {
    return this.blockRegistry;
  }

  /**
   * Get an EnchantableBlock by Block.
   *
   * @param block the Block
   * @return the EnchantableBlock, or null the Block is not an enchanted block
   */
  public @Nullable EnchantableBlock getBlock(@NotNull final Block block) {

    EnchantableBlock enchantableBlock = this.blockMap.get(block);
    if (enchantableBlock != null
        && enchantableBlock.getConfig().enabled.get(block.getWorld().getName())) {
      return enchantableBlock;
    }

    return null;

  }

  public @Nullable EnchantableBlock createBlock(
      @NotNull final Block block,
      @NotNull final ItemStack itemStack) {

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
   * Helper method for ensuring an {@link ItemStack} is a block.
   *
   * @param itemStack the item
   * @return true if the item is not a block
   */
  private boolean isInvalidBlock(@Nullable ItemStack itemStack) {
    return itemStack == null
        ||itemStack.getType().isAir()
        || !itemStack.getType().isBlock()
        || itemStack.getEnchantments().isEmpty();
  }

  /**
   * Create an EnchantableBlock from an ItemStack.
   *
   * @param block     the Block this EnchantableBlock is attached to
   * @param itemStack the ItemStack to create the
   * @return the EnchantableBlock or null if no EnchantableBlock is valid for the given ItemStack
   */
  private @Nullable EnchantableBlock newBlock(
      @NotNull final Block block,
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

  private @NotNull ConfigurationSection getBlockStorage(@NotNull Block block) {
    var chunkStorage = this.getChunkStorage(block);
    var blockPath = getBlockPath(block);

    if (chunkStorage.isConfigurationSection(blockPath)) {
      return Objects.requireNonNull(chunkStorage.getConfigurationSection(blockPath));
    }

    return chunkStorage.createSection(blockPath);
  }

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
   * Load an EnchantableBlock from storage.
   *
   * @param block   the Block
   * @param storage the ConfigurationSection to load the EnchantableBlock from
   * @return the EnchantableBlock, or null if the EnchantableBlock is not valid.
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
   * Remove an EnchantableBlock.
   *
   * @param block the EnchantableBlock
   * @return the ItemStack representation of the EnchantableBlock or null if the Block was not a
   * valid EnchantableBlock
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
    // TODO should this be an implementation-specific handler?
    if (itemStack.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
      // Silk time isn't supposed to be preserved when broken.
      itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
    }

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
   * Load all stored EnchantableBlocks in a chunk.
   *
   * @param chunk the Chunk
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

  public void unloadChunkBlocks(@NotNull final Chunk chunk) {
    // Clear out and clean up loaded EnchantableBlocks.
    this.blockMap.remove(chunk);
  }

  public void expireCache() {
    saveFileCache.expireAll();
  }

  @VisibleForTesting
  static @NotNull String getChunkPath(@NotNull Block block) {
    return getChunkPath(Coords.blockToChunk(block.getX()), Coords.blockToChunk(block.getZ()));
  }

  private static @NotNull String getChunkPath(@NotNull Chunk chunk) {
    return getChunkPath(chunk.getX(), chunk.getZ());
  }

  private static @NotNull String getChunkPath(int chunkX, int chunkZ) {
    return chunkX + "_" + chunkZ;
  }

  @VisibleForTesting
  static @NotNull String getBlockPath(@NotNull Block block) {
    return getBlockPath(block.getX(), block.getY(), block.getZ());
  }

  @VisibleForTesting
  static @NotNull String getBlockPath(int x, int y, int z) {
    return x + "_" + y + "_" + z;
  }

  class RegionStorageData {

    private final @NotNull RegionStorage storage;
    private boolean dirty = false;

    RegionStorageData(@NotNull RegionStorage storage) {
      this.storage = storage;
    }

    public @NotNull RegionStorage getStorage() {
      return storage;
    }

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

    public void setDirty() {
      this.dirty = true;
    }

    public void clean() {
      this.dirty = false;
      final String worldName = storage.getRegion().worldName();
      this.storage.getRegion().forEachChunk((chunkX, chunkZ) ->
          blockMap.get(worldName, chunkX, chunkZ)
              .forEach(enchantableBlock -> enchantableBlock.setDirty(false)));
    }
  }

}
