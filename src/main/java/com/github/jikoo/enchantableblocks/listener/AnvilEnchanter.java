package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.github.jikoo.enchantableblocks.util.enchant.AnvilOperation;
import java.util.ArrayList;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
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

  @EventHandler
  @VisibleForTesting
  void onPrepareAnvil(@NotNull PrepareAnvilEvent event) {
    var clicker = event.getView().getPlayer();

    // Players in creative use vanilla's creative override, allowing any book to be applied to any
    // object. Allowing this to override guarantees that even if the anvil system breaks down,
    // admin functions still work.
    if (clicker.getGameMode() == GameMode.CREATIVE) {
      return;
    }

    AnvilInventory inventory = event.getInventory();
    ItemStack base = inventory.getItem(0);
    ItemStack addition = inventory.getItem(1);

    // Validate stacks - must be single items.
    if (areItemsInvalid(base, addition)) {
      return;
    }

    var registration = registry.get(base.getType());

    // Validate registration and check permissions.
    if (registration == null || !registration.hasEnchantPermission(clicker, "anvil")) {
      return;
    }

    var operation = getOperation(
        registration,
        clicker.getWorld().getName(),
        inventory.getRenameText());

    var anvilResult = operation.apply(base, addition);

    final ItemStack resultItem = anvilResult.getResult();

    event.setResult(resultItem);

    final int repairCost = anvilResult.getCost();
    final ItemStack input = base.clone();
    final ItemStack input2 = addition.clone();

    plugin.getServer().getScheduler().runTask(plugin, () -> {
      if (!input.equals(inventory.getItem(0)) || !input2.equals(inventory.getItem(1))) {
        return;
      }

      inventory.setItem(2, resultItem);
      inventory.setRepairCost(repairCost);
      clicker.setWindowProperty(InventoryView.Property.REPAIR_COST, repairCost);
    });
  }

  /**
   * Set up operation for the given parameters.
   *
   * @param registration the {@link EnchantableRegistration} in use
   * @param worldName the name of the world
   * @param renameText the name being applied by the operation
   * @return the fully set up operation
   */
  private AnvilOperation getOperation(
      @NotNull EnchantableRegistration registration,
      @NotNull String worldName,
      @Nullable String renameText) {
    var operation = new AnvilOperation();
    operation.setMaterialRepairs((a, b) -> false);
    operation.setMergeRepairs(false);

    var enchantments = new ArrayList<>(registration.getEnchants());
    var config = registration.getConfig();
    enchantments.removeAll(config.anvilDisabledEnchants.get(worldName));
    operation.setEnchantApplies(((enchantment, itemStack) -> enchantments.contains(enchantment)));

    var enchantConflicts = config.anvilEnchantmentConflicts.get(worldName);
    operation.setEnchantConflicts((enchantment, enchantment2) ->
        enchantConflicts.get(enchantment).contains(enchantment2)
            || enchantConflicts.get(enchantment2).contains(enchantment));

    operation.setRenameText(renameText);

    return operation;
  }

  /**
   * Ensure base and addition both are single stacked items.
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
    return base == null || addition == null || base.getAmount() > 1 || addition.getAmount() > 1;
  }

}
