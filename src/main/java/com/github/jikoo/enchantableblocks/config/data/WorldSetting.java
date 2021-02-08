package com.github.jikoo.enchantableblocks.config.data;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A generic framework for handling a setting that may be configured per-world.
 *
 * @param <T> the type of value stored
 */
public abstract class WorldSetting<T> {

    static final String WORLD_PATH_FORMAT = "world_overrides.%s.%s";

    protected final ConfigurationSection section;
    protected final String path;
    protected final T defaultValue;

    public WorldSetting(
            @NotNull ConfigurationSection section,
            @NotNull String path,
            @NotNull T defaultValue) {
        if ("world_overrides".equals(path)) {
            throw new IllegalArgumentException("Key \"world_overrides\" is reserved for per-world settings!");
        }
        this.section = section;
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public @NotNull T get(@NotNull String world) {
        T t = getPathSetting(String.format(WORLD_PATH_FORMAT, world, path));
        if (t != null) {
            return t;
        }
        t = getPathSetting(path);
        return t != null ? t : defaultValue;
    }

    protected abstract @Nullable T getPathSetting(@NotNull String path);

}
