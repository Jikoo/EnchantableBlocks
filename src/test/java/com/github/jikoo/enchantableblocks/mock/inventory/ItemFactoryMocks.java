package com.github.jikoo.enchantableblocks.mock.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.KnowledgeBookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.ArgumentMatchers;

public final class ItemFactoryMocks {

  private static final Map<Class<?>, Consumer<ItemMeta>> CLASS_META_METHODS;

  static {
    CLASS_META_METHODS = new HashMap<>();

    CLASS_META_METHODS.put(ItemMeta.class, ItemFactoryMocks::meta);
    CLASS_META_METHODS.put(Repairable.class, ItemFactoryMocks::repairable);
    CLASS_META_METHODS.put(Damageable.class, ItemFactoryMocks::damageable);

    CLASS_META_METHODS.put(EnchantmentStorageMeta.class, ItemFactoryMocks::enchantableBook);
  }

  public static @NotNull ItemFactory mockFactory() {
    ItemFactory factory = mock(ItemFactory.class);

    when(factory.getItemMeta(any(Material.class)))
        .thenAnswer(invocation -> getItemMeta(invocation.getArgument(0), null));
    when(factory.asMetaFor(any(ItemMeta.class), any(Material.class)))
        .thenAnswer(invocation -> getItemMeta(invocation.getArgument(1), invocation.getArgument(0)));
    when(factory.asMetaFor(any(ItemMeta.class), any(ItemStack.class)))
        .thenAnswer(invocation -> factory.asMetaFor(invocation.getArgument(0), invocation.getArgument(1, ItemStack.class).getType()));
    when(factory.equals(any(), any())).thenAnswer(invocation -> equals(invocation.getArgument(0), invocation.getArgument(1)));

    // The below is necessary for ItemStack#setItemMeta0, the default ItemStack meta setting implementation.
    // We could construct a special class extending ItemStack, but this is a lot easier to remember.
    // More specific implementations can re-mock this anyway.
    when(factory.isApplicable(any(ItemMeta.class), any(Material.class))).thenReturn(true);
    when(factory.isApplicable(any(ItemMeta.class), any(ItemStack.class)))
        .thenAnswer(invocation -> factory.isApplicable(invocation.getArgument(0), invocation.getArgument(1, ItemStack.class).getType()));

    return factory;
  }

