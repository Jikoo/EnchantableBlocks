package com.github.jikoo.enchantableblocks.enchanting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

/**
 * Handles enchantments in enchantment tables which support previews.
 *
 * @author Jikoo
 */
public class TablePreviewEnchanter extends TableEnchantmentHandler {

	private final Map<UUID, Map<Integer, Map<Enchantment, Integer>>> enchantments;

	public TablePreviewEnchanter(final EnchantableBlocksPlugin plugin) {
		super(plugin);
		this.enchantments = new HashMap<>();
	}

	public void handlePrepareItemEnchant(final PrepareItemEnchantEvent event) {

		event.setCancelled(false);

		if (!this.enchantments.containsKey(event.getEnchanter().getUniqueId())) {
			this.enchantments.put(event.getEnchanter().getUniqueId(), new HashMap<>());
		}

		Map<Integer, Map<Enchantment, Integer>> enchantmentLevels = this.enchantments.get(event.getEnchanter().getUniqueId());

		for (int i = 0; i < 3; i++) {
			int buttonLevel = EnchantmentUtil.getButtonLevel(i, event.getEnchantmentBonus());
			Map<Enchantment, Integer> enchantments;
			if (enchantmentLevels.containsKey(buttonLevel)) {
				enchantments = enchantmentLevels.get(buttonLevel);
			} else {
				enchantments = EnchantmentUtil.calculateFurnaceEnchants(this.getPlugin(), buttonLevel);
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

	public void handleEnchantItem(final EnchantItemEvent event) {

		// Player has attempted enchanting anything, all enchants must be re-rolled.
		Map<Integer, Map<Enchantment, Integer>> enchantmentLevels = this.enchantments.remove(event.getEnchanter().getUniqueId());

		if (enchantmentLevels == null || !enchantmentLevels.containsKey(event.getExpLevelCost())) {
			return;
		}

		Map<Enchantment, Integer> enchantments = enchantmentLevels.get(event.getExpLevelCost());

		for (Enchantment enchantment : event.getItem().getEnchantments().keySet()) {
			Iterator<Enchantment> iterator = enchantments.keySet().iterator();
			while (iterator.hasNext()) {
				if (!this.getPlugin().areEnchantmentsCompatible(enchantment, iterator.next())) {
					iterator.remove();
				}
			}
		}

		event.getEnchantsToAdd().putAll(enchantments);
	}

}
