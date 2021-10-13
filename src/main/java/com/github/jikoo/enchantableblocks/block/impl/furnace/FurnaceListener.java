package com.github.jikoo.enchantableblocks.block.impl.furnace;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Listener for furnace-specific events.
 */
class FurnaceListener implements Listener {

  private final Plugin plugin;
  private final EnchantableBlockManager manager;

  FurnaceListener(@NotNull Plugin plugin, @NotNull EnchantableBlockManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  @VisibleForTesting
  void onFurnaceBurn(final @NotNull FurnaceBurnEvent event) {
    var enchantableBlock = this.manager.getBlock(event.getBlock());

    if (!(enchantableBlock instanceof EnchantableFurnace enchantableFurnace)) {
      return;
    }

    if (enchantableFurnace.forceResume()) {
      event.setCancelled(true);
      return;
    }

    event.setBurnTime(enchantableFurnace.applyBurnTimeModifiers(event.getBurnTime()));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  @VisibleForTesting
  void onFurnaceStartSmelt(final @NotNull FurnaceStartSmeltEvent event) {
    var enchantableBlock = this.manager.getBlock(event.getBlock());

    if (!(enchantableBlock instanceof EnchantableFurnace enchantableFurnace)) {
      return;
    }

    event.setTotalCookTime(enchantableFurnace.applyCookTimeModifiers(event.getTotalCookTime()));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  @VisibleForTesting
  void onFurnaceSmelt(final @NotNull FurnaceSmeltEvent event) {
    var enchantableBlock = this.manager.getBlock(event.getBlock());

    if (!(enchantableBlock instanceof EnchantableFurnace enchantableFurnace)) {
      return;
    }

    Furnace furnace = enchantableFurnace.getFurnaceTile();

    if (furnace == null) {
      return;
    }

    int fortune = enchantableFurnace.getFortune();
    if (fortune > 0) {
      String world = furnace.getWorld().getName();
      EnchantableFurnaceConfig configuration = enchantableFurnace.getConfig();
      boolean listContains = configuration.fortuneList.get(world)
          .contains(event.getSource().getType());
      if (configuration.fortuneListIsBlacklist.get(world) != listContains) {
        applyFortune(event, fortune);
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

  private static void applyFortune(final @NotNull FurnaceSmeltEvent event, final int fortune) {
    applyFortune(event, () -> FurnaceListener.getFortuneResult(fortune));
  }

  @VisibleForTesting
  static void applyFortune(
      @NotNull FurnaceSmeltEvent event,
      @NotNull IntSupplier bonusCalculator) {
    ItemStack result = event.getResult();
    int tillFullStack = result.getType().getMaxStackSize() - result.getAmount();

    // Ignore results that are already full.
    if (tillFullStack == 0) {
      return;
    }

    // To prevent oversized stacks, restrict bonus to remainder for a max stack.
    int bonus = Math.min(tillFullStack, bonusCalculator.getAsInt());

    // Ignore bonus that shouldn't modify stack.
    if (bonus <= 0) {
      return;
    }

    result.setAmount(result.getAmount() + bonus);
    event.setResult(result);
  }

  @VisibleForTesting
  static int getFortuneResult(int maxBonus) {
    // Fortune result quantities are weighted - 0 bonus has 2 weight, any other number has 1 weight.
    // For simplicity, generate a number between -1 inclusive and fortune level + 1 exclusive.
    return ThreadLocalRandom.current().nextInt(-1, maxBonus + 1);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  @VisibleForTesting
  void onInventoryClick(final @NotNull InventoryClickEvent event) {
    if (event.getView().getTopInventory() instanceof FurnaceInventory furnaceInventory) {
      EnchantableFurnace.update(plugin, manager, furnaceInventory);
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  @VisibleForTesting
  void onInventoryMoveItem(final @NotNull InventoryMoveItemEvent event) {
    if (event.getDestination() instanceof FurnaceInventory furnaceInventory) {
      EnchantableFurnace.update(plugin, manager, furnaceInventory);
    } else if (event.getSource() instanceof FurnaceInventory furnaceInventory) {
      EnchantableFurnace.update(plugin, manager, furnaceInventory);
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  @VisibleForTesting
  void onInventoryDrag(final @NotNull InventoryDragEvent event) {
    if (event.getView().getTopInventory() instanceof FurnaceInventory furnaceInventory) {
      EnchantableFurnace.update(plugin, manager, furnaceInventory);
    }
  }

}
