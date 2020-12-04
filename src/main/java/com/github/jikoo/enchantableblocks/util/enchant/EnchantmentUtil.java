package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.util.WeightedRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility for calculating enchantments.
 *
 * @author Jikoo
 */
public class EnchantmentUtil {

    public static Integer[] getButtonLevels(int shelves) {
        shelves = Math.min(shelves, 15);
        Integer[] levels = new Integer[3];
        Random random = ThreadLocalRandom.current();

        for (int button = 0; button < 3; ++button) {
            // Vanilla - get levels to display in table buttons
            int i = random.nextInt(8) + 1 + (shelves >> 1) + random.nextInt(shelves + 1);
            int level = button == 0 ?  Math.max(i / 3, 1) : button == 1 ?  i * 2 / 3 + 1 : Math.max(i, shelves * 2);

            levels[button] = level > button + 1 ? level : 0;
        }

        return levels;
    }

    // TODO seeded random
    public static Map<Enchantment, Integer> calculateEnchantments(
            @NotNull Collection<Enchantment> enchantments,
            @NotNull BiPredicate<Enchantment, Enchantment> incompatibility,
            int enchantability, int buttonLevel, int seed) {

        // Ensure enchantments present.
        if (enchantments.isEmpty()) {
            return Collections.emptyMap();
        }

        // Determine effective level.
        int effectiveLevel = getEffectiveLevel(enchantability, buttonLevel);
        final int firstEffective = effectiveLevel;

        // Determine available enchantments.
        Collection<EnchantData> enchantData = enchantments.stream().map(EnchantData::of)
                .filter(data -> getEnchantmentLevel(data, firstEffective) > 0).collect(Collectors.toSet());

        // Ensure enchantments are available.
        if (enchantData.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Enchantment, Integer> selected = new HashMap<>();
        addEnchant(selected, enchantData, effectiveLevel, incompatibility);

        while (ThreadLocalRandom.current().nextInt(50) < effectiveLevel) {
            addEnchant(selected, enchantData, effectiveLevel, incompatibility);
            effectiveLevel /= 2;
        }

        return selected;
    }

    private static void addEnchant(@NotNull Map<Enchantment, Integer> selected,
            @NotNull Collection<EnchantData> available, int effectiveLevel,
            @NotNull BiPredicate<Enchantment, Enchantment> incompatibility) {
        if (available.isEmpty())  {
            return;
        }

        EnchantData enchantData = getWeightedEnchant(available);

        if (enchantData == null) {
            return;
        }

        int level = getEnchantmentLevel(enchantData, effectiveLevel);

        if (level <= 0) {
            return;
        }

        selected.put(enchantData.getEnchantment(), level);

        available.removeIf(data -> data == enchantData
                || incompatibility.test(data.getEnchantment(), enchantData.getEnchantment()));

    }

    private static int getEffectiveLevel(int enchantability, int displayedLevel) {
        if (enchantability <= 0) {
            return 0;
        }

        int effectiveLevel = enchantability / 4 + 1;
        Random random = ThreadLocalRandom.current();
        effectiveLevel = displayedLevel + 1 + random.nextInt(effectiveLevel) + random.nextInt(effectiveLevel);
        // Random enchantability penatly/bonus 85-115%
        double bonus = (random.nextDouble() + random.nextDouble() - 1) * 0.15 + 1;
        effectiveLevel = (int) (effectiveLevel * bonus + 0.5);
        return Math.max(effectiveLevel, 1);
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

    private static @Nullable EnchantData getWeightedEnchant(@NotNull Collection<EnchantData> enchants) {
        return WeightedRandom.choose(ThreadLocalRandom.current(), enchants);
    }

    private EnchantmentUtil() {}

}
