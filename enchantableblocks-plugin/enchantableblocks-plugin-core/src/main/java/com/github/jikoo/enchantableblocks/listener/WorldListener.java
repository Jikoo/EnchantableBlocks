package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.util.CompatibilityUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for generic world events.
 *
 * @author Jikoo
 */
public class WorldListener implements Listener {

	private final EnchantableBlocksPlugin plugin;

	public WorldListener(EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
		// TODO: Enable version-specific  block listener
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkLoad(final ChunkLoadEvent event) {
		this.plugin.loadChunkEnchantableBlocks(event.getChunk());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChunkUnload(final ChunkUnloadEvent event) {
		this.plugin.unloadChunkEnchantableBlocks(event.getChunk());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockPlace(final BlockPlaceEvent event) {
		this.plugin.createEnchantableBlock(event.getBlock(), event.getItemInHand());
	}

}
