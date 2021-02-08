package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantOperation;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantingTableUtil;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Listener for handling enchanting in an enchantment table.
 *
 * @author Jikoo
 */
public class TableEnchanter implements Listener {

	private final EnchantableBlocksPlugin plugin;
	private boolean needOwnSeed = false;
	private final NamespacedKey randomSeed;

	public TableEnchanter(EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
		this.randomSeed = new NamespacedKey(plugin, "enchant_seed");
	}

	@EventHandler
	public void onPrepareItemEnchant(final @NotNull PrepareItemEnchantEvent event) {

		if (event.getItem().getEnchantments().size() > 0
				|| event.getItem().getAmount() != 1
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		Class<? extends EnchantableBlock> blockClass = plugin.getRegistry().get(event.getItem().getType());
		if (blockClass == null) {
			return;
		}

		String world = event.getEnchanter().getWorld().getName();
		EnchantableBlockConfig config = plugin.getRegistry().getConfig(blockClass);
		Collection<Enchantment> enchants = new ArrayList<>(plugin.getRegistry().getEnchants(blockClass));
		Set<Enchantment> blacklist = config.tableDisabledEnchants.get(world);
		enchants.removeAll(blacklist);

		if (enchants.isEmpty()) {
			return;
		}

		// Not normally enchantable, event must be un-cancelled.
		event.setCancelled(false);

		// Assemble enchantment calculation details.
		EnchantOperation operation = new EnchantOperation(enchants);

		Multimap<Enchantment, Enchantment> enchantConflicts = config.tableEnchantmentConflicts.get(world);
		operation.setIncompatibility((enchantment, enchantment2) ->
				enchantConflicts.get(enchantment).contains(enchantment2)
						|| enchantConflicts.get(enchantment2).contains(enchantment));
		operation.setEnchantability(config.tableEnchantability.get(world));

		// Calculate levels offered for bookshelf count.
		int[] buttonLevels = EnchantingTableUtil.getButtonLevels(event.getEnchantmentBonus(),
				getEnchantmentSeed(event.getEnchanter()));
		for (int buttonNumber = 0; buttonNumber < 3; ++buttonNumber) {
			event.getOffers()[buttonNumber] = getOffer(operation, event.getEnchanter(), buttonNumber, buttonLevels[buttonNumber]);
		}

		// Force button refresh.
		EnchantingTableUtil.updateButtons(plugin, event.getEnchanter(), event.getOffers());
	}

	/**
	 * Get an offer of the first enchantment that will be rolled at the specified level for the player.
	 *
	 * @param operation the enchantment operation
	 * @param player the player enchanting
	 * @param enchantLevel the level of the enchantment
	 * @return the offer or null if no enchantments will be available
	 */
	private @Nullable EnchantmentOffer getOffer(
			@NotNull EnchantOperation operation,
			@NotNull Player player,
			int buttonNumber,
			int enchantLevel) {
		// If level is too low, no offer.
		if (enchantLevel < 1) {
			return null;
		}

		// Assemble enchantment calculation details.
		operation.setButtonLevel(enchantLevel);
		operation.setSeed(getEnchantmentSeed(player) + buttonNumber);

		// Calculate enchantments offered for levels offered.
		Map<Enchantment, Integer> enchantments = operation.apply();

		// No enchantments available, no offer.
		if (enchantments.isEmpty()) {
			return null;
		}

		// Set up offer.
		Map.Entry<Enchantment, Integer> firstEnchant = enchantments.entrySet().iterator().next();
		return new EnchantmentOffer(firstEnchant.getKey(), firstEnchant.getValue(), enchantLevel);
	}

	@EventHandler
	public void onEnchantItem(final @NotNull EnchantItemEvent event) {

		if (event.getItem().getAmount() != 1
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		Class<? extends EnchantableBlock> blockClass = plugin.getRegistry().get(event.getItem().getType());
		if (blockClass == null) {
			return;
		}

		String world = event.getEnchanter().getWorld().getName();
		EnchantableBlockConfig config = plugin.getRegistry().getConfig(blockClass);
		Collection<Enchantment> enchants = new ArrayList<>(plugin.getRegistry().getEnchants(blockClass));
		Set<Enchantment> blacklist = config.tableDisabledEnchants.get(world);
		enchants.removeAll(blacklist);

		if (enchants.isEmpty()) {
			return;
		}

		// Assemble enchantment calculation details.
		EnchantOperation operation = new EnchantOperation(enchants);

		Multimap<Enchantment, Enchantment> enchantConflicts = config.tableEnchantmentConflicts.get(world);
		operation.setIncompatibility((enchantment, enchantment2) ->
				enchantConflicts.get(enchantment).contains(enchantment2)
						|| enchantConflicts.get(enchantment2).contains(enchantment));
		operation.setEnchantability(config.tableEnchantability.get(world));
		operation.setButtonLevel(event.getExpLevelCost());
		operation.setSeed(getEnchantmentSeed(event.getEnchanter()) + event.whichButton());

		// Calculate enchantments offered for levels offered.
		Map<Enchantment, Integer> enchantments = operation.apply();

		event.getEnchantsToAdd().putAll(enchantments);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onEnchantItemSucceed(final @NotNull EnchantItemEvent event) {
		// Player has attempted enchanting anything, all enchants are re-rolled.
		resetSeed(event.getEnchanter());
	}

	/**
	 * Get the enchantment seed of a player.
	 *
	 * @param player the player
	 * @return the enchantment seed
	 */
	private long getEnchantmentSeed(Player player) {
		try {
			Object nmsPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
			return (int) nmsPlayer.getClass().getDeclaredMethod("eG").invoke(nmsPlayer);
		} catch (ReflectiveOperationException | ClassCastException e) {
			needOwnSeed = true;
			Integer integer = player.getPersistentDataContainer().get(randomSeed, PersistentDataType.INTEGER);

			if (integer == null) {
				integer = ThreadLocalRandom.current().nextInt();
				player.getPersistentDataContainer().set(randomSeed, PersistentDataType.INTEGER, integer);
			}

			return integer;
		}
	}

	/**
	 * Reset the enchantment seed of a player.
	 *
	 * @param player the player
	 */
	private void resetSeed(Player player) {
		// Only reset if we're using our own seed - enchantment event completion randomizes internal seed.
		if (needOwnSeed) {
			player.getPersistentDataContainer().remove(randomSeed);
		}
	}

}
