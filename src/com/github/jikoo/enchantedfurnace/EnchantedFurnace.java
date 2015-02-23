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

import com.google.common.collect.HashMultimap;

/**
 * Plugin for adding effects to furnaces based on enchantments.
 * 
 * @author Jikoo
 */
public class EnchantedFurnace extends JavaPlugin {

	private static EnchantedFurnace instance;
	private YamlConfiguration furnaceSaves;
	private HashSet<Enchantment> enchantments;
	private Map<Location, Furnace> furnaces;
	private ArrayList<String> fortuneList;
	private boolean isBlacklist;
	private HashMultimap<Enchantment, Enchantment> incompatibleEnchants;

	@Override
	public void onEnable() {
		instance = this;

		this.furnaces = new HashMap<Location, Furnace>();
		this.loadFurnaces();

		updateConfig();

		ArrayList<String> disabledWorlds = new ArrayList<String>();
		for (String worldName : getConfig().getStringList("disabled_worlds")) {
			if (!disabledWorlds.contains(worldName.toLowerCase())) {
				disabledWorlds.add(worldName.toLowerCase());
			}
		}
		getConfig().set("disabled_worlds", disabledWorlds);

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
		getConfig().set("fortune_list", fortuneList);

		HashSet<String> allowedEnchantments = new HashSet<String>();
		allowedEnchantments.add("DIG_SPEED");
		allowedEnchantments.add("DURABILITY");
		allowedEnchantments.add("LOOT_BONUS_BLOCKS");
		allowedEnchantments.add("SILK_TOUCH");
		for (String enchantment : getConfig().getStringList("furnace_enchantments")) {
			if (allowedEnchantments.contains(enchantment)) {
				allowedEnchantments.remove(enchantment);
			}
		}
		enchantments = new HashSet<Enchantment>();
		for (String enchantment : allowedEnchantments) {
			enchantments.add(Enchantment.getByName(enchantment));
		}

		// Enchantability < 4 would pass a Random 0 or lower.
		// Enchantablity < 8 has no effect on end enchantments.
		if (getConfig().getInt("furnace_enchantability") < 4) {
			getConfig().set("furnace_enchantability", 4);
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

		// Potential for multiple changes along the way, just save to be safe.
		saveConfig();

		getServer().getPluginManager().registerEvents(new FurnaceListener(), this);
		getServer().getPluginManager().registerEvents(new Enchanter(), this);
		getServer().getPluginManager().registerEvents(new AnvilEnchanter(), this);
		new FurnaceEfficiencyIncrement().runTaskTimer(this, 1, 2);
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		for (Furnace furnace : this.furnaces.values()) {
			this.saveFurnace(furnace, true);
		}
		saveFurnaceStorage();
		this.furnaces.clear();
		this.furnaces = null;
		this.enchantments.clear();
		this.enchantments = null;
		instance = null;
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

	public void createFurnace(Block block, ItemStack is) {
		if (is.getType() != Material.FURNACE
				|| getConfig().getStringList("disabled_worlds").contains(
						block.getWorld().getName().toLowerCase())) {
			return;
		}
		Location location = block.getLocation();
		Furnace f = new Furnace(location, is.clone());
		if (f.getCookModifier() > 0 || f.getBurnModifier() > 0 || f.getFortune() > 0 || f.canPause()) {
			this.furnaces.put(location, f);
			saveFurnace(f, false);
		}
	}

	public ItemStack destroyFurnace(Block b) {
		Furnace f = furnaces.remove(b.getLocation());
		if (f == null || b.getType() != Material.FURNACE && b.getType() != Material.BURNING_FURNACE) {
			return null;
		}
		getFurnaceStorage().set(getSaveString(b.getLocation()), null);
		saveFurnaceStorage();
		if (f.getItemStack().containsEnchantment(Enchantment.SILK_TOUCH)) {
			// Silk time isn't supposed to be preserved when broken.
			f.getItemStack().addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
		}
		return f.getItemStack();
	}

	public Collection<Furnace> getFurnaces() {
		return furnaces.values();
	}

	public Furnace getFurnace(Block b) {
		return furnaces.get(b.getLocation());
	}

	public boolean isFurnace(Block b) {
		return furnaces.containsKey(b.getLocation());
	}

	public static EnchantedFurnace getInstance() {
		return instance;
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
		for (String xyz : chunkSection.getKeys(false)) {
			String[] split = xyz.split("_");
			try {
				Location location = new Location(chunk.getWorld(), Integer.valueOf(split[0]),
						Integer.valueOf(split[1]), Integer.valueOf(split[2]));
				Material type = location.getBlock().getType();
				ItemStack itemStack = (ItemStack) getFurnaceStorage().get(path + '.' + xyz + ".itemstack");
				if (type == Material.FURNACE || type == Material.BURNING_FURNACE) {
					furnaces.put(location, new Furnace(location, itemStack));
				} else {
					getFurnaceStorage().set(path + '.' + xyz, null);
					getLogger().warning("Removed invalid save: " + itemStack.toString() + " at " + location.toString());
				}
			} catch (NumberFormatException e) {
				getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(split));
			} catch (ClassCastException e) {
				getLogger().warning("Invalid itemstack saved for " + path + '.' + xyz);
			}
		}
	}

	public void unloadChunkFurnaces(Chunk chunk) {
		for (Iterator<Entry<Location, Furnace>> iterator = furnaces.entrySet().iterator(); iterator.hasNext();) {
			Entry<Location, Furnace> entry = iterator.next();
			if (!entry.getKey().getWorld().equals(chunk.getWorld())
					|| entry.getKey().getBlockX() >> 4 != chunk.getX()
					|| entry.getKey().getBlockZ() >> 4 != chunk.getZ()) {
				continue;
			}
			iterator.remove();
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
		Set<String> legacyFurnaces = section.getKeys(false);
		for (String legacy : legacyFurnaces) {
			Block block = locStringToBlock(legacy);
			if (getConfig().getStringList("disabled_worlds").contains(block.getWorld().getName().toLowerCase())) {
				continue;
			}
			if (block != null && (block.getType() == Material.FURNACE || block.getType() == Material.BURNING_FURNACE)) {
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
				Furnace furnace = new Furnace(block.getLocation(), furnaceStack);
				section.set(legacy, null);
				saveFurnace(furnace, true);
				continue;
			}
		}
		saveFurnaceStorage();
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
		if (furnaceSaves == null) {
			return;
		}
		File file = new File(getDataFolder(), "furnaces.yml");
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			furnaceSaves.save(file);
		} catch (IOException e) {
			throw new RuntimeException("Cannot write furnace save file! Make sure your file permissions are set up properly.", e);
		}
	}

	private void saveFurnace(Furnace f, boolean batch) {
		getFurnaceStorage().set(getSaveString(f.getLocation()) + ".itemstack", f.getItemStack());
		if (!batch) {
			saveFurnaceStorage();
		}
	}

	private Block locStringToBlock(String s) {
		String[] loc = s.split(",");
		try {
			if (loc.length != 4) {
				getLogger().warning("Unable to split location properly! " + s + " split to " + Arrays.toString(loc));
				return null;
			}
			World world = getServer().getWorld(loc[0]);
			if (world == null) {
				getLogger().warning("No world by the name of \"" + loc[0] + "\" exists!");
				return null;
			}
			return new Location(world, Integer.valueOf(loc[1]), Integer.valueOf(loc[2]), Integer.valueOf(loc[3])).getBlock();
		} catch (Exception e) {
			getLogger().warning("Error loading block: " + s);
			if (e instanceof NumberFormatException) {
				getLogger().warning("Coordinates cannot be parsed from " + Arrays.toString(loc));
			} else {
				getLogger().severe("An unknown exception occurred!");
				e.printStackTrace();
				getLogger().severe("Please report this error!");
			}
			return null;
		}
	}

	private String getSaveString(Location location) {
		return new StringBuilder(location.getWorld().getName()).append('.')
				.append((location.getBlockX() >> 4)).append('_')
				.append(location.getBlockZ() >> 4).append('.')
				.append(location.getBlockX()).append('_')
				.append(location.getBlockY()).append('_')
				.append(location.getBlockZ()).toString();
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
