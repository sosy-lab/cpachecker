// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;

/** A limit that measures the elapsed time as returned by {@link System#nanoTime()}. */
public class WalltimeLimit implements ResourceLimit {

  private final long duration;
  private final long endTime;

  private WalltimeLimit(long pDuration) {
    duration = pDuration;
    endTime = getCurrentValue() + pDuration;
  }

  public static WalltimeLimit fromNowOn(TimeSpan timeSpan) {
    return fromNowOn(timeSpan.asNanos(), TimeUnit.NANOSECONDS);
  }

  public static WalltimeLimit fromNowOn(long time, TimeUnit unit) {
    checkArgument(time > 0);
    long nanoDuration = TimeUnit.NANOSECONDS.convert(time, unit);
    return new WalltimeLimit(nanoDuration);
  }

  @Override
  public long getCurrentValue() {
    return System.nanoTime();
  }

  @Override
  public boolean isExceeded(long pCurrentValue) {
    return pCurrentValue >= endTime;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    return endTime - pCurrentValue;
  }

  @Override
  public String getName() {
    return "walltime limit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
  }
}
