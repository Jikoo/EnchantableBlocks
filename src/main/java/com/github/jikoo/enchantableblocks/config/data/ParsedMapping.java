package com.github.jikoo.enchantableblocks.config.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ParsedMapping<K, V> extends Mapping<K, V> {

    private final Map<String, Map<K, V>> cache = new HashMap<>();

    protected ParsedMapping(@NotNull ConfigurationSection section,
            @NotNull String path,
            @NotNull Function<@NotNull K, @NotNull V> defaultValue) {
        super(section, path, defaultValue);
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
            K parsedKey = convertKey(rawKey);

            if (parsedKey == null || !testValue(localSection, rawKey)) {
                continue;
            }

            V parsedValue = convertValue(localSection, rawKey);
            if (parsedValue != null) {
                mappings.put(parsedKey, parsedValue);
            }
        }

        cache.put(path, mappings.isEmpty() ? null : mappings);

        return mappings.get(key);
    }

    protected abstract @Nullable K convertKey(@NotNull String key);

    protected abstract boolean testValue(@NotNull ConfigurationSection localSection, @NotNull String path);

    protected abstract @Nullable V convertValue(@NotNull ConfigurationSection localSection, @NotNull String path);

}
