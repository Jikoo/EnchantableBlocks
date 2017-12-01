package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * WorldListener implementing methods from 1.9 through 1.11.
 *
 * @author Jikoo
 */
public class BlockListener1_11 implements Listener {

	private final EnchantableBlocksPlugin plugin;

	public BlockListener1_11(EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockBreakEvent event) {
		ItemStack itemStack = this.plugin.destroyEnchantableBlock(event.getBlock());

		if (itemStack == null) {
			return;
		}

		event.setCancelled(true);
		event.getBlock().setType(Material.AIR);

		// Schedule a task with no delay so block removal completes before items are dropped - prevents weird ejections.
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
