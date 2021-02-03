package com.github.jikoo.enchantableblocks.util;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MockClock extends Clock {

    private final AtomicLong now;

    public MockClock() {
        this(0L);
    }

    public MockClock(long now) {
        this.now = new AtomicLong(now);
    }

    public void add(long value) {
        now.addAndGet(value);
    }

    public void add(long value, TimeUnit timeUnit) {
        now.addAndGet(TimeUnit.MILLISECONDS.convert(value, timeUnit));
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zoneId) {
        throw new UnsupportedOperationException("Cannot set timezone of fake clock.");
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(now.get());
    }

}
