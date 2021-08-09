package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for furnace-specific events.
 *
 * @author Jikoo
 */
public class FurnaceListener implements Listener {

	private final EnchantableBlocksPlugin plugin;

	public FurnaceListener(final @NotNull EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onFurnaceConsumeFuel(final @NotNull FurnaceBurnEvent event) {
		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(event.getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace enchantableFurnace)) {
			return;
		}

		if (enchantableFurnace.isPaused() && enchantableFurnace.resume()) {
			event.setCancelled(true);
			return;
		}

		event.setBurnTime(enchantableFurnace.applyBurnTimeModifiers(event.getBurnTime()));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onFurnaceStartSmelt(final @NotNull FurnaceStartSmeltEvent event) {
		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(event.getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace enchantableFurnace)) {
			return;
		}

		event.setTotalCookTime(enchantableFurnace.applyCookTimeModifiers(event.getTotalCookTime()));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onFurnaceSmelt(final @NotNull FurnaceSmeltEvent event) {
		EnchantableBlock enchantableBlock = this.plugin.getEnchantableBlockByBlock(event.getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace enchantableFurnace)) {
			return;
		}

		Furnace furnace = enchantableFurnace.getFurnaceTile();

		if (furnace == null) {
			return;
		}

		if (enchantableFurnace.getFortune() > 0) {
			String world = furnace.getWorld().getName();
			boolean listContains = EnchantableFurnace.getConfig().fortuneList.get(world).contains(event.getSource().getType());
			if (EnchantableFurnace.getConfig().fortuneListIsBlacklist.get(world) != listContains) {
				this.applyFortune(event, enchantableFurnace);
			}
		}

		if (!enchantableFurnace.canPause()) {
			return;
		}

		if (enchantableFurnace.shouldPause(event)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					enchantableFurnace.pause();
				}
			}.runTask(this.plugin);
		}
	}

	private void applyFortune(final @NotNull FurnaceSmeltEvent event,
			final @NotNull EnchantableFurnace enchantableFurnace) {
		ItemStack result = event.getResult();

		// Fortune result quantities are weighted - 0 bonus has 2 weight, any other number has 1 weight
		// To easily recreate this, a random number between -1 inclusive and fortune level exclusive is generated.
		int bonus = ThreadLocalRandom.current().nextInt(enchantableFurnace.getFortune() + 2) - 1;

		// To prevent oversized stacks, restrict bonus to remainder for a max stack.
		bonus = Math.min(result.getType().getMaxStackSize() - result.getAmount(), bonus);

		if (bonus <= 0) {
			return;
		}

		result.setAmount(result.getAmount() + bonus);
		event.setResult(result);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	private void onInventoryClick(final @NotNull InventoryClickEvent event) {
		if (event.getView().getTopInventory() instanceof FurnaceInventory) {
			EnchantableFurnace.update(plugin, (FurnaceInventory) event.getView().getTopInventory());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	private void onInventoryMoveItem(final @NotNull InventoryMoveItemEvent event) {
		if (event.getDestination() instanceof FurnaceInventory) {
			EnchantableFurnace.update(plugin, (FurnaceInventory) event.getDestination());
		} else if (event.getSource() instanceof FurnaceInventory) {
			EnchantableFurnace.update(plugin, (FurnaceInventory) event.getSource());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	private void onInventoryDrag(final @NotNull InventoryDragEvent event) {
		if (event.getView().getTopInventory() instanceof FurnaceInventory) {
			EnchantableFurnace.update(plugin, (FurnaceInventory) event.getView().getTopInventory());
		}
	}

}
