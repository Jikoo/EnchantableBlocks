package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.planarwrappers.function.ThrowingFunction;
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

    /**
     * Fetch an enchantment's internal rarity.
     *
     * @param enchantment the enchantment
     * @return the rarity or UNKNOWN if unable to fetch
     */
    static Rarity getRarity(Enchantment enchantment) {
        return nmsHandler(enchantment, nmsEnchant -> {
            Object enchantmentRarity = nmsEnchant.getClass().getDeclaredMethod("d").invoke(nmsEnchant);
            int weight = (int) enchantmentRarity.getClass().getDeclaredMethod("a").invoke(enchantmentRarity);
            return Rarity.of(weight);
        }, Rarity.UNKNOWN);
    }

    /**
     * Fetch an enchantment's internal minimum enchantment quality calculation method.
     *
     * @param enchantment the enchantment
     * @return the internal method or the default value if the internal method is not available
     */
    static IntUnaryOperator getMinEnchantQuality(Enchantment enchantment) {
        return nmsIntUnaryOperator(enchantment, "a", EnchantDataReflection::defaultMinEnchantQuality);
    }

    /**
     * Default value for minimum enchantment quality method.
     *
     * @param level the level of the enchantment
     * @return the calculated enchantment quality minimum
     */
    private static int defaultMinEnchantQuality(int level) {
        return 1 + level * 10;
    }

    /**
     * Fetch an enchantment's internal maximum enchantment quality calculation method.
     *
     * @param enchantment the enchantment
     * @return the internal method or the default value if the internal method is not available
     */
    protected static IntUnaryOperator getMaxEnchantQuality(Enchantment enchantment) {
        return nmsIntUnaryOperator(enchantment, "b", EnchantDataReflection::defaultMaxEnchantQuality);
    }


    /**
     * Default value for maximum enchantment quality method.
     *
     * @param level the level of the enchantment
     * @return the calculated enchantment quality maximum
     */
    private static int defaultMaxEnchantQuality(int level) {
        return defaultMinEnchantQuality(level) + 5;
    }

    /**
     * Helper for fetching an internal method to convert an int into another int.
     *
     * @param enchantment the enchantment
     * @param methodName the name of the internal method
     * @param defaultOperator the default method
     * @return the internal method or default value if the internal method is not available
     */
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

    /**
     * Helper for fetching an internal object from an enchantment.
     *
     * @param enchantment the enchantment
     * @param function the function to obtain the value
     * @param defaultValue the default value
     * @param <T> the type of value
     * @return the internal value or default value if the internal value is not available
     */
    private static <T> T nmsHandler(@NotNull Enchantment enchantment,
            @NotNull ThrowingFunction<Object, T, ReflectiveOperationException> function, @NotNull T defaultValue) {
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
