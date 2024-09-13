/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java17.internal;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class DurationUtil {
  private static final double NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);
  private static final double MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);

  /**
   * Convert provided Duration object to floating point seconds representation.
   *
   * @param duration value to be converted.
   * @return seconds with fractional part included.
   */
  public static double toSeconds(Duration duration) {
    double epochSecs = (double) duration.getSeconds();
    return epochSecs + duration.getNano() / NANOS_PER_SECOND;
  }

  /**
   * Convert provided number of milliseconds to floating point seconds representation.
   *
   * @param millis number of milliseconds to be converted.
   * @return seconds with fractional part included.
   */
  public static double millisToSeconds(long millis) {
    return millis / MILLIS_PER_SECOND;
  }

  private DurationUtil() {}
}
