package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.planarenchanting.table.EnchantingTable;
import com.github.jikoo.planarenchanting.table.TableEnchantListener;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Listener for handling enchanting in an enchantment table.
 */
public class TableEnchanter extends TableEnchantListener {
  private final EnchantableBlockRegistry registry;

  /**
   * Construct a new {@code TableEnchanter} to provide enchantments for blocks.
   *
   * @param plugin the owning {@link Plugin}
   * @param registry the {@link EnchantableBlockRegistry} providing block details
   */
  public TableEnchanter(@NotNull Plugin plugin, @NotNull EnchantableBlockRegistry registry) {
    super(plugin);
    this.registry = registry;
  }

  @Override
  protected boolean isIneligible(@NotNull Player player, @NotNull ItemStack itemStack) {
    // Basic checks already ensure item is unstacked and unenchanted.
    // Advanced permissions checks will handle when registry is fetched for table setup.
    return false;
  }

  @Override
  protected @Nullable EnchantingTable getTable(
      @NotNull Player player,
      @NotNull ItemStack itemStack) {
    var registration = registry.get(itemStack.getType());
    if (registration == null|| registration.getEnchants().isEmpty()
        || !registration.hasEnchantPermission(player, "table")) {
      return null;
    }

    var world = player.getWorld().getName();
    var config = registration.getConfig();
    var enchants = new ArrayList<>(registration.getEnchants());
    var blacklist = config.tableDisabledEnchants.get(world);
    enchants.removeAll(blacklist);

    if (enchants.isEmpty()) {
      return null;
    }

    EnchantingTable operation = new EnchantingTable(enchants, config.tableEnchantability.get(world));

    var enchantConflicts = config.tableEnchantmentConflicts.get(world);
    operation.setIncompatibility((enchantment, enchantment2) ->
        enchantConflicts.get(enchantment).contains(enchantment2)
            || enchantConflicts.get(enchantment2).contains(enchantment));

    return operation;
  }

}
