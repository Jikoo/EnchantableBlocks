package com.github.jikoo.enchantableblocks.util.enchant;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Representation of materials' enchantability.
 *
 * @author Jikoo
 */
public enum Enchantability {

    LEATHER(15),
    CHAIN(12),
    IRON_ARMOR(9),
    GOLD_ARMOR(25),
    DIAMOND(10),
    TURTLE(9),
    NETHERITE(15),
    WOOD(15),
    STONE(5),
    IRON_TOOL(14),
    GOLD_TOOL(22),
    BOOK(1),
    TRIDENT(1);

    private final int enchantability;

    Enchantability(int enchantability) {
        this.enchantability = enchantability;
    }

    /**
     * Gets the magic enchantability value.
     *
     * @return the magic value
     */
    int getEnchantability() {
        return enchantability;
    }

    /**
     * Convert a magic number into an enchantability value.
     *
     * <p>Not for production use.
     *
     * @param enchantability the magic enchantability number
     * @return the closest enchantability enum value
     */
    public static @NotNull Enchantability convert(int enchantability) {
        TreeMap<Integer, Enchantability> values = getMappedValues();

        // Get floor and ceiling values.
        Map.Entry<Integer, Enchantability> floor = values.floorEntry(enchantability);
        Map.Entry<Integer, Enchantability> ceiling = values.ceilingEntry(enchantability);

        // If ceiling is null, no values are higher, so floor is highest.
        if (ceiling == null || floor != null && floor.getKey() == enchantability) {
            return floor.getValue();
        }

        // If floor is null, no values are lower, so ceiling is lowest.
        if (floor == null || ceiling.getKey() == enchantability) {
            return ceiling.getValue();
        }

        int floorDiff = enchantability - floor.getKey();
        int ceilDiff = ceiling.getKey() - enchantability;

        // If no exact match exists, select the closer value with a preference for the lower.
        return floorDiff <= ceilDiff ? floor.getValue() : ceiling.getValue();
    }

    /**
     * Get sorted values mapped to magic internal enchantability.
     *
     * <p>Note: Conflicts result in the first value being preserved.
     * <br>For example, {@link #LEATHER}, {@link #NETHERITE}, and {@link #WOOD} all have the same magic value.
     * {@link #LEATHER} comes first, so {@link #NETHERITE} and {@link #WOOD} will never be present.
     *
     * @return a map of enchantability for magic values
     */
    private static @NotNull TreeMap<Integer, Enchantability> getMappedValues() {
        return Arrays.stream(values()).collect(Collectors.toMap(
                Enchantability::getEnchantability, Function.identity(), (a, b) -> a, TreeMap::new));
    }

}
