package com.github.jikoo.enchantableblocks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.enchanting.AnvilEnchanter;
import com.github.jikoo.enchantableblocks.enchanting.TableEnchanter;
import com.github.jikoo.enchantableblocks.listener.FurnaceListener;
import com.github.jikoo.enchantableblocks.listener.WorldListener;
import com.github.jikoo.enchantableblocks.util.Cache;
import com.github.jikoo.enchantableblocks.util.Cache.CacheBuilder;
import com.github.jikoo.enchantableblocks.util.CompatibilityUtil;
import com.github.jikoo.enchantableblocks.util.CoordinateConversions;
import com.github.jikoo.enchantableblocks.util.Function;
import com.github.jikoo.enchantableblocks.util.LoadFunction;
import com.github.jikoo.enchantableblocks.util.Wrapper;

import com.google.common.collect.HashMultimap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * A Bukkit plugin for adding effects to block based on enchantments.
 *
 * @author Jikoo
 */
public class EnchantableBlocksPlugin extends JavaPlugin {

	private final Map<String, TreeMap<Integer, TreeMap<Integer, Map<Integer, EnchantableBlock>>>> chunkEnchantedBlocks = new HashMap<>();

	private Cache<Pair<String, String>, Pair<YamlConfiguration, Boolean>> saveFileCache;
	private HashSet<Enchantment> enchantments;
	private ArrayList<String> fortuneList;
	private boolean isBlacklist;
	private HashMultimap<Enchantment, Enchantment> incompatibleEnchants;

	@Override
	public void onEnable() {
		this.saveFileCache = new CacheBuilder<Pair<String, String>, Pair<YamlConfiguration, Boolean>>()
				.withRetention(Math.max(this.getConfig().getInt("autosave", 5) * 6000, 6000L))
				.withInUseCheck(new Function<Pair<String, String>, Pair<YamlConfiguration, Boolean>>() {
					@Override
					public boolean run(final Pair<String, String> key,
									   final Pair<YamlConfiguration, Boolean> value) {
						boolean loaded = false;
						World world = Bukkit.getWorld(key.getLeft());
						String[] regionSplit = key.getRight().split("_");

						try {
							int minChunkX = CoordinateConversions.regionToChunk(Integer.parseInt(regionSplit[0]));
							int minChunkZ = CoordinateConversions.regionToChunk(Integer.parseInt(regionSplit[1]));

							// Ensure backing YAML is up-to-date and check if chunks are loaded
							for (int chunkX = minChunkX, maxChunkX = minChunkX + 32; chunkX < maxChunkX; ++chunkX) {
								for (int chunkZ = minChunkZ, maxChunkZ = minChunkZ + 32; chunkZ < maxChunkZ; ++chunkZ) {
									EnchantableBlocksPlugin.this.storeChunkEnchantedBlocks(key.getLeft(), chunkX, chunkZ);
									if (!loaded && world != null) {
										loaded = world.isChunkLoaded(chunkX, chunkZ);
									}
								}
							}
						} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
							// Shouldn't be possible, but what the heck.
							e.printStackTrace();
						}

						if (!value.getRight()) {
							return loaded;
						}

						File saveFile = EnchantableBlocksPlugin.this.getSaveFile(key);

						Collection<String> keys = value.getLeft().getKeys(true);
						boolean delete = true;
						for (String path : keys) {
							if (value.getLeft().get(path) != null) {
								delete = false;
								break;
							}
						}

						if (delete) {
							saveFile.delete();
							File parentFile = saveFile.getParentFile();
							String[] files = parentFile.list();
							if (files == null || files.length == 0) {
								parentFile.delete();
							}
							return loaded;
						}

						try {
							value.getLeft().save(saveFile);
							value.setValue(false);
						} catch (IOException e) {
							e.printStackTrace();
						}

						return loaded;
					}
				}).withLoadFunction(new LoadFunction<Pair<String, String>, Pair<YamlConfiguration, Boolean>>() {
					@Override
					public Pair<YamlConfiguration, Boolean> run(final Pair<String, String> key, final boolean create) {
						File flagFile = EnchantableBlocksPlugin.this.getSaveFile(key);
						if (flagFile.exists()) {
							return new MutablePair<>(YamlConfiguration.loadConfiguration(flagFile), false);
						}
						return create ? new MutablePair<>(new YamlConfiguration(), false) : null;
					}
				}).build();

		this.updateConfig();

