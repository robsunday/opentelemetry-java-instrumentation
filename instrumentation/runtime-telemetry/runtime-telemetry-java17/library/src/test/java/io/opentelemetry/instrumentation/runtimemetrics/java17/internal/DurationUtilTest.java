/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java17.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DurationUtilTest {

  @Test
  void shouldConvertDurationToSeconds() {
    // Given
    Duration duration = Duration.ofSeconds(7, 312144);

    // When
    double seconds = DurationUtil.toSeconds(duration);

    // Then
    assertEquals(7.000312144, seconds);
  }

  @Test
  void shouldConvertMillisecondsToSeconds() {
    // Given
    long duration = TimeUnit.SECONDS.toMillis(3) + 142;

    // When
    double seconds = DurationUtil.millisToSeconds(duration);

    // Then
    assertEquals(3.142, seconds);
  }
}
