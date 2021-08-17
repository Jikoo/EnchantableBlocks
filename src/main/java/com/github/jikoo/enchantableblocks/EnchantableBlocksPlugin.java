package com.github.jikoo.enchantableblocks;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.enchanting.AnvilEnchanter;
import com.github.jikoo.enchantableblocks.enchanting.TableEnchanter;
import com.github.jikoo.enchantableblocks.listener.FurnaceListener;
import com.github.jikoo.enchantableblocks.listener.WorldListener;
import com.github.jikoo.enchantableblocks.util.Cache;
import com.github.jikoo.enchantableblocks.util.Cache.CacheBuilder;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import com.github.jikoo.planarwrappers.collections.BlockMap;
import com.github.jikoo.planarwrappers.tuple.Pair;
import com.github.jikoo.planarwrappers.tuple.Triple;
import com.github.jikoo.planarwrappers.util.Coords;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Bukkit plugin for adding effects to block based on enchantments.
 *
 * @author Jikoo
 */
public class EnchantableBlocksPlugin extends JavaPlugin {

	private final EnchantableBlockRegistry blockRegistry = new EnchantableBlockRegistry();
	private final BlockMap<EnchantableBlock> blockMap = new BlockMap<>();
	private Cache<Triple<World, Integer, Integer>, Pair<RegionStorage, Boolean>> saveFileCache;

	public EnchantableBlocksPlugin() {
		super();
	}

