package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Listener for block loading, unloading, creation, and destruction.
 */
public class WorldListener implements Listener {

  private final Plugin plugin;
  private final EnchantableBlockManager manager;

  /**
   * Construct a new {@code WorldListener} to manage world events for
   * {@link com.github.jikoo.enchantableblocks.block.EnchantableBlock EnchantableBlocks}.
   *
   * @param plugin the owning {@link Plugin}
   * @param manager the {@link EnchantableBlockManager} managing blocks
   */
  public WorldListener(@NotNull Plugin plugin, @NotNull EnchantableBlockManager manager) {
    this.plugin = plugin;
    this.manager = manager;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  @VisibleForTesting
  void onChunkLoad(@NotNull ChunkLoadEvent event) {
    plugin.getServer().getScheduler().runTask(plugin, () -> manager.loadChunkBlocks(event.getChunk()));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  @VisibleForTesting
  void onChunkUnload(@NotNull ChunkUnloadEvent event) {
    manager.unloadChunkBlocks(event.getChunk());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  @VisibleForTesting
  void onBlockPlace(@NotNull BlockPlaceEvent event) {
    manager.createBlock(event.getBlock(), event.getItemInHand());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  @VisibleForTesting
  void onBlockBreak(@NotNull BlockBreakEvent event) {
    Block block = event.getBlock();
    ItemStack drop = manager.destroyBlock(block);

    if (drop == null || drop.getType() == Material.AIR
        || !event.isDropItems() || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
      return;
    }

    Player player = event.getPlayer();
    if (!block.isPreferredTool(player.getInventory().getItemInMainHand())) {
      return;
    }

    // Take over drop handling entirely. We'll fire our own BlockDropItemEvent.
    event.setDropItems(false);

    List<ItemStack> drops = new ArrayList<>();
    drops.add(drop);

    BlockState state = block.getState();
    if (state instanceof InventoryHolder holder) {
      // Add contents to drop list.
      for (ItemStack content : holder.getInventory().getContents()) {
        if (content != null && content.getType() != Material.AIR) {
          drops.add(content);
        }
      }

      // Clear inventory. This prevents Spigot/Paper inconsistencies - Spigot still drops inventory
      // contents if BlockBreakEvent#isDropItems is false.
      holder.getInventory().clear();
    }

    // Schedule a task with no delay to drop the items. We use a delay to prevent spawning items
    // inside a block (it should be air post-break)
    plugin.getServer().getScheduler().runTask(
        plugin,
        () -> doBlockDrops(block, state, player, drops));
  }

  private void doBlockDrops(
      @NotNull Block block,
      @NotNull BlockState state,
      @NotNull Player player,
      @NotNull List<ItemStack> drops) {
    List<Item> itemEntities = new ArrayList<>();
    World world = block.getWorld();
    // Add item entities to world.
    for (ItemStack itemStack : drops) {
      itemEntities.add(world.dropItem(block.getLocation().add(0.5, 0.1, 0.5), itemStack));
    }

    // Fire event.
    BlockDropItemEvent event = new BlockDropItemEvent(block, state, player, new ArrayList<>(itemEntities));
    plugin.getServer().getPluginManager().callEvent(event);

    // Remove any item entities removed by handling plugins.
    for (Item itemEntity : itemEntities) {
      if (!event.getItems().contains(itemEntity)) {
        itemEntity.remove();
      }
    }
  }

}
