package com.github.jikoo.enchantableblocks.listener;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import com.github.jikoo.enchantableblocks.EnchantableBlock;
import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener for furnace-specific events.
 *
 * @author Jikoo
 */
public class FurnaceListener implements Listener {

	private final EnchantableBlocksPlugin plugin;

	public FurnaceListener(final EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onFurnaceConsumeFuel(final FurnaceBurnEvent event) {
		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(event.getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace)) {
			return;
		}

		EnchantableFurnace enchantableFurnace = (EnchantableFurnace) enchantableBlock;

		if (enchantableFurnace.isPaused() && enchantableFurnace.resume()) {
			event.setCancelled(true);
			return;
		}

		// Unbreaking causes furnace to burn for longer, increase burn time
		int burnTime = this.getCappedTicks(event.getBurnTime(), -enchantableFurnace.getBurnModifier(), 0.2);

		// Efficiency causes furnace to burn faster, reduce burn time to match smelt rate increase
		burnTime = this.getCappedTicks(burnTime, enchantableFurnace.getCookModifier(), 0.5);

		event.setBurnTime(burnTime);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onFurnaceSmelt(final FurnaceSmeltEvent event) {
		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(event.getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace)) {
			return;
		}

		EnchantableFurnace enchantableFurnace = (EnchantableFurnace) enchantableBlock;

		if (enchantableFurnace.getFortune() > 0) {
			boolean listContains = this.plugin.getFortuneList().contains(event.getSource().getType().name());
			if (this.plugin.isBlacklist() != listContains) {
				this.applyFortune(event, enchantableFurnace);
			}
		}

		if (enchantableFurnace.shouldPause(event)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					enchantableFurnace.pause();
				}
			}.runTask(this.plugin);
		} else if (ReflectionUtil.areFurnacesSupported()) {
			final int cookModifier = enchantableFurnace.getCookModifier();
			if (cookModifier != 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						BlockState state = event.getBlock().getState();
						if (!(state instanceof org.bukkit.block.Furnace)) {
							return;
						}
						org.bukkit.block.Furnace tile = (org.bukkit.block.Furnace) state;
						// PaperSpigot compatibility: lag compensation patch can set furnaces to negative cook time.
						if (tile.getCookTime() < 0) {
							tile.setCookTime((short) 0);
							tile.update();
						}
						ReflectionUtil.setFurnaceCookTime(enchantableFurnace.getBlock(), FurnaceListener.this.getCappedTicks(200, cookModifier, 0.5));
					}
				}.runTask(this.plugin);
			}
		}
	}

	private int getCappedTicks(final int baseTicks, final int baseModifier, final double fractionModifier) {
		return Math.max(1, Math.min(Short.MAX_VALUE, this.getModifiedTicks(baseTicks, baseModifier, fractionModifier)));
	}

	private int getModifiedTicks(final int baseTicks, final int baseModifier, final double fractionModifier) {
		if (baseModifier == 0) {
			return baseTicks;
		}
		if (baseModifier > 0) {
			return (int) (baseTicks / (1 + baseModifier * fractionModifier));
		}
		return (int) (baseTicks * (1 + -baseModifier * fractionModifier));
	}

	@SuppressWarnings("deprecation")
	private void applyFortune(final FurnaceSmeltEvent event, final EnchantableFurnace enchantableFurnace) {
		FurnaceInventory inventory = enchantableFurnace.getFurnaceTile().getInventory();
		// Fortune result quantities are weighted - 0 bonus has 2 weight, any other number has 1 weight
		// To easily recreate this, a random number between -1 inclusive and fortune level exclusive is generated.
		int bonus = ThreadLocalRandom.current().nextInt(enchantableFurnace.getFortune() + 2) - 1;
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
			}
			if (newResult == null) {
				this.plugin.getLogger().warning("Unable to obtain fortune result for MaterialData "
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
	public void onInventoryClick(final InventoryClickEvent event) {
		if (event.getView().getTopInventory().getType() != InventoryType.FURNACE) {
			return;
		}
		this.furnaceContentsChanged(event.getView().getTopInventory());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryMoveItem(final InventoryMoveItemEvent e) {
		Inventory furnace;
		if (e.getDestination().getType() == InventoryType.FURNACE) {
			furnace = e.getDestination();
		} else if (e.getSource().getType() == InventoryType.FURNACE) {
			furnace = e.getSource();
		} else {
			return;
		}
		this.furnaceContentsChanged(furnace);
	}

	private void furnaceContentsChanged(final Inventory inventory) {
		if (!(inventory.getHolder() instanceof org.bukkit.block.Furnace)) {
			return;
		}

		final org.bukkit.block.Furnace tile = (org.bukkit.block.Furnace) inventory.getHolder();
		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(tile.getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace)) {
			return;
		}

		final EnchantableFurnace enchantableFurnace = (EnchantableFurnace) enchantableBlock;
		final int cookModifier = enchantableFurnace.getCookModifier();

		if ((!ReflectionUtil.areFurnacesSupported() || cookModifier != 0) && !enchantableFurnace.canPause()) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (ReflectionUtil.areFurnacesSupported() && cookModifier != 0) {
					// PaperSpigot compatibility: lag compensation patch can set furnaces to negative cook time.
					if (tile.getCookTime() < 0) {
						tile.setCookTime((short) 0);
						tile.update();
					}
					ReflectionUtil.setFurnaceCookTime(enchantableFurnace.getBlock(), FurnaceListener.this.getCappedTicks(200, cookModifier, 0.5));
				}
				if (enchantableFurnace.isPaused()) {
					enchantableFurnace.resume();
				} else if (enchantableFurnace.shouldPause(null)) {
					enchantableFurnace.pause();
				}
			}
		}.runTask(this.plugin);
	}

}
