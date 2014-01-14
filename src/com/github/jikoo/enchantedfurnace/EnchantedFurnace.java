package com.github.jikoo.enchantedfurnace;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Jikoo
 *
 */
public class EnchantedFurnace extends JavaPlugin {

	private Map<Block, Furnace> furnaces;
	private static EnchantedFurnace instance;

	@Override
	public void onEnable() {
		instance = this;
		furnaces = new HashMap<Block, Furnace>();
		this.load();
		getServer().getPluginManager().registerEvents(new FurnaceListener(), this);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new FurnaceTick(), 1, 1);
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelAllTasks();;
		this.save();
		instance = null;
	}

	protected void createFurnace(Block b, ItemStack is) {
		if (is.getType() != Material.FURNACE) {
			return;
		}

		Furnace f = new Furnace(b, is.getEnchantmentLevel(Enchantment.DIG_SPEED),
				is.getEnchantmentLevel(Enchantment.DURABILITY),
				is.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS),
				is.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0);
		if (f.getCookModifier() > 0 || f.getBurnModifier() > 0 || f.getFortune() > 0 || f.canPause()) {
			this.furnaces.put(b, f);
		}
	}

	protected ItemStack destroyFurnace(Block b) {
		Furnace f = furnaces.remove(b);
		if (f == null) {
			return null;
		}
		ItemStack drop = new ItemStack(Material.FURNACE);
		ItemMeta im = drop.getItemMeta();
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

	protected Collection<Furnace> getFurnaces() {
		return furnaces.values();
	}

	protected Furnace getFurnace(Block b) {
		return furnaces.get(b);
	}

	protected boolean isFurnace(Block b) {
		return furnaces.containsKey(b);
	}

	protected static EnchantedFurnace getInstance() {
		return instance;
	}

	private void load() {
		Set<String> furnaceLocs;
		try {
			furnaceLocs = getConfig().getConfigurationSection("furnaces").getKeys(false);
		} catch (NullPointerException e) {
			return; // Config nonexistant
		}
		for (String s : furnaceLocs) {
			Block b = locStringToBlock(s);
			if (b != null && (b.getType() == Material.FURNACE || b.getType() == Material.BURNING_FURNACE)) {
				furnaces.put(b, new Furnace(b, getConfig().getInt("furnaces." + s + ".efficiency"),
						getConfig().getInt("furnaces." + s + ".unbreaking"),
						getConfig().getInt("furnaces." + s + ".fortune"),
						(short) getConfig().getInt("furnaces." + s + ".silk")));
			}
		}
		getConfig().set("furnaces", null);
		saveConfig();
	}

	private void save() {
		for (Furnace f : furnaces.values()) {
			String loc =blockToLocString(f.getBlock());
			getConfig().set("furnaces." + loc + ".efficiency", f.getCookModifier());
			getConfig().set("furnaces." + loc + ".unbreaking", f.getBurnModifier());
			getConfig().set("furnaces." + loc + ".fortune", f.getFortune());
			getConfig().set("furnaces." + loc + ".silk", f.canPause() ? f.getFrozenTicks() : -1);
		}
		saveConfig();
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
			return null;
		}
	}
}
