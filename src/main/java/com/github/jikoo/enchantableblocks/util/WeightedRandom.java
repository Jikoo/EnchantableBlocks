package com.github.jikoo.enchantableblocks.util;

import java.util.Collection;
import java.util.Random;
import org.jetbrains.annotations.NotNull;

/**
 * A utility for selecting a weighted choice randomly.
 *
 * @author Jikoo
 */
public final class WeightedRandom {

    /**
     * Choose an element from a collection of choices.
     *
     * @param random the Random to use
     * @param choices the choices
     * @param <T> the type of choice
     * @return the selected choice
     */
    public static <T extends Choice> @NotNull T choose(Random random, Collection<T> choices) {
        int choiceMax = sum(choices);

        if (choiceMax <= 0) {
            throw new IllegalArgumentException("Must provide at least 1 choice with weight!");
        }

        int chosen = random.nextInt(choiceMax);

        for (T choice : choices) {
            chosen -= choice.getWeight();
            if (chosen <= 0) {
                return choice;
            }
        }

        throw new IllegalStateException("Generated an index out of bounds with " + random.getClass().getName());
    }

    private static int sum(Collection<? extends WeightedRandom.Choice> choices) {
        return choices.stream().mapToInt(Choice::getWeight).sum();
    }

    public interface Choice {
        int getWeight();
    }

    private WeightedRandom() {}

}
