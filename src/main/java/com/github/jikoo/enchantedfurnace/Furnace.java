package com.github.jikoo.enchantedfurnace;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * Class for tracking custom furnace properties and applying certain effects.
 * 
 * @author Jikoo
 */
public class Furnace {

	private final Block block;
	private final boolean canPause;
	private final ItemStack furnaceItem;

	public Furnace(Block block, ItemStack furnaceItem) {
		this.block = block;
		furnaceItem.getEnchantments().containsKey(Enchantment.SILK_TOUCH);
		this.canPause = furnaceItem.getEnchantments().containsKey(Enchantment.SILK_TOUCH);
		this.furnaceItem = furnaceItem;
		if (canPause && furnaceItem.getEnchantmentLevel(Enchantment.SILK_TOUCH) == 1) {
			// New furnaces shouldn't get 1 tick flame for free, but old furnaces need to re-light
			this.furnaceItem.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 0);
		}
		this.furnaceItem.setAmount(1);
	}

	public Block getBlock() {
		return block;
	}

	public ItemStack getItemStack() {
		return furnaceItem;
	}

	public org.bukkit.block.Furnace getFurnaceTile() {
		try {
			if (block.getType() == Material.FURNACE || block.getType() == Material.BURNING_FURNACE) {
				return (org.bukkit.block.Furnace) block.getState();
			}
			return null;
		} catch (Exception e) {
			// This should not be capable of happening, but just in case I'd rather not break efficiency.
			e.printStackTrace();
			return null;
		}
	}

	public int getCookModifier() {
		return this.furnaceItem.getEnchantmentLevel(Enchantment.DIG_SPEED);
	}

	public int getBurnModifier() {
		return this.furnaceItem.getEnchantmentLevel(Enchantment.DURABILITY);
	}

	public int getFortune() {
		return this.furnaceItem.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
	}

	public boolean canPause() {
		return this.canPause;
	}

	public boolean shouldPause(Event event) {
		if (!this.canPause) {
			return false;
		}

		org.bukkit.block.Furnace f = this.getFurnaceTile();
		if (f == null || f.getBurnTime() <= 0) {
			return false;
		}

		// Is there an input?
		FurnaceInventory i = f.getInventory();
		if (i.getSmelting() == null) {
			return true;
		}

		// Is the result slot too full for more product?
		if (i.getResult() != null) {
			int stack = i.getResult().getType().getMaxStackSize();
			if (event instanceof FurnaceSmeltEvent) {
				stack -= 1;
			}
			if (i.getResult().getAmount() >= stack) {
				return true;
			}
		}

		// Will the input slot be empty once the FurnaceSmeltEvent has completed?
		if (event instanceof FurnaceSmeltEvent && i.getSmelting() != null && i.getSmelting().getAmount() == 1) {
			return true;
		}

		// Verify that the smelting item cannot produce a result
		return !canProduceResult(i.getResult(), i.getSmelting());
	}

	@SuppressWarnings("deprecation")
	private boolean canProduceResult(ItemStack result, ItemStack smelting) {
		Iterator<Recipe> ri =  result != null ? Bukkit.getRecipesFor(result).iterator() : Bukkit.recipeIterator();
		while (ri.hasNext()) {
			Recipe r = ri.next();
			if (!(r instanceof FurnaceRecipe)) {
				continue;
			}
			ItemStack input = ((FurnaceRecipe) r).getInput();
			ItemStack output = ((FurnaceRecipe) r).getResult();
			if (input.getType() != smelting.getType()) {
				continue;
			}
			if (input.getData().getData() > -1 && !input.getData().equals(smelting.getData())) {
				continue;
			}
			if (result != null && !result.isSimilar(output)) {
				continue;
			}
			return true;
		}
		return false;
	}

	public void pause() {
		if (!this.canPause) {
			return;
		}

		org.bukkit.block.Furnace f = this.getFurnaceTile();
		furnaceItem.addUnsafeEnchantment(Enchantment.SILK_TOUCH, f.getBurnTime());
		f.setBurnTime((short) 0);
		f.update(true);
	}

	public boolean resume() {
		org.bukkit.block.Furnace f = this.getFurnaceTile();
		// Is furnace unfrozen already?
		if (f.getBurnTime() > 0 || this.getFrozenTicks() < 1) {
			return false;
		}

		// Is there an input?
		FurnaceInventory i = f.getInventory();
		if (i.getSmelting() == null) {
			return false;
		}

		// Is the output full?
		if (i.getResult() != null && i.getResult().getAmount() == i.getResult().getType().getMaxStackSize()) {
			return false;
		}

		if (!canProduceResult(i.getResult(), i.getSmelting())) {
			return false;
		}

		f.setBurnTime(getFrozenTicks());
		f.update(true);
		furnaceItem.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 0);
		return true;
	}

	public boolean isPaused() {
		return this.canPause && this.furnaceItem.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
	}

	public short getFrozenTicks() {
		return (short) this.furnaceItem.getEnchantmentLevel(Enchantment.SILK_TOUCH);
	}
}
