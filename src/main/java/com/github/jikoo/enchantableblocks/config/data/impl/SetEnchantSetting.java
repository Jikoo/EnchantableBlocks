package com.github.jikoo.enchantableblocks.config.data.impl;

import com.github.jikoo.enchantableblocks.config.data.SimpleSetSetting;
import com.github.jikoo.enchantableblocks.config.data.ValueConverters;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A setting for a {@code Set<Enchantment>}.
 */
public class SetEnchantSetting extends SimpleSetSetting<Enchantment> {

    public SetEnchantSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull Set<Enchantment> defaultValue) {
        super(section, key, defaultValue);
    }

    @Override
    protected @Nullable Enchantment convertValue(@NotNull String value) {
        return ValueConverters.toEnchant(value);
    }

}
