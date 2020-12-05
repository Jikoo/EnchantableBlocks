package com.github.jikoo.enchantableblocks.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Base for an enchantable block.
 *
 * @author Jikoo
 */
public abstract class EnchantableBlock {

	private final Block block;
	private final ItemStack itemStack;
	private final ConfigurationSection storage;
	private boolean dirty = false;

	EnchantableBlock(@NotNull final Block block, @NotNull final ItemStack itemStack, @NotNull ConfigurationSection storage) {
		this.block = block;
		this.itemStack = itemStack;
		if (itemStack.getAmount() > 1) {
			itemStack.setAmount(1);
		}
		this.storage = storage;
		this.updateStorage();
	}

	/**
	 * Gets the in-world Block of the EnchantableBlock.
	 *
	 * @return the Block
	 */
	public @NotNull Block getBlock() {
		return this.block;
	}

	/**
	 * Gets the ItemStack used to create the EnchantableBlock.
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
	public abstract boolean isCorrectType(@NotNull Material material);

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
	 * Sets whether or not the EnchantableBlock needs to be saved.
	 *
	 * @param dirty true if the EnchantableBlock needs to be saved
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * Updates the ConfigurationSection containing the EnchantableBlock's save data.
	 */
	public void updateStorage() {
		if (!this.itemStack.equals(getStorage().getItemStack("itemstack"))) {
			getStorage().set("itemstack", this.itemStack);
			this.dirty = true;
		}
	}

	/**
	 * Gets the ConfigurationSection containing the EnchantableBlock's save data.
	 *
	 * @return the ConfigurationSection
	 */
	protected @NotNull ConfigurationSection getStorage() {
		return storage;
	}

	@Override
	public String toString() {
		return getClass().getName() + "{itemStack=" + itemStack.toString() + "}";
	}

}
