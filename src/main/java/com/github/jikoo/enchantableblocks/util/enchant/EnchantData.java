package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.planarwrappers.util.WeightedRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

/**
 * Container for extra enchantment-related data necessary to generate enchantments.
 */
class EnchantData implements WeightedRandom.Choice {

  private static final Map<Enchantment, EnchantData> ENCHANT_DATA = new HashMap<>();

  static {
    IntUnaryOperator modLootBonus = modLvl(15, 9);
    IntUnaryOperator modLootMax = level -> modLootBonus.applyAsInt(level) + 50;
    addLoot(Enchantment.LOOT_BONUS_MOBS, modLootBonus, modLootMax);
    addLoot(Enchantment.LOOT_BONUS_BLOCKS, modLootBonus, modLootMax);
    addLoot(Enchantment.LUCK, modLootBonus, modLootMax);
    addLoot(Enchantment.LURE, modLootBonus, modLootMax);

    addProtection(Enchantment.PROTECTION_ENVIRONMENTAL, Rarity.COMMON, 1, 11);
    addProtection(Enchantment.PROTECTION_FIRE, Rarity.UNCOMMON, 10, 8);
    addProtection(Enchantment.PROTECTION_FALL, Rarity.UNCOMMON, 5, 6);
    addProtection(Enchantment.PROTECTION_EXPLOSIONS, Rarity.RARE, 5, 8);
    addProtection(Enchantment.PROTECTION_PROJECTILE, Rarity.UNCOMMON, 3, 6);

    IntUnaryOperator lvlTimes10 = level -> level * 10;

    add(Enchantment.OXYGEN, Rarity.RARE, lvlTimes10, 30);
    add(Enchantment.WATER_WORKER, Rarity.RARE, val(1), 40);
    add(Enchantment.THORNS, Rarity.VERY_RARE, modLvl(10, 20));
    add(Enchantment.DEPTH_STRIDER, Rarity.RARE, lvlTimes10, 15);
    add(Enchantment.FROST_WALKER, Rarity.RARE, lvlTimes10, 15);
    add(Enchantment.SOUL_SPEED, Rarity.VERY_RARE, lvlTimes10, 15);

    add(Enchantment.DAMAGE_ALL, Rarity.COMMON, modLvl(1, 11), 20);
    add(Enchantment.DAMAGE_UNDEAD, Rarity.UNCOMMON, modLvl(5, 8), 20);
    add(Enchantment.DAMAGE_ARTHROPODS, Rarity.UNCOMMON, modLvl(5, 8), 20);
    add(Enchantment.KNOCKBACK, Rarity.UNCOMMON, modLvl(5, 20));
    add(Enchantment.FIRE_ASPECT, Rarity.RARE, modLvl(10, 20));
    add(Enchantment.SWEEPING_EDGE, Rarity.RARE, modLvl(5, 9), 15);

    add(Enchantment.DIG_SPEED, Rarity.COMMON, modLvl(1, 10));
    add(Enchantment.SILK_TOUCH, Rarity.VERY_RARE, val(15), modLvl(61, 10));
    add(Enchantment.DURABILITY, Rarity.UNCOMMON, modLvl(5, 8));

    IntUnaryOperator val25 = val(25);
    IntUnaryOperator val50 = val(50);

    add(Enchantment.VANISHING_CURSE, Rarity.VERY_RARE, val25, val50);
    add(Enchantment.BINDING_CURSE, Rarity.VERY_RARE, val25, val50);

    IntUnaryOperator val20 = val(20);

    add(Enchantment.ARROW_DAMAGE, Rarity.COMMON, modLvl(1, 10), 15);
    add(Enchantment.ARROW_KNOCKBACK, Rarity.RARE, modLvl(12, 20), 25);
    add(Enchantment.ARROW_FIRE, Rarity.RARE, val20, val50);
    add(Enchantment.ARROW_INFINITE, Rarity.VERY_RARE, val20, val50);
    add(Enchantment.LOYALTY, Rarity.UNCOMMON, modLvl(12, 7), val50);
    add(Enchantment.IMPALING, Rarity.RARE, modLvl(1, 8), 20);
    add(Enchantment.RIPTIDE, Rarity.RARE, modLvl(17, 7), val50);
    add(Enchantment.CHANNELING, Rarity.VERY_RARE, val25, val50);
    add(Enchantment.MULTISHOT, Rarity.RARE, val20, val50);
    add(Enchantment.QUICK_CHARGE, Rarity.UNCOMMON, level -> 12 + (level - 1) * 20, val50);
    add(Enchantment.PIERCING, Rarity.COMMON, modLvl(1, 10), val50);
    add(Enchantment.MENDING, Rarity.RARE, level -> level * 25);
  }

