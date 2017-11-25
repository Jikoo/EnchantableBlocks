package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.util.ReflectionUtil;
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
		ItemStack itemStack = ReflectionUtil.getItemInHand(event);

		if (itemStack != null) {
			this.plugin.createEnchantableBlock(event.getBlock(), itemStack);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockBreakEvent event) {
		ItemStack is = this.plugin.destroyEnchantableBlock(event.getBlock());

		if (is == null) {
			return;
		}

		// TODO: 1.12+ rewrite
		event.setCancelled(true);
		event.getBlock().setType(Material.AIR);
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.CREATIVE
				&& !event.getBlock().getDrops(ReflectionUtil.getItemInHand(event)).isEmpty()) {
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
		}
	}

}
