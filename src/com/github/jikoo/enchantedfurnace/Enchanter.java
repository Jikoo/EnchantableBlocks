package com.github.jikoo.enchantedfurnace;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

/**
 * Originally based on @andrepl's
 * <a href=https://gist.github.com/andrepl/5522053>example plugin</a>
 * for his Craftbukkit pull, overhauled completely for EnchantedFurnace 1.2.0
 * 
 * @author Jikoo
 */
public class Enchanter  implements Listener {

	private HashSet<Enchantment> enchantments;
	private Random rand;

	public Enchanter() {
		enchantments = new HashSet<Enchantment>();
		enchantments.add(Enchantment.DIG_SPEED);
		enchantments.add(Enchantment.DURABILITY);
		enchantments.add(Enchantment.LOOT_BONUS_BLOCKS);
		enchantments.add(Enchantment.SILK_TOUCH);
		rand = new Random();
	}

	private int getButtonLevel(int slot, int shelves) {
		// Vanilla - get levels to display in table buttons
		if (shelves > 15) {
			shelves = 15;
		}
		int j = rand.nextInt(8) + 1 + (shelves >> 1) + rand.nextInt(shelves + 1);
		if (slot == 0) {
			return Math.max(j / 3, 1);
		}
		if (slot == 1) {
			return (j * 2 / 3 + 1);
		}
		return Math.max(j, shelves * 2);
	}

	@EventHandler(ignoreCancelled = false)
	public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
		if (event.getItem().getEnchantments().size() == 0
				&& event.getItem().getType().equals(Material.FURNACE)
				&& event.getEnchanter().hasPermission("enchantedfurnace.enchant")) {
			event.setCancelled(false);
			for (int i = 0; i < 3; i++) {
				event.getExpLevelCostsOffered()[i] = getButtonLevel(i, event.getEnchantmentBonus());
			}
		}
	}

	@EventHandler
	public void onEnchantItem(EnchantItemEvent event) {
		if (event.getItem().getType().equals(Material.FURNACE)) {
			if (!event.getEnchanter().hasPermission("enchantedfurnace.enchant")) {
				return;
			}
			int effectiveLevel = getEnchantingLevel(event.getExpLevelCost());
			@SuppressWarnings("unchecked")
			HashSet<Enchantment> possibleEnchants = (HashSet<Enchantment>) enchantments.clone();
			Iterator<Enchantment> iterator = possibleEnchants.iterator();
			while (iterator.hasNext()) {
				if (getEnchantmentLevel(iterator.next(), effectiveLevel) == 0) {
					iterator.remove();
				}
			}
			Enchantment ench = getWeightedEnchant(possibleEnchants);
			event.getEnchantsToAdd().put(ench, getEnchantmentLevel(ench, effectiveLevel));
			possibleEnchants.remove(ench);
			iterator = possibleEnchants.iterator();
			while (iterator.hasNext()) {
				if (ench.conflictsWith(iterator.next())) {
					iterator.remove();
				}
			}
			while (rand.nextDouble() < ((effectiveLevel / Math.pow(2, event.getEnchantsToAdd().size())) / 50) && possibleEnchants.size() > 0) {
				ench = getWeightedEnchant(possibleEnchants);
				event.getEnchantsToAdd().put(ench, getEnchantmentLevel(ench, effectiveLevel));
				possibleEnchants.remove(ench);
				iterator = possibleEnchants.iterator();
				while (iterator.hasNext()) {
					if (ench.conflictsWith(iterator.next())) {
						iterator.remove();
					}
				}
			}
		}
	}

	private int getEnchantingLevel(int displayedLevel) {
		// Furnaces are (logically) as enchantable as stone.
		// Vanilla: enchant level = button level + rand(enchantabity / 4) + rand(enchantabity / 4) + 1
		// Stone is 5 enchantability. No need to redo division each time, comes to 1 with int rounding.
		// Random.nextInt(int i) is 0 inclusive to i exclusive, so nextInt(1) is 0.
		int enchantingLevel = displayedLevel + 1;
		// Vanilla: random enchantability bonus 85-115%
		double bonus = (rand.nextDouble() + rand.nextDouble() - 1) * 0.15 + 1;
		return (int) (enchantingLevel * bonus + 0.5);
	}

	private int getEnchantmentLevel(Enchantment enchant, int lvl) {
		// Not worried about high end cap reducing silk/fortune rates - maximum possible random level is 37 since using stone.

		// Enchantments use upper value if within multiple ranges. Why there's a larger range at all, I don't know.
		if (enchant == Enchantment.DIG_SPEED) {
			// Efficiency 1:1–51 2:11–61 3:21–71 4:31–81 5:41–91
			return lvl < 1 ? 0 : lvl < 11 ? 1 : lvl < 21 ? 2 : lvl < 31 ? 3 : lvl < 41 ? 4 : 5;
		}
		if (enchant == Enchantment.DURABILITY) {
			// Unbreaking 1:5-55 2:13-63 3:21-71
			return lvl < 5 ? 0 : lvl < 13 ? 1 : lvl < 21 ? 2 : 3;
		}
		if (enchant == Enchantment.LOOT_BONUS_BLOCKS) {
			// Fortune 1:15-65 2:24-74 3:33-83
			return lvl < 15 ? 0 : lvl < 24 ? 1 : lvl < 33 ? 2 : 3;
		}
		if (enchant == Enchantment.SILK_TOUCH) {
			// Silk Touch 1:15-65
			return lvl < 15 ? 0 : 1;
		}
		return 0;
	}

	private int getWeight(Enchantment enchant) {
		if (enchant == Enchantment.DIG_SPEED) {
			return 10;
		}
		if (enchant == Enchantment.DURABILITY) {
			return 5;
		}
		if (enchant == Enchantment.LOOT_BONUS_BLOCKS) {
			return 2;
		}
		if (enchant == Enchantment.SILK_TOUCH) {
			return 1;
		}
		return 0;
	}

	private Enchantment getWeightedEnchant(HashSet<Enchantment> enchants) {
		int random = 0;
		for (Enchantment ench : enchants) {
			random += getWeight(ench);
		}
		if (random <= 0) {
			// Gets hit sometimes cause I'm bad at code apparently.
			return Enchantment.DIG_SPEED;
		}
		random = rand.nextInt(random) + 1;
		for (Enchantment ench : enchants) {
			int weight = getWeight(ench);
			if (random >= weight) {
				random -= weight;
			} else {
				return ench;
			}
		}
		// Shouldn't ever hit this, but it's here just in case. Efficiency is a safe default.
		return Enchantment.DIG_SPEED;
	}
}
