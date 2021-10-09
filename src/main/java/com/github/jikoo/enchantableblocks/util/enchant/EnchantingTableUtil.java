package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.planarwrappers.util.WeightedRandom;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility for calculating enchantments in a similar fashion to enchanting tables.
 */
public final class EnchantingTableUtil {

  private static final Random RANDOM = new Random();

  /**
   * Get three integers representing button levels in an enchantment table.
   *
   * @param shelves the number of bookshelves to use when calculating levels
   * @param seed the seed of the calculation
   * @return an array of three ints
   */
  public static int[] getButtonLevels(int shelves, long seed) {
    shelves = Math.min(shelves, 15);
    int[] levels = new int[3];
    RANDOM.setSeed(seed);

    for (int button = 0; button < 3; ++button) {
      levels[button] = getButtonLevel(button, shelves);
    }

    return levels;
  }

  /**
   * Get an integer for an enchantment level in an enchantment table.
   *
   * @param button the number of the button
   * @param shelves the number of bookshelves present
   * @return the calculated button level
   */
  private static int getButtonLevel(int button, int shelves) {
    int level = RANDOM.nextInt(8) + 1 + (shelves >> 1) + RANDOM.nextInt(shelves + 1);

    if (button == 0) {
      level = Math.max(level / 3, 1);
    } else if (button == 1) {
      level = level * 2 / 3 + 1;
    } else {
      level = Math.max(level, shelves * 2);
    }

    return level >= button + 1 ? level : 0;
  }

  /**
   * Update enchantment table buttons for a player after a tick has passed.
   * This fixes desync problems that prevent the client from enchanting ordinarily un-enchantable objects.
   *
   * @param player the player enchanting
   * @param offers the enchantment offers
   */
  public static void updateButtons(@NotNull Plugin plugin, @NotNull Player player,
      @Nullable EnchantmentOffer @NotNull [] offers) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      for (int i = 1; i <= 3; ++i) {
        EnchantmentOffer offer = offers[i - 1];
        if (offer != null) {
          player.setWindowProperty(InventoryView.Property.valueOf("ENCHANT_BUTTON" + i), offer.getCost());
          player.setWindowProperty(InventoryView.Property.valueOf("ENCHANT_LEVEL" + i), offer.getEnchantmentLevel());
          player.setWindowProperty(InventoryView.Property.valueOf("ENCHANT_ID" + i), getEnchantmentId(offer.getEnchantment()));
        }
      }
    }, 1L);
  }

  /**
   * Get the magic enchantment ID for use in packets.
   *
   * @param enchantment the enchantment
   * @return the magic value or 0 if the value cannot be obtained
   */
  private static int getEnchantmentId(Enchantment enchantment) {
    String[] split = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
    String nmsVersion = split[split.length - 1];

    try {
      Class<?> clazzIRegistry = Class.forName("net.minecraft.core.IRegistry");
      Object enchantmentRegistry = clazzIRegistry.getDeclaredField("X").get(null);
      Method methodIRegistryGetId = clazzIRegistry.getDeclaredMethod("getId", Object.class);

      Class<?> clazzCraftEnchant = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".enchantments.CraftEnchantment");
      Method methodCraftEnchantGetRaw = clazzCraftEnchant.getDeclaredMethod("getRaw", Enchantment.class);

      return (int) methodIRegistryGetId.invoke(enchantmentRegistry, methodCraftEnchantGetRaw.invoke(null, enchantment));
    } catch (ReflectiveOperationException | ClassCastException e) {
      return 0;
    }
  }

  /**
   * Generate a set of levelled enchantments in a similar fashion to vanilla.
   * Follows provided rules for enchantment incompatibility.
   *
   * @param operation the data used to calculate the results of the operation
   * @return the selected enchantments mapped to their corresponding levels
   */
  static Map<Enchantment, Integer> calculateEnchantments(EnchantOperation operation) {

    // Ensure enchantments present.
    if (operation.getEnchantments().isEmpty()) {
      return Collections.emptyMap();
    }

    // Seed random as specified.
    RANDOM.setSeed(operation.getSeed());

    // Determine effective level.
    int enchantQuality = getEnchantQuality(operation.getEnchantability(), operation.getButtonLevel());
    final int firstEffective = enchantQuality;

    // Determine available enchantments.
    Collection<EnchantData> available = operation.getEnchantments().stream().map(EnchantData::of)
        .filter(enchData -> getEnchantmentLevel(enchData, firstEffective) > 0).collect(Collectors.toSet());

    // Ensure enchantments are available.
    if (available.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<Enchantment, Integer> selected = new HashMap<>();
    addEnchant(selected, available, enchantQuality, operation.getIncompatibility());

    while (!available.isEmpty() && RANDOM.nextInt(50) < enchantQuality) {
      addEnchant(selected, available, enchantQuality, operation.getIncompatibility());
      enchantQuality /= 2;
    }

    return selected;
  }

  /**
   * Randomly select and add an enchantment based on quality.
   *
   * <p>If quality is too low or high, enchantment may not be applied.
   *
   * @param selected the map of already-selected enchantments
   * @param available the available enchantments
   * @param enchantQuality the quality of the enchantment
   * @param incompatibility the way to determine if two enchantments are incompatible
   */
  private static void addEnchant(@NotNull Map<Enchantment, Integer> selected,
      @NotNull Collection<EnchantData> available, int enchantQuality,
      @NotNull BiPredicate<Enchantment, Enchantment> incompatibility) {
    if (available.isEmpty())  {
      return;
    }

    EnchantData enchantData = WeightedRandom.choose(EnchantingTableUtil.RANDOM, available);

    int level = getEnchantmentLevel(enchantData, enchantQuality);

    if (level <= 0) {
      return;
    }

    selected.put(enchantData.getEnchantment(), level);

    available.removeIf(data -> data == enchantData
        || incompatibility.test(data.getEnchantment(), enchantData.getEnchantment()));

  }

  /**
   * Determine enchantment quality for the given enchantability and enchantment level.
   *
   * @param enchantability the enchantability of the item
   * @param enchantLevel the level of the enchantment
   * @return the enchantment quality
   */
  private static int getEnchantQuality(@NotNull Enchantability enchantability, int enchantLevel) {
    if (enchantability.getValue() <= 0) {
      return 0;
    }

    int enchantQuality = enchantability.getValue() / 4 + 1;
    enchantQuality = enchantLevel + 1 + RANDOM.nextInt(enchantQuality) + RANDOM.nextInt(enchantQuality);
    // Random enchantability penalty/bonus 85-115%
    double bonus = (RANDOM.nextDouble() + RANDOM.nextDouble() - 1) * 0.15 + 1;
    enchantQuality = (int) (enchantQuality * bonus + 0.5);
    return Math.max(enchantQuality, 1);
  }

  /**
   * Get an enchantment level based on enchantment quality.
   *
   * <p>Returns 0 if enchantment is too low or high quality. Enchantments of level 0 should not be added.
   *
   * @param enchant the enchantment
   * @param enchantQuality the quality of the enchantment
   * @return the level of the enchantment
   */
  private static int getEnchantmentLevel(@NotNull EnchantData enchant, int enchantQuality) {
    for (int i = enchant.getEnchantment().getMaxLevel(); i >= enchant.getEnchantment().getStartLevel(); --i) {
      if (enchant.getMaxEffectiveLevel(i) < enchantQuality) {
        return 0;
      }
      if (enchant.getMinEffectiveLevel(i) <= enchantQuality) {
        return i;
      }
    }
    return 0;
  }

  private EnchantingTableUtil() {}

}
