package com.github.jikoo.enchantableblocks;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.enchanting.AnvilEnchanter;
import com.github.jikoo.enchantableblocks.enchanting.TableEnchanter;
import com.github.jikoo.enchantableblocks.listener.FurnaceListener;
import com.github.jikoo.enchantableblocks.listener.WorldListener;
import com.github.jikoo.enchantableblocks.util.BlockMap;
import com.github.jikoo.enchantableblocks.util.Cache;
import com.github.jikoo.enchantableblocks.util.Cache.CacheBuilder;
import com.github.jikoo.enchantableblocks.util.CoordinateConversions;
import com.github.jikoo.enchantableblocks.util.Function;
import com.github.jikoo.enchantableblocks.util.LoadFunction;
import com.github.jikoo.enchantableblocks.util.Pair;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import com.github.jikoo.enchantableblocks.util.Triple;
import com.google.common.collect.HashMultimap;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A Bukkit plugin for adding effects to block based on enchantments.
 *
 * @author Jikoo
 */
public class EnchantableBlocksPlugin extends JavaPlugin {

	private final BlockMap<EnchantableBlock> blockMap = new BlockMap<>();
	private Cache<Triple<World, Integer, Integer>, Pair<RegionStorage, Boolean>> saveFileCache;

	private HashSet<Enchantment> enchantments;
	private ArrayList<String> fortuneList;
	private boolean isBlacklist;
	private HashMultimap<Enchantment, Enchantment> incompatibleEnchants;

	@Override
	public void onEnable() {
		this.saveFileCache = new CacheBuilder<Triple<World, Integer, Integer>, Pair<RegionStorage, Boolean>>()
				.withRetention(Math.max(this.getConfig().getInt("autosave", 5) * 6000, 6000L))
				.withInUseCheck(new Function<Triple<World, Integer, Integer>, Pair<RegionStorage, Boolean>>() {
					@Override
					public boolean run(final Triple<World, Integer, Integer> key, final Pair<RegionStorage, Boolean> value) {

						if (value == null) {
							return false;
						}

						RegionStorage storage = value.getLeft();
						int minChunkX = CoordinateConversions.regionToChunk(storage.getRegionX());
						int minChunkZ = CoordinateConversions.regionToChunk(storage.getRegionZ());
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
					}
				}).withLoadFunction(new LoadFunction<Triple<World, Integer, Integer>, Pair<RegionStorage, Boolean>>() {
					@Override
					public Pair<RegionStorage, Boolean> run(final Triple<World, Integer, Integer> key, final boolean create) {
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
					}
				}).build();

		this.updateConfig();

		ArrayList<String> disabledWorlds = new ArrayList<>();
		for (String worldName : this.getConfig().getStringList("disabled_worlds")) {
			if (!disabledWorlds.contains(worldName.toLowerCase())) {
				disabledWorlds.add(worldName.toLowerCase());
			}
		}

		this.isBlacklist = this.getConfig().getString("fortune_list_mode", "blacklist").matches(".*[Bb][Ll][Aa][Cc][Kk].*");

		this.fortuneList = new ArrayList<>();
		for (String next : this.getConfig().getStringList("fortune_list")) {
			next = next.toUpperCase();
			if (this.fortuneList.contains(next)) {
				continue;
			}
			Material m = Material.getMaterial(next);
			if (m == null) {
				this.getLogger().warning("No material by the name of \"" + next + "\" could be found!");
				this.getLogger().info("Please use material names listed in https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
			} else {
				this.fortuneList.add(m.name());
			}
		}

		HashSet<String> allowedEnchantments = new HashSet<>();
		allowedEnchantments.add("DIG_SPEED");
		allowedEnchantments.add("DURABILITY");
		allowedEnchantments.add("LOOT_BONUS_BLOCKS");
		allowedEnchantments.add("SILK_TOUCH");
		for (String enchantment : this.getConfig().getStringList("disabled_furnace_enchantments")) {
			allowedEnchantments.remove(enchantment);
		}
		// TODO swap to enchantment keys
		this.enchantments = new HashSet<>();
		for (String enchantment : allowedEnchantments) {
			this.enchantments.add(Enchantment.getByName(enchantment));
		}

		this.incompatibleEnchants = HashMultimap.create();
		if (this.getConfig().isConfigurationSection("enchantment_incompatibilities")) {
			for (String enchantment : this.getConfig().getConfigurationSection("enchantment_incompatibilities").getKeys(false)) {
				Enchantment key = Enchantment.getByName(enchantment);
				String enchantmentValue = this.getConfig().getString("enchantment_incompatibilities." + enchantment);
				Enchantment value = Enchantment.getByName(enchantmentValue);
				if (key == null || value == null) {
					this.getLogger().warning("Removing invalid incompatible enchantment mapping: " + enchantment + ": " + enchantmentValue);
					this.getConfig().set("enchantment_incompatibilities." + enchantment, null);
				}
				if (this.incompatibleEnchants.containsEntry(key, value)) {
					// User probably included reverse mapping
					continue;
				}
				this.incompatibleEnchants.put(key, value);
				this.incompatibleEnchants.put(value, key);
			}
		}

		this.getServer().getPluginManager().registerEvents(new FurnaceListener(this), this);
		this.getServer().getPluginManager().registerEvents(new WorldListener(this), this);
		this.getServer().getPluginManager().registerEvents(new TableEnchanter(this), this);
		this.getServer().getPluginManager().registerEvents(new AnvilEnchanter(this), this);

		this.loadEnchantableBlocks();

	}

	@Override
	public void onDisable() {
		this.getServer().getScheduler().cancelTasks(this);
		this.saveFileCache.expireAll();
		this.enchantments.clear();
		this.enchantments = null;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length < 1 || !args[0].equalsIgnoreCase("reload")) {
			sender.sendMessage("EnchantableBlocks v" + getDescription().getVersion());
			return false;
		}

		EnchantableFurnace.clearCache();
		reloadConfig();
		sender.sendMessage("[EnchantableBlocks v" + getDescription().getVersion() + "] Reloaded config and recipe cache.");
		return true;
	}

