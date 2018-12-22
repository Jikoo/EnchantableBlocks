package com.github.jikoo.enchantableblocks.listener;

import java.util.concurrent.ThreadLocalRandom;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.util.CompatibilityUtil;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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

		BlockState state = event.getBlock().getState();
		if (!(state instanceof Furnace)) {
			return;
		}

		FurnaceRecipe recipe = CompatibilityUtil.getFurnaceRecipe(((Furnace) state).getInventory());

		if (enchantableFurnace.getFortune() > 0) {
			boolean listContains = this.plugin.getFortuneList().contains(event.getSource().getType().name());
			if (this.plugin.isBlacklist() != listContains) {
				this.applyFortune(event, enchantableFurnace, recipe);
			}
		}

		if (enchantableFurnace.shouldPause(event, recipe)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					enchantableFurnace.pause();
				}
			}.runTask(this.plugin);
		} else if (CompatibilityUtil.areFurnacesSupported()) {
			final int cookModifier = enchantableFurnace.getCookModifier();
			if (cookModifier != 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						BlockState state = event.getBlock().getState();
						if (!(state instanceof Furnace)) {
							return;
						}
						CompatibilityUtil.setFurnaceCookTime(enchantableFurnace.getBlock(), FurnaceListener.this.getCappedTicks(200, cookModifier, 0.5));
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
	private void applyFortune(final FurnaceSmeltEvent event, final EnchantableFurnace enchantableFurnace,
			  final FurnaceRecipe recipe) {
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
		ItemStack newResult = inventory.getResult() == null ? recipe.getResult() : inventory.getResult().clone();
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

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryDrag(final InventoryDragEvent event) {
		if (event.getView().getTopInventory().getType() != InventoryType.FURNACE) {
			return;
		}
		this.furnaceContentsChanged(event.getView().getTopInventory());
	}

	private void furnaceContentsChanged(final Inventory inventory) {
		if (!(inventory.getHolder() instanceof Furnace)) {
			return;
		}

		final Furnace tile = (Furnace) inventory.getHolder();
		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(tile.getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace)) {
			return;
		}

		final EnchantableFurnace enchantableFurnace = (EnchantableFurnace) enchantableBlock;
		final int cookModifier = enchantableFurnace.getCookModifier();

		if ((!CompatibilityUtil.areFurnacesSupported() || cookModifier != 0) && !enchantableFurnace.canPause()) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (CompatibilityUtil.areFurnacesSupported() && cookModifier != 0) {
					CompatibilityUtil.setFurnaceCookTime(enchantableFurnace.getBlock(), FurnaceListener.this.getCappedTicks(200, cookModifier, 0.5));
				}
				if (enchantableFurnace.isPaused()) {
					enchantableFurnace.resume();
				} else if (enchantableFurnace.shouldPause(null, null)) {
					enchantableFurnace.pause();
				}
			}
		}.runTask(this.plugin);
	}

}
