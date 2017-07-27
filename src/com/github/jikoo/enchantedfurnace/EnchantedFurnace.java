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

		this.furnaces = new HashMap<Block, Furnace>();
		this.loadFurnaces();

		updateConfig();

		ArrayList<String> disabledWorlds = new ArrayList<String>();
		for (String worldName : getConfig().getStringList("disabled_worlds")) {
			if (!disabledWorlds.contains(worldName.toLowerCase())) {
				disabledWorlds.add(worldName.toLowerCase());
			}
		}

		isBlacklist = getConfig().getString("fortune_list_mode").matches(".*[Bb][Ll][Aa][Cc][Kk].*");

		// TODO allow MaterialData
		fortuneList = new ArrayList<String>();
		for (Iterator<String> iterator = getConfig().getStringList("fortune_list").iterator(); iterator.hasNext();) {
			String next = iterator.next().toUpperCase();
			if (fortuneList.contains(next)) {
				continue;
			}
			Material m = Material.getMaterial(next);
			if (m == null) {
				getLogger().warning("No material by the name of \"" + next + "\" could be found!");
				getLogger().info("Please use material names listed in https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
			} else {
				fortuneList.add(m.name());
			}
		}

		HashSet<String> allowedEnchantments = new HashSet<String>();
		allowedEnchantments.add("DIG_SPEED");
		allowedEnchantments.add("DURABILITY");
		allowedEnchantments.add("LOOT_BONUS_BLOCKS");
		allowedEnchantments.add("SILK_TOUCH");
		for (String enchantment : getConfig().getStringList("disabled_furnace_enchantments")) {
			if (allowedEnchantments.contains(enchantment)) {
				allowedEnchantments.remove(enchantment);
			}
		}
		enchantments = new HashSet<Enchantment>();
		for (String enchantment : allowedEnchantments) {
			enchantments.add(Enchantment.getByName(enchantment));
		}

		incompatibleEnchants = HashMultimap.create();
		for (String enchantment : getConfig().getConfigurationSection("enchantment_incompatibilities").getKeys(false)) {
			Enchantment key = Enchantment.getByName(enchantment);
			String enchantmentValue = getConfig().getString("enchantment_incompatibilities." + enchantment);
			Enchantment value = Enchantment.getByName(enchantmentValue);
			if (key == null || value == null) {
				getLogger().warning("Removing invalid incompatible enchantment mapping: " + enchantment + ": " + enchantmentValue);
				getConfig().set("enchantment_incompatibilities." + enchantment, null);
			}
			if (incompatibleEnchants.containsEntry(key, value)) {
				// User probably included reverse mapping
				continue;
			}
			incompatibleEnchants.put(key, value);
			incompatibleEnchants.put(value, key);
		}

		getServer().getPluginManager().registerEvents(new FurnaceListener(this), this);

		try {
			Class.forName("org.bukkit.enchantments.EnchantmentOffer");
			getServer().getPluginManager().registerEvents(new TablePreviewEnchanter(this), this);
		} catch (ClassNotFoundException e) {
			getServer().getPluginManager().registerEvents(new TableEnchanter(this), this);
		}
		if (ReflectionUtils.areAnvilsSupported()) {
			getServer().getPluginManager().registerEvents(new AnvilEnchanter(this), this);
		}

		for (World world : Bukkit.getWorlds()) {
			if (getConfig().getStringList("disabled_worlds").contains(world.getName().toLowerCase())) {
				continue;
			}
			for (Chunk chunk : world.getLoadedChunks()) {
				loadChunkFurnaces(chunk);
			}
		}

		if (!ReflectionUtils.areFurnacesSupported()) {
			new FurnaceEfficiencyIncrement(this).runTaskTimer(this, 1, 2);
		}

		if (getConfig().getInt("autosave") > 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					saveFurnaceStorage();
				}
			}.runTaskTimer(this, 0, getConfig().getInt("autosave") * 1200);
		}
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		for (Furnace furnace : this.furnaces.values()) {
			this.saveFurnace(furnace);
		}
		saveFurnaceStorage();
		this.furnaces.clear();
		this.furnaces = null;
		this.enchantments.clear();
		this.enchantments = null;
	}

	@SuppressWarnings("unchecked")
	public HashSet<Enchantment> getEnchantments() {
		return (HashSet<Enchantment>) enchantments.clone();
	}

	public boolean isBlacklist() {
		return isBlacklist;
	}

	public List<String> getFortuneList() {
		return fortuneList;
	}

	public int getFurnaceEnchantability() {
		return getConfig().getInt("furnace_enchantability");
	}

	public boolean areEnchantmentsCompatible(Enchantment ench1, Enchantment ench2) {
		return !ench1.equals(ench2) && !incompatibleEnchants.containsEntry(ench1, ench2);
	}

	public void createFurnace(final Block block, ItemStack is) {
		if (is.getType() != Material.FURNACE
				|| getConfig().getStringList("disabled_worlds").contains(
						block.getWorld().getName().toLowerCase())) {
			return;
		}
		final Furnace furnace = new Furnace(block, is.clone());
		if (furnace.getCookModifier() > 0 || furnace.getBurnModifier() > 0 || furnace.getFortune() > 0 || furnace.canPause()) {
			this.furnaces.put(block, furnace);
			saveFurnace(furnace);
		}
		if (getConfig().getInt("autosave") < 1) {
			saveFurnaceStorage();
		}
	}

	public ItemStack destroyFurnace(Block block) {
		Furnace f = furnaces.remove(block);
		if (f == null || block.getType() != Material.FURNACE && block.getType() != Material.BURNING_FURNACE) {
			return null;
		}
		getFurnaceStorage().set(getSaveString(block), null);
		dirty = true;
		if (getConfig().getInt("autosave") < 1) {
			saveFurnaceStorage();
		}
		ItemStack is = f.getItemStack();
		if (is.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
			// Silk time isn't supposed to be preserved when broken.
			is.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
		}
		return is;
	}

	public Collection<Furnace> getFurnaces() {
		return furnaces.values();
	}

	public Furnace getFurnace(Block block) {
		return furnaces.get(block);
	}

	public boolean isFurnace(Block block) {
		return furnaces.containsKey(block);
	}

	public void loadChunkFurnaces(Chunk chunk) {
		if (getConfig().getStringList("disabled_worlds").contains(chunk.getWorld().getName().toLowerCase())) {
			return;
		}
		String path = new StringBuilder(chunk.getWorld().getName()).append('.')
				.append(chunk.getX()).append('_').append(chunk.getZ()).toString();
		ConfigurationSection chunkSection = getFurnaceStorage().getConfigurationSection(path);
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
				ItemStack itemStack = getFurnaceStorage().getItemStack(path + '.' + xyz + ".itemstack");
				if (type == Material.FURNACE || type == Material.BURNING_FURNACE) {
					Furnace furnace = new Furnace(block, itemStack);
					furnaces.put(block, furnace);
				} else {
					iterator.remove();
					getFurnaceStorage().set(path + '.' + xyz, null);
					dirty = true;
					getLogger().warning("Removed invalid save: " + itemStack.toString() + " at " + block.getLocation().toString());
				}
			} catch (NumberFormatException e) {
				getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(split));
			} catch (ClassCastException e) {
				getLogger().warning("Invalid itemstack saved for " + path + '.' + xyz);
			}
		}
		if (chunkKeys.isEmpty()) {
			getFurnaceStorage().set(path, null);
		}
	}

	public void unloadChunkFurnaces(Chunk chunk) {
		for (Iterator<Entry<Block, Furnace>> iterator = furnaces.entrySet().iterator(); iterator.hasNext();) {
			Entry<Block, Furnace> entry = iterator.next();
			if (!entry.getKey().getWorld().equals(chunk.getWorld())
					|| entry.getKey().getX() >> 4 != chunk.getX()
					|| entry.getKey().getZ() >> 4 != chunk.getZ()) {
				continue;
			}
			if (entry.getValue().canPause()) {
				saveFurnace(entry.getValue());
			}
			iterator.remove();
		}
		if (getConfig().getInt("autosave") < 1) {
			saveFurnaceStorage();
		}
	}

	private void loadFurnaces() {
		// Backwards compatibility for version < 1.4.0
		ConfigurationSection furnaceSection = getFurnaceStorage().getConfigurationSection("furnaces");
		convertLegacyFurnaces(furnaceSection);
		getFurnaceStorage().set("furnaces", null);

		// Backwards compatibility for version < 1.3.0
		furnaceSection = getConfig().getConfigurationSection("furnaces");
		convertLegacyFurnaces(furnaceSection);
		getConfig().set("furnaces", null);

		// Save converted furnaces, if any
		if (getConfig().getInt("autosave") < 1) {
			saveFurnaceStorage();
		}

		Set<String> worlds = getFurnaceStorage().getKeys(false);
		for (World world : getServer().getWorlds()) {
			if (!worlds.contains(world.getName())
					|| getConfig().getStringList("disabled_worlds").contains(
							world.getName().toLowerCase())) {
				continue;
			}
			for (Chunk chunk : world.getLoadedChunks()) {
				loadChunkFurnaces(chunk);
			}
		}
	}

	private void convertLegacyFurnaces(ConfigurationSection section) {
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
				getLogger().warning("Unable to split location properly! " + legacy + " split to " + Arrays.toString(loc));
				continue;
			}
			if (getConfig().getStringList("disabled_worlds").contains(loc[0].toLowerCase())) {
				continue;
			}
			try {
				World world = getServer().getWorld(loc[0]);
				if (world != null) {
					block = new Location(world, Integer.valueOf(loc[1]), Integer.valueOf(loc[2]), Integer.valueOf(loc[3])).getBlock();
					modern = getSaveString(block);
				} else {
					modern = getSaveString(loc[0], Integer.valueOf(loc[1]), Integer.valueOf(loc[2]), Integer.valueOf(loc[3]));
				}
			} catch (Exception e) {
				getLogger().warning("Error loading block: " + legacy);
				if (e instanceof NumberFormatException) {
					getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(loc));
				} else {
					getLogger().severe("An unknown exception occurred!");
					e.printStackTrace();
					getLogger().severe("Please report this error!");
				}
				continue;
			}

			// Verify block is a furnace if world is loaded
			if (block != null && (block.getType() != Material.FURNACE && block.getType() != Material.BURNING_FURNACE)) {
				continue;
			}

			ItemStack furnaceStack = new ItemStack(Material.FURNACE);
			ItemMeta im = furnaceStack.getItemMeta();
			String furnaceName = ((org.bukkit.block.Furnace) block.getState()).getInventory().getTitle();
			if (!furnaceName.equals("container.furnace")) {
				im.setDisplayName(furnaceName);
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
		if (furnaceSaves != null) {
			return furnaceSaves;
		}
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}
		File file = new File(getDataFolder(), "furnaces.yml");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Cannot write furnace save file! Make sure your file permissions are set up properly.", e);
			}
		}
		furnaceSaves = YamlConfiguration.loadConfiguration(file);
		return furnaceSaves;
	}

	private void saveFurnaceStorage() {
		if (furnaceSaves == null || !dirty) {
			return;
		}
		File file = new File(getDataFolder(), "furnaces.yml");
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			furnaceSaves.save(file);
			dirty = false;
		} catch (IOException e) {
			throw new RuntimeException("Cannot write furnace save file! Make sure your file permissions are set up properly.", e);
		}
	}

	private void saveFurnace(Furnace furnace) {
		getFurnaceStorage().set(getSaveString(furnace.getBlock()) + ".itemstack", furnace.getItemStack());
		dirty = true;
	}

	private String getSaveString(Block block) {
		return this.getSaveString(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	private String getSaveString(String world, int x, int y, int z) {
		return String.format("%s.%s_%s.%s_%s_%s", world, x >> 4, z >> 4, x, y, z);
	}

	private void updateConfig() {
		saveDefaultConfig();
		Set<String> options = getConfig().getDefaults().getKeys(false);
		Set<String> current = getConfig().getKeys(false);

		for (String s : options) {
			if (s.equals("enchantment_incompatibilities")) {
				continue;
			}
			if (!current.contains(s)) {
				getConfig().set(s, getConfig().getDefaults().get(s));
			}
		}

		for (String s : current) {
			if (!options.contains(s)) {
				getConfig().set(s, null);
			}
		}

		getConfig().options().copyHeader(true);
	}
}