		ArrayList<String> disabledWorlds = new ArrayList<>();
		for (String worldName : this.getConfig().getStringList("disabled_worlds")) {
			if (!disabledWorlds.contains(worldName.toLowerCase())) {
				disabledWorlds.add(worldName.toLowerCase());
			}
		}

		this.isBlacklist = this.getConfig().getString("fortune_list_mode").matches(".*[Bb][Ll][Aa][Cc][Kk].*");

		// TODO allow MaterialData
		this.fortuneList = new ArrayList<>();
		for (Iterator<String> iterator = this.getConfig().getStringList("fortune_list").iterator(); iterator.hasNext();) {
			String next = iterator.next().toUpperCase();
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

		if (CompatibilityUtil.areAnvilsSupported()) {
			this.getServer().getPluginManager().registerEvents(new AnvilEnchanter(this), this);
		}

		this.loadEnchantableBlocks();

		if (!CompatibilityUtil.areFurnacesSupported()) {
			// TODO: Other block types
			new BukkitRunnable() {
				@Override
				public void run() {
					for (EnchantableBlock enchantableBlock : EnchantableBlocksPlugin.this.getLoadedEnchantableBlocks()) {
						enchantableBlock.tick();
					}
				}

			}.runTaskTimer(this, 1, 2);
		}
	}

	@Override
	public void onDisable() {
		this.getServer().getScheduler().cancelTasks(this);
		this.saveFileCache.expireAll();
		this.enchantments.clear();
		this.enchantments = null;
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

	public boolean areEnchantmentsCompatible(final Enchantment ench1, final Enchantment ench2) {
		return !ench1.equals(ench2) && !this.incompatibleEnchants.containsEntry(ench1, ench2);
	}

	public EnchantableBlock createEnchantableBlock(final Block block, final ItemStack itemStack) {

		if (block == null) {
			throw new IllegalArgumentException("Block cannot be null.");
		}

		if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getEnchantments().isEmpty()) {
			return null;
		}

		if (this.getConfig().getStringList("disabled_worlds").contains(block.getWorld().getName().toLowerCase())) {
			return null;
		}

		final EnchantableBlock enchantableBlock = this.getEnchantableBlock(block, itemStack);

		if (enchantableBlock == null) {
			return null;
		}

		this.chunkEnchantedBlocks.compute(block.getWorld().getName(), (worldName, worldMap) -> {
			if (worldMap == null) {
				worldMap = new TreeMap<>();
			}

			worldMap.compute(block.getX(), (blockX, blockXMap) -> {
				if (blockXMap == null) {
					blockXMap = new TreeMap<>();
				}

				blockXMap.compute(block.getZ(), (blockZ, blockZMap) -> {
					if (blockZMap == null) {
						blockZMap = new HashMap<>();
					}

					blockZMap.put(block.getY(), enchantableBlock);
					// Saving is handled when the cache is cleaned, no need to actually alter the entry.
					this.saveFileCache.get(this.getSaveFileIdentifier(worldName,
							CoordinateConversions.blockToChunk(blockX),
							CoordinateConversions.blockToChunk(blockZ))).setValue(true);

					return blockZMap;
				});

				return blockXMap;
			});

			return worldMap;
		});

		return enchantableBlock;
	}

	/**
	 * Remove an EnchantableBlock.
	 *
	 * @param block the EnchantableBlock
	 * @return the ItemStack representation of the EnchantableBlock or null if the Block was not a valid EnchantableBlock
	 */
	public ItemStack destroyEnchantableBlock(final Block block) {
		Wrapper<EnchantableBlock> wrapper = new Wrapper<>();

		this.chunkEnchantedBlocks.computeIfPresent(block.getWorld().getName(), (worldName, worldMap) -> {
			worldMap.computeIfPresent(block.getX(), (blockX, blockXMap) -> {
				blockXMap.computeIfPresent(block.getZ(), (blockZ, blockZMap) -> {
					wrapper.set(blockZMap.remove(block.getY()));
					return blockZMap.isEmpty() ? null : blockZMap;
				});
				return blockXMap.isEmpty() ? null : blockXMap;
			});
			return worldMap.isEmpty() ? null : worldMap;
		});

		EnchantableBlock enchantableBlock = wrapper.get();

		if (enchantableBlock == null || !enchantableBlock.isCorrectType(block.getType())) {
			return null;
		}

		int chunkX = CoordinateConversions.blockToChunk(block.getX());
		int chunkZ = CoordinateConversions.blockToChunk(block.getZ());
		Pair<YamlConfiguration, Boolean> saveData = this.saveFileCache
				.get(this.getSaveFileIdentifier(block.getWorld().getName(), chunkX, chunkZ));

		String chunkPath = chunkX + "_" + chunkZ;

		ItemStack itemStack = enchantableBlock.getItemStack();
		if (itemStack != null && itemStack.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
			// Silk time isn't supposed to be preserved when broken.
			itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
		}

		if (!saveData.getLeft().isConfigurationSection(chunkPath)) {
			saveData.getLeft().set(chunkPath, null);
			return itemStack;
		}

		ConfigurationSection chunkSection = saveData.getLeft().getConfigurationSection(chunkPath);
		String coordPath = block.getX() + "_" + block.getY() + "_" + block.getZ();

		chunkSection.set(coordPath, null);

		if (chunkSection.getKeys(false).isEmpty()) {
			saveData.getLeft().set(chunkPath, null);
		}

		saveData.setValue(true);

		return itemStack;
	}

