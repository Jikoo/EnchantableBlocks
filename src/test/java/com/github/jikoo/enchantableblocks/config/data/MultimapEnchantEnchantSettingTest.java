package com.github.jikoo.enchantableblocks.config.data;

import com.google.common.collect.HashMultimap;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Config: Setting for a Multimap of Enchantments to Enchantments.")
@TestInstance(Lifecycle.PER_CLASS)
class MultimapEnchantEnchantSettingTest {

  private MockedStatic<Bukkit> bukkit;
  private Enchantment enchantment;
  private MultimapEnchantEnchantSetting setting;

  @BeforeAll
  void setUp() {
    bukkit = mockStatic();
    bukkit.when(() -> Bukkit.getRegistry(any())).thenAnswer(invocation -> {
      Registry<?> registry = mock(Registry.class);
      if (Enchantment.class.isAssignableFrom(invocation.getArgument(0))) {
        doAnswer(invocation1 -> {
          Enchantment localEnch = mock(Enchantment.class);
          NamespacedKey key = invocation1.getArgument(0);
          doReturn(key).when(localEnch).getKey();
          doReturn(key).when(localEnch).getKeyOrThrow();
          return localEnch;
        }).when(registry).getOrThrow(any());
        doAnswer(invocation1 -> registry.getOrThrow(invocation1.getArgument(0))).when(registry).get(any());
      }
      return registry;
    });

    enchantment = Enchantment.UNBREAKING;
  }

  @AfterAll
  void tearDown() {
    bukkit.close();
  }

  @BeforeEach
  void beforeEach() {
    var section = mock(ConfigurationSection.class);
    setting = new MultimapEnchantEnchantSetting(section, "test", HashMultimap.create());
  }

  @DisplayName("Keys are converted to enchantments.")
  @Test
  void testConvertKey() {
    Enchantment converted = setting.convertKey(enchantment.getKeyOrThrow().toString());
    assertThat("Key is converted to enchantment", converted, is(enchant(enchantment)));
  }

  @DisplayName("Values are converted to enchantments.")
  @Test
  void testConvertValue() {
    Enchantment converted = setting.convertValue(enchantment.getKeyOrThrow().toString());
    assertThat("Value is converted to enchantment", converted, is(enchant(enchantment)));
  }

}