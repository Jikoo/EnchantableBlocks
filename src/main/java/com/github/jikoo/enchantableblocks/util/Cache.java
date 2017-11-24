package com.github.jikoo.enchantableblocks.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.collect.TreeMultimap;

/**
 * A minimal thread-safe time-based cache implementation backed by a HashMap and TreeMultimap.
 *
 * @author Jikoo
 */
public class Cache<K, V> {

	public static class CacheBuilder<K, V> {
		private long retention = 300000L;
		private LoadFunction<K, V> load;
		private Function<K, V> inUseCheck, postRemoval;

		public CacheBuilder<K, V> withLoadFunction(final LoadFunction<K, V> load) {
			this.load = load;
			return this;
		}

		public CacheBuilder<K, V> withInUseCheck(final Function<K, V> function) {
			this.inUseCheck = function;
			return this;
		}

		public CacheBuilder<K, V> withPostRemoval(final Function<K, V> function) {
			this.postRemoval = function;
			return this;
		}

		public CacheBuilder<K, V> withRetention(final long retention) {
			if (retention < 6000L) {
				throw new IllegalArgumentException("Cache retention must be at least 6000ms.");
			}
			this.retention = retention;
			return this;
		}

		public Cache<K, V> build() {
			return new Cache<>(this.retention, this.load, this.inUseCheck, this.postRemoval);
		}
	}

	private final Map<K, V> internal;
	private final TreeMultimap<Long, K> expiry;
	private final long retention;
	private final LoadFunction<K, V> load;
	private final Function<K, V> inUseCheck, postRemoval;

	/**
	 * Constructs a Cache with the specified retention duration, in use function, and post-removal
	 * function.
	 *
	 * @param retention duration after which keys are automatically invalidated if not in use
	 * @param inUseCheck Function used to check if a key is considered in use
	 * @param postRemoval Function used to perform any operations required when a key is invalidated
	 */
	private Cache(final long retention, final LoadFunction<K, V> load,
			final Function<K, V> inUseCheck, final Function<K, V> postRemoval) {
		this.internal = new HashMap<>();

		this.expiry = TreeMultimap.create(Comparator.naturalOrder(), (k1, k2) -> k1 == k2 || k1.equals(k2) ? 0 : 1);

		this.load = load;
		this.retention = retention;
		this.inUseCheck = inUseCheck;
		this.postRemoval = postRemoval;
	}

	/**
	 * Set a key and value pair. Keys are unique. Using an existing key will cause the old value to
	 * be overwritten and the expiration timer to be reset.
	 *
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 */
	public void put(final K key, final V value) {
		// Invalidate key - runs lazy check and ensures value won't be cleaned up early
		this.invalidate(key);

		if (value == null) {
			return;
		}

		synchronized (this.internal) {
			this.internal.put(key, value);
			this.expiry.put(System.currentTimeMillis() + this.retention, key);
		}
	}

	/**
	 * Gets the value for a specific key.
	 * <p>
	 * If a load function is defined and the key is not present, the load function will be used to create a new value.
	 * The load function may return null values.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or null if no value is mapped for the
	 *         key and no load function is defined
	 */
	public V get(final K key) {
		return this.get(key, true);
	}

	/**
	 * Gets the value for a specific key.
	 * <p>
	 * If a load function is defined and the key is not present, the load function will be used to create a new value
	 * if requested. The load function may return null values.
	 *
	 * @param key the key whose associated value is to be returned
	 * @param create whether or not the load function should create a new value if none exists to be loaded
	 * @return the value to which the specified key is mapped or null
	 */
	public V get(final K key, final boolean create) {
		// Run lazy check to clean cache
		this.lazyCheck();

		synchronized (this.internal) {
			if (!this.internal.containsKey(key) && this.load != null) {
				V value = this.load.run(key, create);
				if (value != null) {
					this.put(key, value);
				}
				return value;
			}

			return this.internal.get(key);
		}
	}

	/**
	 * Returns true if the specified key is mapped to a value.
	 *
	 * @param key key to check if a mapping exists for
	 * @return true if a mapping exists for the specified key
	 */
	public boolean containsKey(final K key) {
		// Run lazy check to clean cache
		this.lazyCheck();

		synchronized (this.internal) {
			return this.internal.containsKey(key);
		}
	}

	/**
	 * Forcibly invalidates a key, even if it is considered to be in use. Note that this will NOT
	 * cause the post-removal function to be run.
	 *
	 * @param key key to invalidate
	 */
	public void invalidate(final K key) {
		// Run lazy check to clean cache
		this.lazyCheck();

		synchronized (this.internal) {
			if (!this.internal.containsKey(key)) {
				// Value either not present or cleaned by lazy check. Either way, we're good
				return;
			}

			// Remove stored object
			this.internal.remove(key);

			// Remove expiration entry - prevents more work later, plus prevents issues with values invalidating early
			this.expiry.entries().removeIf(entry -> entry.getValue().equals(key));
		}
	}

	/**
	 * Forcibly expires all keys, requiring them to be in use to be kept.
	 */
	public void expireAll() {
		synchronized (this.internal) {
			this.expiry.clear();
			this.internal.keySet().forEach(key -> this.expiry.put(0L, key));
		}

		this.lazyCheck();
	}

	/**
	 * Invalidate all expired keys that are not considered in use. If a key is expired but is
	 * considered in use by the provided Function, its expiration time is reset.
	 */
	private void lazyCheck() {
		long now = System.currentTimeMillis();
		synchronized (this.internal) {
			SortedMap<Long, Collection<K>> subMap = this.expiry.asMap().headMap(now);
			Collection<K> keys = subMap.values().stream().collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);

			// Wipe original map
			subMap.clear();

			long nextExpiry = now + this.retention;

			keys.forEach(key -> {

				if (this.inUseCheck != null && this.inUseCheck.run(key, this.internal.get(key))) {
					this.expiry.put(nextExpiry, key);
					return;
				}

				V value = this.internal.remove(key);

				if (value == null) {
					return;
				}

				if (this.postRemoval != null) {
					this.postRemoval.run(key, value);
				}
			});
		}
	}

}
