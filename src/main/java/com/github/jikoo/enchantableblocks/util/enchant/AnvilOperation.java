package com.github.jikoo.enchantableblocks.util.enchant;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A container for data required to calculate an anvil combination.
 */
public class AnvilOperation {

    /**
     * Unmodifiable constant for vanilla settings.
     */
    public static final AnvilOperation VANILLA = new AnvilOperation() {

        private static final String MESSAGE_UNSUPPORTED = "AnvilOperation for vanilla behavior is not manipulable.";

        @Override
        public void setCombineEnchants(boolean combineEnchants) {
            throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
        }

        @Override
        public void setEnchantApplies(@NotNull BiPredicate<Enchantment, ItemStack> enchantApplies) {
            throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
        }

        @Override
        public void setEnchantConflicts(@NotNull BiPredicate<Enchantment, Enchantment> enchantConflicts) {
            throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
        }

        @Override
        public void setEnchantMaxLevel(@NotNull ToIntFunction<Enchantment> enchantMaxLevel) {
            throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
        }

        @Override
        public void setMaterialCombines(@NotNull BiPredicate<ItemStack, ItemStack> materialCombines) {
            throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
        }

        @Override
        public void setMaterialRepairs(@NotNull BiPredicate<ItemStack, ItemStack> materialRepairs) {
            throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
        }

        @Override
        public void setMergeRepairs(boolean mergeRepairs) {
            throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
        }

    };

    private boolean combineEnchants;
    private BiPredicate<Enchantment, ItemStack> enchantApplies;
    private BiPredicate<Enchantment, Enchantment> enchantConflicts;
    private ToIntFunction<Enchantment> enchantMaxLevel;
    private BiPredicate<ItemStack, ItemStack> materialRepairs;
    private BiPredicate<ItemStack, ItemStack> materialCombines;
    private boolean mergeRepairs;

    /**
     * Constructor for a new AnvilOperation.
     */
    public AnvilOperation() {
        combineEnchants = true;
        enchantApplies = Enchantment::canEnchantItem;
        enchantConflicts = Enchantment::conflictsWith;
        enchantMaxLevel = Enchantment::getMaxLevel;
        materialRepairs = AnvilRepairMaterial::repairs;
        materialCombines = (base, addition) ->
                base.getType() == addition.getType() || addition.getType() == Material.ENCHANTED_BOOK;
        mergeRepairs = true;
    }

    /**
     * Get whether enchantments should be combined by the operation.
     *
     * @return true if enchantments should be combined
     */
    public boolean isCombineEnchants() {
        return combineEnchants;
    }

    /**
     * Set whether enchantments should be combined by the operation.
     *
     * @param combineEnchants whether enchantments should be combined
     */
    public void setCombineEnchants(boolean combineEnchants) {
        this.combineEnchants = combineEnchants;
    }

    /**
     * Get the method for determining if an enchantment is applicable for an ItemStack.
     *
     * @return the method for determining if an enchantment is applicable
     */
    public @NotNull BiPredicate<Enchantment, ItemStack> getEnchantApplies() {
        return enchantApplies;
    }

    /**
     * Set the method for determining if an enchantment is applicable for an ItemStack.
     *
     * @param enchantApplies the method for determining if an enchantment is applicable
     */
    public void setEnchantApplies(
            @NotNull BiPredicate<Enchantment, ItemStack> enchantApplies) {
        this.enchantApplies = enchantApplies;
    }

    /**
     * Get the method for determining if enchantments conflict.
     *
     * @return the method for determining if enchantments conflict
     */
    public @NotNull BiPredicate<Enchantment, Enchantment> getEnchantConflicts() {
        return enchantConflicts;
    }

    /**
     * Set the method for determining if enchantments conflict.
     *
     * @param enchantConflicts the method for determining if enchantments conflict
     */
    public void setEnchantConflicts(
            @NotNull BiPredicate<Enchantment, Enchantment> enchantConflicts) {
        this.enchantConflicts = enchantConflicts;
    }

    /**
     * Get the method supplying maximum level for an enchantment.
     *
     * @return the method supplying maximum level for an enchantment
     */
    public @NotNull ToIntFunction<Enchantment> getEnchantMaxLevel() {
        return enchantMaxLevel;
    }

    /**
     * Set the method supplying maximum level for an enchantment.
     *
     * @param enchantMaxLevel the method supplying maximum level for an enchantment
     */
    public void setEnchantMaxLevel(@NotNull ToIntFunction<Enchantment> enchantMaxLevel) {
        this.enchantMaxLevel = enchantMaxLevel;
    }

    /**
     * Get the method determining whether an item should combine its enchantments with another item.
     *
     * @return the method determining whether an item should combine its enchantments
     */
    public @NotNull BiPredicate<ItemStack, ItemStack> getMaterialCombines() {
        return materialCombines;
    }

    /**
     * Set the method determining whether an item should combine its enchantments with another item.
     *
     * @param materialCombines the method determining whether an item should combine its enchantments
     */
    public void setMaterialCombines(
            @NotNull BiPredicate<ItemStack, ItemStack> materialCombines) {
        this.materialCombines = materialCombines;
    }

    /**
     * Get the method determining whether an item is repaired by another item.
     *
     * @see #setMaterialRepairs
     * @return the method determining whether an item is repaired by another item
     */
    public @NotNull BiPredicate<ItemStack, ItemStack> getMaterialRepairs() {
        return materialRepairs;
    }

    /**
     * Set the method determining whether an item is repaired by another item.
     *
     * <p>N.B. Only {@link org.bukkit.inventory.meta.Damageable Damageable} items can be repaired.
     * This is not the same as a repair via combination!
     * Matching materials' combination should be controlled via {@link #setMergeRepairs}.
     * A material repair restores 25% of the durability of an item per material consumed.
     *
     * @param materialRepairs the method determining whether an item is repaired by another item
     */
    public void setMaterialRepairs(
            @NotNull BiPredicate<ItemStack, ItemStack> materialRepairs) {
        this.materialRepairs = materialRepairs;
    }

    /**
     * Get whether the base item is allowed to be repaired by merging with the addition.
     *
     * @see #setMergeRepairs
     * @return whether the base item is allowed to be repaired by the addition
     */
    public boolean isMergeRepairs() {
        return mergeRepairs;
    }

    /**
     * Set whether the base item is allowed to be repaired by merging with the addition.
     *
     * <p>N.B. Only {@link org.bukkit.inventory.meta.Damageable Damageable} items can be repaired.
     * A merge repair combines the remaining durability of both items and adds a bonus of 12% of maximum durability.
     *
     * @param mergeRepairs whether the base item is allowed to be repaired by the addition
     */
    public void setMergeRepairs(boolean mergeRepairs) {
        this.mergeRepairs = mergeRepairs;
    }

    /**
     * Get an AnvilResult for this anvil operation.
     *
     * @param base the base item
     * @param addition the added item
     * @return the AnvilResult
     */
    public @NotNull AnvilResult apply(@NotNull ItemStack base, @NotNull ItemStack addition) {
        return AnvilUtil.combine(base, addition, this);
    }

}
