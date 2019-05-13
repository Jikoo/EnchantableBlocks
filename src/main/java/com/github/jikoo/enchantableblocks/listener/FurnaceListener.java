package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

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

		final int cookModifier = enchantableFurnace.getCookModifier();
		if (cookModifier != 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Furnace furnace = enchantableFurnace.getFurnaceTile();
					if (furnace == null) {
						return;
					}
					CookingRecipe recipe = EnchantableFurnace.getFurnaceRecipe(furnace.getInventory());
					if (recipe != null) {
						enchantableFurnace.setCookTimeTotal(recipe);
					}
				}
			}.runTask(this.plugin);
		}

		if (enchantableFurnace.isPaused() && enchantableFurnace.resume()) {
			event.setCancelled(true);
			return;
		}

		event.setBurnTime(enchantableFurnace.applyBurnTimeModifiers(event.getBurnTime()));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onFurnaceSmelt(final FurnaceSmeltEvent event) {
		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(event.getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace)) {
			return;
		}

		EnchantableFurnace enchantableFurnace = (EnchantableFurnace) enchantableBlock;
		Furnace furnace = enchantableFurnace.getFurnaceTile();

		if (furnace == null) {
			return;
		}

		CookingRecipe recipe = EnchantableFurnace.getFurnaceRecipe(furnace.getInventory());

		if (enchantableFurnace.shouldPause(event, recipe)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					enchantableFurnace.pause();
				}
			}.runTask(this.plugin);
		}

		if (recipe == null) {
			return;
		}

		if (enchantableFurnace.getFortune() > 0) {
			boolean listContains = this.plugin.getFortuneList().contains(event.getSource().getType().name());
			if (this.plugin.isBlacklist() != listContains) {
				this.applyFortune(event, enchantableFurnace, recipe);
			}
		}

		final int cookModifier = enchantableFurnace.getCookModifier();
		if (cookModifier != 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					enchantableFurnace.setCookTimeTotal(recipe);
				}
			}.runTask(this.plugin);
		}
	}

	private void applyFortune(final FurnaceSmeltEvent event, final EnchantableFurnace enchantableFurnace,
			  final CookingRecipe recipe) {
		Furnace furnace = enchantableFurnace.getFurnaceTile();
		if (furnace == null) {
			return;
		}
		FurnaceInventory inventory = furnace.getInventory();
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
		ItemStack newResult = inventory.getResult() == null ? recipe.getResult() : inventory.getResult().clone();
		newResult.setAmount(1 + bonus);
		event.setResult(newResult);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryClick(final InventoryClickEvent event) {
		if (event.getView().getTopInventory() instanceof FurnaceInventory) {
			this.furnaceContentsChanged((FurnaceInventory) event.getView().getTopInventory());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
		if (event.getDestination() instanceof FurnaceInventory) {
			this.furnaceContentsChanged((FurnaceInventory) event.getDestination());
		} else if (event.getSource() instanceof FurnaceInventory) {
			this.furnaceContentsChanged((FurnaceInventory) event.getSource());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryDrag(final InventoryDragEvent event) {
		if (event.getView().getTopInventory() instanceof FurnaceInventory) {
			this.furnaceContentsChanged((FurnaceInventory) event.getView().getTopInventory());
		}
	}

	private void furnaceContentsChanged(final FurnaceInventory inventory) {
		if (inventory.getHolder() == null) {
			return;
		}

		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(inventory.getHolder().getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace)) {
			return;
		}

		final EnchantableFurnace enchantableFurnace = (EnchantableFurnace) enchantableBlock;
		final int cookModifier = enchantableFurnace.getCookModifier();

		if (cookModifier != 0 && !enchantableFurnace.canPause()) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				CookingRecipe recipe = EnchantableFurnace.getFurnaceRecipe(inventory);
				if (cookModifier != 0 && recipe != null) {
					enchantableFurnace.setCookTimeTotal(recipe);
				}
				if (enchantableFurnace.isPaused()) {
					enchantableFurnace.resume();
				} else if (enchantableFurnace.shouldPause(null, recipe)) {
					enchantableFurnace.pause();
				}
			}
		}.runTask(this.plugin);
	}

}
