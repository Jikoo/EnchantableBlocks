package com.github.jikoo.enchantableblocks.config.data;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A ParsedSetting that fetches its value from a single string.
 *
 * @param <T>
 */
public abstract class ParsedSimpleSetting<T> extends ParsedSetting<T> {

    protected ParsedSimpleSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull T defaultValue) {
        super(section, key, defaultValue);
    }

    @Override
    protected boolean test(@NotNull String path) {
        return section.isString(path);
    }

    @Override
    protected @Nullable T convert(@NotNull String path) {
        return convertString(section.getString(path));
    }

    protected abstract @Nullable T convertString(@Nullable String value);

}
