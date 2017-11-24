package com.github.jikoo.enchantableblocks;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * Base for an enchantable block.
 *
 * @author Jikoo
 */
public abstract class EnchantableBlock {

	private final Block block;
	private final ItemStack itemStack;
	private boolean dirty = false;

	public EnchantableBlock(final Block block, final ItemStack itemStack) {
		this.block = block;
		this.itemStack = itemStack;
	}

	/**
	 * Gets the in-world Block of the EnchantableBlock.
	 *
	 * @return the Block
	 */
	public Block getBlock() {
		return this.block;
	}

	/**
	 * Gets the ItemStack used to create the EnchantableBlock.
	 *
	 * @return the ItemStack
	 */
	public ItemStack getItemStack() {
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
	public abstract boolean isCorrectType(Material material);

	/**
	 * Ticks the EnchantableBlock.
	 */
	public abstract void tick();

	/**
	 * Check if the EnchantableBlock has unsaved changes pending.
	 *
	 * @return true if the EnchantableBlock needs to be saved
	 */
	public boolean isDirty() {
		return this.dirty;
	}

	/**
	 * Set the EnchantableBlock as needing to be saved.
	 */
	public void setDirty() {
		this.setDirty(true);
	}

	/**
	 * Sets whether or not the EnchantableBlock needs to be saved.
	 *
	 * @param dirty true if the EnchantableBlock needs to be saved
	 */
	public void setDirty(final boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * Saves the EnchantableBlock to a ConfigurationSection.
	 * <p>
	 * If the EnchantableBlock is being saved for the first time, the return value is ignored.
	 * However, if the EnchantableBlock already exists, the return value should reflect whether or
	 * not the stored data has changed.
	 *
	 * @param saveSection the ConfigurationSection
	 * @return true if existing data may have changed
	 */
	public boolean save(final ConfigurationSection saveSection) {
		if (this.itemStack == null || this.itemStack.equals(saveSection.getItemStack("itemstack"))) {
			return false;
		}

		saveSection.set("itemstack", this.itemStack);
		return true;
	}

}
