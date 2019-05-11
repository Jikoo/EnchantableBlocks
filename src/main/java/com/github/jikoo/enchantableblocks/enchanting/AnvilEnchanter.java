package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Handles enchantments/combinations in an anvil.
 *
 * @author Jikoo
 */
public class AnvilEnchanter implements Listener {

	private final EnchantableBlocksPlugin plugin;

	public AnvilEnchanter(final EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
	}

	private void combine(final InventoryView view, ItemStack base, final ItemStack addition) {
		if (!(view.getTopInventory() instanceof AnvilInventory)) {
			return;
		}
		AnvilInventory inventory = (AnvilInventory) view.getTopInventory();
		ItemMeta baseMeta = base.getItemMeta();
		ItemMeta additionMeta = addition.getItemMeta();
		if (!(baseMeta instanceof Repairable) || !(additionMeta instanceof Repairable)) {
			return;
		}
		Repairable baseRepairable = (Repairable) baseMeta;
		Repairable additionRepairable = (Repairable) additionMeta;

		// Base cost for next: prior cost from both
		int cost = 0;
		if (baseRepairable.hasRepairCost()) {
			cost += baseRepairable.getRepairCost();
		}
		if (additionRepairable.hasRepairCost()) {
			cost += additionRepairable.getRepairCost();
		}

		Map<Enchantment, Integer> resultEnchants = new HashMap<>(base.getEnchantments());
		Map<Enchantment, Integer> additionEnchants;
		boolean book = additionMeta instanceof EnchantmentStorageMeta;
		if (book) {
			additionEnchants = ((EnchantmentStorageMeta) additionMeta).getStoredEnchants();
		} else {
			additionEnchants = additionMeta.getEnchants();
		}

		nextEnchant: for (Entry<Enchantment, Integer> entry : additionEnchants.entrySet()) {
			if (!this.plugin.getEnchantments().contains(entry.getKey())) {
				continue;
			}
			for (Enchantment e : resultEnchants.keySet()) {
				if (!e.equals(entry.getKey()) && this.plugin.areEnchantmentsIncompatible(e, entry.getKey())) {
					// Incompatible but valid enchant: +1 cost
					cost += 1;
					continue nextEnchant;
				}
			}
			if (!resultEnchants.containsKey(entry.getKey())) {
				// Compatible enchant: + multiplier * final level
				int level = entry.getValue();
				if (level > entry.getKey().getMaxLevel()) {
					level = entry.getKey().getMaxLevel();
				}
				cost += this.getEnchantmentMultiplier(entry.getKey(), book) * level;
				resultEnchants.put(entry.getKey(), level);
				continue;
			}
			int baseLvl = resultEnchants.get(entry.getKey());
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
			cost += this.getEnchantmentMultiplier(entry.getKey(), book) * baseLvl;
			resultEnchants.put(entry.getKey(), baseLvl);
		}

		if (base.getEnchantments().equals(resultEnchants)) {
			return;
		}

		ItemStack result = base.clone();
		result.addUnsafeEnchantments(resultEnchants);
		ItemMeta resultMeta = base.getItemMeta();

		if (!(resultMeta instanceof Repairable)) {
			return;
		}

		String displayName = inventory.getRenameText();
		if (baseMeta.hasDisplayName() && !baseMeta.getDisplayName().equals(displayName)
				|| !baseMeta.hasDisplayName() && displayName != null) {
			resultMeta.setDisplayName(displayName);
			// Renaming always adds 1 to the cost
			cost += 1;
		}

		Repairable resultRepairable = (Repairable) resultMeta;
		resultRepairable.setRepairCost(baseRepairable.hasRepairCost() ? baseRepairable.getRepairCost() * 2 + 1 : 1);
		result.setItemMeta(resultMeta);

		// Update inventory
		inventory.setItem(0, null);
		inventory.setItem(1, null);
		List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
		viewers.forEach(HumanEntity::closeInventory);
		inventory.setItem(0, base);
		inventory.setItem(1, addition);
		inventory.setItem(2, result);
		inventory.setRepairCost(cost);
		viewers.forEach(humanEntity -> humanEntity.openInventory(view));
	}

	private int getEnchantmentMultiplier(final Enchantment enchantment, final boolean book) {
		// TODO new enchants
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

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryClick(final InventoryClickEvent event) {
		this.onInventoryInteract(event);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryClick(final InventoryDragEvent event) {
		this.onInventoryInteract(event);
	}

	private void onInventoryInteract(final InventoryInteractEvent event) {
		InventoryView view = event.getView();
		if (view.getTopInventory().getType() != InventoryType.ANVIL
				|| !(event.getWhoClicked() instanceof Player)) {
			return;
		}
		Player clicker = (Player) event.getWhoClicked();
		if (!clicker.hasPermission("enchantableblocks.enchant.anvil")
				|| clicker.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
			Inventory inv = view.getTopInventory();
			ItemStack base = inv.getItem(0);
			ItemStack addition = inv.getItem(1);
			if (base == null || addition == null || !EnchantableFurnace.isApplicableMaterial(base.getType())
					|| base.getAmount() > 1 || addition.getAmount() > 1
					|| addition.getType() != Material.ENCHANTED_BOOK && base.getType() != addition.getType()) {
				return;
			}

			AnvilEnchanter.this.combine(view, base, addition);
		});
	}

}
