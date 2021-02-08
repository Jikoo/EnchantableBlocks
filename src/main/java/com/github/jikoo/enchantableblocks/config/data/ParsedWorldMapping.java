package com.github.jikoo.enchantableblocks.config.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParsedWorldMapping<K, V> extends WorldMapping<K, V> {

    private final Map<String, Map<K, V>> cache = new HashMap<>();
    private final Function<@NotNull String, @Nullable K> keyConverter;
    private final BiPredicate<@NotNull ConfigurationSection, @NotNull String> valueTester;
    private final BiFunction<@NotNull ConfigurationSection, @NotNull String, @Nullable V> valueConverter;

    public ParsedWorldMapping(@NotNull ConfigurationSection section,
            @NotNull String path,
            @NotNull Function<@NotNull String, @Nullable K> keyConverter,
            @NotNull BiPredicate<@NotNull ConfigurationSection, @NotNull String> valueTester,
            @NotNull BiFunction<@NotNull ConfigurationSection, @NotNull String, @Nullable V> valueConverter,
            @NotNull Function<@NotNull K, @NotNull V> defaultValue) {
        super(section, path, defaultValue);
        this.keyConverter = keyConverter;
        this.valueTester = valueTester;
        this.valueConverter = valueConverter;
    }

    @Override
    protected @Nullable V getPathSetting(@NotNull String path, @NotNull K key) {
        if (cache.containsKey(path)) {
            Map<K, V> cachedMap = cache.get(path);
            if (cachedMap != null && cachedMap.containsKey(key)) {
                return cachedMap.get(key);
            }
            return null;
        }

        if (!section.isConfigurationSection(path)) {
            cache.put(path, null);
            return null;
        }

        ConfigurationSection localSection = section.getConfigurationSection(path);
        assert localSection != null;

        Map<K, V> mappings = new HashMap<>();
        for (String rawKey : localSection.getKeys(true)) {
            K parsedKey = keyConverter.apply(rawKey);

            if (parsedKey == null || !valueTester.test(localSection, rawKey)) {
                continue;
            }

            V parsedValue = valueConverter.apply(localSection, rawKey);
            if (parsedValue != null) {
                mappings.put(parsedKey, parsedValue);
            }
        }

        cache.put(path, mappings.isEmpty() ? null : mappings);

        return mappings.get(key);
    }

}
