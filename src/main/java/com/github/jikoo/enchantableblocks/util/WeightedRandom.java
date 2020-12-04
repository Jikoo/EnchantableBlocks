package com.github.jikoo.enchantableblocks.util;

import java.util.Collection;
import java.util.Random;
import org.jetbrains.annotations.Nullable;

/**
 * A utility for selecting a weighted choice randomly.
 *
 * @author Jikoo
 */
public class WeightedRandom {

    public static <T extends Choice> @Nullable T choose(Random random, Collection<T> choices) {
        int choiceMax = sum(choices);

        if (choiceMax <= 0) {
            throw new IllegalArgumentException("Must provide at least 1 choice with weight!");
        }

        int chosen = random.nextInt(choiceMax);

        return choose(choices, chosen);
    }

    public static <T extends Choice> @Nullable T choose(Collection<T> choices, int chosen) {
        for (T choice : choices) {
            chosen -= choice.getWeight();
            if (chosen <= 0) {
                return choice;
            }
        }

        return null;
    }

    private static int sum(Collection<? extends WeightedRandom.Choice> choices) {
        return choices.stream().mapToInt(Choice::getWeight).sum();
    }

    public interface Choice {
        int getWeight();
    }

}
