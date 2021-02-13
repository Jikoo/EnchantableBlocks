package com.github.jikoo.enchantableblocks.config.data;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A more complex world setting that parses a value from an entire ConfigurationSection.
 *
 * @param <T> the type of value stored
 */
public abstract class ParsedComplexSetting<T> extends ParsedSetting<T> {

    protected ParsedComplexSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull T defaultValue) {
        super(section, key, defaultValue);
    }

    @Override
    protected boolean test(@NotNull String path) {
        return section.contains(path);
    }

    @Override
    protected @Nullable T convert(@NotNull String path) {
        return convert(section.getConfigurationSection(path));
    }

    protected abstract @Nullable T convert(@Nullable ConfigurationSection value);

}
