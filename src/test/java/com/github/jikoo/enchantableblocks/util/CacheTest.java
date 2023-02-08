package com.github.jikoo.enchantableblocks.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Cache and reuse data within a certain time period")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheTest {

  private static final long MIN_RETENTION = 60_000L;
  private static final String KEY = "Able was I ere I saw Elba";
  private static final String VALUE = "ablE was I ere I saw elbA";

  private Clock clock;

  @BeforeEach
  void beforeEach() {
    clock = mock(Clock.class);
  }

  @DisplayName("Cache must accept input.")
  @Test
  void testPut() {
    Cache<String, String> cache = new Cache.CacheBuilder<String, String>().build();

    assertThat("Key must not have content.", cache.containsKey(KEY), is(false));
    assertThat("Value must not be set.", cache.get(KEY), nullValue());

    cache.put(KEY, VALUE);

    assertThat("Key must have content.", cache.containsKey(KEY), is(true));
    assertThat("Value must be set.", cache.get(KEY), is(VALUE));
  }

  @DisplayName("Cache must generate values when requested.")
  @Test
  void testGenerate() {
    Cache<String, String> cache = new Cache.CacheBuilder<String, String>()
        .withLoadFunction((key, create) -> create ? VALUE : null).build();

    assertThat("Key must not have content.", cache.containsKey(KEY), is(false));
    assertThat("Value must not be created when specified.", cache.get(KEY, false), nullValue());
    assertThat("Value must be generated correctly.", cache.get(KEY), is(VALUE));
    assertThat("Key must have content.", cache.containsKey(KEY));
  }

  @DisplayName("Cache must handle generation of null values gracefully.")
  @Test
  void testGenerateNull() {
    Cache<String, String> cache = new Cache.CacheBuilder<String, String>()
        .withLoadFunction((key, create) -> null).build();

    assertDoesNotThrow(() -> cache.get(VALUE));
  }

  @DisplayName("Cache must remove invalidated values.")
  @Test
  void testInvalidate() {
    Cache<String, String> cache = new Cache.CacheBuilder<String, String>().withPostRemoval((key, value) -> {
      throw new IllegalStateException("Post-removal function may not be called during invalidation!");
    }).withInUseCheck((key, value) -> {
      throw new IllegalStateException("In use check may not be called during invalidation!");
    }).build();

    // Ensure invalidation of nonexistant key functions correctly.
    assertDoesNotThrow(() -> cache.invalidate(KEY));

    cache.put(KEY, VALUE);

    assertDoesNotThrow(() -> cache.invalidate(KEY));

    assertThat("Value must be removed by invalidation.", cache.containsKey(KEY), is(false));

  }

  @DisplayName("Cache must remove mappings when keys are expired.")
  @Test
  void testExpireAll() {
    Cache<String, String> cache = new Cache.CacheBuilder<String, String>().build();
    cache.put(KEY, VALUE);
    cache.expireAll();

    assertThat("Value must be removed when all keys expire.", cache.containsKey(KEY), is(false));
  }

  @DisplayName("Cache must retain values for the specified duration.")
  @Test
  void testRetentionDuration() {
    Cache<String, String> cache = new Cache.CacheBuilder<String, String>()
        .withClock(clock)
        .withRetention(MIN_RETENTION)
        .withInUseCheck((key, value) -> false)
        .withLazyFrequency(0)
        .withPostRemoval((key, value) -> {})
        .build();
    cache.put(KEY, VALUE);
    when(clock.millis()).thenReturn(MIN_RETENTION - 1L);

    assertThat("Value must remain set if retention duration has not elapsed.", cache.containsKey(KEY));

    when(clock.millis()).thenReturn(MIN_RETENTION + 1L);

    assertThat("Value must be removed by retention policy.", cache.containsKey(KEY), is(false));
  }

  @DisplayName("Cache must not check retention more often than lazy frequency.")
  @Test
  void testRetentionFrequency() {
    int lazyFrequency = 10_000;
    Cache<String, String> cache = new Cache.CacheBuilder<String, String>()
        .withClock(clock)
        .withRetention(MIN_RETENTION)
        .withLazyFrequency(lazyFrequency)
        .build();
    cache.put(KEY, null);
    when(clock.millis()).thenReturn(MIN_RETENTION - 1L);

    assertThat("Value must remain set if retention duration has not elapsed.", cache.containsKey(KEY));

    when(clock.millis()).thenReturn(MIN_RETENTION - 1L + lazyFrequency - 1L);

    assertThat(
        "Value must remain set because retention may not re-run before lazy frequency elapses.",
        cache.containsKey(KEY));

    when(clock.millis()).thenReturn(MIN_RETENTION - 1L + lazyFrequency);

    assertThat("Value must be removed by retention policy.", cache.containsKey(KEY), is(false));
  }

  @DisplayName("Cache must not remove values in use.")
  @Test
  void testRetentionInUse() {
    AtomicBoolean inUse = new AtomicBoolean(true);
    Cache<String, String> cache = new Cache.CacheBuilder<String, String>()
        .withClock(clock)
        .withRetention(MIN_RETENTION)
        .withLazyFrequency(0)
        .withInUseCheck((key, value) -> inUse.get())
        .build();
    cache.put(KEY, VALUE);
    when(clock.millis()).thenReturn(MIN_RETENTION + 1L);

    assertThat("Value must remain set if in use", cache.containsKey(KEY));

    inUse.set(false);
    when(clock.millis()).thenReturn(2 * MIN_RETENTION + 2L);

    assertThat("Value must be removed by retention policy.", cache.containsKey(KEY), is(false));
  }

}
