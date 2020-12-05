package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.util.enchant.Enchantability;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantmentUtil;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

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

	@SuppressWarnings("ConstantConditions") // Suppressed for null checks in improper notnull annotation on PrepareItemEnchantEvent#getOffers
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
		int[] buttonLevels = EnchantmentUtil.getButtonLevels(event.getEnchantmentBonus(),
				getEnchantmentSeed(event.getEnchanter()));
		for (int buttonNumber = 0; buttonNumber < 3; ++buttonNumber) {
			// If level is too low, no offer.
			if (buttonLevels[buttonNumber] < 1) {
				event.getOffers()[buttonNumber] = null;
				continue;
			}

			// Calculate enchantments offered for levels offered.
			Map<Enchantment, Integer> enchantments = EnchantmentUtil.calculateEnchantments(
					plugin.getEnchantments(), plugin::areEnchantmentsIncompatible,
					Enchantability.STONE, buttonLevels[buttonNumber], getEnchantmentSeed(event.getEnchanter()));

			// No enchantments available, no offer.
			if (enchantments.isEmpty()) {
				event.getOffers()[buttonNumber] = null;
				continue;
			}

			// Set up offer.
			Map.Entry<Enchantment, Integer> firstEnchant = enchantments.entrySet().iterator().next();
			event.getOffers()[buttonNumber] = new EnchantmentOffer(firstEnchant.getKey(), firstEnchant.getValue(), buttonLevels[buttonNumber]);
		}

		// Force button refresh.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (int i = 1; i <= 3; ++i) {
				EnchantmentOffer offer = event.getOffers()[i - 1];
				if (offer != null) {
					event.getEnchanter().setWindowProperty(InventoryView.Property.valueOf("ENCHANT_BUTTON" + i), offer.getCost());
					event.getEnchanter().setWindowProperty(InventoryView.Property.valueOf("ENCHANT_LEVEL" + i), offer.getEnchantmentLevel());
					event.getEnchanter().setWindowProperty(InventoryView.Property.valueOf("ENCHANT_ID" + i), getEnchantmentId(offer.getEnchantment()));
				}
			}
		}, 1L);

	}

	@EventHandler
	public void onEnchantItem(final @NotNull EnchantItemEvent event) {

		if (!EnchantableFurnace.isApplicableMaterial(event.getItem().getType())
				|| event.getItem().getAmount() != 1
				|| !event.getEnchanter().hasPermission("enchantableblocks.enchant.table")) {
			return;
		}

		Map<Enchantment, Integer> enchantments = EnchantmentUtil.calculateEnchantments(
				plugin.getEnchantments(), plugin::areEnchantmentsIncompatible,
				Enchantability.STONE, event.getExpLevelCost(), getEnchantmentSeed(event.getEnchanter()));

		event.getEnchantsToAdd().putAll(enchantments);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onEnchantItemSucceed(final @NotNull EnchantItemEvent event) {
		// Player has attempted enchanting anything, all enchants are re-rolled.
		resetSeed(event.getEnchanter());
	}

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

	private void resetSeed(Player player) {
		if (needOwnSeed) {
			player.getPersistentDataContainer().remove(randomSeed);
		}
	}

	private int getEnchantmentId(Enchantment enchantment) {
		String[] split = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
		String nmsVersion = split[split.length - 1];

		try {
			Class<?> clazzIRegistry = Class.forName("net.minecraft.server." + nmsVersion + ".IRegistry");
			Object enchantmentRegistry = clazzIRegistry.getDeclaredField("ENCHANTMENT").get(null);
			Method methodIRegistry_a = clazzIRegistry.getDeclaredMethod("a", Object.class);

			Class<?> clazzCraftEnchant = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".enchantments.CraftEnchantment");
			Method methodCraftEnchant_getRaw = clazzCraftEnchant.getDeclaredMethod("getRaw", Enchantment.class);

			return (int) methodIRegistry_a.invoke(enchantmentRegistry, methodCraftEnchant_getRaw.invoke(null, enchantment));
		} catch (ReflectiveOperationException | ClassCastException e) {
			return 0;
		}
	}

}
