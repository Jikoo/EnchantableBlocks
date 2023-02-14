package com.github.jikoo.enchantableblocks.config.data;

import static com.github.jikoo.enchantableblocks.mock.matcher.EnchantMatchers.enchant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import com.github.jikoo.enchantableblocks.mock.enchantments.EnchantmentMocks;
import com.google.common.collect.HashMultimap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@DisplayName("Config: Setting for a Multimap of Enchantments to Enchantments.")
@TestInstance(Lifecycle.PER_CLASS)
class MultimapEnchantEnchantSettingTest {

  private MultimapEnchantEnchantSetting setting;

  @BeforeAll
  void beforeAll() {
    EnchantmentMocks.init();
  }

  @BeforeEach
  void beforeEach() {
    var section = mock(ConfigurationSection.class);
    setting = new MultimapEnchantEnchantSetting(section, "test", HashMultimap.create());
  }

  @DisplayName("Keys are converted to enchantments.")
  @Test
  void testConvertKey() {
    Enchantment original = Enchantment.DURABILITY;
    Enchantment converted = setting.convertKey(original.getKey().toString());
    assertThat("Key is converted to enchantment", converted, is(enchant(original)));
  }

  @DisplayName("Values are converted to enchantments.")
  @Test
  void testConvertValue() {
    Enchantment original = Enchantment.DURABILITY;
    Enchantment converted = setting.convertValue(original.getKey().toString());
    assertThat("Value is converted to enchantment", converted, is(enchant(original)));
  }

}