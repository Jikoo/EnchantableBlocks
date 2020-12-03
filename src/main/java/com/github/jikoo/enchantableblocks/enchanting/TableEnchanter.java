package com.github.jikoo.enchantableblocks.enchanting;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for handling enchanting in an enchantment table.
 *
 * @author Jikoo
 */
public class TableEnchanter implements Listener {

	private final EnchantableBlocksPlugin plugin;
	private final Map<UUID, Map<Material, Map<Integer, Map<Enchantment, Integer>>>> enchantmentOffers;
	private final Map<UUID, Map<Material, Map<Integer, Integer[]>>> enchantmentOfferLevels;

	public TableEnchanter(EnchantableBlocksPlugin plugin) {
		this.plugin = plugin;
		this.enchantmentOffers = new HashMap<>();
		this.enchantmentOfferLevels = new HashMap<>();
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

		UUID uuid = event.getEnchanter().getUniqueId();
		Material material = event.getItem().getType();

		// Calculate levels offered for bookshelf count
		this.enchantmentOfferLevels.compute(uuid, (id, materialOfferLevels) -> {
			if (materialOfferLevels == null) {
				materialOfferLevels = new HashMap<>();
			}

			materialOfferLevels.compute(event.getItem().getType(), (mat, buttonOfferLevels) -> {
				if (buttonOfferLevels == null) {
					buttonOfferLevels = new HashMap<>();
				}

				buttonOfferLevels.putIfAbsent(event.getEnchantmentBonus(),
						EnchantmentUtil.getButtonLevels(event.getEnchantmentBonus()));

				return buttonOfferLevels;
			});

			return materialOfferLevels;
		});

		// Calculate enchantments offered for levels offered
		this.enchantmentOffers.compute(event.getEnchanter().getUniqueId(), (id, materialOffers) -> {
			if (materialOffers == null) {
				materialOffers = new HashMap<>();
			}
			materialOffers.compute(event.getItem().getType(), (mat, levelOffers) -> {
				if (levelOffers == null) {
					levelOffers = new HashMap<>();
				}

				Integer[] levels = enchantmentOfferLevels.get(uuid).get(material).get(event.getEnchantmentBonus());

				// Set up EnchantmentOffers
				for (int i = 0; i < event.getOffers().length; ++i) {
					if (i >= levels.length) {
						event.getOffers()[i] = null;
						continue;
					}

					levelOffers.computeIfAbsent(levels[i], (level) ->
							EnchantmentUtil.calculateFurnaceEnchants(this.plugin, level));

					int buttonLevel = levels[i];
					Map<Enchantment, Integer> enchantments = levelOffers.get(buttonLevel);

					if (enchantments.isEmpty() || levels[i] < 1) {
						//noinspection ConstantConditions See javadocs, @NotNull only pertains to array, not contents
						event.getOffers()[i] = null;
						continue;
					}

					Map.Entry<Enchantment, Integer> firstEnchant = enchantments.entrySet().iterator().next();
					event.getOffers()[i] = new EnchantmentOffer(firstEnchant.getKey(), firstEnchant.getValue(), buttonLevel);
				}

				return levelOffers;
			});

			return materialOffers;
		});

		// Force button refresh
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

		UUID uuid = event.getEnchanter().getUniqueId();
		Map<Enchantment, Integer> enchantments = this.enchantmentOffers.get(uuid).get(event.getItem().getType()).get(event.getExpLevelCost());

		event.getEnchantsToAdd().putAll(enchantments);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onEnchantItemSucceed(final @NotNull EnchantItemEvent event) {
		// Player has attempted enchanting anything, all enchants are re-rolled.
		UUID uuid = event.getEnchanter().getUniqueId();
		this.enchantmentOfferLevels.remove(uuid);
		this.enchantmentOffers.remove(uuid);
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
		} catch (ReflectiveOperationException e) {
			return 0;
		}
	}

}
