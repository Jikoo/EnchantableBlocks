package com.github.jikoo.enchantableblocks.mock.server;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class TestRegistryAccess implements RegistryAccess {

  private static final Map<RegistryKey<? extends Keyed>, Registry<? extends @NotNull Keyed>> REGISTERS = new HashMap<>();

  @Override
  @Deprecated(since = "1.20.6", forRemoval = true)
  public @Nullable <T extends Keyed> Registry<@NotNull T> getRegistry(@NotNull Class<T> type) {
    return getRegistry(RegistryHelper.getRegistryKey(type));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends @NotNull Keyed> @NotNull Registry<T> getRegistry(@NotNull RegistryKey<T> registryKey) {
    return (Registry<T>) REGISTERS.computeIfAbsent(registryKey, regKey -> {
      Registry<?> registry = mock();

      AtomicReference<Class<T>> clazzRef = new AtomicReference<>();
      RegistryHelper.forEachRegistryClass((BiConsumer<Field, Class<T>>) (field, clazz) -> {
        if (RegistryHelper.getRegistryKey(clazz).equals(regKey)) {
          clazzRef.set(clazz);
        }
      });

      Map<NamespacedKey, T> values = new HashMap<>();
      Class<T> clazz = clazzRef.get();

      // Paper added several helpers and then moved ItemType to them.
      // Mock these first so Mockito doesn't try to clobber the original overload
      // because NamespacedKey extends Key.
      doAnswer(invocation -> {
        Key key = invocation.getArgument(0);
        return registry.get(Objects.requireNonNull(NamespacedKey.fromString(key.asString())));
      }).when(registry).get(any(Key.class));
      doAnswer(invocation -> {
        Key key = invocation.getArgument(0);
        return registry.getOrThrow(Objects.requireNonNull(NamespacedKey.fromString(key.asString())));
      }).when(registry).getOrThrow(any(Key.class));

      Answer<T> getOrThrow = invocationGetEntry -> {
        NamespacedKey key = invocationGetEntry.getArgument(0);
        return values.computeIfAbsent(key, key1 -> RegistryHelper.getOrThrow(clazz, key1));
      };

      doAnswer(getOrThrow).when(registry).getOrThrow(any(NamespacedKey.class));
      // For get, return null for nonexistant constants.
      doAnswer(invocation -> {
        try {
          return getOrThrow.answer(invocation);
        } catch (RuntimeException e) {
          if (e.getCause() instanceof NoSuchFieldException) {
            return null;
          }
          throw e;
        }
      }).when(registry).get(any(NamespacedKey.class));

      Map<TagKey<T>, Tag<T>> tags = new HashMap<>();

      doAnswer(invocation -> {
        TagKey<T> tagKey = invocation.getArgument(0);
        return tags.computeIfAbsent(tagKey, key -> RegistryHelper.getTag(clazz, key));
      }).when(registry).getTag(any(TagKey.class));

      return registry;
    });
  }

}
