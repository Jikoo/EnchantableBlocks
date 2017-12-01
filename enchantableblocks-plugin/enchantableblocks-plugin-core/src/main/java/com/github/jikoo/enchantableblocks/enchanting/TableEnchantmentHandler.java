package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

/**
 * Abstraction for handling enchantment-related events.
 *
 * @author Jikoo
 */
public abstract class TableEnchantmentHandler {

	private final EnchantableBlocksPlugin plugin;

	public TableEnchantmentHandler(EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
	}

	public abstract void handlePrepareItemEnchant(PrepareItemEnchantEvent event);

	public abstract void handleEnchantItem(EnchantItemEvent event);

	protected EnchantableBlocksPlugin getPlugin() {
		return this.plugin;
	}

}
