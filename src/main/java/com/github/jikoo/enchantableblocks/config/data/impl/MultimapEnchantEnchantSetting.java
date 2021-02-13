package com.github.jikoo.enchantableblocks.config.data.impl;

import com.github.jikoo.enchantableblocks.config.data.SimpleMultimapSetting;
import com.github.jikoo.enchantableblocks.config.data.ValueConverters;
import com.google.common.collect.Multimap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A setting for a {@code Multimap<Enchantment, Enchantment>}.
 */
public class MultimapEnchantEnchantSetting extends SimpleMultimapSetting<Enchantment, Enchantment> {

    public MultimapEnchantEnchantSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull Multimap<Enchantment, Enchantment> defaultValue) {
        super(section, key, defaultValue);
    }

    @Override
    protected @Nullable Enchantment convertKey(@NotNull String key) {
        return ValueConverters.toEnchant(key);
    }

    @Override
    protected @Nullable Enchantment convertValue(@NotNull String value) {
        return ValueConverters.toEnchant(value);
    }

}
