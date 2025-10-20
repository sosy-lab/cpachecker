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

  // For output (and debug) purposes we save the used factor, total, min, and max times potentially
  // used to create this thread time limit
  private final Optional<TimeSpan> minimumThreadTime;
  private final Optional<TimeSpan> maximumThreadTime;
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
   * @param pMaximumThreadTimeUsedInCreation if a maximum time-limit for threads is specified and
   *     has been used as a limiter, it should be given here, else empty. This parameter has no
   *     influence on the used time-limit and is only used for better output.
   * @param pMinimumThreadTimeUsedInCreation if a minimum time-limit for threads is specified and
   *     has been used as a limiter, it should be given here, else empty. This parameter has no
   *     influence on the used time-limit and is only used for better output.
   */
  private ThreadCpuTimeLimit(
      long pLimit,
      TimeUnit pUnit,
      Optional<Double> pFactorUsedInCreation,
      Optional<TimeSpan> pTotalCpuTimeUsedInThreadTimeCalculation,
      Optional<TimeSpan> pMaximumThreadTimeUsedInCreation,
      Optional<TimeSpan> pMinimumThreadTimeUsedInCreation) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);

    checkArgument(
        pFactorUsedInCreation.isEmpty() || pTotalCpuTimeUsedInThreadTimeCalculation.isPresent(),
        "If a factor on the total CPU time is used to create a thread time limit, the total CPU"
            + " time also needs to be given.");
    checkArgument(
        pTotalCpuTimeUsedInThreadTimeCalculation.isEmpty() || pFactorUsedInCreation.isPresent(),
        "If a total CPU time limit with an factor is used to create a thread time limit, the factor"
            + " also needs to be given.");
    factorOfTotalTime = pFactorUsedInCreation;
    totalCpuTimeUsedInThreadTimeCalculation = pTotalCpuTimeUsedInThreadTimeCalculation;
    checkArgument(
        pMaximumThreadTimeUsedInCreation.isEmpty() || pMinimumThreadTimeUsedInCreation.isEmpty(),
        "Only the minimum or maximum time limit could have been applied, but both are given.");
    minimumThreadTime = pMinimumThreadTimeUsedInCreation;
    maximumThreadTime = pMaximumThreadTimeUsedInCreation;
  }

  /**
   * Creates a thread cpu time limit as the multiplication of the factor given with the totalCpuTime
   * given, with the value being not less than minimum and not exceeding maximum. Used thread cpu
   * time limit = max(min((factor * totalCpuTime), minimumThreadCpuTime), maximumThreadCpuTime).
   *
   * @param factor the factor used of the totalCpuTime as time limit for this thread. 0.0 < factor
   *     <= 1.0.
   * @param totalCpuTime the total CPU {@link TimeSpan} used to calculate the thread time limit
   *     from.
   * @param maximumThreadCpuTime maximum value used for threadCpuTime if not empty.
   * @param minimumThreadCpuTime minimal value used for threadCpuTime if not empty.
   */
  public static ThreadCpuTimeLimit createByFactorOfTotalCpuTime(
      double factor,
      TimeSpan totalCpuTime,
      Optional<TimeSpan> maximumThreadCpuTime,
      Optional<TimeSpan> minimumThreadCpuTime) {
    checkArgument(factor > 0.0 && factor <= 1.0);
    checkArgument(totalCpuTime.compareTo(TimeSpan.empty()) > 0);

    TimeSpan threadLimit =
        TimeSpan.ofNanos(
            BigDecimal.valueOf(factor)
                .multiply(BigDecimal.valueOf(totalCpuTime.asNanos()))
                .longValue());
    return applyMinAndMaxAndCreate(
        threadLimit,
        maximumThreadCpuTime,
        minimumThreadCpuTime,
        Optional.of(factor),
        Optional.of(totalCpuTime));
  }

  /**
   * Creates a {@link ThreadCpuTimeLimit} from threadCpuTime with max and min as upper and lower
   * limits if they are given.
   *
   * @param threadCpuTime the time limit for the thread. If given, max and min is applied if this
   *     value exceeds them.
   * @param maximumThreadCpuTime maximum value allowed for threadCpuTime if not empty.
   * @param minimumThreadCpuTime minimal value allowed for threadCpuTime if not empty.
   */
  public static ThreadCpuTimeLimit create(
      TimeSpan threadCpuTime,
      Optional<TimeSpan> maximumThreadCpuTime,
      Optional<TimeSpan> minimumThreadCpuTime) {
    return applyMinAndMaxAndCreate(
        threadCpuTime,
        maximumThreadCpuTime,
        minimumThreadCpuTime,
        Optional.empty(),
        Optional.empty());
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
          (factorOfTotalTime.orElseThrow() * 100.0)
              + "% of total CPU time-limit "
              + totalCpuTimeUsedInThreadTimeCalculation.orElseThrow().asSeconds()
              + "s";
    }
    if (maximumThreadTime.isPresent()) {
      // Max was applied
      additionalInfo += ", with upper limit " + maximumThreadTime.orElseThrow().asSeconds() + "s";
    } else if (minimumThreadTime.isPresent()) {
      // Min was applied
      additionalInfo += ", with lower limit " + minimumThreadTime.orElseThrow().asSeconds() + "s";
    }
    if (!additionalInfo.isEmpty()) {
      additionalInfo = " (" + additionalInfo + ")";
    }
    return "Thread CPU-time limit of "
        + TimeUnit.NANOSECONDS.toSeconds(duration)
        + "s"
        + additionalInfo;
  }

  /**
   * Applies the max (if not empty) and min (if not empty) to timeSpanToApplyTo and creates the
   * {@link ThreadCpuTimeLimit} afterward with the result. maybeFactorInformation and
   * maybeTotalCpuTimeInformation are only used for more detailed user output and should only be
   * given if timeSpanToApplyTo is calculated using them.
   *
   * @param timeSpanToApplyTo the {@link TimeSpan} to
   * @param maximumThreadTime if not empty, the maximum value of this and timeSpanToApplyTo is used
   *     to create the time-limit.
   * @param minimumThreadTime if not empty, the minimum value of this and timeSpanToApplyTo is used
   *     to create the time-limit.
   * @param maybeFactorInformation the factor of total CPU times used to create timeSpanToApplyTo if
   *     not empty. Only used for more detailed output.
   * @param maybeTotalCpuTimeInformation the total CPU times used to create timeSpanToApplyTo if not
   *     empty. Only used for more detailed output.
   */
  private static ThreadCpuTimeLimit applyMinAndMaxAndCreate(
      TimeSpan timeSpanToApplyTo,
      Optional<TimeSpan> maximumThreadTime,
      Optional<TimeSpan> minimumThreadTime,
      Optional<Double> maybeFactorInformation,
      Optional<TimeSpan> maybeTotalCpuTimeInformation) {

    if (maximumThreadTime.isPresent()
        && timeSpanToApplyTo.compareTo(maximumThreadTime.orElseThrow()) > 0) {
      // maximumThreadTime < timeSpanToApplyTo -> use maximumThreadTime
      checkArgument(maximumThreadTime.orElseThrow().compareTo(TimeSpan.empty()) > 0);
      return new ThreadCpuTimeLimit(
          maximumThreadTime.orElseThrow().asNanos(),
          TimeUnit.NANOSECONDS,
          maybeFactorInformation,
          maybeTotalCpuTimeInformation,
          maximumThreadTime,
          Optional.empty());

    } else if (minimumThreadTime.isPresent()
        && timeSpanToApplyTo.compareTo(minimumThreadTime.orElseThrow()) < 0) {
      // timeSpanToApplyTo < minimumThreadTime -> use minimumThreadTime
      checkArgument(minimumThreadTime.orElseThrow().compareTo(TimeSpan.empty()) > 0);
      return new ThreadCpuTimeLimit(
          minimumThreadTime.orElseThrow().asNanos(),
          TimeUnit.NANOSECONDS,
          maybeFactorInformation,
          maybeTotalCpuTimeInformation,
          Optional.empty(),
          minimumThreadTime);
    }

    return new ThreadCpuTimeLimit(
        timeSpanToApplyTo.asNanos(),
        TimeUnit.NANOSECONDS,
        maybeFactorInformation,
        maybeTotalCpuTimeInformation,
        Optional.empty(),
        Optional.empty());
  }
}
