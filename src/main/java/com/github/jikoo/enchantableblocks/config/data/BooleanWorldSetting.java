package com.github.jikoo.enchantableblocks.config.data;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * A world setting representing a boolean value.
 */
public class BooleanWorldSetting extends ConfigWorldSetting<Boolean> {

    public BooleanWorldSetting(@NotNull ConfigurationSection section, @NotNull String key, boolean defaultValue) {
        super(section, key, ConfigurationSection::isBoolean, ConfigurationSection::getBoolean, defaultValue);
    }

}
