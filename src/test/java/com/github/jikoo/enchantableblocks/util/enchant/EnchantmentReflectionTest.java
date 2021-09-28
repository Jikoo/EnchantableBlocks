package com.github.jikoo.enchantableblocks.util.enchant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import be.seeseemelk.mockbukkit.MockBukkit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for unsupported new enchantments.
 *
 * <p>As a developer, I want to be able to support new enchantments
 * without additional maintanence.
 *
 * <p><b>Feature:</b> Sanely support unexpected enchantments
 * <br><b>Given</b> I am a developer
 * <br><b>When</b> I attempt to obtain information about a new enchantment
 * <br><b>Then</b> the information should be available or a sane default will be provided
 */
@DisplayName("Feature: Attempt support for unknown enchantments.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantmentReflectionTest {

  @BeforeAll
  void beforeAll() {
    MockBukkit.mock();

    EnchantmentHelper.getRegisteredEnchantments().stream().map(enchantment -> {
      // Keep mending default to check fallthrough.
      if (enchantment.equals(Enchantment.MENDING)) {
        return enchantment;
      }
      EnchantData data = EnchantData.of(enchantment);
      return new FakeNmsEnchant(enchantment, data.getWeight(),
          data::getMinEffectiveLevel, data::getMaxEffectiveLevel);
    }).forEach(enchantment -> EnchantmentHelper.putEnchant(enchantment.getKey(), enchantment));
    EnchantmentHelper.setupToolEnchants();
  }

  static Stream<Enchantment> enchantmentStream() {
    return EnchantmentHelper.getRegisteredEnchantments().stream();
  }

  private int getRandomLevel(Enchantment enchantment) {
    return enchantment.getMaxLevel() > 0 ?
        ThreadLocalRandom.current().nextInt(enchantment.getMaxLevel()) + 1 : 1;
  }

  @DisplayName("Reflection should grab minimum quality method or fall through gracefully.")
  @ParameterizedTest
  @MethodSource("enchantmentStream")
  void testReflectiveMin(Enchantment enchantment) {
    // Mending is not set up so that it can be used to test fallthrough.
    if (enchantment.equals(Enchantment.MENDING)) {
      return;
    }
    EnchantData enchantData = EnchantData.of(enchantment);
    int level = getRandomLevel(enchantment);
    assertThat("Reflection should provide expected value",
        enchantData.getMinEffectiveLevel(level),
        is(EnchantDataReflection.getMinEnchantQuality(enchantment).applyAsInt(level)));
  }

  @DisplayName("Reflection should grab maximum quality method or fall through gracefully.")
  @ParameterizedTest
  @MethodSource("enchantmentStream")
  void testReflectiveMax(Enchantment enchantment) {
    // Mending is not set up so that it can be used to test fallthrough.
    if (enchantment.equals(Enchantment.MENDING)) {
      return;
    }
    EnchantData enchantData = EnchantData.of(enchantment);
    int level = getRandomLevel(enchantment);
    assertThat("Reflection should provide expected value",
        enchantData.getMaxEffectiveLevel(level),
        is(EnchantDataReflection.getMaxEnchantQuality(enchantment).applyAsInt(level)));
  }

  @DisplayName("Reflection should grab minimum method or fall through gracefully.")
  @ParameterizedTest
  @MethodSource("enchantmentStream")
  void testReflectiveRarity(Enchantment enchantment) {
    // Mending is not set up so that it can be used to test fallthrough.
    if (enchantment.equals(Enchantment.MENDING)) {
      return;
    }
    EnchantData enchantData = EnchantData.of(enchantment);
    assertThat("Reflection should provide expected value",
        enchantData.getRarity(), is(EnchantDataReflection.getRarity(enchantment)));
  }

  @DisplayName("Reflection should provide usable defaults.")
  @Test
  void testReflectiveDefaults() {
    Enchantment untouched = Enchantment.MENDING;
    assertThat("Reflection should fall through gracefully", 1,
        is(EnchantDataReflection.getMinEnchantQuality(untouched).applyAsInt(0)));
    assertThat("Reflection should fall through gracefully", 21,
        is(EnchantDataReflection.getMinEnchantQuality(untouched).applyAsInt(2)));
    assertThat("Reflection should fall through gracefully", 6,
        is(EnchantDataReflection.getMaxEnchantQuality(untouched).applyAsInt(0)));
    assertThat("Reflection should fall through gracefully", 26,
        is(EnchantDataReflection.getMaxEnchantQuality(untouched).applyAsInt(2)));
    assertThat("Reflection should fall through gracefully", 0,
        is(EnchantDataReflection.getRarity(untouched).getWeight()));
  }

  @AfterAll
  void afterAll() {
    MockBukkit.unmock();
  }

}
