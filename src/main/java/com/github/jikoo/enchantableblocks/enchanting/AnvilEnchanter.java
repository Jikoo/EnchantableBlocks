package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.util.enchant.AnvilOperation;
import com.github.jikoo.enchantableblocks.util.enchant.AnvilResult;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
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

  public AnvilEnchanter(final @NotNull EnchantableBlocksPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPrepareAnvil(final @NotNull PrepareAnvilEvent event) {
    if (!(event.getView().getPlayer() instanceof Player clicker)) {
      return;
    }

    if (!clicker.hasPermission("enchantableblocks.enchant.anvil")
        || clicker.getGameMode() == GameMode.CREATIVE) {
      return;
    }

    AnvilInventory inventory = event.getInventory();
    ItemStack base = inventory.getItem(0);
    ItemStack addition = inventory.getItem(1);

    if (base == null || addition == null || base.getAmount() > 1 || addition.getAmount() > 1) {
      return;
    }

    var registration = plugin.getBlockManager().getRegistry().get(base.getType());

    if (registration == null) {
      return;
    }

    AnvilOperation operation = new AnvilOperation();
    operation.setMaterialRepairs((a, b) -> false);
    operation.setMergeRepairs(false);

    Collection<Enchantment> enchantments = new ArrayList<>(registration.getEnchants());
    EnchantableBlockConfig config = registration.getConfig();
    String world = clicker.getWorld().getName();
    Set<Enchantment> disabledEnchants = config.anvilDisabledEnchants.get(world);
    enchantments.removeAll(disabledEnchants);
    operation.setEnchantApplies(((enchantment, itemStack) -> enchantments.contains(enchantment)));

    Multimap<Enchantment, Enchantment> enchantConflicts = config.anvilEnchantmentConflicts.get(world);
    operation.setEnchantConflicts((enchantment, enchantment2) ->
        enchantConflicts.get(enchantment).contains(enchantment2)
            || enchantConflicts.get(enchantment2).contains(enchantment));

    // TODO move renameText to cost application

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
    final ItemStack input = base.clone();
    final ItemStack input2 = addition.clone();

    plugin.getServer().getScheduler().runTask(plugin, () -> {
      if (!input.equals(inventory.getItem(0)) || !input2.equals(inventory.getItem(1))) {
        return;
      }

      inventory.setItem(2, result);
      inventory.setRepairCost(repairCost);
      clicker.setWindowProperty(InventoryView.Property.REPAIR_COST, repairCost);
    });
  }

}
