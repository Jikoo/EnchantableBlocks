package com.github.jikoo.enchantableblocks.util.function;

/**
 * A function that performs reflective operations and may throw an exception.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface ReflectiveFunction<T, R> {

    R apply(T t) throws ReflectiveOperationException;

}
