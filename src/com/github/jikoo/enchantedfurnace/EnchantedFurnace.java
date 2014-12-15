package com.github.jikoo.enchantedfurnace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

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
	private boolean isBlacklist, isDefaultFortune;

	@Override
	public void onEnable() {
		instance = this;

		enchantments = new HashSet<Enchantment>();
		enchantments.add(Enchantment.DIG_SPEED);
		enchantments.add(Enchantment.DURABILITY);
		enchantments.add(Enchantment.LOOT_BONUS_BLOCKS);
		enchantments.add(Enchantment.SILK_TOUCH);

		this.furnaces = new HashMap<Block, Furnace>();
		this.loadFurnaces();

		updateConfig();

		isBlacklist = getConfig().getString("fortune_list_mode").matches(".*[Bb][Ll][Aa][Cc][Kk].*");

		// Only use vanilla fortune if explicitly defined as it results in higher average production
		isDefaultFortune = !getConfig().getString("fortune_mode").matches("[Vv][Aa][Nn][Ii][Ll][Ll][Aa]");

		// TODO compare string to MaterialData
		fortuneList = new ArrayList<String>(getConfig().getStringList("fortune_list"));

		getServer().getPluginManager().registerEvents(new FurnaceListener(), this);
		getServer().getPluginManager().registerEvents(new Enchanter(), this);
		getServer().getPluginManager().registerEvents(new AnvilEnchanter(), this);
		new FurnaceEfficiencyIncrement().runTaskTimer(this, 1, 2);
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		instance = null;
		for (Furnace furnace : this.furnaces.values()) {
			this.saveFurnace(furnace);
		}
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

	public boolean isDefaultFortune() {
		return isDefaultFortune;
	}

	public List<String> getFortuneList() {
		return fortuneList;
	}

	public void createFurnace(Block b, ItemStack is) {
		if (is.getType() != Material.FURNACE) {
			return;
		}

		Furnace f = new Furnace(b, is.getEnchantmentLevel(Enchantment.DIG_SPEED),
				is.getEnchantmentLevel(Enchantment.DURABILITY),
				is.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS),
				is.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0);
		if (f.getCookModifier() > 0 || f.getBurnModifier() > 0 || f.getFortune() > 0 || f.canPause()) {
			this.furnaces.put(b, f);
			saveFurnace(f);
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
			if (b != null && (b.getType() == Material.FURNACE || b.getType() == Material.BURNING_FURNACE)) {
				Furnace furnace = new Furnace(b, getConfig().getInt("furnaces." + s + ".efficiency"),
						getConfig().getInt("furnaces." + s + ".unbreaking"),
						getConfig().getInt("furnaces." + s + ".fortune"),
						(short) getConfig().getInt("furnaces." + s + ".silk"));
				furnaces.put(b, furnace);
				if (save) {
					saveFurnace(furnace);
				}
			}
		}
	}

	private YamlConfiguration getFurnaceStorage() {
		if (furnaceSaves != null) {
			return furnaceSaves;
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

	private void saveFurnace(Furnace f) {
		String loc = blockToLocString(f.getBlock());
		getFurnaceStorage().set("furnaces." + loc + ".efficiency", f.getCookModifier());
		getFurnaceStorage().set("furnaces." + loc + ".unbreaking", f.getBurnModifier());
		getFurnaceStorage().set("furnaces." + loc + ".fortune", f.getFortune());
		getFurnaceStorage().set("furnaces." + loc + ".silk", f.getFrozenTicks());
		saveFurnaceStorage();
	}

	private String blockToLocString(Block b) {
		return b.getWorld().getName() + "," + b.getX() + "," + b.getY() + "," + b.getZ();
	}

	private Block locStringToBlock(String s) {
		String[] loc = s.split(",");
		try {
			return new Location(getServer().getWorld(loc[0]), Integer.valueOf(loc[1]), Integer.valueOf(loc[2]), Integer.valueOf(loc[3])).getBlock();
		} catch (Exception e) {
			getLogger().warning("Invalid saved furnace: " + s);
			getConfig().set("furnaces." + s, null);
			return null;
		}
	}

	private void updateConfig() {
		Set<String> options = getConfig().getDefaults().getKeys(false);
		Set<String> current = getConfig().getKeys(false);
		boolean changed = false;

		for (String s : options) {
			if (!current.contains(s)) {
				getConfig().set(s, getConfig().getDefaults().get(s));
				changed = true;
			}
		}

		for (String s : current) {
			if (!options.contains(s)) {
				getConfig().set(s, null);
				changed = true;
			}
		}

		getConfig().options().copyHeader(true);

		if (changed) {
			saveConfig();
		}
	}
}
