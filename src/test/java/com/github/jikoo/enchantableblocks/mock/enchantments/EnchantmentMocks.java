package com.github.jikoo.enchantableblocks.mock.enchantments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EnchantmentMocks {

  private static final Tag<Material> TAG_EMPTY = new Tag<>() {
    @Override
    public boolean isTagged(@NotNull Material item) {
      return false;
    }

    @NotNull
    @Override
    public Set<Material> getValues() {
      return Set.of();
    }

    @NotNull
    @Override
    public NamespacedKey getKey() {
      return Objects.requireNonNull(NamespacedKey.fromString("mock:empty"));
    }
  };

  public static void init() {
    List<Enchantment> protections = List.of(Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION, Enchantment.PROJECTILE_PROTECTION);
    setUpEnchant(Enchantment.PROTECTION, 4, Tag.ITEMS_ENCHANTABLE_ARMOR, protections);
    setUpEnchant(Enchantment.FIRE_PROTECTION, 4, Tag.ITEMS_ENCHANTABLE_ARMOR, protections);
    setUpEnchant(Enchantment.FEATHER_FALLING, 4, Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR);
    setUpEnchant(Enchantment.BLAST_PROTECTION, 4, Tag.ITEMS_ENCHANTABLE_ARMOR, protections);
    setUpEnchant(Enchantment.PROJECTILE_PROTECTION, 4, Tag.ITEMS_ENCHANTABLE_ARMOR, protections);

    setUpEnchant(Enchantment.RESPIRATION, 3, Tag.ITEMS_ENCHANTABLE_HEAD_ARMOR);
    setUpEnchant(Enchantment.AQUA_AFFINITY, 1, Tag.ITEMS_ENCHANTABLE_HEAD_ARMOR);

    setUpEnchant(Enchantment.THORNS, 3, Tag.ITEMS_ENCHANTABLE_ARMOR);

    setUpEnchant(Enchantment.DEPTH_STRIDER, 3, Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR, List.of(Enchantment.FROST_WALKER));
    setUpEnchant(Enchantment.FROST_WALKER, 3, TAG_EMPTY, Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR, List.of(Enchantment.DEPTH_STRIDER));
    setUpEnchant(Enchantment.SOUL_SPEED, 3, TAG_EMPTY, Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR, List.of());
    setUpEnchant(Enchantment.SWIFT_SNEAK, 3, TAG_EMPTY, Tag.ITEMS_ENCHANTABLE_LEG_ARMOR, List.of());

    setUpEnchant(Enchantment.BINDING_CURSE, 1, TAG_EMPTY, Tag.ITEMS_ENCHANTABLE_EQUIPPABLE, List.of());

    setUpEnchant(Enchantment.SHARPNESS, 5, Tag.ITEMS_ENCHANTABLE_SWORD, Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON, List.of(Enchantment.BANE_OF_ARTHROPODS, Enchantment.SMITE));
    setUpEnchant(Enchantment.SMITE, 5, Tag.ITEMS_ENCHANTABLE_SWORD, Tag.ITEMS_ENCHANTABLE_WEAPON, List.of(Enchantment.SHARPNESS, Enchantment.BANE_OF_ARTHROPODS));
    setUpEnchant(Enchantment.BANE_OF_ARTHROPODS, 5, Tag.ITEMS_ENCHANTABLE_SWORD, Tag.ITEMS_ENCHANTABLE_WEAPON, List.of(Enchantment.SHARPNESS, Enchantment.SMITE));
    setUpEnchant(Enchantment.KNOCKBACK, 2, Tag.ITEMS_ENCHANTABLE_SWORD);
    setUpEnchant(Enchantment.FIRE_ASPECT, 2, Tag.ITEMS_ENCHANTABLE_FIRE_ASPECT);
    setUpEnchant(Enchantment.LOOTING, 3, Tag.ITEMS_ENCHANTABLE_SWORD);
    setUpEnchant(Enchantment.SWEEPING_EDGE, 3, Tag.ITEMS_ENCHANTABLE_SWORD);

    setUpEnchant(Enchantment.EFFICIENCY, 5, Tag.ITEMS_ENCHANTABLE_MINING);
    setUpEnchant(Enchantment.SILK_TOUCH, 1, Tag.ITEMS_ENCHANTABLE_MINING_LOOT, List.of(Enchantment.FORTUNE));

    setUpEnchant(Enchantment.UNBREAKING, 3, Tag.ITEMS_ENCHANTABLE_DURABILITY);

    setUpEnchant(Enchantment.FORTUNE, 3, Tag.ITEMS_ENCHANTABLE_MINING_LOOT, List.of(Enchantment.SILK_TOUCH));

    setUpEnchant(Enchantment.POWER, 5, Tag.ITEMS_ENCHANTABLE_BOW);
    setUpEnchant(Enchantment.PUNCH, 2, Tag.ITEMS_ENCHANTABLE_BOW);
    setUpEnchant(Enchantment.FLAME, 1, Tag.ITEMS_ENCHANTABLE_BOW);
    setUpEnchant(Enchantment.INFINITY, 1, Tag.ITEMS_ENCHANTABLE_BOW, List.of(Enchantment.MENDING));

    setUpEnchant(Enchantment.LUCK_OF_THE_SEA, 3, Tag.ITEMS_ENCHANTABLE_FISHING);
    setUpEnchant(Enchantment.LURE, 3, Tag.ITEMS_ENCHANTABLE_FISHING);

    setUpEnchant(Enchantment.LOYALTY, 3, Tag.ITEMS_ENCHANTABLE_TRIDENT, List.of(Enchantment.RIPTIDE));
    setUpEnchant(Enchantment.IMPALING, 5, Tag.ITEMS_ENCHANTABLE_TRIDENT);
    setUpEnchant(Enchantment.RIPTIDE, 3, Tag.ITEMS_ENCHANTABLE_TRIDENT, List.of(Enchantment.CHANNELING, Enchantment.LOYALTY));
    setUpEnchant(Enchantment.CHANNELING, 1, Tag.ITEMS_ENCHANTABLE_TRIDENT, List.of(Enchantment.RIPTIDE));

    setUpEnchant(Enchantment.MULTISHOT, 1, Tag.ITEMS_ENCHANTABLE_CROSSBOW, List.of(Enchantment.PIERCING));
    setUpEnchant(Enchantment.QUICK_CHARGE, 3, Tag.ITEMS_ENCHANTABLE_CROSSBOW);
    setUpEnchant(Enchantment.PIERCING, 4, Tag.ITEMS_ENCHANTABLE_CROSSBOW, List.of(Enchantment.MULTISHOT));
    setUpEnchant(Enchantment.WIND_BURST, 3, TAG_EMPTY, Tag.ITEMS_ENCHANTABLE_MACE, List.of());
    setUpEnchant(Enchantment.BREACH, 4, Tag.ITEMS_ENCHANTABLE_MACE);
    setUpEnchant(Enchantment.DENSITY, 5, Tag.ITEMS_ENCHANTABLE_MACE);

    setUpEnchant(Enchantment.MENDING, 1, TAG_EMPTY, Tag.ITEMS_ENCHANTABLE_DURABILITY, List.of(Enchantment.INFINITY));
    setUpEnchant(Enchantment.VANISHING_CURSE, 1, TAG_EMPTY, Tag.ITEMS_ENCHANTABLE_VANISHING, List.of());

    checkMissingEnchantments();
  }

  private static void checkMissingEnchantments() {
    Set<String> missingInternalEnchants = new HashSet<>();
    try {
      for (Field field : Enchantment.class.getFields()) {
        if (Modifier.isStatic(field.getModifiers()) && Enchantment.class.equals(field.getType())) {
          Enchantment declaredEnchant = (Enchantment) field.get(null);
          // If max leveel is 0, enchantment was not set up.
          if (declaredEnchant.getMaxLevel() == 0) {
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

  private static void setUpEnchant(
      @NotNull Enchantment enchantment,
      int maxLevel,
      @NotNull Tag<Material> target) {
    setUpEnchant(enchantment, maxLevel, target, target, List.of());
  }

  private static void setUpEnchant(
      @NotNull Enchantment enchantment,
      int maxLevel,
      @NotNull Tag<Material> target,
      @NotNull Collection<Enchantment> conflicts) {
    setUpEnchant(enchantment, maxLevel, target, target, conflicts);
  }

  private static void setUpEnchant(
      @NotNull Enchantment enchantment,
      int maxLevel,
      @NotNull Tag<Material> tableTarget,
      @NotNull Tag<Material> anvilTarget,
      @NotNull Collection<Enchantment> conflicts) {
    doReturn(1).when(enchantment).getStartLevel();
    doReturn(maxLevel).when(enchantment).getMaxLevel();
    // Hopefully in the future the enchantment API gets expanded, making separate table+anvil targets available
    doAnswer(invocation -> {
      ItemStack item = invocation.getArgument(0);
      return item != null && anvilTarget.isTagged(item.getType());
    }).when(enchantment).canEnchantItem(any());
    doReturn(tableTarget.getValues().isEmpty()).when(enchantment).isTreasure();
    // Note: Usual implementation allows contains check, but as these are
    // mocks that cannot be relied on.
    doAnswer(invocation -> {
      NamespacedKey otherKey = invocation.getArgument(0, Enchantment.class).getKey();
      return otherKey.equals(enchantment.getKey()) || conflicts.stream().anyMatch(conflict -> conflict.getKey().equals(otherKey));
    }).when(enchantment).conflictsWith(any());
  }

}
