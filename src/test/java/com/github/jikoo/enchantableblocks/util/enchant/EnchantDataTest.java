package com.github.jikoo.enchantableblocks.util.enchant;

import static org.hamcrest.MatcherAssert.assertThat;

import be.seeseemelk.mockbukkit.enchantments.EnchantmentsMock;
import java.util.Arrays;
import java.util.stream.Stream;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Feature: Expose extra enchantment data.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantDataTest {

  @BeforeAll
  void setUpAll() {
    EnchantmentsMock.registerDefaultEnchantments();
  }

  @DisplayName("Enchantments must be explicitly supported.")
  @ParameterizedTest
  @MethodSource("getEnchants")
  void isPresent(Enchantment enchantment) {
    assertThat("Enchantment must be supported", EnchantData.isPresent(enchantment));
  }

  static Stream<Enchantment> getEnchants() {
    return Arrays.stream(Enchantment.values());
  }

}