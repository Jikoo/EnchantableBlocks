package com.github.jikoo.enchantableblocks.config.data;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * An enum setting parsed by name.
 *
 * @param <T> the type of enum
 */
public class EnumWorldSetting<T extends Enum<?>> extends ParsedSimpleWorldSetting<T> {

    public EnumWorldSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull T defaultValue) {
        super(section, key, enumName -> ValueConverters.toEnum(defaultValue, enumName), defaultValue);
    }

}
