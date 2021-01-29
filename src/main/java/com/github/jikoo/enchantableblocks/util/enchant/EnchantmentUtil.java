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
     * Get enchantments from an ItemMeta.
     *
     * @param meta the ItemMeta
     * @return the stored enchantments
     */
    public static @NotNull Map<Enchantment, Integer> getEnchants(@Nullable ItemMeta meta) {
        if (meta == null) {
            return Collections.emptyMap();
        }

        if (meta instanceof EnchantmentStorageMeta) {
            return ((EnchantmentStorageMeta) meta).getStoredEnchants();
        }

        return meta.getEnchants();
    }

    /**
     * Set an enchantment on an IemMeta.
     *
     * @param meta the ItemMeta
     * @param enchant the Enchantment to add
     * @param level the level of the Enchantment
     */
    public static void applyEnchant(@Nullable ItemMeta meta, Enchantment enchant, int level) {
        if (meta == null) {
            return;
        }
        if (meta instanceof EnchantmentStorageMeta) {
            ((EnchantmentStorageMeta) meta).addStoredEnchant(enchant, level, true);
        } else {
            meta.addEnchant(enchant, level, true);
        }
    }

    private EnchantmentUtil() {}

}
