package com.github.jikoo.enchantedfurnace;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

	private Block b;
	private int cookModifier;
	private int burnModifier;
	private int fortune;
	private boolean canPause;
	private short frozenTicks = 0;

	public Furnace(Block b, int cookModifier, int burnModifier, int fortune, boolean canPause) {
		this(b, cookModifier, burnModifier, fortune, (short) (canPause ? 0 : -1));
	}

	public Furnace(Block b, int cookModifier, int burnModifier, int fortune, short frozenTicks) {
		this.b = b;
		this.cookModifier = cookModifier;
		this.burnModifier = burnModifier;
		this.fortune = fortune;
		this.canPause = frozenTicks >= 0;
		this.frozenTicks = frozenTicks;
	}

	public Block getBlock() {
		return b;
	}

	public org.bukkit.block.Furnace getFurnaceTile() {
		if (!b.getWorld().isChunkLoaded(b.getChunk())) {
			// Chunk must be loaded to get BlockState
			return null;
		}
		if (b.getType() == Material.FURNACE || b.getType() == Material.BURNING_FURNACE) {
			return (org.bukkit.block.Furnace) b.getState();
		}
		return null;
	}

	public int getCookModifier() {
		return this.cookModifier;
	}

	public int getBurnModifier() {
		return this.burnModifier;
	}

	public int getFortune() {
		return this.fortune;
	}

	public boolean canPause() {
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

		if (i.getResult() != null && i.getResult().getAmount() >= i.getResult().getType().getMaxStackSize()) {
			return true;
		}

		// Verify that the smelting item cannot produce a result
		return !canProduceResult(i.getResult() != null ? Bukkit.getRecipesFor(i.getResult()).iterator() : Bukkit.recipeIterator(), i.getSmelting(), i.getResult());
	}

	@SuppressWarnings("deprecation")
	private boolean canProduceResult(Iterator<Recipe> ri, ItemStack smelting, ItemStack result) {
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
		frozenTicks = f.getBurnTime();
		f.setBurnTime((short) 0);
		f.update(true);
	}

	public boolean resume() {
		org.bukkit.block.Furnace f = this.getFurnaceTile();
		// Is furnace unfrozen already?
		if (f.getBurnTime() > 0 || this.frozenTicks < 1) {
			return false;
		}

		// Is there an input?
		FurnaceInventory i = f.getInventory();
		if (i.getSmelting() == null) {
			return false;
		}

		if (!canProduceResult(i.getResult() != null ? Bukkit.getRecipesFor(i.getResult()).iterator() : Bukkit.recipeIterator(), i.getSmelting(), i.getResult())) {
			return false;
		}

		f.setBurnTime(frozenTicks);
		f.update(true);
		frozenTicks = 0;
		return true;
	}

	public boolean isPaused() {
		return this.frozenTicks > 0;
	}

	public short getFrozenTicks() {
		return this.frozenTicks;
	}
}
