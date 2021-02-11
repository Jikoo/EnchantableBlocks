package com.github.jikoo.enchantableblocks.config.data;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ValueConverters {

    private static final Pattern VALID_NAMESPACE = Pattern.compile("([a-z0-9._-]+:)?[a-z0-9/._-]+");

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

        if (!VALID_NAMESPACE.matcher(key).matches()) {
            return null;
        }

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

    public static @Nullable Material toMaterial(@Nullable String key) {
        if (key == null) {
            return null;
        }

        Material value = toKeyed(Registry.MATERIAL::get, key);

        if (value != null) {
            return value;
        }

        return Material.matchMaterial(key);
    }

    private ValueConverters() {}

}
