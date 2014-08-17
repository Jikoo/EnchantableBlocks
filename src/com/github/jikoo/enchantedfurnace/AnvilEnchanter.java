package com.github.jikoo.enchantedfurnace;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles enchantments/combinations in an anvil.
 * 
 * @author Jikoo
 */
public class AnvilEnchanter implements Listener {

	@EventHandler
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

		new AnvilEnchantUpdate((AnvilInventory) event.getView().getTopInventory()).runTask(EnchantedFurnace.getInstance());
	}

	private class AnvilEnchantUpdate extends BukkitRunnable {

		private AnvilInventory inv;
		public AnvilEnchantUpdate(AnvilInventory inv) {
			this.inv = inv;
		}

		@Override
		public void run() {
			if (inv.getItem(0) == null || inv.getItem(1) == null
					|| inv.getItem(0).getType() != Material.FURNACE
					|| inv.getItem(1).getType() != Material.ENCHANTED_BOOK) {
				return;
			}

			EnchantmentStorageMeta enchbook = (EnchantmentStorageMeta) inv.getItem(1).getItemMeta();
			ItemStack result = new ItemStack(Material.FURNACE);

			Set<Enchantment> legalEnchants = EnchantedFurnace.getInstance().getEnchantments();

			for (Entry<Enchantment, Integer> entry : inv.getItem(0).getEnchantments().entrySet()) {
				if (!legalEnchants.contains(entry.getKey())) {
					continue;
				}
				if (enchbook.getStoredEnchantLevel(entry.getKey()) > entry.getValue()) {
					result.addUnsafeEnchantment(entry.getKey(), enchbook.getStoredEnchantLevel(entry.getKey()));
				} else if (entry.getKey().getMaxLevel() > entry.getValue() && entry.getValue() == enchbook.getStoredEnchantLevel(entry.getKey())) {
					result.addUnsafeEnchantment(entry.getKey(), entry.getValue() + 1);
				} else {
					result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
				}
			}

			secondItem: for (Entry<Enchantment, Integer> entry : enchbook.getStoredEnchants().entrySet()) {
				if (!legalEnchants.contains(entry.getKey())) {
					continue;
				}
				for (Enchantment e : result.getEnchantments().keySet()) {
					if (e == entry.getKey() || e.conflictsWith(entry.getKey())) {
						continue secondItem;
					}
				}
				result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
			}
			inv.setItem(2, result);
		}
	}
}
