package com.github.jikoo.enchantableblocks.block;

import com.github.jikoo.enchantableblocks.util.CompatibilityUtil;
import com.github.jikoo.enchantableblocks.util.FurnaceRecipeContainer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Class for tracking custom furnace properties and applying certain effects.
 *
 * @author Jikoo
 */
public class EnchantableFurnace extends EnchantableBlock {

	private final boolean canPause;

	public EnchantableFurnace(final Block block, final ItemStack itemStack) {
		super(block, itemStack);
		this.canPause = itemStack.getEnchantments().containsKey(Enchantment.SILK_TOUCH);
		if (this.canPause && itemStack.getEnchantmentLevel(Enchantment.SILK_TOUCH) == 1) {
			// New furnaces shouldn't get 1 tick flame for free, but old furnaces need to re-light
			itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 0);
			this.setDirty();
		}
		if (itemStack.getAmount() > 1) {
			itemStack.setAmount(1);
			this.setDirty();
		}
	}

	public Furnace getFurnaceTile() {
		try {
			if (this.getBlock().getType() == Material.FURNACE) {
				return (Furnace) this.getBlock().getState();
			}
			return null;
		} catch (Exception e) {
			// This should not be capable of happening, but just in case I'd rather not break efficiency.
			e.printStackTrace();
			return null;
		}
	}

	public int getCookModifier() {
		return this.getItemStack().getEnchantmentLevel(Enchantment.DIG_SPEED);
	}

	public int getBurnModifier() {
		return this.getItemStack().getEnchantmentLevel(Enchantment.DURABILITY);
	}

	public int getFortune() {
		return this.getItemStack().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
	}

	public boolean canPause() {
		return this.canPause;
	}

	public boolean shouldPause(final Event event, FurnaceRecipeContainer recipe) {
		if (!this.canPause) {
			return false;
		}

		Furnace furnace = this.getFurnaceTile();
		if (furnace == null || furnace.getBurnTime() <= 0) {
			return false;
		}

		// Is there an input?
		FurnaceInventory furnaceInv = furnace.getInventory();
		if (furnaceInv.getSmelting() == null) {
			return true;
		}

		// Is the result slot too full for more product?
		if (furnaceInv.getResult() != null) {
			int stack = furnaceInv.getResult().getType().getMaxStackSize();
			if (event instanceof FurnaceSmeltEvent) {
				stack -= 1;
			}
			if (furnaceInv.getResult().getAmount() >= stack) {
				return true;
			}
		}

		// Will the input slot be empty once the FurnaceSmeltEvent has completed?
		if (event instanceof FurnaceSmeltEvent && furnaceInv.getSmelting() != null && furnaceInv.getSmelting().getAmount() == 1) {
			return true;
		}

		if (recipe == null) {
			recipe = CompatibilityUtil.getFurnaceRecipe(furnaceInv);
		}

		if (recipe == null) {
			return true;
		}

		// Verify that the smelting item cannot produce a result
		return !recipe.getEligibleMaterials().contains(furnaceInv.getSmelting().getType())
				|| (furnaceInv.getResult() != null && furnaceInv.getResult().getType() != Material.AIR
				&& !recipe.getResult().isSimilar(furnaceInv.getResult()));

	}

	public void pause() {
		if (!this.canPause) {
			return;
		}

		Furnace furnace = this.getFurnaceTile();
		this.getItemStack().addUnsafeEnchantment(Enchantment.SILK_TOUCH, furnace.getBurnTime());
		this.setDirty();
		furnace.setBurnTime((short) 0);
		furnace.update(true);
	}

	public boolean resume() {
		Furnace furnace = this.getFurnaceTile();
		// Is furnace unfrozen already?
		if (furnace.getBurnTime() > 0 || this.getFrozenTicks() < 1) {
			return false;
		}

		// Is there an input?
		FurnaceInventory furnaceInv = furnace.getInventory();
		if (furnaceInv.getSmelting() == null) {
			return false;
		}

		// Is the output full?
		if (furnaceInv.getResult() != null && furnaceInv.getResult().getAmount() == furnaceInv.getResult().getType().getMaxStackSize()) {
			return false;
		}

		FurnaceRecipeContainer recipe = CompatibilityUtil.getFurnaceRecipe(furnaceInv);

		if (recipe == null) {
			return false;
		}

		if (!recipe.getEligibleMaterials().contains(furnaceInv.getSmelting().getType())
				|| (furnaceInv.getResult() != null && furnaceInv.getResult().getType() != Material.AIR
				&& !recipe.getResult().isSimilar(furnaceInv.getResult()))) {
			return false;
		}

		furnace.setBurnTime(this.getFrozenTicks());
		furnace.update(true);
		this.getItemStack().addUnsafeEnchantment(Enchantment.SILK_TOUCH, 0);
		this.setDirty();
		return true;
	}

	public boolean isPaused() {
		return this.canPause && this.getItemStack().getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
	}

	private short getFrozenTicks() {
		return (short) this.getItemStack().getEnchantmentLevel(Enchantment.SILK_TOUCH);
	}

	@Override
	public boolean isCorrectType(final Material type) {
		return type == Material.FURNACE;
	}

	@Override
	public void tick() {
		if (CompatibilityUtil.areFurnacesSupported()) {
			return;
		}

		if (this.getCookModifier() <= 0) {
			// Not efficiency, we're done here
			return;
		}

		Furnace furnace = this.getFurnaceTile();
		if (furnace == null) {
			// Unloaded furnace or not actually a furnace
			return;
		}
		try {
			// Update cook progress only if there is fuel and something is cooking
			// tile.getInventory().getSmelting() != null incorrectly returns true sometimes
			if (furnace.getBurnTime() > 0 && furnace.getCookTime() > 0) {
				// PaperSpigot compatibility: lag compensation patch can set furnaces to negative cook time.
				int cookTime = Math.max(0, furnace.getCookTime()) + this.getCookModifier();
				if (cookTime > 200) {
					cookTime = 200;
				}
				furnace.setCookTime((short) cookTime);
				furnace.update();
			}
		} catch (Exception e) {
			/*
			 * User reported a NPE with a stack trace pointing to CraftFurnace.getBurnTime()
			 * That can only be thrown if the CraftFurnace's internal TileEntityFurnace is null
			 * or if TileEntityFurnace.burnTime is null. Neither of those issues are my fault,
			 * and I can neither replicate nor fix them.
			 *
			 * Just eat all exceptions - if anything happens here, it's a server implementation issue.
			 */
		}
	}

}
