package com.github.jikoo.enchantedfurnace;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runnable used to tick all efficiency-enchanted furnaces.
 * 
 * @author Jikoo
 */
public class FurnaceEfficiencyIncrement extends BukkitRunnable {

	private final EnchantedFurnace plugin;

	public FurnaceEfficiencyIncrement(EnchantedFurnace plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		for (Furnace f : plugin.getFurnaces()) {
			if (f.getCookModifier() <= 0) {
				// Not efficiency, we're done here
				continue;
			}
			org.bukkit.block.Furnace tile = f.getFurnaceTile();
			if (tile == null) {
				// Unloaded furnace or not a furnace
				continue;
			}
			try {
				// Update cook progress only if there is fuel and something is cooking
				// tile.getInventory().getSmelting() != null incorrectly returns true sometimes
				if (tile.getBurnTime() > 0 && tile.getCookTime() > 0) {
					int cookTime = tile.getCookTime() + f.getCookModifier();
					if (cookTime > 200) {
						cookTime = 200;
					}
					tile.setCookTime((short) cookTime);
					tile.update();
				}
			} catch (Exception e) {
				/* 
				 * User reported a NPE with a stack trace pointing to CraftFurnace.getBurnTime()
				 * That can only be thrown if the CraftFurnace's internal TileEntityFurnace is null
				 * or if TileEntityFurnace.burnTime is null. Neither of those issues are my fault,
				 * and I can neither replicate nor fix them.
				 * 
				 * Just eat all exceptions - if anything happens here, it's a CB/Spigot issue.
				 */
			}
		}
	}
}
