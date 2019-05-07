package com.github.jikoo.enchantableblocks.enchanting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;

import org.bukkit.enchantments.Enchantment;

/**
 * Utility for calculating enchantments.
 *
 * @author Jikoo
 */
class EnchantmentUtil {

	private EnchantmentUtil() {}

	static Integer[] getButtonLevels(int shelves) {

		Integer[] levels = new Integer[3];

		for (int button = 0; button < 3; ++button) {
			// Vanilla - get levels to display in table buttons
			if (shelves > 15) {
				shelves = 15;
			}
			Random random = ThreadLocalRandom.current();
			int i = random.nextInt(8) + 1 + (shelves >> 1) + random.nextInt(shelves + 1);
			int level = button == 0 ?  Math.max(i / 3, 1) : button == 1 ?  i * 2 / 3 + 1 : Math.max(i, shelves * 2);

			levels[button] = level > button + 1 ? level : 0;
		}

		return levels;
	}

	static Map<Enchantment, Integer> calculateFurnaceEnchants(EnchantableBlocksPlugin plugin, int enchantingLevel) {
		int effectiveLevel = getFurnaceEnchantingLevel(plugin, enchantingLevel);
		HashSet<Enchantment> possibleEnchants = plugin.getEnchantments();
		Iterator<Enchantment> iterator = possibleEnchants.iterator();

		while (iterator.hasNext()) {
			if (getFurnaceEnchantmentLevel(iterator.next(), effectiveLevel) == 0) {
				iterator.remove();
			}
		}

		Map<Enchantment, Integer> enchantments = new HashMap<>();
		boolean firstRun = true;

		while (firstRun || ThreadLocalRandom.current().nextDouble() < ((effectiveLevel / Math.pow(2, enchantments.size())) / 50) && possibleEnchants.size() > 0) {
			firstRun = false;
			Enchantment ench = getWeightedEnchant(possibleEnchants);
			enchantments.put(ench, getFurnaceEnchantmentLevel(ench, effectiveLevel));
			possibleEnchants.remove(ench);
			iterator = possibleEnchants.iterator();
			while (iterator.hasNext()) {
				if (plugin.areEnchantmentsIncompatible(ench, iterator.next())) {
					iterator.remove();
				}
			}
		}

		return enchantments;
	}

	private static int getFurnaceEnchantingLevel(EnchantableBlocksPlugin plugin, int displayedLevel) {
		// Vanilla: enchant level = button level + rand(enchantabity / 4 + 1) + rand(enchantabity / 4 + 1) + 1
		int enchantability = plugin.getFurnaceEnchantability() / 4 + 1;
		Random random = ThreadLocalRandom.current();
		int enchantingLevel = displayedLevel + 1 + random.nextInt(enchantability) + random.nextInt(enchantability);
		// Vanilla: random enchantability bonus 85-115%
		double bonus = (random.nextDouble() + random.nextDouble() - 1) * 0.15 + 1;
		enchantingLevel = (int) (enchantingLevel * bonus + 0.5);
		return enchantingLevel < 1 ? 1 : enchantingLevel;
	}

	private static int getFurnaceEnchantmentLevel(Enchantment enchant, int lvl) {
		// Not worried about high end cap reducing silk/fortune rates

		// Enchantments use upper value if within multiple ranges. Why there's a larger range at all, I don't know.
		if (enchant.equals(Enchantment.DIG_SPEED)) {
			// Efficiency 1:1–51 2:11–61 3:21–71 4:31–81 5:41–91
			return lvl < 1 ? 0 : lvl < 11 ? 1 : lvl < 21 ? 2 : lvl < 31 ? 3 : lvl < 41 ? 4 : 5;
		}
		if (enchant.equals(Enchantment.DURABILITY)) {
			// Unbreaking 1:5-55 2:13-63 3:21-71
			return lvl < 5 ? 0 : lvl < 13 ? 1 : lvl < 21 ? 2 : 3;
		}
		if (enchant.equals(Enchantment.LOOT_BONUS_BLOCKS)) {
			// Fortune 1:15-65 2:24-74 3:33-83
			return lvl < 15 ? 0 : lvl < 24 ? 1 : lvl < 33 ? 2 : 3;
		}
		if (enchant.equals(Enchantment.SILK_TOUCH)) {
			// Silk Touch 1:15-65
			return lvl < 15 ? 0 : 1;
		}
		return 0;
	}

	private static int getWeight(Enchantment enchant) {
		if (enchant.equals(Enchantment.DIG_SPEED)) {
			return 10;
		}
		if (enchant.equals(Enchantment.DURABILITY)) {
			return 5;
		}
		if (enchant.equals(Enchantment.LOOT_BONUS_BLOCKS)) {
			return 2;
		}
		if (enchant.equals(Enchantment.SILK_TOUCH)) {
			return 1;
		}
		return 0;
	}

	private static Enchantment getWeightedEnchant(HashSet<Enchantment> enchants) {
		int randInt = 0;
		for (Enchantment ench : enchants) {
			randInt += getWeight(ench);
		}
		if (randInt <= 0) {
			// Gets hit sometimes cause I'm bad at code apparently.
			return Enchantment.DIG_SPEED;
		}
		randInt = ThreadLocalRandom.current().nextInt(randInt) + 1;
		for (Enchantment ench : enchants) {
			int weight = getWeight(ench);
			if (randInt >= weight) {
				randInt -= weight;
			} else {
				return ench;
			}
		}
		// Shouldn't ever hit this, but it's here just in case. Efficiency is a safe default.
		return Enchantment.DIG_SPEED;
	}

}
