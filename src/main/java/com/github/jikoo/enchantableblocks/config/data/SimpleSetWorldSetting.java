package com.github.jikoo.enchantableblocks.config.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A parsed world setting representing a Set of a simple objects.
 *
 * <p>The "set" configuration-side is a list of strings.
 *
 * @param <T> the type of object in the set
 */
public class SimpleSetWorldSetting<T> extends ParsedWorldSetting<Set<T>> {

    public SimpleSetWorldSetting(@NotNull ConfigurationSection section,
            @NotNull String key,
            @NotNull Function<@Nullable String, @Nullable T> converter,
            @NotNull Set<T> defaultValue) {
        super(section,
                key,
                ConfigurationSection::isList,
                (section1, path) -> {
                    HashSet<T> convertedSet = new HashSet<>();
                    List<String> values = section1.getStringList(path);
                    for (String value : values) {
                        T converted = converter.apply(value);
                        if (converted != null) {
                            convertedSet.add(converted);
                        }
                    }
                    return Collections.unmodifiableSet(convertedSet);
                },
                defaultValue);
    }

}
