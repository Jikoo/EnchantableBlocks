package com.github.jikoo.enchantableblocks.util;

/**
 * Abstraction for some simple cache calls.
 *
 * @author Jikoo
 */
public abstract class Function<K, V> {

    public abstract boolean run(K key, V value);

}
