package com.github.jikoo.enchantableblocks.config.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import com.github.jikoo.planarenchanting.table.Enchantability;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@DisplayName("Config: Setting for Enchantability values.")
@TestInstance(Lifecycle.PER_CLASS)
class EnchantabilitySettingTest {

  private static final Enchantability DEFAULT_VALUE = Enchantability.GOLD_ARMOR;

  EnchantabilitySetting setting;

  @BeforeEach
  void beforeEach() {
    var section = mock(ConfigurationSection.class);
    setting = new EnchantabilitySetting(section, "test", DEFAULT_VALUE);
  }

  @DisplayName("Null yields null.")
  @Test
  void testConvertNull() {
    assertThat("Null yields null", setting.convertString(null), is(nullValue()));
  }

  @DisplayName("Number yields corresponding Enchantability.")
  @Test
  void testConvertNumber() {
    assertThat("Number is parsed", setting.convertString("5"), is(new Enchantability(5)));
  }

  @DisplayName("Numbers below zero are clamped to allowed range.")
  @Test
  void testConvertIllegalNumber() {
    assertThat(
        "Number is always at least minimum",
        setting.convertString("0"),
        is(new Enchantability(1)));
  }

  @DisplayName("Invalid Enchantability name yields null.")
  @Test
  void testConvertFieldNameMissing() {
    assertThat(
        "Invalid field is null",
        setting.convertString("not a field"),
        is(nullValue()));
  }

  @DisplayName("Enchantability can be parsed by name.")
  @Test
  void testConvertFieldName() {
    assertThat(
        "Valid field name returns field",
        setting.convertString("STONE"),
        is(Enchantability.STONE));
  }

  @DisplayName("Non-Enchantability fields yield null.")
  @Test
  void testConvertFieldNameNotEnchantability() {
    // Note that Enchantability doesn't have any public fields that aren't Enchantability instances.
    // Since we don't set the field accessible, we can't hit the instanceof check.
    // This test is technically useless, but it's good to be mindful of.
    assertThat(
        "Valid non-Enchantability field returns null",
        setting.convertString("BY_MATERIAL"),
        is(nullValue()));
  }

}