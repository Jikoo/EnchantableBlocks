package com.github.jikoo.enchantableblocks.util.enchant;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;
import org.bukkit.enchantments.Enchantment;

/**
 * A container for data required to calculate enchantments.
 */
public class EnchantOperation {

    public static final BiPredicate<Enchantment, Enchantment> DEFAULT_INCOMPATIBILITY = (enchantment, enchantment2) -> {
        if (enchantment.equals(enchantment2)) {
            return true;
        }
        return enchantment.conflictsWith(enchantment2);
    };

    Collection<Enchantment> enchantments;
    int buttonLevel = -1;
    long seed = -1;
    BiPredicate<Enchantment, Enchantment> incompatibility = DEFAULT_INCOMPATIBILITY;
    Enchantability enchantability = Enchantability.BOOK;

    /**
     * Constructor for a new EnchantOperation.
     *
     * @param enchantments the enchantments that may be applied
     */
    public EnchantOperation(Collection<Enchantment> enchantments) {
        this.enchantments = enchantments;
    }

    /**
     * Get the enchantments that may be applied by the enchantment operation.
     *
     *  @return a Collection of enchantments that may be applied
     */
    public Collection<Enchantment> getEnchantments() {
        return this.enchantments;
    }

    /**
     * Set the level of the button used for enchanting.
     *
     * @param buttonLevel the button level
     */
    public void setButtonLevel(int buttonLevel) {
        this.buttonLevel = buttonLevel;
    }

    /**
     * Get the level of the button used for enchanting.
     *
     * @return the enchantment level
     */
    public int getButtonLevel() {
        return this.buttonLevel;
    }

    /**
     * Set the seed used by the {@link java.util.Random Random} to ensure consistent results for consistent inputs.
     *
     * @param seed the enchanting seed
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    /**
     * Get the seed for enchanting.
     *
     * @return the enchanting seed
     */
    public long getSeed() {
        return this.seed;
    }

    /**
     * Set the {@link Enchantability} of the operation.
     *
     * @param enchantability the enchantability of the operation
     */
    public void setEnchantability(Enchantability enchantability) {
        this.enchantability = enchantability;
    }

    /**
     * Get the {@link Enchantability} of the operation.
     *
     * @return the enchatability of the operation
     */
    public Enchantability getEnchantability() {
        return this.enchantability;
    }

    /**
     * Set the method determining if two enchantments are incompatible.
     *
     * @param incompatibility the method determining enchantments incompatible
     */
    public void setIncompatibility(BiPredicate<Enchantment, Enchantment> incompatibility) {
        this.incompatibility = incompatibility;
    }

    /**
     * Get the method for comparing enchantments to determine incompatiblity.
     *
     * @return the enchantment incompatibility comparison
     */
    public BiPredicate<Enchantment, Enchantment> getIncompatibility() {
        return this.incompatibility;
    }

    /**
     * Get the enchantments resulting from the enchanting operation.
     *
     * @return the enchantments resulting from the enchanting operation
     */
    public Map<Enchantment, Integer> apply() {
        return EnchantingTableUtil.calculateEnchantments(this);
    }

}
