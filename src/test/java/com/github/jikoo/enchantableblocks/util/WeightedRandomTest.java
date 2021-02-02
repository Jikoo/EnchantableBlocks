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
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Collection<WeightedRandom.Choice> choices = Collections.emptyList();

        assertThrows(IllegalArgumentException.class, () -> WeightedRandom.choose(random, choices));
    }

    @DisplayName("A minimum of 1 available element must have weight.")
    @Test
    void testNoWeight() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Collection<WeightedRandom.Choice> choices = Arrays.asList(() -> 0, () -> 0);

        assertThrows(IllegalArgumentException.class, () -> WeightedRandom.choose(random, choices));
    }

    @DisplayName("Invalid random behavior should generate an exception.")
    @Test
    void testBadRandom() {
        Random random = new Random() {
            @Override
            public int nextInt(int bound) {
                return bound + 1;
            }
        };
        Collection<WeightedRandom.Choice> choices = Arrays.asList(() -> 1, () -> 2, () -> 3);

        assertThrows(IllegalStateException.class, () -> WeightedRandom.choose(random, choices));
    }

}
