package com.github.jikoo.enchantableblocks.util;

public final class MathHelper {

  /**
   * Clamp a value between {@code 0} and {@link Short#MAX_VALUE} and cast to short.
   *
   * @param value the value to clamp
   * @return the clamped value
   */
  public static short clampPositiveShort(double value) {
    return (short) Math.max(0, Math.min(Short.MAX_VALUE, value));
  }

  /**
   * Use a sigmoid curve to modify a value.
   *
   * <p>For an inverse sigmoid curve, invert the {@code x} parameter.
   *
   * @param initialValue the initial value
   * @param x the X value for the sigmoid curve
   * @param flavor a value modifying the sharpness of the sigmoid curve
   * @return the modified value
   */
  public static double sigmoid(double initialValue, double x, double flavor) {
    return initialValue * (1D + (x / (flavor + Math.abs(x))));
  }

  private MathHelper() {}

}
