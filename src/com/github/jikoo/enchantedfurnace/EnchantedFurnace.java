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
import java.util.Set;

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
	private Map<Block, Furnace> furnaces;
	private ArrayList<String> fortuneList;
	private boolean isBlacklist;
	private HashMultimap<Enchantment, Enchantment> incompatibleEnchants;

	@Override
	public void onEnable() {
		instance = this;

		this.furnaces = new HashMap<Block, Furnace>();
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

	public void createFurnace(Block b, ItemStack is) {
		if (is.getType() != Material.FURNACE
				|| getConfig().getStringList("disabled_worlds").contains(
						b.getWorld().getName().toLowerCase())) {
			return;
		}

		Furnace f = new Furnace(b, is.getEnchantmentLevel(Enchantment.DIG_SPEED),
				is.getEnchantmentLevel(Enchantment.DURABILITY),
				is.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS),
				is.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0);
		if (f.getCookModifier() > 0 || f.getBurnModifier() > 0 || f.getFortune() > 0 || f.canPause()) {
			this.furnaces.put(b, f);
			saveFurnace(f, false);
		}
	}

	public ItemStack destroyFurnace(Block b) {
		Furnace f = furnaces.remove(b);
		if (f == null || b.getType() != Material.FURNACE && b.getType() != Material.BURNING_FURNACE) {
			return null;
		}
		getFurnaceStorage().set("furnaces." + blockToLocString(f.getBlock()), null);
		saveFurnaceStorage();
		ItemStack drop = new ItemStack(Material.FURNACE);
		ItemMeta im = drop.getItemMeta();
		String furnaceName = ((org.bukkit.block.Furnace) b.getState()).getInventory().getTitle();
		if (!furnaceName.equals("container.furnace")) {
			im.setDisplayName(furnaceName);
		}
		if (f.getCookModifier() > 0) {
			im.addEnchant(Enchantment.DIG_SPEED, f.getCookModifier(), true);
		}
		if (f.getBurnModifier() > 0) {
			im.addEnchant(Enchantment.DURABILITY, f.getBurnModifier(), true);
		}
		if (f.getFortune() > 0) {
			im.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, f.getFortune(), true);
		}
		if (f.canPause()) {
			im.addEnchant(Enchantment.SILK_TOUCH, 1, true);
		}
		drop.setItemMeta(im);
		return drop;
	}

	public Collection<Furnace> getFurnaces() {
		return furnaces.values();
	}

	public Furnace getFurnace(Block b) {
		return furnaces.get(b);
	}

	public boolean isFurnace(Block b) {
		return furnaces.containsKey(b);
	}

	public static EnchantedFurnace getInstance() {
		return instance;
	}

	private void loadFurnaces() {
		ConfigurationSection furnaceSection = getFurnaceStorage().getConfigurationSection("furnaces");
		loadFurnacesFromConfigSection(furnaceSection, false);

		// Backwards compatibility for version < 1.3.0
		furnaceSection = getConfig().getConfigurationSection("furnaces");
		loadFurnacesFromConfigSection(furnaceSection, true);
		// Wipe old newly loaded and saved furnaces.
		getConfig().set("furnaces", null);
		saveConfig();
	}

	private void loadFurnacesFromConfigSection(ConfigurationSection section, boolean save) {
		if (section == null) {
			// No saves here
			return;
		}
		Set<String> furnaceLocs = section.getKeys(false);
		for (String s : furnaceLocs) {
			Block b = locStringToBlock(s);
			if (getConfig().getStringList("disabled_worlds").contains(b.getWorld().getName().toLowerCase())) {
				continue;
			}
			if (b != null && (b.getType() == Material.FURNACE || b.getType() == Material.BURNING_FURNACE)) {
				Furnace furnace = new Furnace(b, section.getInt(s + ".efficiency", 0),
						section.getInt(s + ".unbreaking", 0), section.getInt(s + ".fortune", 0),
						(short) section.getInt(s + ".silk", -1));
				furnaces.put(b, furnace);
				if (save) {
					saveFurnace(furnace, true);
				}
			}
		}
		if (save) {
			saveFurnaceStorage();
		}
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
		String loc = blockToLocString(f.getBlock());
		if (f.getCookModifier() > 0) {
			getFurnaceStorage().set("furnaces." + loc + ".efficiency", f.getCookModifier());
		}
		if (f.getBurnModifier() > 0) {
			getFurnaceStorage().set("furnaces." + loc + ".unbreaking", f.getBurnModifier());
		}
		if (f.getFortune() > 0) {
			getFurnaceStorage().set("furnaces." + loc + ".fortune", f.getFortune());
		}
		if (f.getFrozenTicks() > -1) {
			getFurnaceStorage().set("furnaces." + loc + ".silk", f.getFrozenTicks());
		}
		if (!batch) {
			saveFurnaceStorage();
		}
	}

	private String blockToLocString(Block b) {
		return b.getWorld().getName() + "," + b.getX() + "," + b.getY() + "," + b.getZ();
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
