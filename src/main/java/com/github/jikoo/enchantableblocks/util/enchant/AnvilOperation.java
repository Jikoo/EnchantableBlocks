package com.github.jikoo.enchantableblocks.util.enchant;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A container for data required to calculate an anvil combination.
 */
public class AnvilOperation {

  /**
   * Constant for vanilla settings. Only renameText is modifiable.
   */
  public static final AnvilOperation VANILLA = new AnvilOperation() {

    private static final String MESSAGE_UNSUPPORTED =
        "AnvilOperation for vanilla behavior is not manipulable.";

    @Override
    public void setCombineEnchants(boolean combineEnchants) {
      throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
    }

    @Override
    public void setEnchantApplies(@NotNull BiPredicate<Enchantment, ItemStack> enchantApplies) {
      throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED);
    }

    @Override
    public void setEnchantConflicts(
        @NotNull BiPredicate<Enchantment, Enchantment> enchantConflicts) {
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
  private @NotNull BiPredicate<@NotNull Enchantment, @NotNull ItemStack> enchantApplies;
  private @NotNull BiPredicate<@NotNull Enchantment, @NotNull Enchantment> enchantConflicts;
  private @NotNull ToIntFunction<@NotNull Enchantment> enchantMaxLevel;
  private @NotNull BiPredicate<@NotNull ItemStack, @NotNull ItemStack> materialRepairs;
  private @NotNull BiPredicate<@NotNull ItemStack, @NotNull ItemStack> materialCombines;
  private boolean mergeRepairs;
  private @Nullable String renameText;

  /**
   * Construct a new {@code AnvilOperation}.
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
   * Get whether {@link Enchantment Enchantments} should be combined by the operation.
   *
   * @return true if {@code Enchantments} should be combined
   */
  public boolean isCombineEnchants() {
    return combineEnchants;
  }

  /**
   * Set whether {@link Enchantment Enchantments} should be combined by the operation.
   *
   * @param combineEnchants whether {@code Enchantments} should be combined
   */
  public void setCombineEnchants(boolean combineEnchants) {
    this.combineEnchants = combineEnchants;
  }

  /**
   * Get the method for determining if an {@link Enchantment} is applicable for an
   * {@link ItemStack}.
   *
   * @return the method for determining if an {@code Enchantment} is applicable
   */
  public @NotNull BiPredicate<@NotNull Enchantment, @NotNull ItemStack> getEnchantApplies() {
    return enchantApplies;
  }

  /**
   * Set the method for determining if an {@link Enchantment} is applicable for an
   * {@link ItemStack}.
   *
   * @param enchantApplies the method for determining if an {@code Enchantment} is applicable
   */
  public void setEnchantApplies(
      @NotNull BiPredicate<@NotNull Enchantment, @NotNull ItemStack> enchantApplies) {
    this.enchantApplies = enchantApplies;
  }

  /**
   * Get the method for determining if {@link Enchantment Enchantments} conflict.
   *
   * @return the method for determining if {@code Enchantments} conflict
   */
  public @NotNull BiPredicate<@NotNull Enchantment, @NotNull Enchantment> getEnchantConflicts() {
    return enchantConflicts;
  }

  /**
   * Set the method for determining if {@link Enchantment Enchantments} conflict.
   *
   * @param enchantConflicts the method for determining if {@code Enchantments} conflict
   */
  public void setEnchantConflicts(
      @NotNull BiPredicate<@NotNull Enchantment, @NotNull Enchantment> enchantConflicts) {
    this.enchantConflicts = enchantConflicts;
  }

  /**
   * Get the method supplying maximum level for an {@link Enchantment}.
   *
   * @return the method supplying maximum level for an {@code Enchantment}
   */
  public @NotNull ToIntFunction<@NotNull Enchantment> getEnchantMaxLevel() {
    return enchantMaxLevel;
  }

  /**
   * Set the method supplying maximum level for an {@link Enchantment}.
   *
   * @param enchantMaxLevel the method supplying maximum level for an {@code Enchantment}
   */
  public void setEnchantMaxLevel(@NotNull ToIntFunction<@NotNull Enchantment> enchantMaxLevel) {
    this.enchantMaxLevel = enchantMaxLevel;
  }

  /**
   * Get the method determining whether an item should combine its {@link Enchantment Enchantments}
   * with another item.
   *
   * @return the method determining whether an item should combine its {@code Enchantments}
   */
  public @NotNull BiPredicate<@NotNull ItemStack, @NotNull ItemStack> getMaterialCombines() {
    return materialCombines;
  }

  /**
   * Set the method determining whether an item should combine its {@link Enchantment Enchantments}
   * with another item.
   *
   * @param materialCombines the method determining whether an item should combine its
   *                         {@code Enchantments}
   */
  public void setMaterialCombines(
      @NotNull BiPredicate<@NotNull ItemStack, @NotNull ItemStack> materialCombines) {
    this.materialCombines = materialCombines;
  }

  /**
   * Get the method determining whether an item is repaired by another item.
   *
   * @see #setMaterialRepairs
   * @return the method determining whether an item is repaired by another item
   */
  public @NotNull BiPredicate<@NotNull ItemStack, @NotNull ItemStack> getMaterialRepairs() {
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
      @NotNull BiPredicate<@NotNull ItemStack, @NotNull ItemStack> materialRepairs) {
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
   * A merge repair combines the remaining durability of both items and adds a bonus of 12% of
   * maximum durability.
   *
   * @param mergeRepairs whether the base item is allowed to be repaired by the addition
   */
  public void setMergeRepairs(boolean mergeRepairs) {
    this.mergeRepairs = mergeRepairs;
  }

  /**
   * Get the name applied to the resulting item.
   *
   * @return the rename text
   */
  public @Nullable String getRenameText() {
    return renameText;
  }

  /**
   * Set the name applied to the resulting item by this operation.
   *
   * @param renameText the name applied to the resulting item
   */
  public void setRenameText(@Nullable String renameText) {
    this.renameText = renameText;
  }

  /**
   * Get an {@link AnvilResult} for this anvil operation.
   *
   * <p>N.B. for ease of operation reuse, calls to
   * {@code AnvilOperation#apply(ItemStack, ItemStack)} also reset {@link #getRenameText()} to
   * {@code null}.
   *
   * @param base the base {@link ItemStack}
   * @param addition the added {@code ItemStack}
   * @return the {@code AnvilResult}
   * @see #setRenameText(String)
   */
  public @NotNull AnvilResult apply(@NotNull ItemStack base, @NotNull ItemStack addition) {
    var result = AnvilUtil.combine(base, addition, this);
    this.setRenameText(null);
    return result;
  }

}
