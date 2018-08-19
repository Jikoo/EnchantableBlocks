package com.github.jikoo.enchantableblocks.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;

/**
 * FurnaceRecipeContainer
 *
 * @author Jikoo
 */
public class FurnaceRecipeContainer {

	private final EnumSet<Material> eligibleMaterials;
	private final int cookTime;
	private final ItemStack result;

	public FurnaceRecipeContainer(EnumSet<Material> eligibleMaterials, int cookTime, ItemStack result) {
		this.eligibleMaterials = eligibleMaterials;
		this.cookTime = cookTime;
		this.result = result;
	}

	public EnumSet<Material> getEligibleMaterials() {
		return eligibleMaterials;
	}

	public int getCookTime() {
		return cookTime;
	}

	public ItemStack getResult() {
		return result;
	}

}
