package com.github.jikoo.enchantableblocks.util.logging;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A logging handler that counts the number of matches for a specific {@link Pattern}.
 */
public class PatternCountHandler extends Handler {

  private final Pattern pattern;
  private final AtomicInteger count;

  /**
   * Construct a new {@code PatternCountHandler} with the given {@link Pattern regular expression}.
   *
   * @param pattern the pattern string
   * @throws PatternSyntaxException if the pattern string is malformed
   */
  public PatternCountHandler(String pattern) throws PatternSyntaxException {
    this.pattern = Pattern.compile(pattern);
    this.count = new AtomicInteger();
  }

  @Override
  public void publish(LogRecord record) {
    if (pattern.matcher(record.getMessage()).find()) {
      this.count.incrementAndGet();
      // Set level to fine - if it's an expected line, we probably don't need to see it in testing.
      record.setLevel(Level.FINE);
    }
  }

  @Override
  public void flush() {}

  @Override
  public void close() throws SecurityException {}

  /**
   * Get the number of times a {@link LogRecord} with a matching message has been published.
   *
   * @return the number of matching log messages
   */
  public int getMatches() {
    return this.count.get();
  }

}
