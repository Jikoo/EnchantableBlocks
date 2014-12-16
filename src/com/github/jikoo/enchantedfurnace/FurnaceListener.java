package com.github.jikoo.enchantedfurnace;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener for all furnace-related events.
 * 
 * @author Jikoo
 */
public class FurnaceListener implements Listener {
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onFurnaceConsumeFuel(FurnaceBurnEvent e) {
		Furnace f = EnchantedFurnace.getInstance().getFurnace(e.getBlock());
		if (f == null) {
			return;
		}
		if (f.isPaused() && f.resume()) {
			e.setCancelled(true);
			return;
		}
		if (f.getBurnModifier() > 0) {
			// + 1/5 fuel burn length per level unbreaking
			e.setBurnTime((int) ((1 + .2 * f.getBurnModifier()) * e.getBurnTime()));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onItemSmelt(FurnaceSmeltEvent e) {
		final Furnace f = EnchantedFurnace.getInstance().getFurnace(e.getBlock());
		if (f == null) {
			return;
		}

		if (f.getFortune() > 0) {
			boolean listContains = EnchantedFurnace.getInstance().getFortuneList().contains(e.getSource().getType().name());
			if (EnchantedFurnace.getInstance().isBlacklist() ? !listContains : listContains) {
				applyFortune(e, f);
			}
		}

		if (f.canPause()) {
			new BukkitRunnable() {
				public void run() {
					f.pause();
				}
			}.runTask(EnchantedFurnace.getInstance());
		}
	}

	@SuppressWarnings("deprecation")
	private void applyFortune(FurnaceSmeltEvent e, Furnace f) {
		FurnaceInventory i = f.getFurnaceTile().getInventory();
		int extraResults = 1;
		// Fortune 1 is 30% chance of product, 2 is 25%, and 3+ is 20% for vanilla picks.
		double fortuneChance = f.getFortune() < 2 ? .33 : f.getFortune() == 2 ? .25 : .2;
		for (int j = 0; j < f.getFortune(); j++) {
			if (Math.random() < fortuneChance) {
				extraResults++;
			}
		}
		// There's always going to be 1 item created, extra output is 1 less than total.
		extraResults--;
		// Check extras against max - 1 because of guaranteed single output
		if (i.getResult() != null && i.getResult().getAmount() + extraResults > i.getResult().getType().getMaxStackSize() - 1) {
			extraResults = i.getResult().getType().getMaxStackSize() - 1 - i.getResult().getAmount();
		}
		if (extraResults <= 0) {
			return;
		}
		ItemStack newResult = null;
		if (i.getResult() == null) {
			Iterator<Recipe> ri = Bukkit.recipeIterator();
			while (ri.hasNext()) {
				Recipe r = ri.next();
				if (!(r instanceof FurnaceRecipe)) {
					continue;
				}
				ItemStack input = ((FurnaceRecipe) r).getInput();
				if (input.getType() != i.getSmelting().getType()) {
					continue;
				}
				if (input.getData().getData() > -1) {
					if (input.getData().equals(i.getSmelting().getData())) {
						// Exact match
						newResult = new ItemStack(r.getResult());
						break;
					}
					// Incorrect data, not a match
				} else {
					// Inexact match, continue iterating
					newResult = new ItemStack(r.getResult());
				}
			}
			if (newResult == null) {
				EnchantedFurnace.getInstance().getLogger().warning("Unable to obtain fortune result for MaterialData "
						+ i.getSmelting().getData() + ". Please report this error.");
				return;
			}
		} else {
			newResult = i.getResult().clone();
		}
		newResult.setAmount(1 + extraResults);
		e.setResult(newResult);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent e) {
		if (e.getItemInHand() != null && e.getItemInHand().getType() == Material.FURNACE) {
			EnchantedFurnace.getInstance().createFurnace(e.getBlock(), e.getItemInHand());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getBlock().getType() != Material.FURNACE && e.getBlock().getType() != Material.BURNING_FURNACE) {
			return;
		}
		ItemStack is = EnchantedFurnace.getInstance().destroyFurnace(e.getBlock());
		if (is != null) {
			if (e.getPlayer().getGameMode() != GameMode.CREATIVE
					&& !e.getBlock().getDrops(e.getPlayer().getItemInHand()).isEmpty()) {
				e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), is);
			}
			e.setCancelled(true);
			e.getBlock().setType(Material.AIR);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryMoveItem(InventoryMoveItemEvent e) {
		Inventory furnace;
		if (e.getDestination().getType() == InventoryType.FURNACE) {
			furnace = e.getDestination();
		} else if (e.getSource().getType() == InventoryType.FURNACE) {
			furnace = e.getSource();
		} else {
			return;
		}
		final Furnace f = EnchantedFurnace.getInstance().getFurnace(((org.bukkit.block.Furnace) furnace.getHolder()).getBlock());
		if (f == null || !f.canPause()) {
			return;
		}
		if (f.isPaused()) {
			new BukkitRunnable() {
				public void run() {
					f.resume();
				}
			}.runTask(EnchantedFurnace.getInstance());
		} else {
			new BukkitRunnable() {
				public void run() {
					f.pause();
				}
			}.runTask(EnchantedFurnace.getInstance());
		}
	}
}
