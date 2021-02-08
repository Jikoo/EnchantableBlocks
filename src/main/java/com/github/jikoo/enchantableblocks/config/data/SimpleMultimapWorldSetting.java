package com.github.jikoo.enchantableblocks.config.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Multimap representation of a ConfigurationSection containing multiple lists of strings.
 *
 * @param <K> the type of key used by the Multimap
 * @param <V> the type of value stored in the Multimap
 */
public class SimpleMultimapWorldSetting<K, V> extends ParsedComplexWorldSetting<Multimap<K, V>> {

    public SimpleMultimapWorldSetting(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull Function<@NotNull String, @Nullable K> keyConverter,
            @NotNull Function<@Nullable String, @Nullable V> valueConverter,
            @NotNull Multimap<K, V> defaultValue) {
        super(section, key, section1 -> {
            if (section1 == null) {
                return null;
            }
            Multimap<K, V> multimap = HashMultimap.create();
            for (String section1Key : section1.getKeys(true)) {
                K convertedKey = keyConverter.apply(section1Key);
                if (convertedKey == null || !section1.isList(section1Key)) {
                    continue;
                }

                for (String rawValue : section1.getStringList(section1Key)) {
                    V convertedValue = valueConverter.apply(rawValue);
                    if (convertedValue != null) {
                        multimap.put(convertedKey, convertedValue);
                    }
                }
            }

            return multimap;
        }, defaultValue);
    }

}
