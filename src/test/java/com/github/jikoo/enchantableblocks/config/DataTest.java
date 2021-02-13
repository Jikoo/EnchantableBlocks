package com.github.jikoo.enchantableblocks.config;

import com.github.jikoo.enchantableblocks.config.data.impl.BooleanSetting;
import com.github.jikoo.enchantableblocks.config.data.impl.EnumSetting;
import com.github.jikoo.enchantableblocks.config.data.ParsedMapping;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Feature: Elements used for simple configuration parsing")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataTest {

    @DisplayName("Boolean world setting should be retrievable as expected")
    @ParameterizedTest
    @CsvSource({ "world,true", "boring_world,false" })
    void testBooleanWorldSetting(String worldName, boolean value) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world_overrides." + worldName + ".key", value);

        BooleanSetting key = new BooleanSetting(config, "key", !value);
        assertThat("World override must be used", key.get(worldName), is(value));
        assertThat("Invalid world must fall through", key.get("%not-a-world%"), is(not(value)));
    }

    @DisplayName("Enum world setting should be retrievable as expected")
    @ParameterizedTest
    @CsvSource({ "world,STONE,AIR", "second_world,COBBLESTONE,DIAMOND_BLOCK" })
    void testEnumWorldSetting(String worldName, Material value, Material defaultVal) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world_overrides." + worldName + ".key", value.name());

        EnumSetting<Material> key = new EnumSetting<>(config, "key", defaultVal);
        assertThat("World override must be used", key.get(worldName), is(value));
        assertThat("Invalid world must fall through", key.get("%not-a-world%"), is(defaultVal));
    }

    @DisplayName("Data holders should not allow using world override path as a key.")
    @Test
    void testBlockedKey() {
        String worldOverrides = "world_overrides";
        YamlConfiguration config = new YamlConfiguration();

        assertThrows(IllegalArgumentException.class, () ->
                new EnumSetting<>(config, worldOverrides, Material.AIR));

        Function<String, String> keyConverter = Function.identity();

        assertThrows(IllegalArgumentException.class, () ->
                new ParsedMapping<String, String>(config, worldOverrides, keyConverter) {
                    @Override
                    protected @NotNull String convertKey(@NotNull String key) {
                        return key;
                    }

                    @Override
                    protected boolean testValue(@NotNull ConfigurationSection localSection, @NotNull String path) {
                        return localSection.isString(path);
                    }

                    @Override
                    protected @Nullable String convertValue(@NotNull ConfigurationSection localSection, @NotNull String path) {
                        return localSection.getString(path);
                    }
                });
    }

}
