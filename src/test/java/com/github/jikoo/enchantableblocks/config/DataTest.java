package com.github.jikoo.enchantableblocks.config;

import com.github.jikoo.enchantableblocks.config.data.BooleanWorldSetting;
import com.github.jikoo.enchantableblocks.config.data.EnumWorldSetting;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@DisplayName("Feature: Elements used for simple configuration parsing")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataTest {

    @DisplayName("Boolean world setting should be retrievable as expected")
    @ParameterizedTest
    @CsvSource({ "world,true", "boring_world,false" })
    void testBooleanWorldSetting(String worldName, boolean value) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world_overrides." + worldName + ".key", value);

        BooleanWorldSetting key = new BooleanWorldSetting(config, "key", !value);
        assertThat("World override must be used", key.get(worldName), is(value));
        assertThat("Invalid world must fall through", key.get("%not-a-world%"), is(not(value)));
    }

    @DisplayName("Enum world setting should be retrievable as expected")
    @ParameterizedTest
    @CsvSource({ "world,STONE,AIR", "second_world,COBBLESTONE,DIAMOND_BLOCK" })
    void testEnumWorldSetting(String worldName, Material value, Material defaultVal) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world_overrides." + worldName + ".key", value.name());

        EnumWorldSetting<Material> key = new EnumWorldSetting<>(config, "key", defaultVal);
        assertThat("World override must be used", key.get(worldName), is(value));
        assertThat("Invalid world must fall through", key.get("%not-a-world%"), is(defaultVal));
    }

}
