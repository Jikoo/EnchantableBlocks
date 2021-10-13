package com.github.jikoo.enchantableblocks.util.enchant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for enchantments.
 *
 * <p>As a developer, I want to be able to generate enchantments
 * because I would like to support enchanting tables.
 *
 * <p><b>Feature:</b> Calculate enchantments for special items
 * <br><b>Given</b> I am a user
 * <br><b>When</b> I attempt to enchant an item
 * <br><b>And</b> the item is a special item
 * <br><b>Then</b> the item should recieve applicable enchantments
 */
@DisplayName("Feature: Calculate enchantments for enchanting tables.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantingTableTest {

  private static final Collection<Enchantment> TOOL_ENCHANTS = Arrays.asList(
      Enchantment.DIG_SPEED, Enchantment.DURABILITY, Enchantment.LOOT_BONUS_BLOCKS,
      Enchantment.SILK_TOUCH);

  @BeforeAll
  void beforeAll() {
    MockBukkit.mock();
    EnchantmentHelper.setupToolEnchants();
  }

  @DisplayName("Enchantability should be able to be converted from magic values.")
  @ParameterizedTest
  @CsvSource({"-10,BOOK", "5,STONE", "50,GOLD_ARMOR", "15,LEATHER"})
  void testEnchantabilityConvert(int number, Enchantability enchantability) {
    assertThat("Enchantability should match expected", enchantability,
        is(Enchantability.convert(number)));
  }

  @DisplayName("When enchantments are selected.")
  @Nested
  class EnchantmentAttempt {

    private Map<Enchantment, Integer> selected;

    @BeforeEach
    void beforeEach() {
      EnchantOperation operation = new EnchantOperation(TOOL_ENCHANTS);
      operation.setIncompatibility(this::conflicts);
      operation.setEnchantability(Enchantability.STONE);
      operation.setButtonLevel(ThreadLocalRandom.current().nextInt(1, 31));
      operation.setSeed(System.currentTimeMillis());
      selected = operation.apply();
    }

    @DisplayName("One or more enchantments should be selected.")
    @Test
    void checkSize() {
      EnchantOperation operation = new EnchantOperation(TOOL_ENCHANTS);
      operation.setIncompatibility(this::conflicts);
      operation.setEnchantability(Enchantability.STONE);
      operation.setButtonLevel(30);
      operation.setSeed(System.currentTimeMillis());
      selected = operation.apply();
      assertThat("One or more enchantments must be selected", false, is(selected.isEmpty()));
    }

    @DisplayName("Enchantments should not conflict.")
    @RepeatedTest(10)
    void checkConflict() {
      Enchantment[] enchantments = selected.keySet().toArray(new Enchantment[0]);
      for (int i = 0; i < enchantments.length; ++i) {
        for (int j = 0; j < enchantments.length; ++j) {
          if (i == j) {
            continue;
          }
          assertThat("Enchantments may not conflict", false,
              is(conflicts(enchantments[i], enchantments[j])));
        }
      }
    }

    private boolean conflicts(Enchantment enchantment1, Enchantment enchantment2) {
      if (enchantment1.equals(enchantment2)) {
        return true;
      }
      return enchantment1.conflictsWith(enchantment2) || enchantment2.conflictsWith(enchantment1);
    }

  }

  @DisplayName("Enchanting table button levels should be calculated consistently.")
  @ParameterizedTest
  @CsvSource({"1,0", "10,0", "15,0", "1,12348", "10,98124", "15,23479"})
  void checkButtonLevels(int shelves, int seed) {
    int[] buttonLevels1 = EnchantingTableUtil.getButtonLevels(shelves, seed);
    int[] buttonLevels2 = EnchantingTableUtil.getButtonLevels(shelves, seed);

    assertThat("There are always three buttons", buttonLevels1.length, is(3));
    assertThat("There are always three buttons", buttonLevels2.length, is(3));

    for (int i = 0; i < 3; ++i) {
      assertThat("Button level should be generated predictably", buttonLevels1[i],
          is(buttonLevels2[i]));
      assertThat("Button level may not exceed 30", buttonLevels1[i], lessThanOrEqualTo(30));
      assertThat("Button level may not be negative", buttonLevels1[i], greaterThanOrEqualTo(0));
    }
  }

  @DisplayName("Button updates send to user as expected.")
  @Test
  void testSendButtonUpdates() {
    var server = MockBukkit.getMock();

    var plugin = MockBukkit.createMockPlugin("SampleText");

    var player = new PlayerMock(server, "sampletext") {
      @Override
      public boolean setWindowProperty(@NotNull InventoryView.Property prop, int value) {
        // Sends packet to client, ignore.
        return true;
      }
    };

    EnchantmentOffer[] offers = new EnchantmentOffer[] {
        new EnchantmentOffer(Enchantment.DIG_SPEED, 5, 1),
        new EnchantmentOffer(Enchantment.DURABILITY, 20, 2),
        new EnchantmentOffer(Enchantment.SILK_TOUCH, 30, 3)
    };

    assertDoesNotThrow(() -> EnchantingTableUtil.updateButtons(plugin, player, offers));

    BukkitSchedulerMock scheduler = server.getScheduler();

    assertDoesNotThrow(() -> scheduler.performTicks(2));
  }

  @AfterAll
  void afterAll() {
    MockBukkit.unmock();
  }

}
