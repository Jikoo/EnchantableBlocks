package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantOperation;
import com.github.jikoo.enchantableblocks.util.enchant.EnchantingTableUtil;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
import org.bukkit.NamespacedKey;
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
  private boolean needPluginSeed = false;
  private final NamespacedKey seedKey;

  /**
   * Construct a new {@code TableEnchanter} to provide enchantments for blocks.
   *
   * @param plugin the owning {@link Plugin}
   * @param registry the {@link EnchantableBlockRegistry} providing block details
   */
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

    var operation = getOperation(event.getItem(), event.getEnchanter());

    if (operation == null) {
      return;
    }

    // Calculate levels offered for bookshelf count.
    int[] buttonLevels = EnchantingTableUtil.getButtonLevels(event.getEnchantmentBonus(),
        getEnchantmentSeed(event.getEnchanter(), this::getRandomSeed));
    for (int buttonNumber = 0; buttonNumber < 3; ++buttonNumber) {
      event.getOffers()[buttonNumber] =
          getOffer(operation, event.getEnchanter(), buttonNumber, buttonLevels[buttonNumber]);
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

    var operation = getOperation(event.getItem(), event.getEnchanter());

    if (operation == null) {
      return;
    }

    operation.setButtonLevel(event.getExpLevelCost());
    operation.setSeed(
        getEnchantmentSeed(event.getEnchanter(), this::getRandomSeed) + event.whichButton());

    // Calculate enchantments offered for levels offered.
    var enchantments = operation.apply();

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
    return registration == null || registration.getEnchants().isEmpty()
        || !registration.hasEnchantPermission(player, "table");
  }

  @VisibleForTesting
  @Nullable EnchantOperation getOperation(@NotNull ItemStack itemStack, @NotNull Player enchanter) {
    var registration = registry.get(itemStack.getType());
    if (registration == null) {
      return null;
    }

    var world = enchanter.getWorld().getName();
    var config = registration.getConfig();
    var enchants = new ArrayList<>(registration.getEnchants());
    var blacklist = config.tableDisabledEnchants.get(world);
    enchants.removeAll(blacklist);

    if (enchants.isEmpty()) {
      return null;
    }

    EnchantOperation operation = new EnchantOperation(enchants);

    var enchantConflicts = config.tableEnchantmentConflicts.get(world);
    operation.setIncompatibility((enchantment, enchantment2) ->
        enchantConflicts.get(enchantment).contains(enchantment2)
            || enchantConflicts.get(enchantment2).contains(enchantment));
    operation.setEnchantability(config.tableEnchantability.get(world));

    return operation;
  }

  /**
   * Get an {@link EnchantmentOffer} of the first enchantment rolled for the {@link Player}.
   *
   * @param operation the enchantment operation
   * @param player the {@code Player} enchanting
   * @param buttonNumber the button index pressed
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
    var enchantments = operation.apply();

    // Get offer for first enchantment if present, otherwise return null.
    return enchantments.entrySet().stream().findFirst()
        .map(entry -> new EnchantmentOffer(entry.getKey(), entry.getValue(), enchantLevel))
        .orElse(null);
  }

  /**
   * Obtain the enchantment seed from the {@link Player}. If unable to use Minecraft's internal
   * seed, falls through to a consistent plugin-created seed.
   *
   * @param player the player
   * @param supplier the way to obtain the seed if not present
   * @return the enchantment seed
   */
  @VisibleForTesting
  long getEnchantmentSeed(@NotNull Player player, @NotNull IntSupplier supplier) {
    if (needPluginSeed) {
      return getPluginSeed(player, supplier);
    }

    try {
      // Attempt to get internal seed
      Object nmsPlayer = player.getClass().getDeclaredMethod("getHandle").invoke(player);
      return (int) nmsPlayer.getClass().getDeclaredMethod("eG").invoke(nmsPlayer);
    } catch (ReflectiveOperationException | ClassCastException e) {
      plugin.getLogger().warning(
          "Cannot obtain seed from EntityPlayer. Falling through to internal seed.");
      needPluginSeed = true;
      return getPluginSeed(player, supplier);
    }
  }

  /**
   * Obtain the plugin-created enchantment seed from the {@link Player}.
   *
   * @param player the player
   * @param supplier the way to obtain the seed if not present
   * @return the enchantment seed
   */
  @VisibleForTesting
  long getPluginSeed(@NotNull Player player, @NotNull IntSupplier supplier) {
    var integer = player.getPersistentDataContainer().get(seedKey, PersistentDataType.INTEGER);

    if (integer == null) {
      integer = supplier.getAsInt();
      player.getPersistentDataContainer().set(seedKey, PersistentDataType.INTEGER, integer);
    }

    return integer;
  }

  /**
   * Get a random seed.
   *
   * @return a random seed
   */
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
