package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantOperation;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantingTableUtil;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Listener for handling enchanting in an enchantment table.
 */
public class TableEnchanter implements Listener {

  private final Plugin plugin;
  private final EnchantableBlockRegistry registry;
  private boolean needOwnSeed = false;
  private final NamespacedKey seedKey;

  public TableEnchanter(@NotNull Plugin plugin, @NotNull EnchantableBlockRegistry registry) {
    this.plugin = plugin;
    this.registry = registry;
    this.seedKey = new NamespacedKey(plugin, "enchant_seed");
  }

  @VisibleForTesting
  @EventHandler
  void onPrepareItemEnchant(final @NotNull PrepareItemEnchantEvent event) {

    if (isUnableToEnchant(event.getItem(), event.getEnchanter())) {
      return;
    }

    EnchantOperation operation = getOperation(event.getItem(), event.getEnchanter());

    if (operation == null) {
      return;
    }

    // Calculate levels offered for bookshelf count.
    int[] buttonLevels = EnchantingTableUtil.getButtonLevels(event.getEnchantmentBonus(),
        getEnchantmentSeed(event.getEnchanter(), this::getRandomSeed));
    for (int buttonNumber = 0; buttonNumber < 3; ++buttonNumber) {
      event.getOffers()[buttonNumber] = getOffer(operation, event.getEnchanter(), buttonNumber, buttonLevels[buttonNumber]);
    }

    // Force button refresh.
    EnchantingTableUtil.updateButtons(plugin, event.getEnchanter(), event.getOffers());
  }

  @VisibleForTesting
  @EventHandler
  void onEnchantItem(final @NotNull EnchantItemEvent event) {

    if (isUnableToEnchant(event.getItem(), event.getEnchanter())) {
      return;
    }

    EnchantOperation operation = getOperation(event.getItem(), event.getEnchanter());

    if (operation == null) {
      return;
    }

    operation.setButtonLevel(event.getExpLevelCost());
    operation.setSeed(getEnchantmentSeed(event.getEnchanter(), this::getRandomSeed) + event.whichButton());

    // Calculate enchantments offered for levels offered.
    Map<Enchantment, Integer> enchantments = operation.apply();

    event.getEnchantsToAdd().putAll(enchantments);
  }

  @VisibleForTesting
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  void onEnchantItemSucceed(final @NotNull EnchantItemEvent event) {
    // Player has attempted enchanting anything, all enchants are re-rolled.
    resetSeed(event.getEnchanter());
  }

  /**
   * Check if a player is unable to enchant.
   *
   * @param itemStack the item being enchanted
   * @param player the enchanter
   * @return true if the enchanter is unable to enchant the item
   */
  @VisibleForTesting
  boolean isUnableToEnchant(@NotNull ItemStack itemStack, @NotNull Player player) {
    // Item may not be enchanted if it is already enchanted or if it is in a stack.
    if (itemStack.getEnchantments().size() > 0 || itemStack.getAmount() != 1) {
      return true;
    }

    var registration = registry.get(itemStack.getType());

    // No registration, no enchantments.
    if (registration == null || registration.getEnchants().isEmpty()) {
      return true;
    }

    if (player.hasPermission("enchantableblocks.enchant.table")) {
      return false;
    }

    String blockType = registration.getBlockClass().getSimpleName().toLowerCase(Locale.ROOT);

    return !player.hasPermission("enchantableblocks.enchant.table." + blockType);
  }

  @VisibleForTesting
  @Nullable EnchantOperation getOperation(@NotNull ItemStack itemStack, @NotNull Player enchanter) {
    var registration = registry.get(itemStack.getType());
    if (registration == null) {
      return null;
    }

    String world = enchanter.getWorld().getName();
    EnchantableBlockConfig config = registration.getConfig();
    Collection<Enchantment> enchants = new ArrayList<>(registration.getEnchants());
    Set<Enchantment> blacklist = config.tableDisabledEnchants.get(world);
    enchants.removeAll(blacklist);

    if (enchants.isEmpty()) {
      return null;
    }

    EnchantOperation operation = new EnchantOperation(enchants);

    Multimap<Enchantment, Enchantment> enchantConflicts = config.tableEnchantmentConflicts.get(world);
    operation.setIncompatibility((enchantment, enchantment2) ->
        enchantConflicts.get(enchantment).contains(enchantment2)
            || enchantConflicts.get(enchantment2).contains(enchantment));
    operation.setEnchantability(config.tableEnchantability.get(world));

    return operation;
  }

  /**
   * Get an offer of the first enchantment that will be rolled at the specified level for the player.
   *
   * @param operation the enchantment operation
   * @param player the player enchanting
   * @param enchantLevel the level of the enchantment
   * @return the offer or null if no enchantments will be available
   */
  @VisibleForTesting
  @Nullable EnchantmentOffer getOffer(
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
    operation.setSeed(getEnchantmentSeed(player, this::getRandomSeed) + buttonNumber);

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

  /**
   * Get the enchantment seed of a player.
   *
   * @param player the player
   * @return the enchantment seed
   */
  @VisibleForTesting
  long getEnchantmentSeed(@NotNull Player player, @NotNull IntSupplier supplier) {
    if (needOwnSeed) {
      return getOwnSeed(player, supplier);
    }

    try {
      // Attempt to get internal seed
      Object nmsPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
      return (int) nmsPlayer.getClass().getDeclaredMethod("eG").invoke(nmsPlayer);
    } catch (ReflectiveOperationException | ClassCastException e) {
      needOwnSeed = true;
      return getOwnSeed(player, supplier);
    }
  }

  @VisibleForTesting
  long getOwnSeed(@NotNull Player player, @NotNull IntSupplier supplier) {
    Integer integer = player.getPersistentDataContainer().get(seedKey, PersistentDataType.INTEGER);

    if (integer == null) {
      integer = supplier.getAsInt();
      player.getPersistentDataContainer().set(seedKey, PersistentDataType.INTEGER, integer);
    }

    return integer;
  }

  private int getRandomSeed() {
    return ThreadLocalRandom.current().nextInt();
  }

  /**
   * Reset the enchantment seed of a player.
   *
   * @param player the player
   */
  @VisibleForTesting
  void resetSeed(Player player) {
    player.getPersistentDataContainer().remove(seedKey);
  }

}
