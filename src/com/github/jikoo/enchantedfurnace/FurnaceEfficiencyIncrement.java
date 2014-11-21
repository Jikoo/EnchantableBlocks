package com.github.jikoo.enchantedfurnace;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runnable used to tick all efficiency-enchanted furnaces.
 * 
 * @author Jikoo
 */
public class FurnaceEfficiencyIncrement extends BukkitRunnable {
	@Override
	public void run() {
		for (Furnace f : EnchantedFurnace.getInstance().getFurnaces()) {
			if (f.getCookModifier() <= 0) {
				// Not efficiency, we're done here
				continue;
			}
			org.bukkit.block.Furnace tile = f.getFurnaceTile();
			if (tile == null) {
				// Unloaded furnace or not a furnace
				continue;
			}
			// Update cook progress only if there is fuel and something is cooking
			// tile.getInventory().getSmelting() != null incorrectly returns true sometimes
			if (tile.getBurnTime() > 0 && tile.getCookTime() > 0) {
				tile.setCookTime((short) (tile.getCookTime() + f.getCookModifier()));
				tile.update();
			}
		}
	}
}
