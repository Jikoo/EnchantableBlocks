package com.github.jikoo.enchantableblocks.util;

import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.jetbrains.annotations.Nullable;

/**
 * A minimal thread-safe time-based cache implementation backed by a HashMap and TreeMultimap.
 *
 * @author Jikoo
 */
@SuppressWarnings("unused")
public class Cache<K, V> {

	public static class CacheBuilder<K, V> {
		private long retention = 300000L;
		private BiFunction<K, Boolean, V> load;
		private BiFunction<K, V, Boolean> inUseCheck;
		private BiConsumer<K, V> postRemoval;

		public CacheBuilder<K, V> withLoadFunction(final BiFunction<K, Boolean, V> load) {
			this.load = load;
			return this;
		}

		public CacheBuilder<K, V> withInUseCheck(final BiFunction<K, V, Boolean> function) {
			this.inUseCheck = function;
			return this;
		}

		public CacheBuilder<K, V> withPostRemoval(final BiConsumer<K, V> function) {
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
	private final BiFunction<K, Boolean, V> load;
	private final BiFunction<K, V, Boolean> inUseCheck;
	private final BiConsumer<K, V> postRemoval;

	/**
	 * Constructs a Cache with the specified retention duration, in use function, and post-removal
	 * function.
	 *
	 * @param retention duration after which keys are automatically invalidated if not in use
	 * @param inUseCheck Function used to check if a key is considered in use
	 * @param postRemoval Function used to perform any operations required when a key is invalidated
	 */
	private Cache(final long retention, final BiFunction<K, Boolean, V> load,
			final BiFunction<K, V, Boolean> inUseCheck, final BiConsumer<K, V> postRemoval) {
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
		// Run lazy check to clean cache
		this.lazyCheck();

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
	public @Nullable V get(final K key) {
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
	public @Nullable V get(final K key, final boolean create) {
		// Run lazy check to clean cache
		this.lazyCheck();

		synchronized (this.internal) {
			V value;
			if (!this.internal.containsKey(key) && this.load != null) {
				value = this.load.apply(key, create);
				if (value != null) {
					this.internal.put(key, value);
				}
			} else {
				value = this.internal.get(key);
			}

			if (value != null) {
				this.expiry.put(System.currentTimeMillis() + this.retention, key);
			}

			return value;
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

				V value = this.internal.get(key);
				if (value != null && this.inUseCheck != null && this.inUseCheck.apply(key, value)) {
					this.expiry.put(nextExpiry, key);
					return;
				}

				this.internal.remove(key);

				if (value == null) {
					return;
				}

				if (this.postRemoval != null) {
					this.postRemoval.accept(key, value);
				}
			});
		}
	}

}
