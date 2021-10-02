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
   * Get the in-world Block of the EnchantableBlock.
   *
   * @return the Block
   */
  public @NotNull Block getBlock() {
    return this.block;
  }

  /**
   * Get the ItemStack used to create the EnchantableBlock.
   *
   * @return the ItemStack
   */
  public @NotNull ItemStack getItemStack() {
    return this.itemStack;
  }

  /**
   * Check if the EnchantableBlock's in-world location is a Block of a correct Material.
   *
   * @return true if the Block is a valid Material
   */
  public boolean isCorrectBlockType() {
    return this.isCorrectType(this.getBlock().getType());
  }

  /**
   * Check if a Material is valid for the EnchantableBlock.
   *
   * @param material the Material to check
   * @return true if the Material is valid
   */
  public boolean isCorrectType(@NotNull Material material) {
    return this.getRegistration().getMaterials().contains(material);
  }

  /**
   * Ticks the EnchantableBlock.
   */
  public void tick() {}

  /**
   * Check if the EnchantableBlock has unsaved changes pending.
   *
   * @return true if the EnchantableBlock needs to be saved
   */
  public boolean isDirty() {
    this.updateStorage();
    return this.dirty;
  }

  /**
   * Set whether the EnchantableBlock needs to be saved.
   *
   * @param dirty true if the EnchantableBlock needs to be saved
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  /**
   * Update the ConfigurationSection containing the EnchantableBlock's save data.
   */
  public void updateStorage() {
    if (!this.itemStack.equals(getStorage().getItemStack("itemstack"))) {
      getStorage().set("itemstack", this.itemStack);
      this.dirty = true;
    }
  }

  /**
   * Get the ConfigurationSection containing the EnchantableBlock's save data.
   *
   * @return the ConfigurationSection
   */
  protected @NotNull ConfigurationSection getStorage() {
    return storage;
  }

  /**
   * Get the configuration for this EnchantableBlock.
   *
   * @return the configuration
   */
  public @NotNull EnchantableBlockConfig getConfig() {
    return getRegistration().getConfig();
  }

  /**
   * Get the registration providing the EnchantableBlock.
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
