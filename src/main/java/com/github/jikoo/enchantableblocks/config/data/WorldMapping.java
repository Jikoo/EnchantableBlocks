package com.github.jikoo.enchantableblocks.config.data;

import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class WorldMapping<K, V> {

    protected final ConfigurationSection section;
    protected final String path;
    protected final Function<@NotNull K, @NotNull V> defaultValue;

    protected WorldMapping(
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
        V value = getPathSetting(String.format(WorldSetting.WORLD_PATH_FORMAT, world, path), key);
        if (value != null) {
            return value;
        }
        value = getPathSetting(path, key);
        return value != null ? value : defaultValue.apply(key);
    }

    protected abstract @Nullable V getPathSetting(@NotNull String path, @NotNull K key);

}
