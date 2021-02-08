package com.github.jikoo.enchantableblocks.config.data;

import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A more complex world setting that parses a value from an entire ConfigurationSection.
 *
 * @param <T> the type of value stored
 */
public abstract class ParsedComplexWorldSetting<T> extends ParsedWorldSetting<T> {

    protected ParsedComplexWorldSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull Function<@Nullable ConfigurationSection, @Nullable T> converter,
            @NotNull T defaultValue) {
        super(section,
                key,
                ConfigurationSection::isConfigurationSection,
                ((section1, path) -> converter.apply(section1.getConfigurationSection(path))),
                defaultValue);
    }

}
