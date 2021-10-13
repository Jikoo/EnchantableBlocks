package com.github.jikoo.enchantableblocks.util.enchant;

import java.util.Collections;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common enchantment-related functions.
 */
public final class EnchantmentUtil {

  /**
   * Get {@link Enchantment Enchantments} from an {@link ItemMeta}.
   *
   * @param meta the {@code ItemMeta}
   * @return the stored enchantments
   */
  public static @NotNull Map<Enchantment, Integer> getEnchants(@Nullable ItemMeta meta) {
    if (meta == null) {
      return Collections.emptyMap();
    }

    if (meta instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
      return enchantmentStorageMeta.getStoredEnchants();
    }

    return meta.getEnchants();
  }

  /**
   * Set an {@link Enchantment Enchantments} on an {@link ItemMeta}.
   *
   * @param meta the {@code ItemMeta}
   * @param enchant the {@code Enchantment} to add
   * @param level the level of the {@code Enchantment}
   */
  public static void applyEnchant(@Nullable ItemMeta meta, Enchantment enchant, int level) {
    if (meta == null) {
      return;
    }
    if (meta instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
      enchantmentStorageMeta.addStoredEnchant(enchant, level, true);
    } else {
      meta.addEnchant(enchant, level, true);
    }
  }

  private EnchantmentUtil() {}

}
