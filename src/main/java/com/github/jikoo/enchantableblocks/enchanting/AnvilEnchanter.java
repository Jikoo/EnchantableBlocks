package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.util.enchant.AnvilOperation;
import com.github.jikoo.enchantableblocks.util.enchant.AnvilResult;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;

/**
 * Handles enchantments/combinations in an anvil.
 *
 * @author Jikoo
 */
public class AnvilEnchanter implements Listener {

	private final EnchantableBlocksPlugin plugin;
	private final AnvilOperation operation;

	public AnvilEnchanter(final @NotNull EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
		this.operation = new AnvilOperation();
		operation.setEnchantConflicts(plugin::areEnchantmentsIncompatible);
		operation.setEnchantApplies((enchantment, itemStack) -> plugin.getEnchantments().contains(enchantment));
		operation.setMaterialRepairs((a, b) -> false);
		operation.setMergeRepairs(false);
	}

	@EventHandler
	public void onPrepareAnvil(final @NotNull PrepareAnvilEvent event) {
		if (!(event.getView().getPlayer() instanceof Player)) {
			return;
		}
		Player clicker = (Player) event.getView().getPlayer();
		if (!clicker.hasPermission("enchantableblocks.enchant.anvil")
				|| clicker.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		AnvilInventory inventory = event.getInventory();
		ItemStack base = inventory.getItem(0);
		ItemStack addition = inventory.getItem(1);
		if (base == null || addition == null || !EnchantableFurnace.isApplicableMaterial(base.getType())
				|| base.getAmount() > 1 || addition.getAmount() > 1
				|| addition.getType() != Material.ENCHANTED_BOOK && base.getType() != addition.getType()) {
			return;
		}

		AnvilResult anvilResult = operation.apply(base, addition);

		ItemStack result;
		if (anvilResult.getCost() == 0) {
			result = base.clone();
		} else {
			result = anvilResult.getResult();
		}

		String displayName = inventory.getRenameText();
		ItemMeta itemMeta = Objects.requireNonNull(result.getItemMeta());
		int cost = anvilResult.getCost();
		if (itemMeta.hasDisplayName() && !itemMeta.getDisplayName().equals(displayName)
				|| !itemMeta.hasDisplayName() && displayName != null && !displayName.isEmpty()) {
			itemMeta.setDisplayName(displayName);
			// Renaming always adds 1 to the cost
			cost += 1;
		}

		Repairable resultRepairable = (Repairable) itemMeta;
		resultRepairable.setRepairCost(cost);
		result.setItemMeta(itemMeta);

		event.setResult(result);
		final int repairCost = cost;
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			inventory.setRepairCost(repairCost);
			clicker.setWindowProperty(InventoryView.Property.REPAIR_COST, repairCost);
		});
	}

}
