package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.util.function.ThrowingFunction;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.IntUnaryOperator;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

/**
 * A utility for using the server implementation to create new EnchantData instances.
 *
 * @author Jikoo
 */
final class EnchantDataReflection {

    protected static Rarity getRarity(Enchantment enchantment) {
        return nmsHandler(enchantment, nmsEnchant -> {
            Object enchantmentRarity = nmsEnchant.getClass().getDeclaredMethod("d").invoke(nmsEnchant);
            int weight = (int) enchantmentRarity.getClass().getDeclaredMethod("a").invoke(enchantmentRarity);
            return Rarity.of(weight);
        }, Rarity.UNKNOWN);
    }

    protected static IntUnaryOperator getMinEffectiveLevel(Enchantment enchantment) {
        return nmsIntUnaryOperator(enchantment, "a", EnchantDataReflection::defaultMinEffectiveLevel);
    }

    protected static int defaultMinEffectiveLevel(int level) {
        return 1 + level * 10;
    }

    protected static IntUnaryOperator getMaxEffectiveLevel(Enchantment enchantment) {
        return nmsIntUnaryOperator(enchantment, "b", EnchantDataReflection::defaultMaxEffectiveLevel);
    }

    protected static int defaultMaxEffectiveLevel(int level) {
        return defaultMinEffectiveLevel(level) + 5;
    }

    private static IntUnaryOperator nmsIntUnaryOperator(@NotNull Enchantment enchantment,
            @NotNull String methodName, @NotNull IntUnaryOperator defaultOperator) {
        return nmsHandler(enchantment, nmsEnchant -> {
            Method method = nmsEnchant.getClass().getDeclaredMethod(methodName, int.class);
            return level -> {
                try {
                    return (int) method.invoke(nmsEnchant, level);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    return defaultOperator.applyAsInt(level);
                }
            };
        }, defaultOperator);
    }

    private static <T> T nmsHandler(@NotNull Enchantment enchantment,
            @NotNull ThrowingFunction<Object, T> function, @NotNull T defaultValue) {
        try {
            Enchantment craftEnchant = Enchantment.getByKey(enchantment.getKey());

            if (craftEnchant == null) {
                return defaultValue;
            }

            Object nmsEnchant = craftEnchant.getClass().getDeclaredMethod("getHandle").invoke(craftEnchant);

            return function.apply(nmsEnchant);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private EnchantDataReflection() {}

}
