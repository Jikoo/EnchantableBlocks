package com.github.jikoo.enchantableblocks.util.enchant;

import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Helper for overcoming some shortcomings of MockBukkit.
 */
public final class EnchantmentHelper {

    private static final Map<NamespacedKey, Enchantment> KEYS_TO_ENCHANTS;

    static {
        try {
            Field byKey = Enchantment.class.getDeclaredField("byKey");
            byKey.setAccessible(true);
            //noinspection unchecked
            KEYS_TO_ENCHANTS = (Map<NamespacedKey, Enchantment>) byKey.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static Collection<Enchantment> getRegisteredEnchantments() {
        return KEYS_TO_ENCHANTS.values();
    }

    static void putEnchant(NamespacedKey key, Enchantment enchantment) {
        KEYS_TO_ENCHANTS.put(key, enchantment);
    }

    static void wrapCanEnchant() {
        getRegisteredEnchantments().stream().map(enchantment -> {
            EnchantmentMock mock = new EnchantmentMock(enchantment.getKey(), enchantment.getKey().getKey()) {
                @Override
                public boolean canEnchantItem(@NotNull ItemStack item) {
                    // MockBukkit doesn't set up enchantment targets
                    //noinspection ConstantConditions
                    return getItemTarget() != null && getItemTarget().includes(item);
                }
            };
            mock.setItemTarget(enchantment.getItemTarget());
            mock.setMaxLevel(enchantment.getMaxLevel());
            mock.setStartLevel(1);
            // Up to MockBukkit to remove support for curses
            //noinspection deprecation
            mock.setCursed(enchantment.isCursed());
            mock.setTreasure(enchantment.isTreasure());

            return mock;
        }).forEach(enchantmentMock -> putEnchant(enchantmentMock.getKey(), enchantmentMock));
    }

    static void setupToolEnchants() {
        setupEnchant("efficiency", 5, EnchantmentTarget.TOOL);
        setupEnchant("unbreaking", 3, EnchantmentTarget.BREAKABLE);
        setupEnchant("fortune", 3, EnchantmentTarget.TOOL);
        setupEnchant("silk_touch", 1, EnchantmentTarget.TOOL);
        setupEnchant("mending", 1, EnchantmentTarget.BREAKABLE);
    }

    private static void setupEnchant(String id, int levelMax, EnchantmentTarget target) {
        EnchantmentMock mock = (EnchantmentMock) Enchantment.getByKey(NamespacedKey.minecraft(id));
        assert mock != null;
        mock.setMaxLevel(levelMax);
        mock.setStartLevel(1);
        mock.setItemTarget(target);
    }

    private EnchantmentHelper() {}

}
