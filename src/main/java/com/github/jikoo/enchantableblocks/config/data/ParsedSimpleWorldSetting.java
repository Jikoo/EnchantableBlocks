package com.github.jikoo.enchantableblocks.config.data;

import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A ParsedWorldSetting that fetches its value from a single string.
 *
 * @param <T>
 */
public abstract class ParsedSimpleWorldSetting<T> extends ParsedWorldSetting<T> {

    public ParsedSimpleWorldSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull Function<@Nullable String, @Nullable T> converter,
            @NotNull T defaultValue) {
        super(section,
                key,
                ConfigurationSection::isString,
                (section1, path) -> converter.apply(section1.getString(path)),
                defaultValue);
    }

}