	@SuppressWarnings("unchecked")
	public HashSet<Enchantment> getEnchantments() {
		return (HashSet<Enchantment>) this.enchantments.clone();
	}

	public boolean isBlacklist() {
		return this.isBlacklist;
	}

	public List<String> getFortuneList() {
		return this.fortuneList;
	}

	public int getFurnaceEnchantability() {
		return this.getConfig().getInt("furnace_enchantability");
	}

	public boolean areEnchantmentsIncompatible(final Enchantment ench1, final Enchantment ench2) {
		return ench1.equals(ench2) || this.incompatibleEnchants.containsEntry(ench1, ench2);
	}

	@SuppressWarnings("UnusedReturnValue")
	public EnchantableBlock createEnchantableBlock(@NotNull final Block block, @NotNull final ItemStack itemStack) {

		if (itemStack.getType() == Material.AIR || itemStack.getEnchantments().isEmpty()) {
			return null;
		}

		if (this.getConfig().getStringList("disabled_worlds").contains(block.getWorld().getName().toLowerCase())) {
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

		int regionX = CoordinateConversions.blockToRegion(block.getX());
		int regionZ = CoordinateConversions.blockToRegion(block.getZ());
		Pair<RegionStorage, Boolean> saveData = this.saveFileCache
				.get(this.getRegionIdentifier(block.getWorld(), regionX, regionZ));

		int chunkX = CoordinateConversions.blockToChunk(block.getX());
		int chunkZ = CoordinateConversions.blockToChunk(block.getZ());

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
	public EnchantableBlock getEnchantableBlockByBlock(@NotNull final Block block) {
		if (this.getConfig().getStringList("disabled_worlds").contains(block.getWorld().getName().toLowerCase())) {
			return null;
		}

		return this.blockMap.get(block);

	}

	/**
	 * Load all stored EnchantableBlocks in a chunk.
	 *
	 * @param chunk the Chunk
	 */
	public void loadChunkEnchantableBlocks(@NotNull final Chunk chunk) {
		String worldName = chunk.getWorld().getName();
		if (this.getConfig().getStringList("disabled_worlds").contains(worldName.toLowerCase())) {
			return;
		}

		Pair<RegionStorage, Boolean> saveData = this.saveFileCache.get(this.getRegionIdentifier(chunk.getWorld(),
				CoordinateConversions.chunkToRegion(chunk.getX()), CoordinateConversions.chunkToRegion(chunk.getZ())), false);

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

		if (enchantableBlock != null && !enchantableBlock.isCorrectBlockType()) {
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

	public void unloadChunkEnchantableBlocks(@NotNull final Chunk chunk) {
		// Clear out and clean up loaded EnchantableBlocks.
		this.blockMap.remove(chunk);
	}

	private void loadEnchantableBlocks() {

		// Load all EnchantableBlocks for loaded chunks. This isn't too bad due to how the cache works.
		for (World world : this.getServer().getWorlds()) {
			if (this.getConfig().getStringList("disabled_worlds").contains(world.getName().toLowerCase())) {
				continue;
			}
			for (Chunk chunk : world.getLoadedChunks()) {
				this.loadChunkEnchantableBlocks(chunk);
			}
		}

	}

	private ConfigurationSection getChunkStorage(World world, int chunkX, int chunkZ) {
		ConfigurationSection regionStorage = saveFileCache.get(getRegionIdentifier(world,
				CoordinateConversions.chunkToRegion(chunkX), CoordinateConversions.chunkToRegion(chunkZ))).getLeft();
		String chunkPath = chunkX + "_" + chunkZ;

		if (regionStorage.isConfigurationSection(chunkPath)) {
			return regionStorage.getConfigurationSection(chunkPath);
		}

		return regionStorage.createSection(chunkPath);
	}

	private ConfigurationSection getBlockStorage(Block block) {
		ConfigurationSection chunkStorage = this.getChunkStorage(block.getWorld(),
				CoordinateConversions.blockToChunk(block.getX()),
				CoordinateConversions.blockToChunk(block.getZ()));
		String blockPath = block.getX() + "_" + block.getY() + "_" + block.getZ();

		if (chunkStorage.isConfigurationSection(blockPath)) {
			return chunkStorage.getConfigurationSection(blockPath);
		}

		return chunkStorage.createSection(blockPath);
	}

	private Triple<World, Integer, Integer> getRegionIdentifier(World world, int regionX, int regionZ) {
		return new Triple<>(world, regionX, regionZ);
	}

	private void updateConfig() {
		this.saveDefaultConfig();
		Set<String> options = Objects.requireNonNull(this.getConfig().getDefaults()).getKeys(false);
		Set<String> current = this.getConfig().getKeys(false);

		for (String s : options) {
			if (s.equals("enchantment_incompatibilities")) {
				continue;
			}
			if (!current.contains(s)) {
				this.getConfig().set(s, this.getConfig().getDefaults().get(s));
			}
		}

		for (String s : current) {
			if (!options.contains(s)) {
				this.getConfig().set(s, null);
			}
		}

		this.getConfig().options().copyHeader(true);
	}

}
