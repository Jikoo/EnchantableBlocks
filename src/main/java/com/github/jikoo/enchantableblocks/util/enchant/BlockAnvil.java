package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.github.jikoo.planarenchanting.anvil.Anvil;
import com.github.jikoo.planarenchanting.anvil.AnvilFunctions;
import com.github.jikoo.planarenchanting.anvil.AnvilResult;
import com.github.jikoo.planarenchanting.anvil.AnvilState;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;

/**
 * A simplified {@link Anvil} designed for use with
 * {@link com.github.jikoo.enchantableblocks.block.EnchantableBlock} implementations. Unlike a
 * normal operation, this operation does not validate items!
 */
public class BlockAnvil extends Anvil {

  /**
   * A simplified {@link Anvil} designed for use with
   * {@link com.github.jikoo.enchantableblocks.block.EnchantableBlock} implementations.
   *
   * @param registration the {@link EnchantableRegistration} for the block
   * @param worldName the name of the world the operation is applied in
   */
  public BlockAnvil(
      @NotNull EnchantableRegistration registration,
      @NotNull String worldName) {
    super(new BlockAnvilBehavior(registration, worldName));
  }

  @Override
  public @NotNull AnvilResult getResult(@NotNull AnvilView view) {
    var state = new AnvilState(view);
    // Base and addition have already been validated.

    // Apply base cost.
    apply(state, AnvilFunctions.PRIOR_WORK_LEVEL_COST);

    // Apply the rename function first, then update prior work cost - both update prior work.
    apply(state, AnvilFunctions.RENAME);
    apply(state, AnvilFunctions.UPDATE_PRIOR_WORK_COST);

    // Combine enchantments.
    apply(state, AnvilFunctions.COMBINE_ENCHANTMENTS_JAVA_EDITION);

    return forge(state);
  }

}
