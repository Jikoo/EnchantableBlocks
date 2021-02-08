package com.github.jikoo.enchantableblocks.config.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
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
public abstract class ParsedWorldSetting<T> extends WorldSetting<T> {

    private final Map<String, T> cache = new HashMap<>();
    private final BiPredicate<@NotNull ConfigurationSection, @NotNull String> tester;
    private final BiFunction<@NotNull ConfigurationSection, @NotNull String, @Nullable T> converter;

    protected ParsedWorldSetting(@NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull BiPredicate<@NotNull ConfigurationSection, @NotNull String> tester,
            @NotNull BiFunction<@NotNull ConfigurationSection, @NotNull String, @Nullable T> converter,
            @NotNull T defaultValue) {
        super(section, key, defaultValue);

        this.tester = tester;
        this.converter = converter;
    }

    @Override
    protected @NotNull T getPathSetting(@NotNull String path) {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        T value = null;
        if (tester.test(section, path)) {
            value = converter.apply(section, path);
        }

        if (value == null && tester.test(section, this.path)) {
            value = converter.apply(section, this.path);
        }

        if (value == null) {
            value = defaultValue;
        }

        cache.put(path, value);
        return value;
    }

}
