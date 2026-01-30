package com.github.jikoo.enchantableblocks.mock.enchantments;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.mockito.ArgumentMatchers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.bukkit.enchantments.Enchantment.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

public class EnchantmentMocks {

  private static final Map<NamespacedKey, Enchantment> KEYS_TO_ENCHANTS = new HashMap<>();
  private static final Set<Tag<ItemType>> ENCHANTING_TABLE_TAGS = new HashSet<>();

  public static void init() {
    // See net.minecraft.world.item.enchantment.Enchantments
    config(PROTECTION)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_ARMOR)
        .maxLevel(4)
        .minModCost(perLvl(1, 11))
        .maxModCost(perLvl(12, 11))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_ARMOR);
    config(FIRE_PROTECTION)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_ARMOR)
        .weight(5)
        .maxLevel(4)
        .minModCost(perLvl(10, 8))
        .maxModCost(perLvl(18, 8))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_ARMOR);
    config(FEATHER_FALLING)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR)
        .weight(5)
        .maxLevel(4)
        .minModCost(perLvl(5, 6))
        .maxModCost(perLvl(11, 6));
    config(BLAST_PROTECTION)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_ARMOR)
        .weight(2)
        .maxLevel(4)
        .minModCost(perLvl(5, 8))
        .maxModCost(perLvl(13, 8))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_ARMOR);
    config(PROJECTILE_PROTECTION)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_ARMOR)
        .weight(5)
        .maxLevel(4)
        .minModCost(perLvl(3, 6))
        .maxModCost(perLvl(9, 6))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_ARMOR);

    config(RESPIRATION)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(10, 10))
        .maxModCost(perLvl(40, 10));
    config(AQUA_AFFINITY)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR)
        .weight(2)
        .maxLevel(1)
        .minModCost(flat(1))
        .maxModCost(flat(41));

    config(THORNS)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_CHEST_ARMOR)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_ARMOR)
        .weight(1)
        .maxLevel(3)
        .minModCost(perLvl(10, 20))
        .maxModCost(perLvl(60, 20));

    config(DEPTH_STRIDER)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(10, 10))
        .maxModCost(perLvl(25, 10))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_BOOTS);
    config(FROST_WALKER)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR)
        .weight(2)
        .maxLevel(2)
        .minModCost(perLvl(10, 10))
        .maxModCost(perLvl(25, 10))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_BOOTS);

    config(BINDING_CURSE)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_EQUIPPABLE)
        .weight(1)
        .minModCost(flat(25))
        .maxModCost(flat(50));

    config(SOUL_SPEED)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR)
        .weight(1)
        .maxLevel(3)
        .minModCost(perLvl(10, 10))
        .maxModCost(perLvl(25, 10));
    config(SWIFT_SNEAK)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_LEG_ARMOR)
        .weight(1)
        .maxLevel(3)
        .minModCost(perLvl(25, 25))
        .maxModCost(perLvl(75, 25));

    config(SHARPNESS)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_SHARP_WEAPON)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_MELEE_WEAPON)
        .maxLevel(5)
        .minModCost(perLvl(1, 11))
        .maxModCost(perLvl(21, 11))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_DAMAGE);
    config(SMITE)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_WEAPON)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_MELEE_WEAPON)
        .weight(5)
        .maxLevel(5)
        .minModCost(perLvl(5, 8))
        .maxModCost(perLvl(25, 8))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_DAMAGE);
    config(BANE_OF_ARTHROPODS)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_WEAPON)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_MELEE_WEAPON)
        .weight(5)
        .maxLevel(5)
        .minModCost(perLvl(5, 8))
        .maxModCost(perLvl(25, 8))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_DAMAGE);
    config(KNOCKBACK)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_MELEE_WEAPON)
        .weight(5)
        .maxLevel(2)
        .minModCost(perLvl(5, 20))
        .maxModCost(perLvl(55, 20));
    config(FIRE_ASPECT)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_FIRE_ASPECT)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_MELEE_WEAPON)
        .weight(2)
        .maxLevel(2)
        .minModCost(perLvl(10, 20))
        .maxModCost(perLvl(60, 20));
    config(LOOTING)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_MELEE_WEAPON)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(15, 9))
        .maxModCost(perLvl(65, 9));
    config(SWEEPING_EDGE)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_SWEEPING)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(5, 9))
        .maxModCost(perLvl(20, 9));

    config(EFFICIENCY)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_MINING)
        .maxLevel(5)
        .minModCost(perLvl(1, 10))
        .maxModCost(perLvl(51, 10));
    config(SILK_TOUCH)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_MINING_LOOT)
        .weight(1)
        .minModCost(flat(15))
        .maxModCost(flat(65))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_MINING);
    config(UNBREAKING)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_DURABILITY)
        .weight(5)
        .maxLevel(3)
        .minModCost(perLvl(5, 8))
        .maxModCost(perLvl(55, 8));
    config(FORTUNE)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_MINING_LOOT)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(15, 9))
        .maxModCost(perLvl(65, 9))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_MINING);

    config(POWER)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_BOW)
        .maxLevel(5)
        .minModCost(perLvl(1, 10))
        .maxModCost(perLvl(16, 10));
    config(PUNCH)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_BOW)
        .weight(2)
        .maxLevel(2)
        .minModCost(perLvl(12, 20))
        .maxModCost(perLvl(37, 20));
    config(FLAME)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_BOW)
        .weight(2)
        .minModCost(flat(20))
        .maxModCost(flat(50));
    config(INFINITY)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_BOW)
        .weight(1)
        .minModCost(flat(20))
        .maxModCost(flat(50))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_BOW);

    config(LUCK_OF_THE_SEA)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_FISHING)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(15, 9))
        .maxModCost(perLvl(65, 9));
    config(LURE)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_FISHING)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(15, 9))
        .maxModCost(perLvl(65, 9));

    config(LOYALTY)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_TRIDENT)
        .weight(5)
        .maxLevel(3)
        .minModCost(perLvl(12, 7))
        .maxModCost(flat(50));
    config(IMPALING)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_TRIDENT)
        .weight(2)
        .maxLevel(5)
        .minModCost(perLvl(1, 8))
        .maxModCost(perLvl(21, 8))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_DAMAGE);
    config(RIPTIDE)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_TRIDENT)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(17, 7))
        .maxModCost(flat(50))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_RIPTIDE);

    config(LUNGE)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_LUNGE)
        .weight(5)
        .maxLevel(3)
        .minModCost(perLvl(5, 8))
        .maxModCost(perLvl(25, 8));

    config(CHANNELING)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_TRIDENT)
        .weight(1)
        .minModCost(flat(25))
        .maxModCost(flat(50));

    config(MULTISHOT)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_CROSSBOW)
        .weight(2)
        .maxLevel(1)
        .minModCost(flat(20))
        .maxModCost(flat(50))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_CROSSBOW);
    config(QUICK_CHARGE)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_CROSSBOW)
        .weight(5)
        .maxLevel(3)
        .minModCost(perLvl(12, 20))
        .maxModCost(flat(50));
    config(PIERCING)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_CROSSBOW)
        .maxLevel(4)
        .minModCost(perLvl(1, 10))
        .maxModCost(flat(50))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_CROSSBOW);

    config(DENSITY)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_MACE)
        .weight(5)
        .maxLevel(5)
        .minModCost(perLvl(5, 8))
        .maxModCost(perLvl(25, 8))
        .exclusive(EnchantmentTagKeys.EXCLUSIVE_SET_DAMAGE);
    config(BREACH)
        .tableTarget(ItemTypeTagKeys.ENCHANTABLE_MACE)
        .weight(2)
        .maxLevel(4)
        .minModCost(perLvl(15, 9))
        .maxModCost(perLvl(65, 9));
    config(WIND_BURST)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_MACE)
        .weight(2)
        .maxLevel(3)
        .minModCost(perLvl(15, 9))
        .maxModCost(perLvl(65, 9));

    config(MENDING)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_DURABILITY)
        .weight(2)
        .minModCost(perLvl(25, 25))
        .maxModCost(perLvl(75, 25));
    config(VANISHING_CURSE)
        .anvilTarget(ItemTypeTagKeys.ENCHANTABLE_VANISHING)
        .weight(1)
        .minModCost(flat(25))
        .maxModCost(flat(50));

    Set<String> missingInternalEnchants = new HashSet<>();
    try {
      for (Field field : Enchantment.class.getFields()) {
        if (Modifier.isStatic(field.getModifiers()) && Enchantment.class.equals(field.getType())) {
          Enchantment declaredEnchant = (Enchantment) field.get(null);
          Enchantment stored = KEYS_TO_ENCHANTS.get(declaredEnchant.getKey());
          if (stored == null) {
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

    Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
    // When all enchantments are initialized, redirect registry to our map.
    // This allows us to add and test custom enchantments much more easily.
    doAnswer(invocation -> KEYS_TO_ENCHANTS.get(invocation.getArgument(0, NamespacedKey.class)))
        .when(registry).get((NamespacedKey) ArgumentMatchers.notNull());
    doAnswer(invocation -> KEYS_TO_ENCHANTS.values().stream()).when(registry).stream();
    doAnswer(invocation -> Collections.unmodifiableCollection(KEYS_TO_ENCHANTS.values()).iterator()).when(registry).iterator();
  }

  public static void putEnchant(@NotNull Enchantment enchantment) {
    KEYS_TO_ENCHANTS.put(enchantment.getKey(), enchantment);
  }

  public static @NotNull @UnmodifiableView Set<Tag<ItemType>> getEnchantingTableTags() {
    return Collections.unmodifiableSet(ENCHANTING_TABLE_TAGS);
  }

  private static @NotNull IntUnaryOperator perLvl(int base, int perLevel) {
    return level -> base + (level - 1) * perLevel;
  }

  private static @NotNull IntUnaryOperator flat(int value) {
    return integer -> value;
  }

  private static EnchantConfig config(Enchantment enchantment) {
    return new EnchantConfig(enchantment);
  }

  private static record EnchantConfig(Enchantment enchantment) {

    EnchantConfig(Enchantment enchantment) {
      this.enchantment = enchantment;
      KEYS_TO_ENCHANTS.put(enchantment.getKey(), enchantment);
      weight(10);
      doReturn(1).when(enchantment).getStartLevel();
      doReturn(1).when(enchantment).getMaxLevel();
      doAnswer(invocation -> {
        NamespacedKey otherKey = invocation.getArgument(0, Enchantment.class).getKey();
        return otherKey.equals(enchantment.getKey());
      }).when(enchantment).conflictsWith(any());
    }

    EnchantConfig weight(int weight) {
      doReturn(weight).when(enchantment).getWeight();

      // Anvil cost is technically separate, but in practice is based on enchanting table rarity.
      // For known rarities, set it here.
      return switch (weight) {
        case 10 -> anvilCost(1);
        case 5 -> anvilCost(2);
        case 2 -> anvilCost(4);
        case 1 -> anvilCost(8);
        default -> this;
      };
    }

    EnchantConfig maxLevel(int maxLevel) {
      doReturn(maxLevel).when(enchantment).getMaxLevel();
      return this;
    }

    EnchantConfig anvilTarget(TagKey<ItemType> targetKey) {
      // Hopefully in the future the enchantment API gets expanded, making separate table+anvil targets available
      Tag<ItemType> target = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getTag(targetKey);
      doAnswer(invocation -> {
        ItemStack item = invocation.getArgument(0);
        return item != null && target.contains(TypedKey.create(RegistryKey.ITEM, item.getType().getKey()));
      }).when(enchantment).canEnchantItem(any());
      doReturn(target).when(enchantment).getSupportedItems();
      return this;
    }

    EnchantConfig tableTarget(TagKey<ItemType> targetKey) {
      Tag<ItemType> target = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getTag(targetKey);
      doReturn(false).when(enchantment).isTreasure();
      ENCHANTING_TABLE_TAGS.add(target);
      return anvilTarget(targetKey);
    }

    EnchantConfig minModCost(IntUnaryOperator cost) {
      doAnswer(invocation -> cost.applyAsInt(invocation.getArgument(0, Integer.class)))
          .when(enchantment).getMinModifiedCost(anyInt());
      return this;
    }

    EnchantConfig maxModCost(IntUnaryOperator cost) {
      doAnswer(invocation -> cost.applyAsInt(invocation.getArgument(0, Integer.class)))
          .when(enchantment).getMaxModifiedCost(anyInt());
      return this;
    }

    EnchantConfig anvilCost(int cost) {
      doReturn(cost).when(enchantment).getAnvilCost();
      return this;
    }

    EnchantConfig exclusive(TagKey<Enchantment> conflict) {
      Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
      var conflicts = registry.getTag(conflict);
      doAnswer(invocation -> {
        // Apparently no way to map Enchantment -> TypeKey<Enchantment> directly? Seems odd.
        TypedKey<Enchantment> otherKey = EnchantmentKeys.create(invocation.getArgument(0, Enchantment.class).key());
        return conflicts.contains(otherKey);
      }).when(enchantment).conflictsWith(any());
      return this;
    }

  }

}
