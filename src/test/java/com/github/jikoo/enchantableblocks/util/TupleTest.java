package com.github.jikoo.enchantableblocks.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@DisplayName("Feature: Store and retrieve multiple data points in a single tuple")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TupleTest {

    @DisplayName("Tuple accessors should modify tuple accordingly.")
    @ParameterizedTest
    @MethodSource("getAccessors")
    <T> void testSetGet(Consumer<T> setter, Supplier<T> getter, T value) {
        setter.accept(value);
        T setValue = getter.get();
        assertThat("Value is set correctly", setValue, is(value));
    }

    private static Stream<Arguments> getAccessors() {
        Pair<String, String> pair = new Pair<>("", "");
        Triple<String, String, String> triple = new Triple<>("", "", "");

        List<Arguments> arguments = Arrays.asList(
                Arguments.of((Consumer<String>) pair::setLeft, (Supplier<String>) pair::getLeft, "Test value,"),
                Arguments.of((Consumer<String>) pair::setRight, (Supplier<String>) pair::getRight, "please ignore."),
                Arguments.of((Consumer<String>) triple::setLeft, (Supplier<String>) triple::getLeft, "Your cooperation"),
                Arguments.of((Consumer<String>) triple::setMiddle, (Supplier<String>) triple::getMiddle, "is noted"),
                Arguments.of((Consumer<String>) triple::setRight, (Supplier<String>) triple::getRight, "and appreciated.")
        );

        return arguments.stream();
    }

    @DisplayName("Tuples must be equal for matching content")
    @ParameterizedTest
    @MethodSource("getEquals")
    void testEquality(Object a, Object b) {
        assertThat("Tuples must be equal", a, equalTo(b));
        if (Objects.equals(b, a)) {
            assertThat("Hash must be calculated consistently", a.hashCode(), is(b.hashCode()));
        }
    }

    private static Stream<Arguments> getEquals() {
        Pair<String, String> pairA = new Pair<>("left", "right");
        // Messy garbage is an attempt to prevent compiler optimizing string constants.
        Pair<String, String> pairB = new Pair<>(pairA.getLeft().substring(0, 2) + pairA.getLeft().substring(2),
                pairA.getRight().substring(0, 2) + pairA.getRight().substring(2));
        Triple<String, String, String> tripleA = new Triple<>(pairA.getLeft(), "middle", pairA.getRight());
        Triple<String, String, String> tripleB = new Triple<>(pairB.getLeft(),
                tripleA.getMiddle().substring(0, 2) + tripleA.getMiddle().substring(2),
                pairB.getRight());

        return Arrays.stream(new Arguments[] {
                Arguments.of(pairA, pairA),
                Arguments.of(pairA, pairB),
                Arguments.of(pairB, pairA),
                Arguments.of(tripleA, tripleB),
                Arguments.of(tripleB, tripleA),
                Arguments.of(pairA, tripleA)
        });
    }

    @DisplayName("Tuples must not be equal for differing content")
    @ParameterizedTest
    @MethodSource("getInequals")
    void testInequality(Object a, Object b) {
        assertThat("Tuples must not be equal", a, not(equalTo(b)));
        assertThat("Hashes should differ", a.hashCode(), not(equalTo(b.hashCode())));
    }

    private static Stream<Arguments> getInequals() {
        Pair<String, String> pairA = new Pair<>("left", "right");
        Pair<String, String> pairB = new Pair<>("port", "starboard");
        Pair<String, String> pairC = new Pair<>("left", "east");
        Triple<String, String, String> tripleA = new Triple<>(pairA.getLeft(), "middle", pairA.getRight());
        Triple<String, String, String> tripleB = new Triple<>(pairB.getLeft(), "amidships", pairB.getRight());
        Triple<String, String, String> tripleC = new Triple<>(pairA.getLeft(), "muddle", pairA.getRight());

        return Arrays.stream(new Arguments[] {
                Arguments.of(pairA, pairB),
                Arguments.of(pairB, pairA),
                Arguments.of(pairA, pairC),
                Arguments.of(pairA, new Object()),
                Arguments.of(tripleA, tripleB),
                Arguments.of(tripleB, tripleA),
                Arguments.of(tripleA, pairA),
                Arguments.of(tripleA, tripleC)
        });
    }

}
