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
import org.jetbrains.annotations.NotNull;

/**
 * Listener for generic world events.
 *
 * @author Jikoo
 */
public class WorldListener implements Listener {

	private final EnchantableBlocksPlugin plugin;

	public WorldListener(final @NotNull EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkLoad(final @NotNull ChunkLoadEvent event) {
		plugin.getServer().getScheduler().runTask(plugin, () -> plugin.loadChunkEnchantableBlocks(event.getChunk()));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkUnload(final @NotNull ChunkUnloadEvent event) {
		this.plugin.unloadChunkEnchantableBlocks(event.getChunk());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
		this.plugin.createEnchantableBlock(event.getBlock(), event.getItemInHand());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(final @NotNull BlockBreakEvent event) {
		ItemStack itemStack = this.plugin.destroyEnchantableBlock(event.getBlock());

		if (itemStack == null || !event.isDropItems()) {
			return;
		}

		event.setDropItems(false);

		// Schedule a task with no delay so block break completes before items are dropped - prevents weird ejections.
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.CREATIVE
				&& !event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand()).isEmpty()) {
			plugin.getServer().getScheduler().runTask(plugin,
					() -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), itemStack));
		}
	}

}