  private static @NotNull IntUnaryOperator modLvl(int base, int levelMod) {
    return level -> base + (level - 1) * levelMod;
  }

  private static @NotNull IntUnaryOperator val(int value) {
    return integer -> value;
  }

  private static void add(
      @NotNull Enchantment enchantment,
      @NotNull Rarity rarity,
      @NotNull IntUnaryOperator min,
      @NotNull IntUnaryOperator max) {
    EnchantData data = new EnchantData(enchantment, rarity, min, max);
    ENCHANT_DATA.put(data.getEnchantment(), data);
  }

  private static void add(
      @NotNull Enchantment enchantment,
      @NotNull Rarity rarity,
      @NotNull IntUnaryOperator min,
      int maxMod) {
    add(enchantment, rarity, min, level -> min.applyAsInt(level) + maxMod);
  }

  private static void add(
      @NotNull Enchantment enchantment,
      @NotNull Rarity rarity,
      @NotNull IntUnaryOperator min) {
    add(enchantment, rarity, min, 50);
  }

  private static void addProtection(
      @NotNull Enchantment enchantment,
      @NotNull Rarity rarity,
      int base,
      int levelMod) {
    add(enchantment, rarity, modLvl(base, levelMod), levelMod);
  }

  private static void addLoot(
      @NotNull Enchantment enchantment,
      @NotNull IntUnaryOperator min,
      @NotNull IntUnaryOperator max) {
    add(enchantment, Rarity.RARE, min, max);
  }

  @TestOnly
  static boolean isPresent(@NotNull Enchantment enchantment) {
    return ENCHANT_DATA.containsKey(enchantment);
  }

  static EnchantData of(@NotNull Enchantment enchantment) {
    return ENCHANT_DATA.computeIfAbsent(enchantment, EnchantData::new);
  }

  private final @NotNull Enchantment enchantment;
  private final @NotNull Rarity rarity;
  private final @NotNull IntUnaryOperator minEffectiveLevel;
  private final @NotNull IntUnaryOperator maxEffectiveLevel;

  private EnchantData(@NotNull Enchantment enchantment) {
    this(enchantment, EnchantDataReflection.getRarity(enchantment),
        EnchantDataReflection.getMinEnchantQuality(enchantment),
        EnchantDataReflection.getMaxEnchantQuality(enchantment));
  }

  private EnchantData(
      @NotNull Enchantment enchantment,
      @NotNull Rarity rarity,
      @NotNull IntUnaryOperator minEnchantQuality,
      @NotNull IntUnaryOperator maxEnchantQuality) {
    this.enchantment = enchantment;
    this.rarity = rarity;
    this.minEffectiveLevel = minEnchantQuality;
    this.maxEffectiveLevel = maxEnchantQuality;
  }

  @NotNull Enchantment getEnchantment() {
    return this.enchantment;
  }

  @NotNull Rarity getRarity() {
    return this.rarity;
  }

  @Override
  public int getWeight() {
    return this.getRarity().getWeight();
  }

  int getMinEffectiveLevel(int level) {
    return this.minEffectiveLevel.applyAsInt(level);
  }

  int getMaxEffectiveLevel(int level) {
    return this.maxEffectiveLevel.applyAsInt(level);
  }

}