	public Collection<EnchantableBlock> getLoadedEnchantableBlocks() {
		return this.chunkEnchantedBlocks.values().stream().map(Map::values)
				.flatMap(Collection::stream).map(Map::values).flatMap(Collection::stream)
				.map(Map::values).flatMap(Collection::stream).collect(Collectors.toList());
	}

	/**
	 * Gets an EnchantableBlock by Block.
	 *
	 * @param block the Block
	 * @return the EnchantableBlock, or null the Block is not an enchanted block
	 */
	public EnchantableBlock getEnchantableBlockByBlock(final Block block) {
		if (block == null || this.getConfig().getStringList("disabled_worlds").contains(block.getWorld().getName().toLowerCase())) {
			return null;
		}

		Wrapper<EnchantableBlock> wrapper = new Wrapper<>();

		this.chunkEnchantedBlocks.computeIfPresent(block.getWorld().getName(), (worldName, worldMap) -> {
			worldMap.computeIfPresent(block.getX(), (blockX, blockXMap) -> {
				blockXMap.computeIfPresent(block.getZ(), (blockZ, blockZMap) -> {
					wrapper.set(blockZMap.get(block.getY()));
					return blockZMap.isEmpty() ? null : blockZMap;
				});
				return blockXMap.isEmpty() ? null : blockXMap;
			});
			return worldMap.isEmpty() ? null : worldMap;
		});

		return wrapper.get();

	}

