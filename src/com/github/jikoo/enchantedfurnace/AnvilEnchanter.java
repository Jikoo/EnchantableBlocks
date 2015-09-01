package com.github.jikoo.enchantedfurnace;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles enchantments/combinations in an anvil.
 * 
 * @author Jikoo
 */
public class AnvilEnchanter implements Listener {

	private final EnchantedFurnace plugin;

	public AnvilEnchanter(EnchantedFurnace plugin) {
		this.plugin = plugin;
	}

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

		new AnvilEnchantUpdate(event.getView()).runTask(plugin);
	}

	private class AnvilEnchantUpdate extends BukkitRunnable {

		private final InventoryView view;
		public AnvilEnchantUpdate(InventoryView view) {
			this.view = view;
		}

		@Override
		public void run() {
			Inventory inv = view.getTopInventory();
			if (inv.getItem(0) == null || inv.getItem(1) == null || inv.getItem(0).getType() != Material.FURNACE
					|| inv.getItem(0).getAmount() > 1 || inv.getItem(1).getAmount() > 1
					|| (inv.getItem(1).getType() != Material.ENCHANTED_BOOK && inv.getItem(1).getType() != Material.FURNACE)) {
				return;
			}

			ItemStack result = combine(view, inv.getItem(0), inv.getItem(1));
			if (result == null) {
				return;
			}

			inv.setItem(2, result);

			if (view.getPlayer() instanceof Player) {
				((Player) view.getPlayer()).updateInventory();
				updateAnvilExpCost(view);
			}
		}
	}

	private ItemStack combine(InventoryView view, ItemStack base, ItemStack addition) {
		ItemMeta baseMeta = base.getItemMeta();
		Repairable baseRepairable = (Repairable) baseMeta;
		ItemMeta additionMeta = addition.getItemMeta();
		Repairable additionRepairable = (Repairable) additionMeta;

		// Base cost for next: prior cost from both
		int cost = 0;
		if (baseRepairable.hasRepairCost()) {
			cost += baseRepairable.getRepairCost();
		}
		if (additionRepairable.hasRepairCost()) {
			cost += additionRepairable.getRepairCost();
		}

		Map<Enchantment, Integer> baseEnchants = new HashMap<Enchantment, Integer>(base.getEnchantments());
		Map<Enchantment, Integer> additionEnchants;
		boolean book = additionMeta instanceof EnchantmentStorageMeta;
		if (book) {
			additionEnchants = ((EnchantmentStorageMeta) additionMeta).getStoredEnchants();
		} else {
			additionEnchants = additionMeta.getEnchants();
		}

		nextEnchant: for (Entry<Enchantment, Integer> entry : additionEnchants.entrySet()) {
			if (!plugin.getEnchantments().contains(entry.getKey())) {
				continue;
			}
			for (Enchantment e : baseEnchants.keySet()) {
				if (!e.equals(entry.getKey()) && !plugin.areEnchantmentsCompatible(e, entry.getKey())) {
					// Incompatible but valid enchant: +1 cost
					cost += 1;
					continue nextEnchant;
				}
			}
			if (!baseEnchants.containsKey(entry.getKey())) {
				// Compatible enchant: + multiplier * final level
				int level = entry.getValue();
				if (level > entry.getKey().getMaxLevel()) {
					level = entry.getKey().getMaxLevel();
				}
				cost += getEnchantmentMultiplier(entry.getKey(), book) * level;
				baseEnchants.put(entry.getKey(), level);
				continue;
			}
			int baseLvl = baseEnchants.get(entry.getKey());
			int additionLvl = entry.getValue();
			if (baseLvl < additionLvl) {
				baseLvl = additionLvl;
			} else if (baseLvl == additionLvl) {
				baseLvl += 1;
			}
			if (baseLvl > entry.getKey().getMaxLevel()) {
				baseLvl = entry.getKey().getMaxLevel();
			}
			// Compatible enchant: + multiplier * final level
			cost += getEnchantmentMultiplier(entry.getKey(), book) * baseLvl;
			baseEnchants.put(entry.getKey(), baseLvl);
		}

		if (base.getEnchantments().equals(baseEnchants)) {
			return null;
		}

		base = base.clone();
		base.addUnsafeEnchantments(baseEnchants);
		baseMeta = base.getItemMeta();
		String displayName = getNameFromAnvil(view);
		if (baseMeta.hasDisplayName() && !baseMeta.getDisplayName().equals(displayName)
				|| !baseMeta.hasDisplayName() && displayName != null) {
			baseMeta.setDisplayName(displayName);
			// Renaming always adds 1 to the cost
			cost += 1;
		}
		baseRepairable = (Repairable) baseMeta;
		baseRepairable.setRepairCost(baseRepairable.hasRepairCost() ? baseRepairable.getRepairCost() * 2 + 1 : 1);
		base.setItemMeta(baseMeta);

		setAnvilExpCost(view, cost);

		return base;
	}

	private int getEnchantmentMultiplier(Enchantment enchantment, boolean book) {
		int multiplier;
		if (enchantment.equals(Enchantment.ARROW_DAMAGE)
				|| enchantment.equals(Enchantment.DAMAGE_ALL)
				|| enchantment.equals(Enchantment.DIG_SPEED)
				|| enchantment.equals(Enchantment.PROTECTION_ENVIRONMENTAL)) {
			return 1;
		} else if (enchantment.equals(Enchantment.ARROW_INFINITE)
				|| enchantment.equals(Enchantment.SILK_TOUCH)
				|| enchantment.equals(Enchantment.THORNS)) {
			multiplier = 8;
		} else if (enchantment.equals(Enchantment.DAMAGE_ARTHROPODS)
				|| enchantment.equals(Enchantment.DAMAGE_UNDEAD)
				|| enchantment.equals(Enchantment.DURABILITY)
				|| enchantment.equals(Enchantment.KNOCKBACK)
				|| enchantment.equals(Enchantment.PROTECTION_FALL)
				|| enchantment.equals(Enchantment.PROTECTION_FIRE)
				|| enchantment.equals(Enchantment.PROTECTION_PROJECTILE)) {
			multiplier = 2;
		} else {
			// Reasonable default, also the most common multiplier
			multiplier = 4;
		}

		// Books are 1/2 the price to put on a tool
		if (book) {
			multiplier /= 2;
		}

		return multiplier;
	}

	private String getNameFromAnvil(InventoryView view) {
		if (!(view.getTopInventory() instanceof AnvilInventory)) {
			return null;
		}
		try {
			Method method = view.getClass().getMethod("getHandle");
			Object nmsInventory = method.invoke(view);
			// TODO: list of acceptable NMS versions?
			// Field ContainerAnvil.l may have changed name, I only have 1.8-1.8.8 on hand to check
			// Alternative: Loop through all fields for object, select String
			// Currently, and in all prior versions, neither Container nor ContainerAnvil contain any other String fields.
			Field field = nmsInventory.getClass().getDeclaredField("l");
			field.setAccessible(true);
			return (String) field.get(nmsInventory);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void setAnvilExpCost(InventoryView view, int cost) {
		if (!(view.getTopInventory() instanceof AnvilInventory)) {
			return;
		}
		try {
			Method method = view.getClass().getMethod("getHandle");
			Object nmsInventory = method.invoke(view);
			Field field = nmsInventory.getClass().getDeclaredField("a");
			field.set(nmsInventory, cost);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateAnvilExpCost(InventoryView view) {
		if (!(view.getTopInventory() instanceof AnvilInventory)) {
			return;
		}
		try {
			Method method = view.getClass().getMethod("getHandle");
			Object nmsInventory = method.invoke(view);
			Field field = nmsInventory.getClass().getDeclaredField("a");
			method = view.getPlayer().getClass().getMethod("getHandle");
			Object nmsPlayer = method.invoke(view.getPlayer());
			method = nmsPlayer.getClass().getMethod("setContainerData", nmsInventory.getClass().getSuperclass(), int.class, int.class);
			method.invoke(nmsPlayer, nmsInventory, 0, field.get(nmsInventory));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
