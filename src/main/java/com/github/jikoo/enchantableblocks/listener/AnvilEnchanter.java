package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.util.ItemStackHelper;
import com.github.jikoo.enchantableblocks.util.enchant.BlockAnvilOperation;
import com.github.jikoo.planarenchanting.anvil.AnvilResult;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Listener for handling enchantments and combinations in an anvil.
 */
public class AnvilEnchanter implements Listener {

  private final Plugin plugin;
  private final EnchantableBlockRegistry registry;

  /**
   * Construct a new {@code AnvilEnchanter} to provide enchantments for blocks.
   *
   * @param plugin the owning {@link Plugin}
   * @param registry the {@link EnchantableBlockRegistry} providing block details
   */
  public AnvilEnchanter(@NotNull Plugin plugin, @NotNull EnchantableBlockRegistry registry) {
    this.plugin = plugin;
    this.registry = registry;
  }

  @EventHandler(priority = EventPriority.HIGH)
  @VisibleForTesting
  void onPrepareAnvil(@NotNull PrepareAnvilEvent event) {
    var clicker = event.getView().getPlayer();
    var inventory = event.getInventory();
    var base = inventory.getItem(0);
    var addition = inventory.getItem(1);

    if (areItemsInvalid(base, addition)) {
      return;
    }

    var registration = registry.get(base.getType());

    // Validate registration and check permissions.
    if (registration == null || !registration.hasEnchantPermission(clicker, "anvil")) {
      return;
    }

    var operation = new BlockAnvilOperation(registration, clicker.getWorld().getName());
    final var result = operation.apply(inventory);

    if (result == AnvilResult.EMPTY) {
      return;
    }

    final var input = base.clone();
    final var input2 = addition.clone();
    final var resultItem = result.item();

    event.setResult(resultItem);

    plugin.getServer().getScheduler().runTask(plugin, () -> {
      // Ensure inputs have not been modified since our calculations.
      if (!input.equals(inventory.getItem(0)) || !input2.equals(inventory.getItem(1))) {
        return;
      }

      // Set result again - overrides bad enchantment plugins that always write result.
      inventory.setItem(2, resultItem);
      // Set repair cost. As vanilla has no result for our combinations, this is always set to 0
      // after the event has completed and needs to be set again.
      inventory.setRepairCost(result.levelCost());
      // Update level cost window property again just to be safe.
      clicker.setWindowProperty(InventoryView.Property.REPAIR_COST, result.levelCost());
    });
  }

  /**
   * Ensure base and addition both eligible. Base must be a single item, unstacked. Addition must
   * either be an enchanted book or the same material as the base item.
   *
   * @param base the base item
   * @param addition the additional item
   * @return true if both base and addition are single items
   */
  @Contract("null, _ -> true; _, null -> true")
  @VisibleForTesting
  boolean areItemsInvalid(
      @Nullable ItemStack base,
      @Nullable ItemStack addition) {
    return ItemStackHelper.isEmpty(base)
        || base.getAmount() != 1
        || ItemStackHelper.isEmpty(addition)
        || (addition.getType() != Material.ENCHANTED_BOOK && addition.getType() != base.getType());
  }

}
