package com.github.jikoo.enchantedfurnace;

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
import java.util.Map.Entry;
import java.util.Set;

import com.github.jikoo.enchantedfurnace.enchanting.AnvilEnchanter;
import com.github.jikoo.enchantedfurnace.enchanting.TableEnchanter;
import com.github.jikoo.enchantedfurnace.enchanting.TablePreviewEnchanter;

import com.google.common.collect.HashMultimap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Bukkit plugin for adding effects to furnaces based on enchantments.
 *
 * @author Jikoo
 */
public class EnchantedFurnace extends JavaPlugin {

	private YamlConfiguration furnaceSaves;
	private HashSet<Enchantment> enchantments;
	private Map<Block, Furnace> furnaces;
	private ArrayList<String> fortuneList;
	private boolean isBlacklist;
	private HashMultimap<Enchantment, Enchantment> incompatibleEnchants;
	private boolean dirty = false;

	@Override
	public void onEnable() {

		this.furnaces = new HashMap<>();
		this.loadFurnaces();

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
			if (allowedEnchantments.contains(enchantment)) {
				allowedEnchantments.remove(enchantment);
			}
		}
		this.enchantments = new HashSet<>();
		for (String enchantment : allowedEnchantments) {
			this.enchantments.add(Enchantment.getByName(enchantment));
		}

		this.incompatibleEnchants = HashMultimap.create();
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

		this.getServer().getPluginManager().registerEvents(new FurnaceListener(this), this);

		try {
			Class.forName("org.bukkit.enchantments.EnchantmentOffer");
			this.getServer().getPluginManager().registerEvents(new TablePreviewEnchanter(this), this);
		} catch (ClassNotFoundException e) {
			this.getServer().getPluginManager().registerEvents(new TableEnchanter(this), this);
		}
		if (ReflectionUtils.areAnvilsSupported()) {
			this.getServer().getPluginManager().registerEvents(new AnvilEnchanter(this), this);
		}

		for (World world : Bukkit.getWorlds()) {
			if (this.getConfig().getStringList("disabled_worlds").contains(world.getName().toLowerCase())) {
				continue;
			}
			for (Chunk chunk : world.getLoadedChunks()) {
				this.loadChunkFurnaces(chunk);
			}
		}

		if (!ReflectionUtils.areFurnacesSupported()) {
			new FurnaceEfficiencyIncrement(this).runTaskTimer(this, 1, 2);
		}

