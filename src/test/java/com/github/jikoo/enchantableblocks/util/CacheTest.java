package com.github.jikoo.enchantableblocks.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Feature: Cache and reuse data within a certain time period")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheTest {

    private static final long MIN_RETENTION = 60_000L;

    private static String getValue(String key) {
        return new StringBuilder(key).reverse().toString();
    }

    MockClock clock;

    @BeforeAll
    void beforeAll() {
        clock = new MockClock();
    }

    @DisplayName("Retention must be at least 1 minute.")
    @Test
    void testRetentionLimit() {
        Cache.CacheBuilder<Object, Object> cacheBuilder = new Cache.CacheBuilder<>();
        assertThrows(IllegalArgumentException.class, () -> cacheBuilder.withRetention(MIN_RETENTION - 1));
        cacheBuilder.withRetention(MIN_RETENTION);
    }

    @DisplayName("Cache must retain values.")
    @Test
    void testRetention() { // TODO should separate some cases for more thorough testing
        int lazyFrequency = 10_000;
        Cache<String, String> cache = new Cache.CacheBuilder<String, String>()
                .withClock(clock)
                .withRetention(MIN_RETENTION)
                .withInUseCheck((key, value) -> false)
                .withLazyFrequency(lazyFrequency)
                .build();

        String key = "Able was I ere I saw Elba";

        assertThat("Value must not be set.", cache.get(key), nullValue());
        assertThat("Value must not be created.", cache.get(key, true), nullValue());

        String value = getValue(key);
        cache.put(key, value);

        assertThat("Value must be set.", cache.get(key), is(value));

        cache.invalidate(key);

        assertThat("Value must be removed by invalidation.", cache.get(key), nullValue());

        cache.put(key, value);
        clock.add(MIN_RETENTION + 1);

        assertThat("Value must be removed by expiration.", cache.get(key), nullValue());

        cache.put(key, value);
        clock.add(MIN_RETENTION - 1);

        assertThat("Value must remain set.", cache.get(key), is(value));

        clock.add(lazyFrequency - 1);

        assertThat("Value must remain set because expiration may not re-run before lazy frequency elapses.",
                cache.get(key), is(value));

        clock.add(1);

        assertThat("Value must be removed by expiration.", cache.get(key), nullValue());
    }

}
