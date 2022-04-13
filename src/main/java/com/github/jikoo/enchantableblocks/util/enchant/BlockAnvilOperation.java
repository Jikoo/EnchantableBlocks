package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.github.jikoo.planarenchanting.anvil.AnvilFunction;
import com.github.jikoo.planarenchanting.anvil.AnvilOperation;
import com.github.jikoo.planarenchanting.anvil.AnvilOperationState;
import com.github.jikoo.planarenchanting.anvil.AnvilResult;
import java.util.ArrayList;
import org.bukkit.inventory.AnvilInventory;
import org.jetbrains.annotations.NotNull;

/**
 * A simplified {@link AnvilOperation} designed for use with
 * {@link com.github.jikoo.enchantableblocks.block.EnchantableBlock} implementations. Unlike a
 * normal operation, this operation does not validate items!
 */
public class BlockAnvilOperation extends AnvilOperation {

  /**
   * A simplified {@link AnvilOperation} designed for use with
   * {@link com.github.jikoo.enchantableblocks.block.EnchantableBlock} implementations.
   *
   * @param registration the {@link EnchantableRegistration} for the block
   * @param worldName the name of the world the operation is applied in
   */
  public BlockAnvilOperation(
      @NotNull EnchantableRegistration registration,
      @NotNull String worldName) {
    // Set world allowed enchantments.
    var enchantments = new ArrayList<>(registration.getEnchants());
    var config = registration.getConfig();
    enchantments.removeAll(config.anvilDisabledEnchants.get(worldName));
    this.setEnchantApplies(((enchantment, itemStack) -> enchantments.contains(enchantment)));
    // Set world enchantment conflicts.
    var enchantConflicts = config.anvilEnchantmentConflicts.get(worldName);
    this.setEnchantsConflict((enchantment, enchantment2) ->
        enchantConflicts.get(enchantment).contains(enchantment2)
            || enchantConflicts.get(enchantment2).contains(enchantment));
    // Set world max levels.
    this.setEnchantMaxLevel(enchantment -> config.anvilEnchantmentMax.get(worldName, enchantment));
    // Combination validation is handled before calling #apply.
    this.setItemsCombineEnchants((a, b) -> true);
  }

  @Override
  public @NotNull AnvilResult apply(@NotNull AnvilInventory inventory) {
    var state = new AnvilOperationState(this, inventory);
    // Base and addition have already been validated.

    // Apply base cost.
    state.apply(AnvilFunction.PRIOR_WORK_LEVEL_COST);

    // Apply the rename function first, then update prior work cost - both update prior work.
    state.apply(AnvilFunction.RENAME);
    state.apply(AnvilFunction.UPDATE_PRIOR_WORK_COST);

    // Combine enchantments.
    state.apply(AnvilFunction.COMBINE_ENCHANTMENTS_JAVA_EDITION);

    return state.forge();
  }

}
