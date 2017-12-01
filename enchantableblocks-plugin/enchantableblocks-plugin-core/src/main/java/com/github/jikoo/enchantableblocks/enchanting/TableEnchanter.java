package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.util.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

import java.lang.reflect.InvocationTargetException;

/**
 * Listener for handling enchanting in an enchantment table.
 *
 * @author Jikoo
 */
public class TableEnchanter implements Listener {

	private final EnchantableBlocksPlugin plugin;
	private final TableEnchantmentHandler handler;

	public TableEnchanter(EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
		TableEnchantmentHandler handler;
		try {
			handler = CompatibilityUtil.newTableEnchantmentHandler(plugin);
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException
				| InvocationTargetException e) {
			handler = null;
		}
		this.handler = handler;
	}

	@EventHandler(ignoreCancelled = false)
	public void onPrepareItemEnchant(final PrepareItemEnchantEvent event) {
		if (handler == null || event.getItem().getEnchantments().size() > 0
				// TODO: enchantable materials list
				|| !event.getItem().getType().equals(Material.FURNACE)
				|| event.getItem().getAmount() != 1
				|| this.plugin.getEnchantments().size() <= 0
				// TODO: rework permissions
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		this.handler.handlePrepareItemEnchant(event);
	}


	@EventHandler
	public void onEnchantItem(final EnchantItemEvent event) {

		// TODO: all of the above
		if (handler == null || event.getItem().getType() != Material.FURNACE
				|| event.getItem().getAmount() != 1
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		this.handler.handleEnchantItem(event);
	}

}
