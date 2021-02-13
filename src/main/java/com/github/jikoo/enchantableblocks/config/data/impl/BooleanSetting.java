package com.github.jikoo.enchantableblocks.config.data.impl;

import com.github.jikoo.enchantableblocks.config.data.ConfigSetting;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * A setting representing a boolean value.
 */
public class BooleanSetting extends ConfigSetting<Boolean> {

    public BooleanSetting(@NotNull ConfigurationSection section, @NotNull String key, boolean defaultValue) {
        super(section, key, ConfigurationSection::isBoolean, ConfigurationSection::getBoolean, defaultValue);
    }

}
