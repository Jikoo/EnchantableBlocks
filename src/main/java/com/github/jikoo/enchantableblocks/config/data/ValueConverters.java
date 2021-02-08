package com.github.jikoo.enchantableblocks.config.data;

import java.util.Locale;
import java.util.function.Function;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ValueConverters {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends Enum> @Nullable T toEnum(
            @NotNull T t,
            @Nullable String name) {
        if (name == null) {
            return null;
        }

        try {
            return (T) Enum.valueOf(t.getClass(), name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static <T extends Keyed> @Nullable T toKeyed(
            @NotNull Function<NamespacedKey, T> function,
            @Nullable String key) {
        if (key == null) {
            return null;
        }
        key = key.toLowerCase(Locale.ROOT);

        NamespacedKey namespacedKey;
        if (key.indexOf(':') < 0) {
            namespacedKey = NamespacedKey.minecraft(key);
        } else {
            String[] split = key.split(":");
            // No alternative to deprecated API.
            //noinspection deprecation
            namespacedKey = new NamespacedKey(split[0], split[1]);
        }

        return function.apply(namespacedKey);
    }

    public static @Nullable Enchantment toEnchant(@Nullable String key) {
        return toKeyed(Enchantment::getByKey, key);
    }

    // TODO material: combo keyed, enum, and material methods

    private ValueConverters() {}

}
