package com.github.jikoo.enchantableblocks.util;

/**
 * Abstraction for a loading function.
 *
 * @author Jikoo
 */
public abstract class LoadFunction<K, V> {

	public abstract V run(K key, boolean create);

}
