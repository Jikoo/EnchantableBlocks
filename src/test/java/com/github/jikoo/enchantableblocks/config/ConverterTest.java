package com.github.jikoo.enchantableblocks.config;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.jikoo.enchantableblocks.config.data.ValueConverters;
import com.github.jikoo.enchantableblocks.util.enchant.Enchantability;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Feature: Value converters should provide values for strings.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConverterTest {

    @BeforeAll
    void beforeAll() {
        MockBukkit.mock();
    }

    @Test
    void testEnumNull() {
        assertThat("Value must be null.",
                ValueConverters.toEnum(Enchantability.BOOK, null), nullValue());
    }

    @Test
    void testEnumNotPresent() {
        assertThat("Value must be null.",
                ValueConverters.toEnum(Enchantability.BOOK, "invalid"), nullValue());
    }

    @Test
    void testEnumPresent() {
        Enchantability enchantability = Enchantability.STONE;
        assertThat("Value must be obtained correctly.",
                ValueConverters.toEnum(Enchantability.BOOK, enchantability.name()), is(enchantability));
    }

    @Test
    void testKeyedNull() {
        assertThat("Value must be null.",
                ValueConverters.toEnchant(null), nullValue());
    }

    @Test
    void testKeyedInvalid() {
        assertThat("Value must be null.", ValueConverters.toEnchant("bad key:good_value"), nullValue());
        assertThat("Value must be null.", ValueConverters.toEnchant("good_key:bad value"), nullValue());
        assertThat("Value must be null.", ValueConverters.toEnchant("bad value"), nullValue());
        assertThat("Value must be null.", ValueConverters.toEnchant("not_an_enchantment"), nullValue());
    }

    @Test
    void testKeyedPresent() {
        Enchantment enchantment = Enchantment.SILK_TOUCH;
        assertThat("Value must be obtained from namespaced key.",
                ValueConverters.toEnchant(enchantment.getKey().toString()), is(enchantment));
        assertThat("Value must be obtained from un-namespaced key.",
                ValueConverters.toEnchant(enchantment.getKey().getKey()), is(enchantment));
    }

    @Test
    void testMaterialNull() {
        assertThat("Value must be null.", ValueConverters.toMaterial(null), nullValue());
    }

    @Test
    void testMaterialNotPresent() {
        assertThat("Value must be null.", ValueConverters.toMaterial("invalid"), nullValue());
    }

    @Test
    void testMaterialPresent() {
        Material material = Material.GOLD_ORE;
        assertThat("Value must be obtained from namespaced key.",
                ValueConverters.toMaterial(material.getKey().toString()), is(material));
        assertThat("Value must be obtained from un-namespaced key.",
                ValueConverters.toMaterial(material.getKey().getKey()), is(material));
        assertThat("Value must be obtained from raw name.",
                ValueConverters.toMaterial(material.name()), is(material));
        assertThat("Value must be obtained from friendly name.",
                ValueConverters.toMaterial(material.name().replace('_', ' ')), is(material));
    }

    @AfterAll
    void afterAll() {
        MockBukkit.unmock();
    }

}
