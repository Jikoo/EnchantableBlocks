package com.github.jikoo.enchantableblocks.mock.enchantments;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.jetbrains.annotations.NotNull;

public class EnchantmentMocks {

  private static final Map<NamespacedKey, Enchantment> KEYS_TO_ENCHANTS;

  static {
    try {
      Field byKey = Enchantment.class.getDeclaredField("byKey");
      byKey.setAccessible(true);
      KEYS_TO_ENCHANTS = (Map<NamespacedKey, Enchantment>) byKey.get(null);
      // TODO need byName for registering enchants to Enchantment#values()
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void init() {
    setUpEnchant(Enchantment.PROTECTION_ENVIRONMENTAL.getKey(), 4, EnchantmentTarget.ARMOR,
        List.of(Enchantment.PROTECTION_FIRE, Enchantment.PROTECTION_EXPLOSIONS, Enchantment.PROTECTION_PROJECTILE));
    setUpEnchant(Enchantment.PROTECTION_FIRE.getKey(), 4, EnchantmentTarget.ARMOR,
        List.of(Enchantment.PROTECTION_ENVIRONMENTAL, Enchantment.PROTECTION_EXPLOSIONS, Enchantment.PROTECTION_PROJECTILE));
    setUpEnchant(Enchantment.PROTECTION_FALL.getKey(), 4, EnchantmentTarget.ARMOR_FEET);
    setUpEnchant(Enchantment.PROTECTION_EXPLOSIONS.getKey(), 4, EnchantmentTarget.ARMOR,
        List.of(Enchantment.PROTECTION_ENVIRONMENTAL, Enchantment.PROTECTION_FIRE, Enchantment.PROTECTION_PROJECTILE));
    setUpEnchant(Enchantment.PROTECTION_PROJECTILE.getKey(), 4, EnchantmentTarget.ARMOR,
        List.of(Enchantment.PROTECTION_ENVIRONMENTAL, Enchantment.PROTECTION_FIRE, Enchantment.PROTECTION_EXPLOSIONS));

    setUpEnchant(Enchantment.OXYGEN.getKey(), 3, EnchantmentTarget.ARMOR_HEAD);
    setUpEnchant(Enchantment.WATER_WORKER.getKey(), 1, EnchantmentTarget.ARMOR_HEAD);

    setUpEnchant(Enchantment.THORNS.getKey(), 3, EnchantmentTarget.ARMOR);

    setUpEnchant(Enchantment.DEPTH_STRIDER.getKey(), 3, EnchantmentTarget.ARMOR_FEET, List.of(Enchantment.FROST_WALKER));
    setUpEnchant(Enchantment.FROST_WALKER.getKey(), 3, EnchantmentTarget.ARMOR_FEET, true, false, List.of(Enchantment.DEPTH_STRIDER));

    setUpEnchant(Enchantment.BINDING_CURSE.getKey(), 1, EnchantmentTarget.WEARABLE, true, true, List.of());

    setUpEnchant(Enchantment.DAMAGE_ALL.getKey(), 5, EnchantmentTarget.WEAPON, List.of(Enchantment.DAMAGE_ARTHROPODS, Enchantment.DAMAGE_UNDEAD));
    setUpEnchant(Enchantment.DAMAGE_UNDEAD.getKey(), 5, EnchantmentTarget.WEAPON, List.of(Enchantment.DAMAGE_ALL, Enchantment.DAMAGE_ARTHROPODS));
    setUpEnchant(Enchantment.DAMAGE_ARTHROPODS.getKey(), 5, EnchantmentTarget.WEAPON, List.of(Enchantment.DAMAGE_ALL, Enchantment.DAMAGE_UNDEAD));
    setUpEnchant(Enchantment.KNOCKBACK.getKey(), 2, EnchantmentTarget.WEAPON);
    setUpEnchant(Enchantment.FIRE_ASPECT.getKey(), 2, EnchantmentTarget.WEAPON);
    setUpEnchant(Enchantment.LOOT_BONUS_MOBS.getKey(), 3, EnchantmentTarget.WEAPON);
    setUpEnchant(Enchantment.SWEEPING_EDGE.getKey(), 3, EnchantmentTarget.WEAPON);

    setUpEnchant(Enchantment.DIG_SPEED.getKey(), 5, EnchantmentTarget.TOOL);
    setUpEnchant(Enchantment.SILK_TOUCH.getKey(), 1, EnchantmentTarget.TOOL, List.of(Enchantment.LOOT_BONUS_BLOCKS));

    setUpEnchant(Enchantment.DURABILITY.getKey(), 3, EnchantmentTarget.BREAKABLE);

    setUpEnchant(Enchantment.LOOT_BONUS_BLOCKS.getKey(), 3, EnchantmentTarget.TOOL, List.of(Enchantment.SILK_TOUCH));

    setUpEnchant(Enchantment.ARROW_DAMAGE.getKey(), 5, EnchantmentTarget.BOW);
    setUpEnchant(Enchantment.ARROW_KNOCKBACK.getKey(), 2, EnchantmentTarget.BOW);
    setUpEnchant(Enchantment.ARROW_FIRE.getKey(), 1, EnchantmentTarget.BOW);
    setUpEnchant(Enchantment.ARROW_INFINITE.getKey(), 1, EnchantmentTarget.BOW, List.of(Enchantment.MENDING));

    setUpEnchant(Enchantment.LUCK.getKey(), 3, EnchantmentTarget.FISHING_ROD);
    setUpEnchant(Enchantment.LURE.getKey(), 3, EnchantmentTarget.FISHING_ROD);

    setUpEnchant(Enchantment.LOYALTY.getKey(), 3, EnchantmentTarget.TRIDENT, List.of(Enchantment.RIPTIDE));
    setUpEnchant(Enchantment.IMPALING.getKey(), 5, EnchantmentTarget.TRIDENT);
    setUpEnchant(Enchantment.RIPTIDE.getKey(), 3, EnchantmentTarget.TRIDENT, List.of(Enchantment.CHANNELING, Enchantment.LOYALTY));
    setUpEnchant(Enchantment.CHANNELING.getKey(), 1, EnchantmentTarget.TRIDENT, List.of(Enchantment.RIPTIDE));

    setUpEnchant(Enchantment.MULTISHOT.getKey(), 1, EnchantmentTarget.CROSSBOW, List.of(Enchantment.PIERCING));
    setUpEnchant(Enchantment.QUICK_CHARGE.getKey(), 3, EnchantmentTarget.CROSSBOW);
    setUpEnchant(Enchantment.PIERCING.getKey(), 4, EnchantmentTarget.CROSSBOW, List.of(Enchantment.MULTISHOT));

    setUpEnchant(Enchantment.MENDING.getKey(), 1, EnchantmentTarget.BREAKABLE, List.of(Enchantment.ARROW_INFINITE));

    setUpEnchant(Enchantment.VANISHING_CURSE.getKey(), 1, EnchantmentTarget.VANISHABLE, true, true, List.of());

    setUpEnchant(Enchantment.SOUL_SPEED.getKey(), 3, EnchantmentTarget.ARMOR_FEET, true, false, List.of());
    setUpEnchant(Enchantment.SWIFT_SNEAK.getKey(), 3, EnchantmentTarget.ARMOR_LEGS, true, false, List.of());

    Set<String> missingInternalEnchants = new HashSet<>();
    try {
      for (Field field : Enchantment.class.getFields()) {
        if (Modifier.isStatic(field.getModifiers()) && Enchantment.class.equals(field.getType())) {
          Enchantment declaredEnchant = (Enchantment) field.get(null);
          if (!KEYS_TO_ENCHANTS.containsKey(declaredEnchant.getKey())) {
            missingInternalEnchants.add(declaredEnchant.getKey().toString());
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    if (!missingInternalEnchants.isEmpty()) {
      throw new IllegalStateException("Missing enchantment declarations for " + missingInternalEnchants);
    }
  }

  public static @NotNull Collection<Enchantment> getRegisteredEnchantments() {
    return KEYS_TO_ENCHANTS.values();
  }

  public static Enchantment getEnchant(@NotNull NamespacedKey key) {
    return KEYS_TO_ENCHANTS.get(key);
  }

  public static void putEnchant(@NotNull Enchantment enchantment) {
    KEYS_TO_ENCHANTS.put(enchantment.getKey(), enchantment);
  }

  private static void setUpEnchant(
      @NotNull NamespacedKey key,
      int maxLevel,
      @NotNull EnchantmentTarget target) {
    setUpEnchant(key, maxLevel, target, false, false, List.of());
  }

  private static void setUpEnchant(
      @NotNull NamespacedKey key,
      int maxLevel,
      @NotNull EnchantmentTarget target,
      @NotNull Collection<Enchantment> conflicts) {
    setUpEnchant(key, maxLevel, target, false, false, conflicts);
  }

  private static void setUpEnchant(
      @NotNull NamespacedKey key,
      int maxLevel,
      @NotNull EnchantmentTarget target,
      boolean treasure,
      boolean curse,
      @NotNull Collection<Enchantment> conflicts) {
    KEYS_TO_ENCHANTS.put(key, new EnchantmentHolder(key, maxLevel, target, treasure, curse, conflicts));
  }

}
