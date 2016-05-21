package com.github.jikoo.enchantedfurnace;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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

	private final EnchantedFurnace plugin;

	public FurnaceListener(EnchantedFurnace plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onFurnaceConsumeFuel(FurnaceBurnEvent event) {
		Furnace furnace = plugin.getFurnace(event.getBlock());
		if (furnace == null) {
			return;
		}
		if (furnace.isPaused() && furnace.resume()) {
			event.setCancelled(true);
			return;
		}
		if (furnace.getBurnModifier() > 0) {
			// + 1/5 fuel burn length per level unbreaking
			int burnTime = (int) ((1 + .2 * furnace.getBurnModifier()) * event.getBurnTime());
			// Burn time is actually a short internally. Capping it here prevents some wonky behavior
			if (burnTime > Short.MAX_VALUE) {
				burnTime = Short.MAX_VALUE;
			}
			event.setBurnTime(burnTime);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onItemSmelt(FurnaceSmeltEvent event) {
		final Furnace furnace = plugin.getFurnace(event.getBlock());
		if (furnace == null) {
			return;
		}

		if (furnace.getFortune() > 0) {
			boolean listContains = plugin.getFortuneList().contains(event.getSource().getType().name());
			if (plugin.isBlacklist() ? !listContains : listContains) {
				applyFortune(event, furnace);
			}
		}

		if (furnace.shouldPause(event)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					furnace.pause();
				}
			}.runTask(plugin);
		} else if (ReflectionUtils.areFurnacesSupported()) {
			final int cookModifier = furnace.getCookModifier();
			if (cookModifier > 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						ReflectionUtils.setFurnaceCookTime(furnace.getBlock(), 400 / (cookModifier + 2));
					}
				}.runTask(plugin);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void applyFortune(FurnaceSmeltEvent event, Furnace furnace) {
		FurnaceInventory inventory = furnace.getFurnaceTile().getInventory();
		// Fortune result quantities are weighted - 0 bonus has 2 weight, any other number has 1 weight
		// To easily recreate this, a random number between -1 inclusive and fortune level exclusive is generated.
		int bonus = ThreadLocalRandom.current().nextInt(furnace.getFortune() + 2) - 1;
		if (bonus <= 0) {
			return;
		}
		// Check extras against max - 1 because of guaranteed single output
		if (inventory.getResult() != null && inventory.getResult().getAmount() + bonus > inventory.getResult().getType().getMaxStackSize() - 1) {
			bonus = inventory.getResult().getType().getMaxStackSize() - 1 - inventory.getResult().getAmount();
			if (bonus <= 0) {
				return;
			}
		}
		ItemStack newResult = null;
		if (inventory.getResult() == null) {
			Iterator<Recipe> iterator = Bukkit.recipeIterator();
			while (iterator.hasNext()) {
				Recipe recipe = iterator.next();
				if (!(recipe instanceof FurnaceRecipe)) {
					continue;
				}
				ItemStack input = ((FurnaceRecipe) recipe).getInput();
				if (input.getType() != inventory.getSmelting().getType()) {
					continue;
				}
				if (input.getData().getData() == -1) {
					// Inexact match, continue iterating
					newResult = new ItemStack(recipe.getResult());
				}
				if (input.getData().equals(inventory.getSmelting().getData())) {
					// Exact match
					newResult = new ItemStack(recipe.getResult());
					break;
				}
				// Incorrect data, not a match
				continue;
			}
			if (newResult == null) {
				plugin.getLogger().warning("Unable to obtain fortune result for MaterialData "
						+ inventory.getSmelting().getData() + ". Please report this error.");
				return;
			}
		} else {
			newResult = inventory.getResult().clone();
		}
		newResult.setAmount(1 + bonus);
		event.setResult(newResult);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getItemInHand() != null && event.getItemInHand().getType() == Material.FURNACE) {
			plugin.createFurnace(event.getBlock(), event.getItemInHand());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() != Material.FURNACE && event.getBlock().getType() != Material.BURNING_FURNACE) {
			return;
		}
		ItemStack is = plugin.destroyFurnace(event.getBlock());
		if (is != null) {
			Player player = event.getPlayer();
			if (player.getGameMode() != GameMode.CREATIVE
					&& !event.getBlock().getDrops(player.getItemInHand()).isEmpty()) {
				event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
			}
			event.setCancelled(true);
			event.getBlock().setType(Material.AIR);
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
		final Furnace furnace = plugin.getFurnace(((org.bukkit.block.Furnace) inventory.getHolder()).getBlock());
		if (furnace == null) {
			return;
		}
		final int cookModifier = furnace.getCookModifier();
		if ((!ReflectionUtils.areFurnacesSupported() || cookModifier < 1) && !furnace.canPause()) {
			return;
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				if (ReflectionUtils.areFurnacesSupported() && cookModifier > 0) {
					ReflectionUtils.setFurnaceCookTime(furnace.getBlock(), 400 / (cookModifier + 2));
				}
				if (furnace.isPaused()) {
					furnace.resume();
				} else if (furnace.shouldPause(null)) {
					furnace.pause();
				}
			}
		}.runTask(plugin);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event) {
		plugin.loadChunkFurnaces(event.getChunk());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		plugin.unloadChunkFurnaces(event.getChunk());
	}
}
