package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.util.WeightedRandom;

/**
 * A representation of enchantment rarity.
 *
 * @author Jikoo
 */
public enum Rarity implements WeightedRandom.Choice {

    COMMON(10),
    UNCOMMON(5),
    RARE(2),
    VERY_RARE(1),
    UNKNOWN(0);

    private final int rarity;

    Rarity(int rarity) {
        this.rarity = rarity;
    }

    @Override
    public int getWeight() {
        return rarity;
    }

    /**
     * Get a rarity based on weight.
     *
     * @param weight the weight of the rarity
     * @return the rarity or {@link #UNKNOWN} if none match
     */
    static Rarity of(int weight) {
        for (Rarity value : values()) {
            if (value.rarity == weight) {
                return value;
            }
        }
        return UNKNOWN;
    }

}