		if (this.getConfig().getInt("autosave") > 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					long start = System.currentTimeMillis();
					EnchantedFurnace.this.saveFurnaceStorage();
					EnchantedFurnace.this.getLogger().info(String.format("Autosave complete, took %f seconds", (System.currentTimeMillis() - start) / 1000D));
				}
			}.runTaskTimerAsynchronously(this, 0, this.getConfig().getInt("autosave") * 1200);
		}
	}

	@Override
	public void onDisable() {
		this.getServer().getScheduler().cancelTasks(this);
		for (Furnace furnace : this.furnaces.values()) {
			this.saveFurnace(furnace);
		}
		this.saveFurnaceStorage();
		this.furnaces.clear();
		this.furnaces = null;
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

	public void createFurnace(final Block block, final ItemStack is) {
		if (is.getType() != Material.FURNACE
				|| this.getConfig().getStringList("disabled_worlds").contains(
						block.getWorld().getName().toLowerCase())) {
			return;
		}
		final Furnace furnace = new Furnace(block, is.clone());
		if (furnace.getCookModifier() > 0 || furnace.getBurnModifier() > 0 || furnace.getFortune() > 0 || furnace.canPause()) {
			this.furnaces.put(block, furnace);
			this.saveFurnace(furnace);
		}
		if (this.getConfig().getInt("autosave") < 1) {
			this.saveFurnaceStorage();
		}
	}

	public ItemStack destroyFurnace(final Block block) {
		Furnace f = this.furnaces.remove(block);
		if (f == null || block.getType() != Material.FURNACE && block.getType() != Material.BURNING_FURNACE) {
			return null;
		}
		this.getFurnaceStorage().set(this.getSaveString(block), null);
		this.dirty = true;
		if (this.getConfig().getInt("autosave") < 1) {
			this.saveFurnaceStorage();
		}
		ItemStack is = f.getItemStack();
		if (is.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
			// Silk time isn't supposed to be preserved when broken.
			is.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
		}
		return is;
	}

	public Collection<Furnace> getFurnaces() {
		return this.furnaces.values();
	}

	public Furnace getFurnace(final Block block) {
		return this.furnaces.get(block);
	}

	public boolean isFurnace(final Block block) {
		return this.furnaces.containsKey(block);
	}

	public void loadChunkFurnaces(final Chunk chunk) {
		if (this.getConfig().getStringList("disabled_worlds").contains(chunk.getWorld().getName().toLowerCase())) {
			return;
		}
		String path = new StringBuilder(chunk.getWorld().getName()).append('.')
				.append(chunk.getX()).append('_').append(chunk.getZ()).toString();
		ConfigurationSection chunkSection = this.getFurnaceStorage().getConfigurationSection(path);
		if (chunkSection == null) {
			return;
		}
		Set<String> chunkKeys = chunkSection.getKeys(false);
		Iterator<String> iterator = chunkKeys.iterator();
		while (iterator.hasNext()) {
			String xyz = iterator.next();
			String[] split = xyz.split("_");
			try {
				Block block = chunk.getWorld().getBlockAt(Integer.valueOf(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]));
				Material type = block.getType();
				ItemStack itemStack = this.getFurnaceStorage().getItemStack(path + '.' + xyz + ".itemstack");
				if (type == Material.FURNACE || type == Material.BURNING_FURNACE) {
					Furnace furnace = new Furnace(block, itemStack);
					this.furnaces.put(block, furnace);
				} else {
					iterator.remove();
					this.getFurnaceStorage().set(path + '.' + xyz, null);
					this.dirty = true;
					this.getLogger().warning("Removed invalid save: " + itemStack.toString() + " at " + block.getLocation().toString());
				}
			} catch (NumberFormatException e) {
				this.getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(split));
			} catch (ClassCastException e) {
				this.getLogger().warning("Invalid itemstack saved for " + path + '.' + xyz);
			}
		}
		if (chunkKeys.isEmpty()) {
			this.getFurnaceStorage().set(path, null);
		}
	}

	public void unloadChunkFurnaces(final Chunk chunk) {
		for (Iterator<Entry<Block, Furnace>> iterator = this.furnaces.entrySet().iterator(); iterator.hasNext();) {
			Entry<Block, Furnace> entry = iterator.next();
			if (!entry.getKey().getWorld().equals(chunk.getWorld())
					|| entry.getKey().getX() >> 4 != chunk.getX()
					|| entry.getKey().getZ() >> 4 != chunk.getZ()) {
				continue;
			}
			if (entry.getValue().canPause()) {
				this.saveFurnace(entry.getValue());
			}
			iterator.remove();
		}
		if (this.getConfig().getInt("autosave") < 1) {
			this.saveFurnaceStorage();
		}
	}

	private void loadFurnaces() {
		// Backwards compatibility for version < 1.4.0
		ConfigurationSection furnaceSection = this.getFurnaceStorage().getConfigurationSection("furnaces");
		this.convertLegacyFurnaces(furnaceSection);
		this.getFurnaceStorage().set("furnaces", null);

		// Backwards compatibility for version < 1.3.0
		furnaceSection = this.getConfig().getConfigurationSection("furnaces");
		this.convertLegacyFurnaces(furnaceSection);
		this.getConfig().set("furnaces", null);

		// Save converted furnaces, if any
		if (this.getConfig().getInt("autosave") < 1) {
			this.saveFurnaceStorage();
		}

		Set<String> worlds = this.getFurnaceStorage().getKeys(false);
		for (World world : this.getServer().getWorlds()) {
			if (!worlds.contains(world.getName())
					|| this.getConfig().getStringList("disabled_worlds").contains(
							world.getName().toLowerCase())) {
				continue;
			}
			for (Chunk chunk : world.getLoadedChunks()) {
				this.loadChunkFurnaces(chunk);
			}
		}
	}

	private void convertLegacyFurnaces(final ConfigurationSection section) {
		if (section == null) {
			// No saves here
			return;
		}

		this.getLogger().info("Starting conversion of legacy furnaces - this may take a little time.");

		for (String legacy : section.getKeys(false)) {
			String modern;
			Block block = null;
			String[] loc = legacy.split(",");

			// Parse old location string into modern location string
			if (loc.length != 4) {
				this.getLogger().warning("Unable to split location properly! " + legacy + " split to " + Arrays.toString(loc));
				continue;
			}
			if (this.getConfig().getStringList("disabled_worlds").contains(loc[0].toLowerCase())) {
				continue;
			}
			try {
				World world = this.getServer().getWorld(loc[0]);
				if (world != null) {
					block = new Location(world, Integer.valueOf(loc[1]), Integer.valueOf(loc[2]), Integer.valueOf(loc[3])).getBlock();
					modern = this.getSaveString(block);
				} else {
					modern = this.getSaveString(loc[0], Integer.valueOf(loc[1]), Integer.valueOf(loc[2]), Integer.valueOf(loc[3]));
				}
			} catch (Exception e) {
				this.getLogger().warning("Error loading block: " + legacy);
				if (e instanceof NumberFormatException) {
					this.getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(loc));
				} else {
					this.getLogger().severe("An unknown exception occurred!");
					e.printStackTrace();
					this.getLogger().severe("Please report this error!");
				}
				continue;
			}

			// Verify block is a furnace if world is loaded
			if (block != null && block.getType() != Material.FURNACE && block.getType() != Material.BURNING_FURNACE) {
				continue;
			}

			ItemStack furnaceStack = new ItemStack(Material.FURNACE);
			ItemMeta im = furnaceStack.getItemMeta();
			if (block != null) {
				String furnaceName = ((org.bukkit.block.Furnace) block.getState()).getInventory().getTitle();
				if (!furnaceName.equals("container.furnace")) {
					im.setDisplayName(furnaceName);
				}
			}
			furnaceStack.setItemMeta(im);
			int level = section.getInt(legacy + ".efficiency", 0);
			if (level > 0) {
				furnaceStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, level);
			}
			level = section.getInt(legacy + ".unbreaking", 0);
			if (level > 0) {
				furnaceStack.addUnsafeEnchantment(Enchantment.DURABILITY, level);
			}
			level = section.getInt(legacy + ".fortune", 0);
			if (level > 0) {
				furnaceStack.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, level);
			}
			level = section.getInt(legacy + ".silk", -1);
			if (level > -1) {
				furnaceStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, level);
			}

			this.getFurnaceStorage().set(modern + ".itemstack", furnaceStack);
			this.dirty = true;
			section.set(legacy, null);
		}
		this.getLogger().info("Legacy furnace conversion complete!");
	}

	private YamlConfiguration getFurnaceStorage() {
		if (this.furnaceSaves != null) {
			return this.furnaceSaves;
		}
		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdirs();
		}
		File file = new File(this.getDataFolder(), "furnaces.yml");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Cannot write furnace save file! Make sure your file permissions are set up properly.", e);
			}
		}
		this.furnaceSaves = YamlConfiguration.loadConfiguration(file);
		return this.furnaceSaves;
	}

	private void saveFurnaceStorage() {
		if (this.furnaceSaves == null || !this.dirty) {
			return;
		}
		File file = new File(this.getDataFolder(), "furnaces.yml");
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			this.furnaceSaves.save(file);
			this.dirty = false;
		} catch (IOException e) {
			throw new RuntimeException("Cannot write furnace save file! Make sure your file permissions are set up properly.", e);
		}
	}

	private void saveFurnace(final Furnace furnace) {
		this.getFurnaceStorage().set(this.getSaveString(furnace.getBlock()) + ".itemstack", furnace.getItemStack());
		this.dirty = true;
	}

	private String getSaveString(final Block block) {
		return this.getSaveString(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	private String getSaveString(final String world, final int x, final int y, final int z) {
		return String.format("%s.%s_%s.%s_%s_%s", world, x >> 4, z >> 4, x, y, z);
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
