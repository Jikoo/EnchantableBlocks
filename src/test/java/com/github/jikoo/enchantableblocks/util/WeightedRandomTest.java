package com.github.jikoo.enchantableblocks.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Feature: Randomly select elements with different weight")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WeightedRandomTest {

    @DisplayName("A minimum of 1 element must be available.")
    @Test
    void testEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> WeightedRandom.choose(ThreadLocalRandom.current(), Collections.emptyList()));
    }

    @DisplayName("A minimum of 1 available element must have weight.")
    @Test
    void testNoWeight() {
        assertThrows(IllegalArgumentException.class,
                () -> WeightedRandom.choose(ThreadLocalRandom.current(), inferredTypeHelper(() -> 0, () -> 0)));
    }

    @DisplayName("Bad random behavior should fail hard.")
    @Test
    void testBadRandom() {
        assertThrows(IllegalStateException.class,
                () -> WeightedRandom.choose(new Random() {
                    @Override
                    public int nextInt(int bound) {
                        return bound + 1;
                    }
                }, inferredTypeHelper(() -> 1, () -> 2, () -> 3)));
    }

    private static Collection<WeightedRandom.Choice> inferredTypeHelper(WeightedRandom.Choice... choices) {
        return Arrays.asList(choices);
    }

}
