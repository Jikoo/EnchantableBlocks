package com.github.jikoo.enchantedfurnace;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * @author Jikoo
 *
 */
public class Furnace {

	private Block b;
	private int cookModifier;
	private int burnModifier;
	private int fortune;
	private boolean canPause;
	private short frozenTicks = 0;

	protected Furnace(Block b, int cookModifier, int burnModifier, int fortune, boolean canPause) {
		this.b = b;
		this.cookModifier = cookModifier;
		this.burnModifier = burnModifier;
		this.fortune = fortune;
		this.canPause = canPause;
	}
	protected Furnace(Block b, int cookModifier, int burnModifier, int fortune, short frozenTicks) {
		this.b = b;
		this.cookModifier = cookModifier;
		this.burnModifier = burnModifier;
		this.fortune = fortune;
		this.canPause = frozenTicks >= 0;
		this.frozenTicks = frozenTicks;
	}

	protected Block getBlock() {
		return b;
	}

	protected org.bukkit.block.Furnace getFurnaceTile() {
		if (b.getType() == Material.FURNACE || b.getType() == Material.BURNING_FURNACE) {
			return (org.bukkit.block.Furnace) b.getState();
		}
		return null;
	}

	protected int getCookModifier() {
		return this.cookModifier;
	}

	protected int getBurnModifier() {
		return this.burnModifier;
	}

	protected int getFortune() {
		return this.fortune;
	}

	protected boolean canPause() {
		return this.canPause;
	}

	protected void pause() {
		if (!this.canPause) {
			return;
		}
		org.bukkit.block.Furnace f = this.getFurnaceTile();
		this.frozenTicks = f.getBurnTime();
		f.setBurnTime((short) 0);
		f.update(true);
	}

	@SuppressWarnings("deprecation")
	protected void resume() {
		org.bukkit.block.Furnace f = this.getFurnaceTile();
		if (f.getBurnTime() > 0 || this.frozenTicks < 1) {
			return;
		}

		FurnaceInventory i = f.getInventory();
		if (i.getSmelting() == null) {
			return;
		}
		if (i.getResult() != null) {
			Iterator<Recipe> ri = Bukkit.recipeIterator();
			while (ri.hasNext()) {
				Recipe r = ri.next();
				if (r instanceof FurnaceRecipe) {
					if (((FurnaceRecipe) r).getInput().getType() == f.getInventory().getSmelting().getType()) {
						if (r.getResult().getType() != i.getResult().getType()) {
							return;
						}
						break;
					}
				}
			}
		}

		ItemStack[] items = i.getContents().clone();
		HashSet<HumanEntity> viewers = new HashSet<HumanEntity>(i.getViewers());
		f.getInventory().clear();
		byte rotation = f.getData().getData();
		f.update(true);
		f.setType(Material.BURNING_FURNACE);
		f.setRawData(rotation);
		f.update(true);
		f = this.getFurnaceTile();
		i = f.getInventory();
		i.setContents(items);
		f.setBurnTime(frozenTicks);
		f.update(true);
		this.frozenTicks = 0;
		for (HumanEntity v : viewers) {
			v.openInventory(i);
		}
	}

	protected boolean isPaused() {
		return this.frozenTicks > 0;
	}

	protected short getFrozenTicks() {
		return this.frozenTicks;
	}
}
