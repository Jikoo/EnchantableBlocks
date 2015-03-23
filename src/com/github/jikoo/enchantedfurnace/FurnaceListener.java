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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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
	public void onFurnaceConsumeFuel(FurnaceBurnEvent event) {
		Furnace f = EnchantedFurnace.getInstance().getFurnace(event.getBlock());
		if (f == null) {
			return;
		}
		if (f.isPaused() && f.resume()) {
			event.setCancelled(true);
			return;
		}
		if (f.getBurnModifier() > 0) {
			// + 1/5 fuel burn length per level unbreaking
			int burnTime = (int) ((1 + .2 * f.getBurnModifier()) * event.getBurnTime());
			// Burn time is actually a short internally. Capping it here prevents some wonky behavior
			if (burnTime > Short.MAX_VALUE) {
				burnTime = Short.MAX_VALUE;
			}
			event.setBurnTime(burnTime);
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

		if (f.shouldPause(e)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					f.pause();
				}
			}.runTask(EnchantedFurnace.getInstance());
		}
	}

	@SuppressWarnings("deprecation")
	private void applyFortune(FurnaceSmeltEvent e, Furnace f) {
		FurnaceInventory i = f.getFurnaceTile().getInventory();
		// Fortune result quantities are weighted - 0 bonus has 2 weight, any other number has 1 weight
		// To easily recreate this, a random number between -1 inclusive and fortune level exclusive is generated.
		int bonus = (int) (Math.random() * (f.getFortune() + 2)) - 1;
		// Check extras against max - 1 because of guaranteed single output
		if (i.getResult() != null && i.getResult().getAmount() + bonus > i.getResult().getType().getMaxStackSize() - 1) {
			bonus = i.getResult().getType().getMaxStackSize() - 1 - i.getResult().getAmount();
		}
		if (bonus <= 0) {
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
		newResult.setAmount(1 + bonus);
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
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getView().getTopInventory().getType() != InventoryType.FURNACE) {
			return;
		}
		furnaceContentsChanged(event.getView().getTopInventory());
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
		furnaceContentsChanged(furnace);
	}

	private void furnaceContentsChanged(Inventory inventory) {
		if (!(inventory.getHolder() instanceof org.bukkit.block.Furnace)) {
			return;
		}
		final Furnace f = EnchantedFurnace.getInstance().getFurnace(((org.bukkit.block.Furnace) inventory.getHolder()).getBlock());
		if (f == null || !f.canPause()) {
			return;
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				if (f.isPaused()) {
					f.resume();
				} else {
					if (f.shouldPause(null)) {
						f.pause();
					}
				}
			}
		}.runTask(EnchantedFurnace.getInstance());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event) {
		EnchantedFurnace.getInstance().loadChunkFurnaces(event.getChunk());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		EnchantedFurnace.getInstance().unloadChunkFurnaces(event.getChunk());
	}
}
