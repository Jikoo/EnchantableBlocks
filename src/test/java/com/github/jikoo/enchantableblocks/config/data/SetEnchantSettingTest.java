package com.github.jikoo.enchantableblocks.config.data;

import static com.github.jikoo.enchantableblocks.mock.matcher.EnchantMatchers.enchant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.enchantments.EnchantmentMocks;
import java.util.Set;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@DisplayName("Config: Setting for a Set of Enchantments")
@TestInstance(Lifecycle.PER_CLASS)
class SetEnchantSettingTest {

  private SetEnchantSetting setting;

  @BeforeAll
  void beforeAll() {
    Server server = ServerMocks.mockServer();
    EnchantmentMocks.init(server);
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
    Enchantment converted = setting.convertValue(original.getKey().toString());
    assertThat("Value is converted to enchantment", converted, is(enchant(original)));
  }

}