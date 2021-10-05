package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Listener for generic world events.
 *
 * @author Jikoo
 */
public class WorldListener implements Listener {

  private final Plugin plugin;
  private final EnchantableBlockManager manager;
  @VisibleForTesting
  final Map<Block, DropReplacement> pendingDrops = new HashMap<>();

  public WorldListener(@NotNull Plugin plugin, @NotNull EnchantableBlockManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  @VisibleForTesting
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  void onChunkLoad(@NotNull ChunkLoadEvent event) {
    plugin.getServer().getScheduler().runTask(plugin, () -> manager.loadChunkBlocks(event.getChunk()));
  }

  @VisibleForTesting
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  void onChunkUnload(@NotNull ChunkUnloadEvent event) {
    manager.unloadChunkBlocks(event.getChunk());
  }

  @VisibleForTesting
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  void onBlockPlace(@NotNull BlockPlaceEvent event) {
    manager.createBlock(event.getBlock(), event.getItemInHand());
  }

  @VisibleForTesting
  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  void onBlockBreak(@NotNull BlockBreakEvent event) {
    ItemStack drop = manager.destroyBlock(event.getBlock());

    if (drop == null || drop.getType() == Material.AIR
        || !event.isDropItems() || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
      return;
    }

    Player player = event.getPlayer();

    ItemStack target = event.getBlock().getDrops(player.getInventory().getItemInMainHand()).stream()
        .findFirst().orElse(new ItemStack(event.getBlock().getType()));

    pendingDrops.put(event.getBlock(), new DropReplacement(target, drop));
    // Schedule a task with no delay to remove pending drop in case someone else modifies event
    plugin.getServer().getScheduler().runTask(plugin, () -> pendingDrops.remove(event.getBlock()));
  }

  @VisibleForTesting
  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  void onBlockDropItem(@NotNull BlockDropItemEvent event) {
    Block block = event.getBlock();
    DropReplacement dropChange = pendingDrops.remove(block);

    if (dropChange == null || dropChange.target().getType().isAir()) {
      return;
    }

    for (Iterator<Item> iterator = event.getItems().iterator(); iterator.hasNext();) {
      if (iterator.next().getItemStack().equals(dropChange.target())) {
        iterator.remove();
        break;
      }
    }

    block.getWorld().dropItem(block.getLocation().add(0.5, 0.1, 0.5), dropChange.replacement());
  }

  @VisibleForTesting
  static record DropReplacement(ItemStack target, ItemStack replacement) {}

}
