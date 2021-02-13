package com.github.jikoo.enchantableblocks.config.data;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A world setting that is not directly retrievable from a ConfigurationSection.
 *
 * <p>To minimize the impact of the extra handling required, values are cached after being parsed.
 *
 * @param <T> the type of value stored
 */
public abstract class ParsedSetting<T> extends Setting<T> {

    private final Map<String, T> cache = new HashMap<>();

    protected ParsedSetting(@NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull T defaultValue) {
        super(section, key, defaultValue);
    }

    @Override
    protected @NotNull T getPathSetting(@NotNull String path) {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        T value = null;
        if (test(path)) {
            value = convert(path);
        }

        if (value == null && test(this.path)) {
            value = convert(this.path);
        }

        if (value == null) {
            value = defaultValue;
        }

        cache.put(path, value);
        return value;
    }

    protected abstract boolean test(@NotNull String path);

    protected abstract @Nullable T convert(@NotNull String path);

}
