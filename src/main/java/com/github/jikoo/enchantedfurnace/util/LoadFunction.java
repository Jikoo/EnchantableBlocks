package com.github.jikoo.enchantedfurnace.util;

/**
 * Abstraction for a loading function.
 *
 * @author Jikoo
 */
public abstract class LoadFunction<K, V> {

	public abstract V run(K key, boolean create);

}
