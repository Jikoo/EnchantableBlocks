package com.github.jikoo.enchantedfurnace;

import java.util.Map.Entry;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles enchantments/combinations in an anvil.
 * 
 * @author Jikoo
 */
public class AnvilEnchanter implements Listener {

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getView().getTopInventory().getType() != InventoryType.ANVIL
				|| !(event.getWhoClicked() instanceof Player)) {
			return;
		}
		Player clicker = (Player) event.getWhoClicked();
		if (!clicker.hasPermission("enchantedfurnace.enchant.anvil")
				|| clicker.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		new AnvilEnchantUpdate((AnvilInventory) event.getView().getTopInventory())
				.runTask(EnchantedFurnace.getInstance());
	}

	private class AnvilEnchantUpdate extends BukkitRunnable {

		private final AnvilInventory inv;
		public AnvilEnchantUpdate(AnvilInventory inv) {
			this.inv = inv;
		}

		@Override
		public void run() {
			if (inv.getItem(0) == null || inv.getItem(1) == null
					|| inv.getItem(0).getType() != Material.FURNACE || inv.getItem(0).getAmount() > 1
					|| inv.getItem(1).getType() != Material.ENCHANTED_BOOK) {
				return;
			}

			EnchantmentStorageMeta enchbook = (EnchantmentStorageMeta) inv.getItem(1).getItemMeta();
			ItemStack result = inv.getItem(0).clone();

			nextEnchant: for (Entry<Enchantment, Integer> entry : enchbook.getStoredEnchants().entrySet()) {
				if (!EnchantedFurnace.getInstance().getEnchantments().contains(entry.getKey())) {
					continue;
				}
				for (Enchantment e : result.getEnchantments().keySet()) {
					if (e.equals(entry.getKey())) {
						continue;
					}
					if (!EnchantedFurnace.getInstance().areEnchantmentsCompatible(e, entry.getKey())) {
						continue nextEnchant;
					}
				}
				if (result.getEnchantmentLevel(entry.getKey()) > entry.getValue()) {
					continue;
				}
				if (entry.getKey().getMaxLevel() > entry.getValue() && entry.getValue() == result.getEnchantmentLevel(entry.getKey())) {
					result.addUnsafeEnchantment(entry.getKey(), entry.getValue() + 1);
				} else {
					result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
				}
			}
			// Allow renames during combination (sort of - the name should be typed in prior to adding the book)
			ItemStack oldResult = inv.getItem(2);
			if (oldResult != null && oldResult.hasItemMeta() && oldResult.getItemMeta().hasDisplayName()) {
				ItemMeta meta = result.getItemMeta();
				meta.setDisplayName(oldResult.getItemMeta().getDisplayName());
			}
			if (!result.equals(inv.getItem(0))) {
				inv.setItem(2, result);
			}
		}
	}
}
