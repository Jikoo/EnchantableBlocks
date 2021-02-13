package com.github.jikoo.enchantableblocks.config.data.impl;

import com.github.jikoo.enchantableblocks.config.data.ParsedSimpleSetting;
import com.github.jikoo.enchantableblocks.config.data.ValueConverters;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An enum setting parsed by name.
 *
 * @param <T> the type of enum
 */
public class EnumSetting<T extends Enum<?>> extends ParsedSimpleSetting<T> {

    public EnumSetting(@NotNull ConfigurationSection section, @NotNull String key, @NotNull T defaultValue) {
        super(section, key, defaultValue);
    }

    @Override
    protected @Nullable T convertString(@Nullable String value) {
        return ValueConverters.toEnum(defaultValue, value);
    }

}
