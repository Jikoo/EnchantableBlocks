package com.github.jikoo.enchantableblocks.config.data;

import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A representation of a mapping available in a configuration.
 *
 * <p>Unlike a {@link Setting Setting&lt;java.util.Map&gt;}, a {@code Mapping} falls through to default
 * values even if there is an override for the world when the specified key does not result in a value.
 *
 * @param <K> the type of key
 * @param <V> the type of value
 */
public abstract class Mapping<K, V> {

    protected final ConfigurationSection section;
    protected final String path;
    protected final Function<@NotNull K, @NotNull V> defaultValue;

    protected Mapping(
            @NotNull ConfigurationSection section,
            @NotNull String path,
            @NotNull Function<@NotNull K, @NotNull V> defaultValue) {
        if ("world_overrides".equals(path)) {
            throw new IllegalArgumentException("Key \"world_overrides\" is reserved for per-world settings!");
        }
        this.section = section;
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public @NotNull V get(@NotNull String world, @NotNull K key) {
        V value = getPathSetting(String.format(Setting.WORLD_PATH_FORMAT, world, path), key);
        if (value != null) {
            return value;
        }
        value = getPathSetting(path, key);
        return value != null ? value : defaultValue.apply(key);
    }

    protected abstract @Nullable V getPathSetting(@NotNull String path, @NotNull K key);

}
