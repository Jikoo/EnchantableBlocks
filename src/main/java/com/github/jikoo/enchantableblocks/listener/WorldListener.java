package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.util.Pair;
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
	private final Map<Block, Pair<ItemStack, ItemStack>> pendingDrops = new HashMap<>();

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
		ItemStack drop = this.plugin.destroyEnchantableBlock(event.getBlock());

		if (drop == null || !event.isDropItems() || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
			return;
		}

		Player player = event.getPlayer();

		ItemStack replace = event.getBlock().getDrops(player.getInventory().getItemInMainHand()).stream()
				.findFirst().orElse(new ItemStack(event.getBlock().getType()));

		pendingDrops.put(event.getBlock(), new Pair<>(replace, drop));
		// Schedule a task with no delay to remove pending drop in case someone else modifies event
		plugin.getServer().getScheduler().runTask(plugin, () -> pendingDrops.remove(event.getBlock()));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockDropItem(BlockDropItemEvent event) {
		Block block = event.getBlock();
		Pair<ItemStack, ItemStack> dropChange = pendingDrops.remove(block);

		if (dropChange == null || dropChange.getRight().getType().isAir()) {
			return;
		}

		for (Iterator<Item> iterator = event.getItems().iterator(); iterator.hasNext();) {
			if (!iterator.next().getItemStack().equals(dropChange.getLeft())) {
				iterator.remove();
				break;
			}
		}

		block.getWorld().dropItem(block.getLocation().add(0.5, 0.1, 0.5), dropChange.getRight());
	}

}
