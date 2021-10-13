package com.github.jikoo.enchantableblocks.util.enchant;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

/**
 * A representation of a result from an anvil operation.
 */
public class AnvilResult {

  private final ItemStack result;
  private final int cost;
  private final int repairCount;

  /**
   * Construct a new empty {@code AnvilResult}.
   */
  AnvilResult() {
    this(new ItemStack(Material.AIR), 0);
  }

  /**
   * Construct a new {@code AnvilResult}.
   *
   * <p>The result is always cloned to prevent accidents. The repair cost is set to the specified
   * cost.
   *
   * @param result the resulting {@link ItemStack}
   * @param cost the cost in experience levels
   */
  AnvilResult(ItemStack result, int cost) {
    this(result, cost, 0);
  }

  /**
   * Construct a new {@code AnvilResult}.
   *
   * <p>The result is always cloned to prevent accidents. The repair cost is set to the specified
   * cost.
   *
   * @param result the resulting {@link ItemStack}
   * @param cost the cost in experience levels
   * @param repairCount the number of items consumed from the addition slot for material repairs
   */
  AnvilResult(ItemStack result, int cost, int repairCount) {
    this.result = result.clone();
    this.cost = cost;

    if (cost > 0) {
      ItemMeta meta = result.getItemMeta();
      if (meta instanceof Repairable repairable) {
        repairable.setRepairCost(cost);
        this.result.setItemMeta(meta);
      }
    }

    this.repairCount = repairCount;
  }

  /**
   * Get the item resulting from an anvil operation.
   *
   * @return the item
   */
  public ItemStack getResult() {
    return this.result;
  }

  /**
   * Get the cost in levels of an anvil operation.
   *
   * @return the cost
   */
  public int getCost() {
    return this.cost;
  }

  /**
   * Get the number of items consumed from the second slot by the anvil operation.
   *
   * <p>Note that unless this operation consumes raw material to restore durability to an item of
   * a different type the number of repairs will be 0.
   *
   * @return the number of additional items consumed
   */
  public int getRepairCount() {
    return this.repairCount;
  }

}