	/**
	 * Load all stored EnchantableBlocks in a chunk.
	 *
	 * @param chunk the Chunk
	 */
	public void loadChunkEnchantableBlocks(final Chunk chunk) {
		String worldName = chunk.getWorld().getName();
		if (this.getConfig().getStringList("disabled_worlds").contains(worldName.toLowerCase())) {
			return;
		}

		Pair<YamlConfiguration, Boolean> saveData = this.saveFileCache.get(this.getSaveFileIdentifier(worldName, chunk.getX(), chunk.getZ()), false);

		if (saveData == null) {
			return;
		}

		String path = chunk.getX() + "_" + chunk.getZ();
		if (!saveData.getLeft().isConfigurationSection(path)) {
			return;
		}

		ConfigurationSection chunkStorage = saveData.getLeft().getConfigurationSection(path);

		Set<String> chunkKeys = chunkStorage.getKeys(false);
		Iterator<String> iterator = chunkKeys.iterator();
		while (iterator.hasNext()) {
			String xyz = iterator.next();
			String[] split = xyz.split("_");
			Block worldBlock;

			try {
				worldBlock = chunk.getWorld().getBlockAt(Integer.valueOf(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]));
			} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
				this.getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(split));
				continue;
			}

			Block block = worldBlock;
			EnchantableBlock enchantableBlock = this.loadEnchantableBlock(block, chunkStorage.getConfigurationSection(xyz));

			if (enchantableBlock == null) {
				// Invalid EnchantableBlock, could not load.
				this.getLogger().warning(String.format("Removed invalid save: %s at %s",
						chunkStorage.getItemStack(xyz + ".itemstack"), block.getLocation()));
				chunkStorage.set(xyz, null);
				saveData.setValue(true);
				continue;
			}

			this.chunkEnchantedBlocks.compute(worldName, (key, worldMap) -> {
				if (worldMap == null) {
					worldMap = new TreeMap<>();
				}

				worldMap.compute(block.getX(), (blockX, blockXMap) -> {
					if (blockXMap == null) {
						blockXMap = new TreeMap<>();
					}

					blockXMap.compute(block.getZ(), (blockZ, blockZMap) -> {
						if (blockZMap == null) {
							blockZMap = new HashMap<>();
						}

						blockZMap.put(block.getY(), enchantableBlock);

						return blockZMap;
					});

					return blockXMap;
				});

				return worldMap;
			});
		}
	}

	/**
	 * Load an EnchantableBlock from storage.
	 *
	 * @param block the Block
	 * @param storage the ConfigurationSection to load the EnchantableBlock from
	 * @return the EnchantableBlock, or null if the EnchantableBlock is not valid.
	 */
	private EnchantableBlock loadEnchantableBlock(final Block block, final ConfigurationSection storage) {
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
	private EnchantableBlock getEnchantableBlock(final Block block, ItemStack itemStack) {
		Material type = itemStack.getType();
		itemStack = itemStack.clone();
		itemStack.setAmount(1);
		if (type == Material.FURNACE) {
			return new EnchantableFurnace(block, itemStack);
		}
		return null;
	}

	public void unloadChunkEnchantableBlocks(final Chunk chunk) {
		this.storeChunkEnchantedBlocks(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

		// Clear out and clean up loaded EnchantableBlocks.
		this.chunkEnchantedBlocks.computeIfPresent(chunk.getWorld().getName(), (worldName, worldMap) -> {
			int blockXMin = CoordinateConversions.chunkToBlock(chunk.getX());
			Map<Integer, TreeMap<Integer, Map<Integer, EnchantableBlock>>> blockXMap = worldMap.subMap(blockXMin, blockXMin + 16);
			blockXMap.entrySet().removeIf(blockXMapping -> {
				int blockZMin = CoordinateConversions.chunkToBlock(chunk.getZ());
				Map<Integer, Map<Integer, EnchantableBlock>> blockZMap = blockXMapping.getValue().subMap(blockZMin, blockZMin + 16);
				blockZMap.entrySet().removeIf(blockZMapping -> {
					blockZMapping.getValue().clear();
					return true;
				});
				return blockXMapping.getValue().isEmpty();
			});
			return worldMap.isEmpty() ? null : worldMap;
		});
	}

	private void storeChunkEnchantedBlocks(final String worldName, final int chunkX, final int chunkZ) {
		this.chunkEnchantedBlocks.computeIfPresent(worldName, (entryWorldName, worldMap) -> {
			int blockXMin = CoordinateConversions.chunkToBlock(chunkX);
			Map<Integer, TreeMap<Integer, Map<Integer, EnchantableBlock>>> blockXMap = worldMap.subMap(blockXMin, blockXMin + 16);
			blockXMap.forEach((blockX, blockZMap) -> {
				int blockZMin = CoordinateConversions.chunkToBlock(chunkZ);
				Map<Integer, Map<Integer, EnchantableBlock>> blockZSubMap = blockZMap.subMap(blockZMin, blockZMin + 16);
				blockZSubMap.forEach((blockZ, blockYMap) -> {

					Pair<YamlConfiguration, Boolean> saveFile = this.saveFileCache.get(this.getSaveFileIdentifier(worldName, chunkX, chunkZ));
					String chunkPath = chunkX + "_" + chunkZ;

					ConfigurationSection chunkSection;
					if (saveFile.getLeft().isConfigurationSection(chunkPath)) {
						chunkSection = saveFile.getLeft().getConfigurationSection(chunkPath);
					} else {
						chunkSection = saveFile.getKey().createSection(chunkPath);
					}

					blockYMap.forEach((blockY, enchantableBlock) -> {
						String blockPath = blockX + "_" + blockY + "_" + blockZ;
						ConfigurationSection blockSection;

						if (chunkSection.isConfigurationSection(blockPath)) {
							blockSection = chunkSection.getConfigurationSection(blockPath);
						} else {
							blockSection = chunkSection.createSection(blockPath);
						}

						if (enchantableBlock.save(blockSection)) {
							saveFile.setValue(true);
						}
					});
				});
			});

			return worldMap.isEmpty() ? null : worldMap;
		});
	}

	private void loadEnchantableBlocks() {

		File enchantedFurnaceFolder = new File(this.getDataFolder().getParentFile(), "EnchantedFurnace");
		File furnaceFile = new File(enchantedFurnaceFolder, "furnaces.yml");
		YamlConfiguration furnaceConfig = YamlConfiguration.loadConfiguration(furnaceFile);

		// Backwards compatibility for versions < 2.0.0
		if (furnaceFile.exists()) {
			this.convertItemDefinedFurnaces(furnaceConfig);
			furnaceFile.renameTo(new File(this.getDataFolder(), "converted-furnaces.yml"));
		}

		if (!this.chunkEnchantedBlocks.isEmpty()) {
			// In the event that EnchantableBlocks exist in the new save format, don't wipe when loading old formats.
			for (World world : this.getServer().getWorlds()) {
				if (this.getConfig().getStringList("disabled_worlds").contains(world.getName().toLowerCase())) {
					continue;
				}
				for (Chunk chunk : world.getLoadedChunks()) {
					this.loadChunkEnchantableBlocks(chunk);
				}
			}
		}

		// Force save for converted EnchantableBlocks.
		this.saveFileCache.expireAll();

		// Release all loaded EnchantableBlocks.
		this.chunkEnchantedBlocks.clear();

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

	private void convertItemDefinedFurnaces(final ConfigurationSection section) {

		for (String worldName : section.getKeys(false)) {
			if (this.getConfig().getStringList("disabled_worlds").contains(worldName.toLowerCase())) {
				continue;
			}

			ConfigurationSection worldSection = section.getConfigurationSection(worldName);

			if (worldSection == null) {
				continue;
			}

			for (String chunkPath : worldSection.getKeys(false)) {
				ConfigurationSection chunkSection = worldSection.getConfigurationSection(chunkPath);

				if (chunkSection == null) {
					continue;
				}

				for (String coordinatePath : chunkSection.getKeys(false)) {
					String[] coordinateSplit = coordinatePath.split("_");
					Vector coordinates;

					try {
						coordinates = new Vector(Integer.valueOf(coordinateSplit[0]), Integer.valueOf(coordinateSplit[1]), Integer.valueOf(coordinateSplit[2]));
					} catch (Exception e) {
						this.getLogger().warning(String.format("Error loading legacy 1.4.0 block: %s.%s.%s", worldName, chunkPath, coordinatePath));
						if (e instanceof NumberFormatException) {
							this.getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(coordinateSplit));
						} else {
							this.getLogger().severe("An unknown exception occurred!");
							e.printStackTrace();
							this.getLogger().severe("Please report this error!");
						}
						continue;
					}

					this.loadLegacyFurnace(worldName, coordinates, chunkSection.getItemStack(coordinatePath + ".itemstack"));
				}
			}

		}
	}

	private void loadLegacyFurnace(final String worldName, final Vector coordinates, final ItemStack itemStack) {

		Block block = null;
		World world = this.getServer().getWorld(worldName);
		if (world != null) {
			block = world.getBlockAt(coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
		}

		EnchantableBlock enchantableBlock = this.getEnchantableBlock(block, itemStack);

		if (enchantableBlock == null || block != null && !enchantableBlock.isCorrectBlockType()) {
			return;
		}

		this.chunkEnchantedBlocks.compute(worldName, (key, worldMap) -> {
			if (worldMap == null) {
				worldMap = new TreeMap<>();
			}

			worldMap.compute(coordinates.getBlockX(), (blockX, blockXMap) -> {
				if (blockXMap == null) {
					blockXMap = new TreeMap<>();
				}

				blockXMap.compute(coordinates.getBlockZ(), (blockZ, blockZMap) -> {
					if (blockZMap == null) {
						blockZMap = new HashMap<>();
					}

					blockZMap.put(coordinates.getBlockY(), enchantableBlock);
					this.saveFileCache.get(this.getSaveFileIdentifier(worldName,
							CoordinateConversions.blockToChunk(blockX),
							CoordinateConversions.blockToChunk(blockZ))).setValue(true);

					return blockZMap;
				});

				return blockXMap;
			});

			return worldMap;
		});
	}

	private File getSaveFile(final Pair<String, String> data) {
		return new File(this.getDataFolder(), String.format("data%1$s%2$s%1$s%3$s.yml", File.separatorChar, data.getLeft(), data.getRight()));
	}

	private Pair<String, String> getSaveFileIdentifier(final String world, final int chunkX, final int chunkZ) {
		return new ImmutablePair<>(world,
				CoordinateConversions.chunkToRegion(chunkX) + "_" + CoordinateConversions.chunkToRegion(chunkZ));
	}

	private void updateConfig() {
		this.saveDefaultConfig();
		Set<String> options = this.getConfig().getDefaults().getKeys(false);
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
