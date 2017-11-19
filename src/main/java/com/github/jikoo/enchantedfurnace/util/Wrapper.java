package com.github.jikoo.enchantedfurnace.util;

/**
 * Simple object wrapper.
 *
 * @author Jikoo
 */
public class Wrapper<T> {

	private T object;

	public T get() {
		return this.object;
	}

	public void set(final T object) {
		this.object = object;
	}

}
