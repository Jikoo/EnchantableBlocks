package com.github.jikoo.enchantableblocks.block;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Base for an enchantable block.
 */
public abstract class EnchantableBlock {

  private final @NotNull EnchantableRegistration registration;
  private final @NotNull Block block;
  private final @NotNull ItemStack itemStack;
  private final @NotNull ConfigurationSection storage;
  private boolean dirty = false;

  protected EnchantableBlock(
      final @NotNull EnchantableRegistration registration,
      final @NotNull Block block,
      final @NotNull ItemStack itemStack,
      final @NotNull ConfigurationSection storage) {
    this.registration = registration;
    this.block = block;
    this.itemStack = itemStack.clone();
    if (this.itemStack.getAmount() > 1) {
      this.itemStack.setAmount(1);
    }
    this.storage = storage;
    this.updateStorage();
  }

  /**
   * Get the in-world {@link Block}.
   *
   * @return the {@code Block}
   */
  public @NotNull Block getBlock() {
    return this.block;
  }

  /**
   * Get the {@link ItemStack} that created this block.
   *
   * @return the {@code ItemStack}
   */
  public @NotNull ItemStack getItemStack() {
    return this.itemStack;
  }

  /**
   * Check if the block's in-world location is a {@link Block} of a correct {@link Material}.
   *
   * @return true if the {@code Block} is a valid type
   */
  public boolean isCorrectBlockType() {
    return this.isCorrectType(this.getBlock().getType());
  }

  /**
   * Check if a {@link Material} is valid.
   *
   * @param material the {@code Material} to check
   * @return true if the {@code Material} is valid
   */
  public boolean isCorrectType(@NotNull Material material) {
    return this.getRegistration().getMaterials().contains(material);
  }

  /**
   * Tick the block.
   */
  public void tick() {}

  /**
   * Check if the block has unsaved changes pending.
   *
   * @return true if the block needs to be saved
   */
  public boolean isDirty() {
    this.updateStorage();
    return this.dirty;
  }

  /**
   * Set whether the block needs to be saved.
   *
   * @param dirty true if the block needs to be saved
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  /**
   * Update the {@link ConfigurationSection} containing the block's save data.
   */
  public void updateStorage() {
    if (!this.itemStack.equals(getStorage().getItemStack("itemstack"))) {
      getStorage().set("itemstack", this.itemStack);
      this.dirty = true;
    }
  }

  /**
   * Get the {@link ConfigurationSection} containing the block's save data.
   *
   * @return the save data
   */
  protected @NotNull ConfigurationSection getStorage() {
    return storage;
  }

  /**
   * Get the {@link EnchantableBlockConfig} for this block.
   *
   * @return the configuration
   */
  public @NotNull EnchantableBlockConfig getConfig() {
    return getRegistration().getConfig();
  }

  /**
   * Get the {@link EnchantableRegistration} providing the block implementation.
   *
   * @return the registration
   */
  public @NotNull EnchantableRegistration getRegistration() {
    return this.registration;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{block=" + block + ",itemStack=" + itemStack + "}";
  }

}
