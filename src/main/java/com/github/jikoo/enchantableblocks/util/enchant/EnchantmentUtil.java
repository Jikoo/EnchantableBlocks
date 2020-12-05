package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.util.WeightedRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

/**
 * A utility for calculating enchantments.
 *
 * @author Jikoo
 */
public class EnchantmentUtil {

    /**
     * Get three integers representing button levels in an enchantment table.
     *
     * @param shelves the number of bookshelves to use when calculating levels
     * @param seed the seed of the calculation
     * @return an array of three integers
     */
    public static @NotNull Integer[] getButtonLevels(int shelves, long seed) {
        shelves = Math.min(shelves, 15);
        Integer[] levels = new Integer[3];
        Random random = new Random(seed);

        for (int button = 0; button < 3; ++button) {
            // Vanilla - get levels to display in table buttons
            int i = random.nextInt(8) + 1 + (shelves >> 1) + random.nextInt(shelves + 1);
            int level = button == 0 ?  Math.max(i / 3, 1) : button == 1 ?  i * 2 / 3 + 1 : Math.max(i, shelves * 2);

            levels[button] = level > button + 1 ? level : 0;
        }

        return levels;
    }

    /**
     * Generate a set of levelled enchantments in a similar fashion to vanilla.
     * Follows provided rules for enchantment incompatibility.
     *
     * @param enchantments the list of eligible enchantments
     * @param incompatibility a method for determining if enchantments are incompatible
     * @param enchantability the enchantability of the item
     * @param buttonLevel the level of the enchantment
     * @param seed the seed of the enchantment
     * @return the selected enchantments mapped to their corresponding levels
     */
    public static Map<Enchantment, Integer> calculateEnchantments(
            @NotNull Collection<Enchantment> enchantments,
            @NotNull BiPredicate<Enchantment, Enchantment> incompatibility,
            @NotNull Enchantability enchantability, int buttonLevel, long seed) {

        // Ensure enchantments present.
        if (enchantments.isEmpty()) {
            return Collections.emptyMap();
        }

        // Seed random as specified.
        Random random = new Random(seed);

        // Determine effective level.
        int enchantQuality = getEnchantQuality(random, enchantability, buttonLevel);
        final int firstEffective = enchantQuality;

        // Determine available enchantments.
        Collection<EnchantData> enchantData = enchantments.stream().map(EnchantData::of)
                .filter(data -> getEnchantmentLevel(data, firstEffective) > 0).collect(Collectors.toSet());

        // Ensure enchantments are available.
        if (enchantData.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Enchantment, Integer> selected = new HashMap<>();
        addEnchant(selected, random, enchantData, enchantQuality, incompatibility);

        while (!enchantData.isEmpty() && random.nextInt(50) < enchantQuality) {
            addEnchant(selected, random, enchantData, enchantQuality, incompatibility);
            enchantQuality /= 2;
        }

        return selected;
    }

    private static void addEnchant(@NotNull Map<Enchantment, Integer> selected,
            @NotNull Random random, @NotNull Collection<EnchantData> available,
            int effectiveLevel, @NotNull BiPredicate<Enchantment, Enchantment> incompatibility) {
        if (available.isEmpty())  {
            return;
        }

        EnchantData enchantData = getWeightedEnchant(random, available);

        int level = getEnchantmentLevel(enchantData, effectiveLevel);

        if (level <= 0) {
            return;
        }

        selected.put(enchantData.getEnchantment(), level);

        available.removeIf(data -> data == enchantData
                || incompatibility.test(data.getEnchantment(), enchantData.getEnchantment()));

    }

    private static int getEnchantQuality(@NotNull Random random, @NotNull Enchantability enchantability, int displayedLevel) {
        if (enchantability.getEnchantability() <= 0) {
            return 0;
        }

        int enchantQuality = enchantability.getEnchantability() / 4 + 1;
        enchantQuality = displayedLevel + 1 + random.nextInt(enchantQuality) + random.nextInt(enchantQuality);
        // Random enchantability penatly/bonus 85-115%
        double bonus = (random.nextDouble() + random.nextDouble() - 1) * 0.15 + 1;
        enchantQuality = (int) (enchantQuality * bonus + 0.5);
        return Math.max(enchantQuality, 1);
    }

    private static int getEnchantmentLevel(@NotNull EnchantData enchant, int effectiveLevel) {
        for (int i = enchant.getEnchantment().getMaxLevel(); i >= enchant.getEnchantment().getStartLevel(); --i) {
            if (enchant.getMaxEffectiveLevel(i) < effectiveLevel) {
                return 0;
            }
            if (enchant.getMinEffectiveLevel(i) <= effectiveLevel) {
                return i;
            }
        }
        return 0;
    }

    private static @NotNull EnchantData getWeightedEnchant(@NotNull Random random,
            @NotNull Collection<EnchantData> enchants) {
        return WeightedRandom.choose(random, enchants);
    }

    private EnchantmentUtil() {}

}
