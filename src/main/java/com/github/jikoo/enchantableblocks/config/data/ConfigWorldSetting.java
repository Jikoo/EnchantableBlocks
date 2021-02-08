package com.github.jikoo.enchantableblocks.config.data;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A generic world setting that is retrievable directly from a ConfigurationSection.
 *
 * @param <T> the type of value stored
 */
public abstract class ConfigWorldSetting<T> extends WorldSetting<T> {

    private final BiPredicate<@NotNull ConfigurationSection, @NotNull String> tester;
    private final BiFunction<@NotNull ConfigurationSection, @NotNull String, T> getter;

    public ConfigWorldSetting(@NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull BiPredicate<@NotNull ConfigurationSection, @NotNull String> tester,
            @NotNull BiFunction<@NotNull ConfigurationSection, @NotNull String, T> getter,
            @NotNull T defaultValue) {
        super(section, key, defaultValue);
        this.tester = tester;
        this.getter = getter;
    }

    protected @Nullable T getPathSetting(@NotNull String path) {
        if (tester.test(section, path)) {
            return getter.apply(section, path);
        }

        return null;
    }

}
