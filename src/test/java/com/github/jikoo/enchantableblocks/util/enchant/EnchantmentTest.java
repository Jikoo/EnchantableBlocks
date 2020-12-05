package com.github.jikoo.enchantableblocks.util.enchant;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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
 *
 * @author Jikoo
 */
@DisplayName("Feature: Calculate enchantments")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EnchantmentTest {

    private static final Collection<Enchantment> TOOL_ENCHANTS = Arrays.asList(
            Enchantment.DIG_SPEED, Enchantment.DURABILITY, Enchantment.LOOT_BONUS_BLOCKS, Enchantment.SILK_TOUCH);

    @BeforeAll
    void beforeAll() {
        MockBukkit.mock();
        fixEnchant("efficiency", 5);
        fixEnchant("unbreaking", 3);
        fixEnchant("fortune", 3);
        fixEnchant("silk_touch", 1);
    }

    private void fixEnchant(String id, int levelMax) {
        EnchantmentMock mock = (EnchantmentMock) Enchantment.getByKey(NamespacedKey.minecraft(id));
        assert mock != null;
        mock.setMaxLevel(levelMax);
        mock.setStartLevel(1);
    }

    @DisplayName("Enchantments should be explicitly supported")
    @ParameterizedTest
    @MethodSource("enchantmentStream")
    public void testExplicitSupport(Enchantment enchantment) {
        EnchantData enchantData = EnchantData.of(enchantment);
        assertThat("Enchantment should be explicitly supported", Rarity.UNKNOWN, not(enchantData.getRarity()));
    }

    static Stream<Enchantment> enchantmentStream() {
        return Arrays.stream(Enchantment.values());
    }

    @DisplayName("When enchantments are selected")
    @Nested
    public class EnchantmentAttempt {

        private Map<Enchantment, Integer> selected;

        @BeforeEach
        public void beforeEach() {
            selected = EnchantmentUtil.calculateEnchantments(TOOL_ENCHANTS, this::conflicts, Enchantability.STONE,
                    ThreadLocalRandom.current().nextInt(1, 31), System.currentTimeMillis());
        }

        @DisplayName("One or more enchantments should be selected")
        @Test
        public void checkSize() {
            selected = EnchantmentUtil.calculateEnchantments(TOOL_ENCHANTS, this::conflicts, Enchantability.STONE,
                    30, System.currentTimeMillis());
            System.out.println(selected.isEmpty());
            assertThat("One or more enchantments must be selected", false, is(selected.isEmpty()));
        }

        @DisplayName("Enchantments should not conflict")
        @RepeatedTest(10)
        public void checkConflict() {
            Enchantment[] enchantments = selected.keySet().toArray(new Enchantment[0]);
            for (int i = 0; i < enchantments.length; ++i) {
                for (int j = 0; j < enchantments.length; ++j) {
                    if (i == j) continue;
                    assertThat("Enchantments may not conflict", false, is(conflicts(enchantments[i], enchantments[j])));
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

    @AfterAll
    void afterAll() {
        MockBukkit.unmock();
    }

}
