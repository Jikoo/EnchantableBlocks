package com.github.jikoo.enchantableblocks.config.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.github.jikoo.planarenchanting.table.Enchantability;
import java.util.Arrays;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(Lifecycle.PER_METHOD)
class EnchantabilitySettingTest {

  private static final Enchantability DEFAULT_VALUE = Enchantability.GOLD_ARMOR;

  EnchantabilitySetting setting;

  @BeforeEach
  void beforeEach() {
    var section = new YamlConfiguration();
    section.set("overrides.numberValue.test", "5");
    section.set("overrides.field.test", "STONE");
    section.set("overrides.notField.test", "NOT_A_FIELD");
    section.set("overrides.badField.test", "BY_MATERIAL");

    setting = new EnchantabilitySetting(section, "test", DEFAULT_VALUE);
  }

  @Test
  void testNullConvert() {
    assertThat("Null yields null", setting.convertString(null), is(nullValue()));
  }

  @ParameterizedTest
  @MethodSource("getSettingPaths")
  void testValue(@NotNull String override, @NotNull Enchantability expected) {
    assertThat("Value must be expected", setting.get(override), is(expected));
  }

  static Stream<Arguments> getSettingPaths() {
    return Arrays.stream(new Arguments[] {
        Arguments.of("numberValue", new Enchantability(5)),
        Arguments.of("field", Enchantability.STONE),
        Arguments.of("notField", DEFAULT_VALUE),
        Arguments.of("badField", DEFAULT_VALUE)
    });
  }

}