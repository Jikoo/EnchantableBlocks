package com.github.jikoo.enchantableblocks.mock.server;

import com.github.jikoo.enchantableblocks.mock.inventory.ItemStackMocks;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.BannerPatternTagKeys;
import io.papermc.paper.registry.keys.tags.BiomeTagKeys;
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys;
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.EntityTypeTagKeys;
import io.papermc.paper.registry.keys.tags.FluidTagKeys;
import io.papermc.paper.registry.keys.tags.GameEventTagKeys;
import io.papermc.paper.registry.keys.tags.InstrumentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.keys.tags.PaintingVariantTagKeys;
import io.papermc.paper.registry.keys.tags.StructureTagKeys;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.Art;
import org.bukkit.Fluid;
import org.bukkit.GameEvent;
import org.bukkit.Instrument;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockType;
import org.bukkit.block.Structure;
import org.bukkit.block.banner.PatternType;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/** Helper class used to prevent class loading order issues when creating registry. */
@SuppressWarnings("unchecked")
enum RegistryHelper {
  ;

  private static final Map<Class<? extends Keyed>, RegistryKey<? extends Keyed>> LEGACY_REGISTRIES = new HashMap<>();

  static {
    forEachRegistryClass(((field, clazz) -> {
      try {
        LEGACY_REGISTRIES.put(clazz, (RegistryKey<? extends Keyed>) field.get(null));
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }));
  }

  static <T extends Keyed> RegistryKey<T> getRegistryKey(Class<T> clazz) {
    return (RegistryKey<T>) LEGACY_REGISTRIES.get(clazz);
  }

  static <T extends Keyed> T getOrThrow(Class<T> clazz, NamespacedKey key) {
    // Some classes (like BlockType and ItemType) have extra generics that will be
    // erased during runtime calls. To ensure accurate typing, grab the constant's field.
    // This approach also allows us to return null for unsupported keys.
    Class<? extends T> constantClazz;
    try {
      constantClazz = (Class<? extends T>) clazz.getField(
          key.getKey().toUpperCase(Locale.ROOT).replace('.', '_')
      ).getType();
    } catch (ClassCastException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
    T keyed = mock(constantClazz);
    doReturn(key).when(keyed).getKey();
    doReturn(key).when(keyed).key();
    if (keyed instanceof ItemType itemType) {
      mockItemType(itemType);
    }
    return keyed;
  }

  static <T extends Keyed> @Nullable Tag<T> getTag(Class<T> clazz, TagKey<T> tagKey) {
    Class<?> source;
    if (PatternType.class.isAssignableFrom(clazz)) {
      source = BannerPatternTagKeys.class;
    } else if (Biome.class.isAssignableFrom(clazz)) {
      source = BiomeTagKeys.class;
    } else if (BlockType.class.isAssignableFrom(clazz)) {
      source = BlockTypeTagKeys.class;
    } else if (DamageType.class.isAssignableFrom(clazz)) {
      source = DamageTypeTagKeys.class;
    } else if (Dialog.class.isAssignableFrom(clazz)) {
      source = DialogTagKeys.class;
    } else if (Enchantment.class.isAssignableFrom(clazz)) {
      source = EnchantmentTagKeys.class;
    } else if (EntityType.class.isAssignableFrom(clazz)) {
      source = EntityTypeTagKeys.class;
    } else if (Fluid.class.isAssignableFrom(clazz)) {
      source = FluidTagKeys.class;
    } else if (GameEvent.class.isAssignableFrom(clazz)) {
      source = GameEventTagKeys.class;
    } else if (Instrument.class.isAssignableFrom(clazz)) {
      source = InstrumentTagKeys.class;
    } else if (ItemType.class.isAssignableFrom(clazz)) {
      source = ItemTypeTagKeys.class;
    } else if (Art.class.isAssignableFrom(clazz)) {
      source = PaintingVariantTagKeys.class;
    } else if (Structure.class.isAssignableFrom(clazz)) {
      source = StructureTagKeys.class;
    } else {
      // Throw rather than return null; if we're trying to use a tag that we can't verify
      // odds are on that it's a new type rather than an invalid tag.
      throw new UnsupportedOperationException("");
    }

    if (!hasField(tagKey.key().value(), source, TagKey.class, clazz)) {
      return null;
    }

    Tag<T> tag = mock();
    doReturn(tagKey).when(tag).tagKey();
    doReturn(Set.of()).when(tag).values();
    doAnswer(invocationIsTagged -> {
      TypedKey<T> keyed = invocationIsTagged.getArgument(0);
      // Since these are mocks, the exact instance might not be equal. Consider equal keys equal.
      return tag.values().contains(keyed) || tag.values().stream().anyMatch(value -> value.key().equals(keyed.key()));
    }).when(tag).contains(notNull());
    return tag;
  }

  static <T extends Keyed> void forEachRegistryClass(BiConsumer<Field, Class<T>> consumer) {
    for (Field field : RegistryKey.class.getFields()) {
      if (field.getType() == RegistryKey.class) {
        Class<T> clazz = (Class<T>) getClass(((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
        consumer.accept(field, clazz);
      }
    }
  }

  private static Class<?> getClass(Type type) {
    if (type instanceof Class<?> clazz) {
      return clazz;
    } else if (type instanceof ParameterizedType parameterized) {
      return (Class<?>) parameterized.getRawType();
    } else {
      throw new UnsupportedOperationException("Unsupported type " + type.getClass());
    }
  }

  private static void mockItemType(@NotNull ItemType itemType) {
    // ItemStack creation.
    doAnswer(invocation -> itemType.createItemStack(1)
    ).when(itemType).createItemStack();
    doAnswer(invocation -> ItemStackMocks.newItemMock(itemType, invocation.getArgument(0)))
        .when(itemType).createItemStack(anyInt());
  }

  private static boolean hasField(String fieldName, Class<?> clazz, Class<?> type, Class<?> generic) {
    fieldName = fieldName.toUpperCase().replaceAll("\\W", "_");
    try {
      Field field = clazz.getDeclaredField(fieldName);
      if (!type.equals(field.getType())) {
        return false;
      }
      Class<?> genericClazz = getClass(((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
      return generic.equals(genericClazz);
    } catch (NoSuchFieldException e) {
      return false;
    }
  }

}
