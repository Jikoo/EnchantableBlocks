package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.util.WeightedRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

/**
 * Container for extra enchantment-related data necessary to generate enchantments.
 *
 * @author Jikoo
 */
class EnchantData implements WeightedRandom.Choice {

    private static final Map<Enchantment, EnchantData> ENCHANT_DATA = new HashMap<>();

    static {
        addProtection(Enchantment.PROTECTION_ENVIRONMENTAL, Rarity.COMMON, 1, 11);
        addProtection(Enchantment.PROTECTION_FIRE, Rarity.UNCOMMON, 10, 8);
        addProtection(Enchantment.PROTECTION_FALL, Rarity.UNCOMMON, 5, 6);
        addProtection(Enchantment.PROTECTION_EXPLOSIONS, Rarity.RARE, 5, 8);
        addProtection(Enchantment.PROTECTION_PROJECTILE, Rarity.UNCOMMON, 3, 6);
        add(Enchantment.OXYGEN, Rarity.RARE, level -> 10 * level, 30);
        add(Enchantment.WATER_WORKER, Rarity.RARE, level -> 1, 40);
        add(Enchantment.THORNS, Rarity.VERY_RARE, 10, 20, 50);
        add(Enchantment.DEPTH_STRIDER, Rarity.RARE, level -> 10 * level, 15);
        add(Enchantment.FROST_WALKER, Rarity.RARE, level -> 10 * level, 15);
        add(Enchantment.BINDING_CURSE, Rarity.VERY_RARE, level -> 25, 25);
        add(Enchantment.SOUL_SPEED, Rarity.VERY_RARE, level -> 10 * level, 15);
        addWeaponDamage(Enchantment.DAMAGE_ALL, Rarity.COMMON, 1, 11);
        addWeaponDamage(Enchantment.DAMAGE_UNDEAD, Rarity.UNCOMMON, 5, 8);
        addWeaponDamage(Enchantment.DAMAGE_ARTHROPODS, Rarity.UNCOMMON, 5, 8);
        add(Enchantment.KNOCKBACK, Rarity.UNCOMMON, 5, 20, 50);
        add(Enchantment.FIRE_ASPECT, Rarity.RARE, 10, 20, 50);
        addLootBonus(Enchantment.LOOT_BONUS_MOBS);
        add(Enchantment.SWEEPING_EDGE, Rarity.RARE, 5, 9, 15);
        add(Enchantment.DIG_SPEED, Rarity.COMMON, 1, 10, 50);
        add(Enchantment.SILK_TOUCH, Rarity.VERY_RARE, level -> 15, level -> level * 10 + 51);
        add(Enchantment.DURABILITY, Rarity.UNCOMMON, 5, 8, 50);
        addLootBonus(Enchantment.LOOT_BONUS_BLOCKS);
        add(Enchantment.ARROW_DAMAGE, Rarity.COMMON, 1, 10, 15);
        add(Enchantment.ARROW_KNOCKBACK, Rarity.RARE, 12, 20, 25);
        add(Enchantment.ARROW_FIRE, Rarity.RARE, level -> 20, level -> 50);
        add(Enchantment.ARROW_INFINITE, Rarity.VERY_RARE, level -> 20, level -> 50);
        addLootBonus(Enchantment.LUCK);
        add(Enchantment.LURE, Rarity.RARE, 15, 9, 50);
        add(Enchantment.LOYALTY, Rarity.UNCOMMON, level -> 5 + 7 * level, level -> 50);
        add(Enchantment.IMPALING, Rarity.RARE, 1, 8, 20);
        add(Enchantment.RIPTIDE, Rarity.RARE, level -> 10 + 7 * level, level -> 50);
        add(Enchantment.CHANNELING, Rarity.VERY_RARE, level -> 25, level -> 50);
        add(Enchantment.MULTISHOT, Rarity.RARE, level -> 20, level -> 50);
        add(Enchantment.QUICK_CHARGE, Rarity.UNCOMMON, level -> 12 + (level - 1) * 20, level -> 50);
        add(Enchantment.PIERCING, Rarity.COMMON, level -> 1 + (level - 1) * 10, level -> 50);
        add(Enchantment.MENDING, Rarity.RARE, level -> level * 25, level -> level * 25 + 50);
        add(Enchantment.VANISHING_CURSE, Rarity.VERY_RARE, level -> 25, level -> 50);
    }

    private static void add(Enchantment enchantment, Rarity rarity, IntUnaryOperator min, IntUnaryOperator max) {
        EnchantData data = new EnchantData(enchantment, rarity, min, max);
        ENCHANT_DATA.put(data.getEnchantment(), data);
    }

    private static void add(Enchantment enchantment, Rarity rarity, IntUnaryOperator min, int maxMod) {
        add(enchantment, rarity, min, level -> min.applyAsInt(level) + maxMod);
    }

    private static void add(Enchantment enchantment, Rarity rarity, int base, int levelMod, int maxMod) {
        add(enchantment, rarity, level -> base + (level - 1) * levelMod, maxMod);
    }

    private static void addProtection(Enchantment enchantment, Rarity rarity, int base, int levelMod) {
        add(enchantment, rarity, base, levelMod, levelMod);
    }

    private static void addWeaponDamage(Enchantment enchantment, Rarity rarity, int base, int levelMod) {
        add(enchantment, rarity, base, levelMod, 20);
    }

    private static void addLootBonus(Enchantment enchantment) {
        add(enchantment, Rarity.RARE, 15, 9, 50);
    }

    public static EnchantData of(Enchantment enchantment) {
        EnchantData enchantData = ENCHANT_DATA.get(enchantment);
        if (enchantData == null) {
            enchantData = new EnchantData(enchantment);
            ENCHANT_DATA.put(enchantment, enchantData);
        }
        return enchantData;
    }

    private final Enchantment enchantment;
    private final Rarity rarity;
    private final IntUnaryOperator minEffectiveLevel;
    private final IntUnaryOperator maxEffectiveLevel;

    EnchantData(@NotNull Enchantment enchantment) {
        this(enchantment, EnchantDataReflection.getRarity(enchantment),
                EnchantDataReflection.getMinEffectiveLevel(enchantment),
                EnchantDataReflection.getMaxEffectiveLevel(enchantment));
    }

    EnchantData(@NotNull Enchantment enchantment, Rarity rarity,
            @NotNull IntUnaryOperator minEffectiveLevel,
            @NotNull IntUnaryOperator maxEffectiveLevel) {
        this.enchantment = enchantment;
        this.rarity = rarity;
        this.minEffectiveLevel = minEffectiveLevel;
        this.maxEffectiveLevel = maxEffectiveLevel;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public Rarity getRarity() {
        return rarity;
    }

    @Override
    public int getWeight() {
        return getRarity().getWeight();
    }

    public int getMinEffectiveLevel(int level) {
        return minEffectiveLevel.applyAsInt(level);
    }

    public int getMaxEffectiveLevel(int level) {
        return maxEffectiveLevel.applyAsInt(level);
    }

}
