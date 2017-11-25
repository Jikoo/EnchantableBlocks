package com.github.jikoo.enchantableblocks.enchanting;

import java.util.Iterator;
import java.util.Map;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

/**
 * Handles enchantments in enchantment tables.
 *
 * @author Jikoo
 */
public class TableEnchanter implements Listener {

	private final EnchantableBlocksPlugin plugin;

	public TableEnchanter(EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = false)
	public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
		if (event.getItem().getEnchantments().size() == 0
				&& event.getItem().getType().equals(Material.FURNACE)
				&& event.getItem().getAmount() == 1
				&& plugin.getEnchantments().size() > 0
				&& event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			event.setCancelled(false);
			for (int i = 0; i < 3; i++) {
				event.getExpLevelCostsOffered()[i] = EnchantmentUtil.getButtonLevel(i, event.getEnchantmentBonus());
			}
		}
	}

	@EventHandler
	public void onEnchantItem(EnchantItemEvent event) {
		if (event.getItem().getType() != Material.FURNACE
				|| event.getItem().getAmount() != 1
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		Map<Enchantment, Integer> enchantments = EnchantmentUtil.calculateFurnaceEnchants(plugin, event.getExpLevelCost());

		for (Enchantment enchantment : event.getItem().getEnchantments().keySet()) {
			Iterator<Enchantment> iterator = enchantments.keySet().iterator();
			while (iterator.hasNext()) {
				if (!plugin.areEnchantmentsCompatible(enchantment, iterator.next())) {
					iterator.remove();
				}
			}
		}

		event.getEnchantsToAdd().putAll(enchantments);
	}

}