  // See CraftItemFactory#getItemMeta
  public static @Nullable ItemMeta getItemMeta(@NotNull Material material, @Nullable ItemMeta meta) {
    if (material == Material.AIR) {
      return null;
    }
    if (material == Material.WRITTEN_BOOK || material == Material.WRITABLE_BOOK) {
      // Note: Internally 2 separate implementations, but the API doesn't differentiate
      return copyDetails(createMeta(BookMeta.class), meta);
    }
    if (material.name().endsWith("_HEAD") || material.name().endsWith("_SKULL")) {
      return copyDetails(createMeta(SkullMeta.class), meta);
    }
    if (material.name().startsWith("LEATHER_")) {
      return copyDetails(createMeta(LeatherArmorMeta.class), meta);
    }
    if (material.name().endsWith("POTION") || material == Material.TIPPED_ARROW) {
      return copyDetails(createMeta(PotionMeta.class), meta);
    }
    if (material == Material.FILLED_MAP) {
      return copyDetails(createMeta(MapMeta.class), meta);
    }
    if (material == Material.FIREWORK_ROCKET) {
      return copyDetails(createMeta(FireworkMeta.class), meta);
    }
    if (material == Material.FIREWORK_STAR) {
      return copyDetails(createMeta(FireworkEffectMeta.class), meta);
    }
    if (material == Material.ENCHANTED_BOOK) {
      return copyDetails(createMeta(EnchantmentStorageMeta.class), meta);
    }
    if (material.name().endsWith("_BANNER")) {
      return copyDetails(createMeta(BannerMeta.class), meta);
    }
    if (material.name().endsWith("_SPAWN_EGG")) {
      return copyDetails(createMeta(SpawnEggMeta.class), meta);
    }
    // CB has a different meta for Armor Stands and one for things like fish buckets but there's no API for in-inventory entities.
    if (material == Material.KNOWLEDGE_BOOK) {
      return copyDetails(createMeta(KnowledgeBookMeta.class), meta);
    }
    if (material.data != null && BlockData.class.isAssignableFrom(material.data)
        || List.of(Material.SPAWNER, Material.ENCHANTING_TABLE, Material.BEACON, Material.SHIELD).contains(material)) {
      return copyDetails(createMeta(BlockStateMeta.class), meta);
    }
    if (material == Material.TROPICAL_FISH_BUCKET) {
      return copyDetails(createMeta(TropicalFishBucketMeta.class), meta);
    }
    if (material == Material.AXOLOTL_BUCKET) {
      return copyDetails(createMeta(AxolotlBucketMeta.class), meta);
    }
    if (material == Material.CROSSBOW) {
      return copyDetails(createMeta(CrossbowMeta.class), meta);
    }
    if (material == Material.SUSPICIOUS_STEW) {
      return copyDetails(createMeta(SuspiciousStewMeta.class), meta);
    }
    if (material == Material.COMPASS) {
      return copyDetails(createMeta(CompassMeta.class), meta);
    }
    if (material == Material.BUNDLE) {
      return copyDetails(createMeta(BundleMeta.class), meta);
    }
    if (material == Material.GOAT_HORN) {
      return copyDetails(createMeta(MusicInstrumentMeta.class), meta);
    }
    return copyDetails(createMeta(ItemMeta.class), meta);
  }

  public static @NotNull <T extends ItemMeta> T createMeta(@NotNull Class<T> metaClass) {
    T meta;
    if (metaClass == ItemMetaHelper.class) {
      meta = mock(metaClass);
    } else {
      meta = mock(metaClass, withSettings().extraInterfaces(ItemMetaHelper.class));
    }

    for (Entry<Class<?>, Consumer<ItemMeta>> classConsumerEntry : CLASS_META_METHODS.entrySet()) {
      if (classConsumerEntry.getKey().isInstance(meta)) {
        classConsumerEntry.getValue().accept(meta);
      }
    }

    // Lazy copy the creation process and details.
    doAnswer(invocation -> copyDetails(createMeta(metaClass), meta)).when(meta).clone();

    return meta;
  }

  @Contract("_, _ -> param1")
  private static @NotNull ItemMeta copyDetails(@NotNull ItemMeta newMeta, @Nullable ItemMeta oldMeta) {
    if (oldMeta == null) {
      return newMeta;
    }

    // Reflectively call all matching getters and setters.
    // Note that certain things such as enchantments have more specific setting methods;
    // setters for bulk handling can be added via the ItemMetaCloneHelper interface.
    for (Method getter : oldMeta.getClass().getMethods()) {
      if (isSimpleGetter(getter)) {
        try {
          // Find matching setter.
          Method setter = newMeta.getClass()
              .getMethod("set" + getter.getName().substring(3), getter.getReturnType());
          // Get value.
          Object oldValue = getter.invoke(oldMeta);
          if (oldValue != null) {
            // Only set if non-null; Null should be default for nullables, and this may just be a stub.
            setter.invoke(newMeta, oldValue);
          }
        } catch (NoSuchMethodException ignored) {
          // No matching method, not a getter + setter pair.
        } catch (InvocationTargetException | IllegalAccessException e) {
          // Method should be accessible and invokable - must be public and types should match.
          throw new RuntimeException(e);
        }
      }
    }

    return newMeta;
  }

