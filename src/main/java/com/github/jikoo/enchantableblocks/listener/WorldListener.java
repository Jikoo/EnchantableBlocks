package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener for generic world events.
 *
 * @author Jikoo
 */
public class WorldListener implements Listener {

	private final EnchantableBlocksPlugin plugin;

	public WorldListener(EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkLoad(final ChunkLoadEvent event) {
		plugin.getServer().getScheduler().runTask(plugin, () -> plugin.loadChunkEnchantableBlocks(event.getChunk()));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkUnload(final ChunkUnloadEvent event) {
		this.plugin.unloadChunkEnchantableBlocks(event.getChunk());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockPlace(final BlockPlaceEvent event) {
		this.plugin.createEnchantableBlock(event.getBlock(), event.getItemInHand());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockBreakEvent event) {
		ItemStack itemStack = this.plugin.destroyEnchantableBlock(event.getBlock());

		if (itemStack == null || !event.isDropItems()) {
			return;
		}

		event.setDropItems(false);

		// Schedule a task with no delay so block break completes before items are dropped - prevents weird ejections.
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.CREATIVE
				&& !event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand()).isEmpty()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), itemStack);
				}
			}.runTask(plugin);
		}
	}

}
