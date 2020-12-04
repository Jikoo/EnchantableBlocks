package com.github.jikoo.enchantableblocks.util.enchant;

import be.seeseemelk.mockbukkit.MockBukkit;
import java.util.Arrays;
import java.util.stream.Stream;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

/**
 * Unit tests for enchantments.
 *
 * @author Jikoo
 */
@DisplayName("Feature: Calculate enchantments in a vanilla style")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EnchantmentTest {

    private Server server;

    @BeforeAll
    void beforeAll() {
        server = MockBukkit.mock();
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

    @AfterAll
    void afterAll() {
        MockBukkit.unmock();
    }

}
