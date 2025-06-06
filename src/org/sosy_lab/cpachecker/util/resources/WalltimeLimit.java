// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;

/**
 * A limit that is based on how much wall time elapses (based on {@link System#nanoTime()}).
 *
 * <p>Created instances should be passed to {@link
 * ResourceLimitChecker#ResourceLimitChecker(org.sosy_lab.common.ShutdownManager, java.util.List)},
 * but not used in any other way.
 */
public class WalltimeLimit implements ResourceLimit {

  private final long duration;
  private long endTime = -1;

  private WalltimeLimit(long pDuration) {
    duration = pDuration;
  }

  public static WalltimeLimit create(TimeSpan timeSpan) {
    return create(timeSpan.asNanos(), TimeUnit.NANOSECONDS);
  }

  public static WalltimeLimit create(long time, TimeUnit unit) {
    checkArgument(time > 0);
    long nanoDuration = TimeUnit.NANOSECONDS.convert(time, unit);
    return new WalltimeLimit(nanoDuration);
  }

  @Override
  public void start(Thread pThread) {
    checkState(endTime == -1);
    endTime = getCurrentMeasurementValue() + duration;
  }

  @Override
  public long getCurrentMeasurementValue() {
    return System.nanoTime();
  }

  @Override
  public boolean isExceeded(long pCurrentValue) {
    checkState(endTime != -1);
    return pCurrentValue >= endTime;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    checkState(endTime != -1);
    return endTime - pCurrentValue;
  }

  @Override
  public String getName() {
    return "walltime limit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
  }
}