  private static boolean isSimpleGetter(@NotNull Method method) {
    // Getter must start with "get".
    return method.getName().startsWith("get")
        // We only accept getters that don't accept parameters - we have no idea what to provide.
        && method.getParameterCount() == 0
        // Getter must actually get a value.
        && method.getReturnType() != void.class
        // Getter must not be part of Mockito's internals.
        && !method.getReturnType().getPackageName().startsWith("org.mockito")
        // Native methods aren't exactly simple. This ignores Object#getClass etc.
        && !Modifier.isNative(method.getModifiers());
  }

  private static boolean equals(@Nullable ItemMeta meta, @Nullable ItemMeta other) {
    if (meta == other) {
      return true;
    }
    if (meta == null || other == null) {
      return false;
    }

    // Obtain all values for shallow getters for both classes.
    Map<String, Object> metaValues = getGetterReturns(meta);
    Map<String, Object> otherValues = getGetterReturns(other);

    // Compare getter result mappings.
    return metaValues.equals(otherValues);
  }

  private static @NotNull Map<String, Object> getGetterReturns(@NotNull Object meta) {
    Map<String, Object> methodValues = new HashMap<>();

    for (Method getter : meta.getClass().getMethods()) {
      if (!isSimpleGetter(getter)) {
        continue;
      }

      try {
        Object value = getter.invoke(meta);
        if (value != null) {
          methodValues.put(getter.getName(), value);
        }
      } catch (ReflectiveOperationException | UnsupportedOperationException ignored) {
        // We don't care about exceptions - any issue means no value is available.
      }
    }

    return methodValues;
  }

  private static void meta(@NotNull ItemMeta meta) {
    // Display name
    AtomicReference<String> displayName = new AtomicReference<>();
    when(meta.hasDisplayName()).thenAnswer(invocation -> displayName.get() != null);
    when(meta.getDisplayName()).thenAnswer(invocation -> displayName.get());
    doAnswer(invocation -> {
      displayName.set(invocation.getArgument(0));
      return null;
    }).when(meta).setDisplayName(any());

    // Enchantments
    Map<Enchantment, Integer> enchants = new HashMap<>();
    when(meta.addEnchant(any(Enchantment.class), anyInt(), anyBoolean())).thenAnswer(invocation -> {
      Enchantment enchant = invocation.getArgument(0);
      if (enchant == null) {
        throw new IllegalArgumentException("null enchant");
      }
      int level = invocation.getArgument(1);
      boolean force = invocation.getArgument(2);

      if (force
          || enchant.getMaxLevel() >= level
          && enchant.getStartLevel() <= level
          && !meta.hasConflictingEnchant(enchant)) {
        Integer oldValue = enchants.put(enchant, level);
        return oldValue == null || oldValue != level;
      }
      return false;
    });
    when(meta.hasEnchant(any(Enchantment.class)))
        .thenAnswer(invocation -> enchants.containsKey(invocation.<Enchantment>getArgument(0)));
    when(meta.getEnchantLevel(any(Enchantment.class)))
        .thenAnswer(invocation -> enchants.getOrDefault(invocation.getArgument(0, Enchantment.class), 0));
    when(meta.hasConflictingEnchant(any(Enchantment.class))).thenAnswer(invocation -> {
      Enchantment newEnchant = invocation.getArgument(0);
      return enchants.keySet().stream().anyMatch(enchantment -> enchantment.conflictsWith(newEnchant));
    });
    when(meta.removeEnchant(any(Enchantment.class)))
        .thenAnswer(invocation -> enchants.remove(invocation.getArgument(0, Enchantment.class)) != 0);
    when(meta.getEnchants()).thenAnswer(invocation -> Map.copyOf(enchants));
    when(meta.hasEnchants()).thenAnswer(invocation -> !enchants.isEmpty());

    if (meta instanceof ItemMetaHelper cloneHelper) {
      doAnswer(invocation -> {
        enchants.clear();
        Map<Object, Object> map = invocation.getArgument(0);
        if (map == null) {
          return null;
        }
        map.forEach((o1, o2) -> {
          if (o1 instanceof Enchantment enchant && o2 instanceof Integer level) {
            enchants.put(enchant, level);
          }
        });
        return null;
      }).when(cloneHelper).setEnchants(any());
    }

    // TODO lore
  }

