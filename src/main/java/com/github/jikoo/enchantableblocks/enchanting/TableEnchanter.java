package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.util.enchant.Enchantability;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantingTableUtil;
import java.util.Map;
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
				|| !EnchantableFurnace.isApplicableMaterial(event.getItem().getType())
				|| event.getItem().getAmount() != 1
				|| this.plugin.getEnchantments().size() <= 0
				// TODO: rework permissions
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		// Not normally enchantable, event must be un-cancelled.
		event.setCancelled(false);

		// Calculate levels offered for bookshelf count.
		int[] buttonLevels = EnchantingTableUtil.getButtonLevels(event.getEnchantmentBonus(),
				getEnchantmentSeed(event.getEnchanter()));
		for (int buttonNumber = 0; buttonNumber < 3; ++buttonNumber) {
			//noinspection ConstantConditions // Improper NotNull annotation, should be EnchantmentOffer @NotNull []
			event.getOffers()[buttonNumber] = getOffer(event.getEnchanter(), buttonNumber, buttonLevels[buttonNumber]);
		}

		// Force button refresh.
		EnchantingTableUtil.updateButtons(plugin, event.getEnchanter(), event.getOffers());
	}

	/**
	 * Get an offer of the first enchantment that will be rolled at the specified level for the player.
	 *
	 * @param player the player enchanting
	 * @param enchantLevel the level of the enchantment
	 * @return the offer or null if no enchantments will be available
	 */
	private @Nullable EnchantmentOffer getOffer(@NotNull Player player, int buttonNumber, int enchantLevel) {
		// If level is too low, no offer.
		if (enchantLevel < 1) {
			return null;
		}

		// Calculate enchantments offered for levels offered.
		Map<Enchantment, Integer> enchantments = EnchantingTableUtil.calculateEnchantments(
				plugin.getEnchantments(), plugin::areEnchantmentsIncompatible,
				Enchantability.STONE, enchantLevel, getEnchantmentSeed(player) + buttonNumber);

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

		if (!EnchantableFurnace.isApplicableMaterial(event.getItem().getType())
				|| event.getItem().getAmount() != 1
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		Map<Enchantment, Integer> enchantments = EnchantingTableUtil.calculateEnchantments(
				plugin.getEnchantments(), plugin::areEnchantmentsIncompatible,
				Enchantability.STONE, event.getExpLevelCost(),
				getEnchantmentSeed(event.getEnchanter()) + event.whichButton());

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