	public EnchantableBlocksPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
		super(loader, description, dataFolder, file);
	}

	@Override
	public void onEnable() {
		this.saveFileCache = new CacheBuilder<Triple<World, Integer, Integer>, Pair<RegionStorage, Boolean>>()
				.withRetention(Math.max(this.getConfig().getInt("autosave", 5) * 60_000L, 60_000L))
				.withInUseCheck((key, value) -> {
					if (value == null) {
						return false;
					}

					RegionStorage storage = value.getLeft();
					int minChunkX = Coords.regionToChunk(storage.getRegionX());
					int minChunkZ = Coords.regionToChunk(storage.getRegionZ());
					boolean loaded = false, dirty = value.getRight();

					// Check if chunks are loaded or dirty
					chunkCheck: for (int chunkX = minChunkX, maxChunkX = minChunkX + 32; chunkX < maxChunkX; ++chunkX) {
						for (int chunkZ = minChunkZ, maxChunkZ = minChunkZ + 32; chunkZ < maxChunkZ; ++chunkZ) {
							loaded = loaded || storage.getWorld().isChunkLoaded(chunkX, chunkZ);
							dirty = dirty || blockMap.get(storage.getWorld().getName(), chunkX, chunkZ).stream()
									.anyMatch(EnchantableBlock::isDirty);
							if (dirty && loaded) {
								break chunkCheck;
							}
						}
					}

					if (!dirty) {
						return loaded;
					}

					Collection<String> keys = storage.getKeys(true);
					boolean delete = true;
					for (String path : keys) {
						if (storage.get(path) != null) {
							delete = false;
							break;
						}
					}

					if (delete) {
						//noinspection ResultOfMethodCallIgnored
						storage.getDataFile().delete();
						return loaded;
					}

					try {
						storage.save();
						for (int chunkX = minChunkX, maxChunkX = minChunkX + 32; chunkX < maxChunkX; ++chunkX) {
							for (int chunkZ = minChunkZ, maxChunkZ = minChunkZ + 32; chunkZ < maxChunkZ; ++chunkZ) {
								blockMap.get(key.getLeft().getName(), chunkX, chunkZ).forEach(
										enchantableBlock -> enchantableBlock.setDirty(false));
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

					return loaded;
				}).withLoadFunction((key, create) -> {
					RegionStorage storage = new RegionStorage(EnchantableBlocksPlugin.this, key.getLeft(), key.getMiddle(), key.getRight());
					if (!storage.getDataFile().exists() && !create) {
						return null;
					}
					try {
						storage.load();
					} catch (IOException | InvalidConfigurationException e) {
						e.printStackTrace();
					}
					return new Pair<>(storage, false);
				}).build();

		this.saveDefaultConfig();
		this.registerEnchantableBlocks();

		this.getServer().getPluginManager().registerEvents(new FurnaceListener(this), this);
		this.getServer().getPluginManager().registerEvents(new WorldListener(this), this);
		this.getServer().getPluginManager().registerEvents(new TableEnchanter(this), this);
		this.getServer().getPluginManager().registerEvents(new AnvilEnchanter(this), this);

		this.loadEnchantableBlocks();

	}

	private void registerEnchantableBlocks() {
		getRegistry().register(
				EnchantableFurnace.MATERIALS,
				EnchantableFurnace.class,
				EnchantableFurnace.ENCHANTMENTS,
				EnchantableFurnace::getConfig,
				EnchantableFurnace::clearCache);
	}

	@Override
	public void onDisable() {
		this.getServer().getScheduler().cancelTasks(this);
		this.saveFileCache.expireAll();
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length < 1 || !args[0].equalsIgnoreCase("reload")) {
			sender.sendMessage("EnchantableBlocks v" + getDescription().getVersion());
			return false;
		}

		reloadConfig();
		getRegistry().reload();
		sender.sendMessage("[EnchantableBlocks v" + getDescription().getVersion() + "] Reloaded config and recipe cache.");
		return true;
	}

	@SuppressWarnings("UnusedReturnValue")
	public @Nullable EnchantableBlock createEnchantableBlock(@NotNull final Block block, @NotNull final ItemStack itemStack) {

		if (itemStack.getType() == Material.AIR || itemStack.getEnchantments().isEmpty()) {
			return null;
		}

		final EnchantableBlock enchantableBlock = this.getEnchantableBlock(block, itemStack);

		if (enchantableBlock == null) {
			return null;
		}

		this.blockMap.put(block, enchantableBlock);

		return enchantableBlock;
	}

	/**
	 * Remove an EnchantableBlock.
	 *
	 * @param block the EnchantableBlock
	 * @return the ItemStack representation of the EnchantableBlock or null if the Block was not a valid EnchantableBlock
	 */
	public @Nullable ItemStack destroyEnchantableBlock(@NotNull final Block block) {
		EnchantableBlock enchantableBlock = this.blockMap.remove(block);

		if (enchantableBlock == null || !enchantableBlock.isCorrectType(block.getType())) {
			return null;
		}

		int regionX = Coords.blockToRegion(block.getX());
		int regionZ = Coords.blockToRegion(block.getZ());
		Pair<RegionStorage, Boolean> saveData = this.saveFileCache
				.get(this.getRegionIdentifier(block.getWorld(), regionX, regionZ));

		if (saveData == null) {
			return null;
		}

		int chunkX = Coords.blockToChunk(block.getX());
		int chunkZ = Coords.blockToChunk(block.getZ());

		String chunkPath = chunkX + "_" + chunkZ;

		ItemStack itemStack = enchantableBlock.getItemStack();
		if (itemStack.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
			// Silk time isn't supposed to be preserved when broken.
			itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
		}

		if (!saveData.getLeft().isConfigurationSection(chunkPath)) {
			saveData.getLeft().set(chunkPath, null);
			return itemStack;
		}

		ConfigurationSection chunkSection = saveData.getLeft().getConfigurationSection(chunkPath);
		String coordPath = block.getX() + "_" + block.getY() + "_" + block.getZ();

		Objects.requireNonNull(chunkSection).set(coordPath, null);

		if (chunkSection.getKeys(false).isEmpty()) {
			saveData.getLeft().set(chunkPath, null);
		}

		saveData.setRight(true);

		return itemStack;
	}

	/**
	 * Gets an EnchantableBlock by Block.
	 *
	 * @param block the Block
	 * @return the EnchantableBlock, or null the Block is not an enchanted block
	 */
	public @Nullable EnchantableBlock getEnchantableBlockByBlock(@NotNull final Block block) {

		EnchantableBlock enchantableBlock = this.blockMap.get(block);
		if (enchantableBlock != null
				&& getRegistry().getConfig(enchantableBlock.getClass()).enabled.get(block.getWorld().getName())) {
			return enchantableBlock;
		}

		return null;

	}

	/**
	 * Load all stored EnchantableBlocks in a chunk.
	 *
	 * @param chunk the Chunk
	 */
	public void loadChunkEnchantableBlocks(@NotNull final Chunk chunk) {

		Pair<RegionStorage, Boolean> saveData = this.saveFileCache.get(this.getRegionIdentifier(chunk.getWorld(),
				Coords.chunkToRegion(chunk.getX()), Coords.chunkToRegion(chunk.getZ())), false);

		if (saveData == null) {
			return;
		}

		String path = chunk.getX() + "_" + chunk.getZ();
		if (!saveData.getLeft().isConfigurationSection(path)) {
			return;
		}

		ConfigurationSection chunkStorage = saveData.getLeft().getConfigurationSection(path);

		for (String xyz : Objects.requireNonNull(chunkStorage).getKeys(false)) {
			if (!chunkStorage.isConfigurationSection(xyz)) {
				continue;
			}

			String[] split = xyz.split("_");
			Block block;

			try {
				block = chunk.getWorld().getBlockAt(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
			} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
				this.getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(split));
				continue;
			}

			EnchantableBlock enchantableBlock = this.loadEnchantableBlock(block, Objects.requireNonNull(chunkStorage.getConfigurationSection(xyz)));

			if (enchantableBlock == null) {
				// Invalid EnchantableBlock, could not load.
				this.getLogger().warning(String.format("Removed invalid save: %s at %s",
						chunkStorage.getItemStack(xyz + ".itemstack"), block.getLocation()));
				chunkStorage.set(xyz, null);
				saveData.setRight(true);
				continue;
			}

			this.blockMap.put(block, enchantableBlock);
		}
	}

	/**
	 * Load an EnchantableBlock from storage.
	 *
	 * @param block the Block
	 * @param storage the ConfigurationSection to load the EnchantableBlock from
	 * @return the EnchantableBlock, or null if the EnchantableBlock is not valid.
	 */
	private @Nullable EnchantableBlock loadEnchantableBlock(@NotNull final Block block, @NotNull final ConfigurationSection storage) {
		ItemStack itemStack = storage.getItemStack("itemstack");

		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return null;
		}

		EnchantableBlock enchantableBlock = this.getEnchantableBlock(block, itemStack);

		if (enchantableBlock == null || !enchantableBlock.isCorrectBlockType()
				|| !getRegistry().getConfig(enchantableBlock.getClass()).enabled.get(block.getWorld().getName())) {
			return null;
		}

		return enchantableBlock;
	}

	/**
	 * Create an EnchantableBlock from an ItemStack.
	 *
	 * @param block the Block this EnchantableBlock is attached to
	 * @param itemStack the ItemStack to create the
	 *
	 * @return the EnchantableBlock or null if no EnchantableBlock is valid for the given ItemStack
	 */
	private @Nullable EnchantableBlock getEnchantableBlock(@NotNull final Block block, @NotNull ItemStack itemStack) {
		Material type = itemStack.getType();
		itemStack = itemStack.clone();
		itemStack.setAmount(1);
		if (EnchantableFurnace.isApplicableMaterial(type)) {
			return new EnchantableFurnace(block, itemStack, getBlockStorage(block));
		}
		return null;
	}

	public EnchantableBlockRegistry getRegistry() {
		return this.blockRegistry;
	}

	public void unloadChunkEnchantableBlocks(@NotNull final Chunk chunk) {
		// Clear out and clean up loaded EnchantableBlocks.
		this.blockMap.remove(chunk);
	}

	private void loadEnchantableBlocks() {

		// Load all EnchantableBlocks for loaded chunks. This isn't too bad due to how the cache works.
		for (World world : this.getServer().getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				this.loadChunkEnchantableBlocks(chunk);
			}
		}

	}

	private @NotNull ConfigurationSection getChunkStorage(World world, int chunkX, int chunkZ) {
		Pair<RegionStorage, Boolean> storagePair = saveFileCache.get(getRegionIdentifier(world,
				Coords.chunkToRegion(chunkX), Coords.chunkToRegion(chunkZ)));
		ConfigurationSection regionStorage = Objects.requireNonNull(storagePair).getLeft();
		String chunkPath = chunkX + "_" + chunkZ;

		if (regionStorage.isConfigurationSection(chunkPath)) {
			return Objects.requireNonNull(regionStorage.getConfigurationSection(chunkPath));
		}

		return regionStorage.createSection(chunkPath);
	}

	private @NotNull ConfigurationSection getBlockStorage(Block block) {
		ConfigurationSection chunkStorage = this.getChunkStorage(block.getWorld(),
				Coords.blockToChunk(block.getX()),
				Coords.blockToChunk(block.getZ()));
		String blockPath = block.getX() + "_" + block.getY() + "_" + block.getZ();

		if (chunkStorage.isConfigurationSection(blockPath)) {
			return Objects.requireNonNull(chunkStorage.getConfigurationSection(blockPath));
		}

		return chunkStorage.createSection(blockPath);
	}

	private Triple<World, Integer, Integer> getRegionIdentifier(World world, int regionX, int regionZ) {
		return new Triple<>(world, regionX, regionZ);
	}

}
