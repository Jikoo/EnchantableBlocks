package com.github.jikoo.enchantedfurnace;

/**
 * Runnable used to tick all efficiency-enchanted furnaces.
 * 
 * @author Jikoo
 */
public class FurnaceTick implements Runnable {
	@Override
	public void run() {
		for (Furnace f : EnchantedFurnace.getInstance().getFurnaces()) {
			try {
				org.bukkit.block.Furnace tile = f.getFurnaceTile();
				if (tile.getBurnTime() > 0) {
					tile.setCookTime((short) (tile.getCookTime() + f.getCookModifier()));
					tile.update();
				}
			} catch (NullPointerException e) {
				// Unloaded chunk or something, tile untickable
			}
		}
	}
}
