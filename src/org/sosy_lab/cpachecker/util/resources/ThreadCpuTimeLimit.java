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

import com.google.common.base.Preconditions;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;

/**
 * A limit that is based on how much CPU time is spent for one particular thread. This includes our
 * code, code in native libraries, the JVM and maybe parts of its garbage collector, etc., if
 * executed within our thread.
 *
 * <p>Which thread is measured is determined later on, it is the thread that calls {@link
 * ResourceLimitChecker#start()}. Which thread creates the instance does not matter.
 *
 * <p>This limit might not be available on all JVMs.
 *
 * <p>Created instances should be passed to {@link
 * ResourceLimitChecker#ResourceLimitChecker(org.sosy_lab.common.ShutdownManager, java.util.List)},
 * but not used in any other way.
 */
public class ThreadCpuTimeLimit implements ResourceLimit {

  private final long duration;
  private long endTime;

  private Thread thread;

  // For output (and debug) purposes we save the used factor and total time used to create this obj
  private final Optional<Double> factorOfTotalTime; // between 0 and 1
  private final Optional<TimeSpan> totalCpuTimeUsedInThreadTimeCalculation;

  /**
   * @param pLimit the duration of the time-limit used for the thread for the {@link TimeUnit} given
   *     matching {@link TimeUnit} pUnit.
   * @param pUnit the {@link TimeUnit} for pLimit.
   * @param pFactorUsedInCreation if the used time-limit for this thread is specified by a factor of
   *     the total CPU time limit used for CPAchecker, the factor used should be given here, else
   *     empty. The factor should be greater than 0 and less or equal to 1. This parameter has no
   *     influence on the used time-limit and is only used for better output.
   * @param pTotalCpuTimeUsedInThreadTimeCalculation if the used time-limit for this thread is
   *     specified by a factor of the total CPU time limit used for CPAchecker, the total CPU time
   *     limit used to calculate the thread time limit should be given here, else empty. This
   *     parameter has no influence on the used time-limit and is only used for better output.
   */
  private ThreadCpuTimeLimit(
      long pLimit,
      TimeUnit pUnit,
      double pFactorUsedInCreation,
      TimeSpan pTotalCpuTimeUsedInThreadTimeCalculation) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);
    factorOfTotalTime = Optional.of(pFactorUsedInCreation);
    totalCpuTimeUsedInThreadTimeCalculation = Optional.of(pTotalCpuTimeUsedInThreadTimeCalculation);
  }

  /**
   * @param pLimit the duration of the time-limit used for the thread for the {@link TimeUnit} given
   *     matching {@link TimeUnit} pUnit.
   * @param pUnit the {@link TimeUnit} for pLimit.
   */
  private ThreadCpuTimeLimit(long pLimit, TimeUnit pUnit) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);
    factorOfTotalTime = Optional.empty();
    totalCpuTimeUsedInThreadTimeCalculation = Optional.empty();
  }

  /**
   * Creates a {@link ThreadCpuTimeLimit} as the multiplication of the factor given with the
   * totalCpuTime given.
   *
   * @param factor the factor used of the totalCpuTime as time limit for this thread. 0.0 < factor
   *     <= 1.0.
   * @param totalCpuTime the total CPU {@link TimeSpan} used to calculate the thread time limit
   *     from.
   */
  public static ThreadCpuTimeLimit createByFactorOfTotalCpuTime(
      double factor, TimeSpan totalCpuTime) {
    checkArgument(factor > 0.0 && factor <= 1.0);
    checkArgument(totalCpuTime.compareTo(TimeSpan.empty()) > 0);

    TimeSpan threadLimit =
        TimeSpan.ofNanos(
            BigDecimal.valueOf(factor)
                .multiply(BigDecimal.valueOf(totalCpuTime.asNanos()))
                .longValue());

    return new ThreadCpuTimeLimit(
        threadLimit.asNanos(), TimeUnit.NANOSECONDS, factor, totalCpuTime);
  }

  /**
   * Creates a {@link ThreadCpuTimeLimit} from given {@link TimeSpan}.
   *
   * @param threadCpuTime the time limit for the thread.
   */
  public static ThreadCpuTimeLimit create(TimeSpan threadCpuTime) {
    return new ThreadCpuTimeLimit(threadCpuTime.asNanos(), TimeUnit.NANOSECONDS);
  }

  @Override
  public void start(Thread pThread) {
    checkState(thread == null);
    thread = pThread;
    endTime = getCurrentMeasurementValue() + duration;
  }

  @Override
  public long getCurrentMeasurementValue() {
    Preconditions.checkState(thread != null);
    return ManagementFactory.getThreadMXBean().getThreadCpuTime(thread.threadId());
  }

  @Override
  public boolean isExceeded(long pCurrentValue) {
    Preconditions.checkState(thread != null);
    return pCurrentValue >= endTime;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    Preconditions.checkState(thread != null);
    return (endTime - pCurrentValue) / 2;
  }

  @Override
  public String getName() {
    String additionalInfo = "";
    if (factorOfTotalTime.isPresent()) {
      checkState(totalCpuTimeUsedInThreadTimeCalculation.isPresent());
      additionalInfo =
          " ("
              + (factorOfTotalTime.orElseThrow() * 100.0)
              + "% of total CPU time-limit "
              + totalCpuTimeUsedInThreadTimeCalculation.orElseThrow().asSeconds()
              + "s)";
    }

    return "Thread CPU-time limit of "
        + TimeUnit.NANOSECONDS.toSeconds(duration)
        + "s"
        + additionalInfo;
  }
}
