package com.github.jikoo.enchantableblocks.config.data;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.MockedStatic;

import static com.github.jikoo.enchantableblocks.mock.matcher.EnchantMatchers.enchant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Config: Mapping for Enchantments to integers.")
@TestInstance(Lifecycle.PER_CLASS)
class EnchantMaxLevelMappingTest {

  private MockedStatic<Bukkit> bukkit;
  private EnchantMaxLevelMapping mapping;

  @BeforeAll
  void setUp() {
    bukkit = mockStatic();
    bukkit.when(() -> Bukkit.getRegistry(any())).thenAnswer(invocation -> {
      Registry<?> registry = mock(Registry.class);
      if (Enchantment.class.isAssignableFrom(invocation.getArgument(0))) {
        doAnswer(invocation1 -> {
          Enchantment enchantment = mock(Enchantment.class);
          NamespacedKey key = invocation1.getArgument(0);
          doReturn(key).when(enchantment).getKey();
          doReturn(key).when(enchantment).getKeyOrThrow();
          return enchantment;
        }).when(registry).getOrThrow(any());
        doAnswer(invocation1 -> registry.getOrThrow(invocation1.getArgument(0))).when(registry).get(any());
      }
      return registry;
    });
    Enchantment enchantment = Enchantment.UNBREAKING;
    doReturn(3).when(enchantment).getMaxLevel();
  }

  @AfterAll
  void tearDown() {
    bukkit.close();
  }

  @BeforeEach
  void beforeEach() {
    var section = mock(ConfigurationSection.class);
    mapping = new EnchantMaxLevelMapping(section, "test");
  }

  @DisplayName("Keys are converted to enchantments.")
  @Test
  void testConvertKey() {
    Enchantment original = Enchantment.UNBREAKING;
    Enchantment converted = mapping.convertKey(original.getKeyOrThrow().toString());
    assertThat("Key is converted to enchantment", converted, is(enchant(original)));
  }

  @DisplayName("Value states are handled by ConfigurationSection.")
  @Test
  void testTestValue() {
    var section = mock(ConfigurationSection.class);
    doReturn(true).when(section).isInt(anyString());

    verify(section, times(0)).isInt(anyString());
    assertThat("Path is tested against config", mapping.testValue(section, "test"), is(true));
    verify(section).isInt(anyString());
  }

  @DisplayName("Values are handled by ConfigurationSection.")
  @Test
  void testConvertValue() {
    var section = mock(ConfigurationSection.class);
    doReturn(1).when(section).getInt(anyString());

    verify(section, times(0)).getInt(anyString());
    assertThat("Path is tested against config", mapping.convertValue(section, "test"), is(1));
    verify(section).getInt(anyString());
  }

  @DisplayName("Defaults are fetched from server.")
  @Test
  void testDefaultMaxLevel() {
    Enchantment enchantment = Enchantment.UNBREAKING;
    assertThat(
        "Max level must fall through to enchantment default",
        mapping.get("none", enchantment),
        is(enchantment.getMaxLevel()));
  }

}