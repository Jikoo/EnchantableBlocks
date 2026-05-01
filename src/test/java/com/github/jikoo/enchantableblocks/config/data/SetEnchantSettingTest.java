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

import java.util.Set;

import static com.github.jikoo.enchantableblocks.mock.matcher.EnchantMatchers.enchant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Config: Setting for a Set of Enchantments")
@TestInstance(Lifecycle.PER_CLASS)
class SetEnchantSettingTest {

  private MockedStatic<Bukkit> bukkit;
  private SetEnchantSetting setting;

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
  }

  @AfterAll
  void tearDown() {
    bukkit.close();
  }

  @BeforeEach
  void beforeEach() {
    var section = mock(ConfigurationSection.class);
    setting = new SetEnchantSetting(section, "test", Set.of());
  }

  @DisplayName("Values are converted to enchantments.")
  @Test
  void testConvertValue() {
    Enchantment original = Enchantment.UNBREAKING;
    doReturn(NamespacedKey.minecraft("unbreaking")).when(original).getKeyOrThrow();
    Enchantment converted = setting.convertValue(original.getKeyOrThrow().toString());
    assertThat("Value is converted to enchantment", converted, is(enchant(original)));
  }

}