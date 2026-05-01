package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.planarenchanting.anvil.Anvil;
import com.github.jikoo.planarenchanting.anvil.AnvilFunctionsProvider;
import com.github.jikoo.planarenchanting.anvil.AnvilResult;
import com.github.jikoo.planarenchanting.anvil.WorkPiece;
import org.bukkit.inventory.view.AnvilView;
import org.jspecify.annotations.NullMarked;

import java.util.function.Function;

/**
 * A minimal {@link com.github.jikoo.planarenchanting.anvil.Anvil}-like representation of
 * the operations performed to produce an anvil result.
 */
@NullMarked
public class BlockAnvil<T> {

  private final Function<AnvilView, WorkPiece<T>> createWork;
  private final AnvilFunctionsProvider<T> functions;

  /**
   * A simplified {@link Anvil} designed for use with
   * {@link com.github.jikoo.enchantableblocks.block.EnchantableBlock} implementations.
   *
   * @param createWork the method for creating a new {@link WorkPiece}
   * @param functions the provider for default anvil functions
   */
  public BlockAnvil(
      Function<AnvilView, WorkPiece<T>> createWork,
      AnvilFunctionsProvider<T> functions
  ) {
    this.createWork = createWork;
    this.functions = functions;
  }

  public AnvilResult getResult(AnvilView view, BlockAnvilBehavior<T> behavior) {
    WorkPiece<T> workPiece = createWork.apply(view);

    // Base and addition have already been validated.

    // Apply the rename function first, then update prior work cost - both update prior work.
    workPiece.apply(behavior, functions.rename());
    workPiece.apply(behavior, functions.setItemPriorWork());

    // Combine enchantments.
    workPiece.apply(behavior, functions.combineEnchantsJava());

    return workPiece.temper();
  }

}
