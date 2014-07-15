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
		try {
			for (Furnace f : EnchantedFurnace.getInstance().getFurnaces()) {
				org.bukkit.block.Furnace tile = f.getFurnaceTile();
				if (tile == null) {
					continue;
				}
				// Update cook progress only if furnace is efficiency, there is fuel, and something is smelting.
				if (f.getCookModifier() > 0 && tile.getBurnTime() > 0 && tile.getInventory().getSmelting() != null) {
					tile.setCookTime((short) (tile.getCookTime() + f.getCookModifier()));
					tile.update();
				}
			}
		} catch (Exception e) {
		}
	}
}
