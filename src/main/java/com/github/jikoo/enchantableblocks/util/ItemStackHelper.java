package com.github.jikoo.enchantableblocks.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * A helper utility for methods relating to {@link ItemStack ItemStacks}.
 */
public final class ItemStackHelper {

  /**
   * Check if an {@link ItemStack} is null or empty.
   *
   * @param itemStack the item
   * @return true if the item is null or empty
   */
  @Contract("null -> true")
  public static boolean isEmpty(@Nullable ItemStack itemStack) {
    return itemStack == null || itemStack.getType() == Material.AIR;
  }

  private ItemStackHelper() {}

}
