package com.github.jikoo.enchantableblocks.util.enchant;

/**
 * A representation of enchantment rarity.
 *
 * @author Jikoo
 */
public enum Rarity {

    COMMON(10, 1),
    UNCOMMON(5, 2),
    RARE(2, 4),
    VERY_RARE(1, 8),
    UNKNOWN(0, 40);

    private final int rarity;
    private final int anvilMultiplier;

    Rarity(int rarity, int anvilMultiplier) {
        this.rarity = rarity;
        this.anvilMultiplier = anvilMultiplier;
    }

    public int getWeight() {
        return rarity;
    }

    public int getAnvilValue() {
        return anvilMultiplier;
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
