package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.GameMode;
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
import org.jetbrains.annotations.NotNull;

/**
 * Listener for generic world events.
 *
 * @author Jikoo
 */
public class WorldListener implements Listener {

  private final EnchantableBlocksPlugin plugin;
  private final EnchantableBlockManager manager;
  private final Map<Block, DropReplacement> pendingDrops = new HashMap<>();

  public WorldListener(final @NotNull EnchantableBlocksPlugin plugin) {
    this.plugin = plugin;
    this.manager = plugin.getBlockManager();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onChunkLoad(final @NotNull ChunkLoadEvent event) {
    plugin.getServer().getScheduler().runTask(plugin, () -> manager.loadChunkBlocks(event.getChunk()));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onChunkUnload(final @NotNull ChunkUnloadEvent event) {
    manager.unloadChunkBlocks(event.getChunk());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
    manager.createBlock(event.getBlock(), event.getItemInHand());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBlockBreak(final @NotNull BlockBreakEvent event) {
    ItemStack drop = manager.destroyBlock(event.getBlock());

    if (drop == null || !event.isDropItems() || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
      return;
    }

    Player player = event.getPlayer();

    ItemStack target = event.getBlock().getDrops(player.getInventory().getItemInMainHand()).stream()
        .findFirst().orElse(new ItemStack(event.getBlock().getType()));

    pendingDrops.put(event.getBlock(), new DropReplacement(target, drop));
    // Schedule a task with no delay to remove pending drop in case someone else modifies event
    plugin.getServer().getScheduler().runTask(plugin, () -> pendingDrops.remove(event.getBlock()));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBlockDropItem(BlockDropItemEvent event) {
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

  private static record DropReplacement(ItemStack target, ItemStack replacement) {}

}
