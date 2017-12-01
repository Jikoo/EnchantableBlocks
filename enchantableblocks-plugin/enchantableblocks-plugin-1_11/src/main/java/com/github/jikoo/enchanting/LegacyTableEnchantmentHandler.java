package com.github.jikoo.enchanting;

import java.util.Iterator;
import java.util.Map;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;

import com.github.jikoo.enchantableblocks.enchanting.EnchantmentUtil;
import com.github.jikoo.enchantableblocks.enchanting.TableEnchantmentHandler;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

/**
 * Handles enchantments in enchantment tables.
 *
 * @author Jikoo
 */
public class LegacyTableEnchantmentHandler extends TableEnchantmentHandler {

	public LegacyTableEnchantmentHandler(EnchantableBlocksPlugin plugin) {
		super(plugin);
	}

	public void handlePrepareItemEnchant(PrepareItemEnchantEvent event) {
		event.setCancelled(false);
		for (int i = 0; i < 3; i++) {
			event.getExpLevelCostsOffered()[i] = EnchantmentUtil.getButtonLevel(i, event.getEnchantmentBonus());
		}
	}

	public void handleEnchantItem(EnchantItemEvent event) {
		Map<Enchantment, Integer> enchantments = EnchantmentUtil.calculateFurnaceEnchants(this.getPlugin(), event.getExpLevelCost());

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
