package com.github.jikoo.enchantableblocks.enchanting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

/**
 * Handles enchantments in enchantment tables which support previews (1.11+).
 *
 * @author Jikoo
 */
public class TablePreviewEnchanter implements Listener {

	private final EnchantableBlocksPlugin plugin;
	private final Map<UUID, Map<Integer, Map<Enchantment, Integer>>> enchantments;

	public TablePreviewEnchanter(final EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
		this.enchantments = new HashMap<>();
	}

	@EventHandler(ignoreCancelled = false)
	public void onPrepareItemEnchant(final PrepareItemEnchantEvent event) {
		if (event.getItem().getEnchantments().size() > 0
				|| !event.getItem().getType().equals(Material.FURNACE)
				|| event.getItem().getAmount() != 1
				|| this.plugin.getEnchantments().size() <= 0
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		event.setCancelled(false);

		if (!this.enchantments.containsKey(event.getEnchanter().getUniqueId())) {
			this.enchantments.put(event.getEnchanter().getUniqueId(), new HashMap<Integer, Map<Enchantment, Integer>>());
		}

		Map<Integer, Map<Enchantment, Integer>> enchantmentLevels = this.enchantments.get(event.getEnchanter().getUniqueId());

		for (int i = 0; i < 3; i++) {
			int buttonLevel = EnchantmentUtil.getButtonLevel(i, event.getEnchantmentBonus());
			Map<Enchantment, Integer> enchantments;
			if (enchantmentLevels.containsKey(buttonLevel)) {
				enchantments = enchantmentLevels.get(buttonLevel);
			} else {
				enchantments = EnchantmentUtil.calculateFurnaceEnchants(this.plugin, buttonLevel);
				enchantmentLevels.put(buttonLevel, enchantments);
			}
			if (enchantments.isEmpty()) {
				event.getOffers()[i] = null;
				continue;
			}
			Entry<Enchantment, Integer> firstEnchant = enchantments.entrySet().iterator().next();
			event.getOffers()[i] = new EnchantmentOffer(firstEnchant.getKey(), firstEnchant.getValue(), buttonLevel);
		}
	}

	@EventHandler
	public void onEnchantItem(final EnchantItemEvent event) {

		// Player has attempted enchanting anything, all enchants must be re-rolled.
		Map<Integer, Map<Enchantment, Integer>> enchantmentLevels = this.enchantments.remove(event.getEnchanter().getUniqueId());

		if (event.getItem().getType() != Material.FURNACE
				|| event.getItem().getAmount() != 1
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")
				|| enchantmentLevels == null) {
			return;
		}

		if (!enchantmentLevels.containsKey(event.getExpLevelCost())) {
			return;
		}

		Map<Enchantment, Integer> enchantments = enchantmentLevels.get(event.getExpLevelCost());

		for (Enchantment enchantment : event.getItem().getEnchantments().keySet()) {
			Iterator<Enchantment> iterator = enchantments.keySet().iterator();
			while (iterator.hasNext()) {
				if (!this.plugin.areEnchantmentsCompatible(enchantment, iterator.next())) {
					iterator.remove();
				}
			}
		}

		event.getEnchantsToAdd().putAll(enchantments);
	}

}