  private static void repairable(@NotNull ItemMeta meta) {
    if (!(meta instanceof Repairable repairable)) {
      return;
    }

    AtomicInteger repairCost = new AtomicInteger();
    when(repairable.getRepairCost()).thenAnswer(invocation -> repairCost.get());
    when(repairable.hasRepairCost()).thenAnswer(invocation -> repairCost.get() != 0);
    doAnswer(invocation -> {
      repairCost.set(invocation.getArgument(0));
      return null;
    }).when(repairable).setRepairCost(ArgumentMatchers.anyInt());
  }

  private static void damageable(@NotNull ItemMeta meta) {
    if (!(meta instanceof Damageable damageable)) {
      return;
    }

    AtomicInteger damage = new AtomicInteger();
    when(damageable.getDamage()).thenAnswer(invocation -> damage.get());
    when(damageable.hasDamage()).thenAnswer(invocation -> damage.get() != 0);
    doAnswer(invocation -> {
      damage.set(invocation.getArgument(0));
      return null;
    }).when(damageable).setDamage(ArgumentMatchers.anyInt());
  }

  private static void enchantableBook(@NotNull ItemMeta meta) {
    if (!(meta instanceof EnchantmentStorageMeta storageMeta)) {
      return;
    }

    Map<Enchantment, Integer> stored = new HashMap<>();
    when(storageMeta.addStoredEnchant(any(Enchantment.class), anyInt(), anyBoolean())).thenAnswer(invocation -> {
      Enchantment enchant = invocation.getArgument(0);
      if (enchant == null) {
        throw new IllegalArgumentException("null enchant");
      }
      int level = invocation.getArgument(1);
      boolean force = invocation.getArgument(2);

      if (force
          || enchant.getMaxLevel() >= level
          && enchant.getStartLevel() <= level
          && !meta.hasConflictingEnchant(enchant)) {
        Integer oldValue = stored.put(enchant, level);
        return oldValue == null || oldValue != level;
      }
      return false;
    });

    when(storageMeta.hasStoredEnchant(any(Enchantment.class)))
        .thenAnswer(invocation -> stored.containsKey(invocation.<Enchantment>getArgument(0)));
    when(storageMeta.getStoredEnchantLevel(any(Enchantment.class)))
        .thenAnswer(invocation -> stored.getOrDefault(invocation.getArgument(0, Enchantment.class), 0));
    when(storageMeta.hasConflictingStoredEnchant(any(Enchantment.class))).thenAnswer(invocation -> {
      Enchantment newEnchant = invocation.getArgument(0);
      return stored.keySet().stream().anyMatch(enchantment -> enchantment.conflictsWith(newEnchant));
    });
    when(storageMeta.removeStoredEnchant(any(Enchantment.class)))
        .thenAnswer(invocation -> stored.remove(invocation.getArgument(0, Enchantment.class)) != 0);
    when(storageMeta.getStoredEnchants()).thenAnswer(invocation -> Map.copyOf(stored));
    when(storageMeta.hasStoredEnchants()).thenAnswer(invocation -> !stored.isEmpty());

    if (meta instanceof ItemMetaHelper cloneHelper) {
      doAnswer(invocation -> {
        stored.clear();
        Map<Object, Object> map = invocation.getArgument(0);
        if (map == null) {
          return null;
        }
        map.forEach((o1, o2) -> {
          if (o1 instanceof Enchantment enchant && o2 instanceof Integer level) {
            stored.put(enchant, level);
          }
        });
        return null;
      }).when(cloneHelper).setStoredEnchants(any());
    }
  }

  private ItemFactoryMocks() {}

}
