package com.github.jikoo.enchantedfurnace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

/**
 * This class is an adaptation of @andrepl's
 * <a href=https://gist.github.com/andrepl/5522053>example plugin</a>
 * for his Craftbukkit pull. Changed Enchantments and Material, added
 * prevention for Silk Touch and Fortune together.
 * 
 * @author andrepl, Jikoo
 */
public class Enchanter  implements Listener {

	private HashMap<Enchantment, Integer> enchantments;
	private Random rand;

	public Enchanter() {
		enchantments = new HashMap<Enchantment, Integer>();
		enchantments.put(Enchantment.DIG_SPEED, 4);
		enchantments.put(Enchantment.DURABILITY, 3);
		enchantments.put(Enchantment.LOOT_BONUS_BLOCKS, 3);
		enchantments.put(Enchantment.SILK_TOUCH, 1);
		rand = new Random();
	}

	private int getEnchantingLevel(int slot, int shelves) {
		// taken from vanilla removed itemstack's enchantability == 0 check.
		if (shelves > 15) {
			shelves = 15;
		}
		int var6 = rand.nextInt(8) + 1 + (shelves >> 1) + rand.nextInt(shelves + 1);
		return slot == 0 ? Math.max(var6 / 3, 1)
				: (slot == 1 ? var6 * 2 / 3 + 1 : Math.max(var6, shelves * 2));
	}

	@EventHandler(ignoreCancelled = false)
	public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
		if (event.getItem().getEnchantments().size() == 0
				&& event.getItem().getType().equals(Material.FURNACE)) {
			event.setCancelled(false);
			for (int i = 0; i < 3; i++) {
				event.getExpLevelCostsOffered()[i] = getEnchantingLevel(i, event.getEnchantmentBonus());
			}
		}
	}

	@EventHandler
	public void onItemEnchant(EnchantItemEvent event) {
		if (event.getItem().getType().equals(Material.FURNACE)) {
			if (!event.getEnchanter().hasPermission("enchantedfurnace.enchant")) {
				return;
			}
			int cost = event.getExpLevelCost();
			List<Enchantment> shuffled = new ArrayList<Enchantment>(enchantments.keySet());
			Collections.shuffle(shuffled);
			for (Enchantment ench : shuffled) {
				if (event.getEnchantsToAdd().size() == 3) {
					break;
				}
				if (ench != Enchantment.SILK_TOUCH && ench != Enchantment.LOOT_BONUS_BLOCKS 
						|| (ench == Enchantment.SILK_TOUCH
						&& !event.getEnchantsToAdd().containsKey(Enchantment.LOOT_BONUS_BLOCKS))
						|| (ench == Enchantment.LOOT_BONUS_BLOCKS
						&& !event.getEnchantsToAdd().containsKey(Enchantment.SILK_TOUCH))) {
					int lvl = 0;
					while (rand.nextDouble() < 0.7 && lvl < ench.getMaxLevel()
							&& cost >= enchantments.get(ench) * lvl * lvl) {
						lvl++;
					}
					if (lvl > 0) {
						cost -= enchantments.get(ench) * lvl * lvl;
						event.getEnchantsToAdd().put(ench, lvl);
					}
				}
			}
		}
	}
}
