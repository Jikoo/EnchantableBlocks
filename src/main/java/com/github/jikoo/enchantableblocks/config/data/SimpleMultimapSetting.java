package com.github.jikoo.enchantableblocks.config.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Multimap representation of a ConfigurationSection containing multiple lists of strings.
 *
 * @param <K> the type of key used by the Multimap
 * @param <V> the type of value stored in the Multimap
 */
public abstract class SimpleMultimapSetting<K, V> extends ParsedComplexSetting<Multimap<K, V>> {

    protected SimpleMultimapSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull Multimap<K, V> defaultValue) {
        super(section, key, defaultValue);
    }

    @Override
    protected @Nullable Multimap<K, V> convert(@Nullable ConfigurationSection value) {
        Multimap<K, V> multimap = HashMultimap.create();

        // Section is set, just isn't a parseable ConfigurationSection.
        if (value == null) {
            return multimap;
        }

        for (String section1Key : value.getKeys(true)) {
            K convertedKey = convertKey(section1Key);
            if (convertedKey == null || !value.isList(section1Key)) {
                continue;
            }

            for (String rawValue : value.getStringList(section1Key)) {
                V convertedValue = convertValue(rawValue);
                if (convertedValue != null) {
                    multimap.put(convertedKey, convertedValue);
                }
            }
        }

        return multimap;
    }

    protected abstract @Nullable K convertKey(@NotNull String key);

    protected abstract @Nullable V convertValue(@NotNull String value);

}
