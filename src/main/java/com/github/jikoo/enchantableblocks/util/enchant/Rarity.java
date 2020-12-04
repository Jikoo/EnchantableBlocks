package com.github.jikoo.enchantableblocks.util.enchant;

/**
 * A representation of enchantment rarity.
 *
 * @author Jikoo
 */
public enum Rarity {

    COMMON(10),
    UNCOMMON(5),
    RARE(2),
    VERY_RARE(1),
    UNKNOWN(0);

    private final int rarity;

    Rarity(int rarity) {
        this.rarity = rarity;
    }

    public int getWeight() {
        return rarity;
    }

    static Rarity of(int rarity) {
        for (Rarity value : values()) {
            if (value.rarity == rarity) {
                return value;
            }
        }
        return UNKNOWN;
    }

}
