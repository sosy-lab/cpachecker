// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.ShutdownNotifier.interruptCurrentThreadOnShutdown;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.LongMath;
import com.google.common.primitives.Longs;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.management.JMException;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;

/**
 * This class handles one or more limits for the usage of external resources. It does not contain
 * code for any specific limits, but instead uses instances of {@link ResourceLimit}. It
 * periodically checks whether any of the given limits is exceeded, and signals this via an instance
 * of {@link ShutdownNotifier}.
 */
public final class ResourceLimitChecker {

  private final Thread thread;
  // Limits must be used in a single-threaded manner, we basically only call them from our thread
  private final List<ResourceLimit> limits;

  /**
   * Create a new instance with a list of limits to check and a {@link ShutdownNotifier} that gets
   * notified when a limit is exceeded. It is safe (but useless) to call this constructor with an
   * empty list of limits, limits that have already been exceeded, or a shutdown notifier that has
   * already been triggered.
   *
   * <p>The given {@link ResourceLimit} instances must be fresh instances and not started yet.
   * Callers should only create them and pass them to this constructor, but not use them in any
   * other way before or afterward.
   *
   * <p>Note that {@link #start()} needs to be called in order to actually start the limits.
   *
   * @param shutdownManager A non-null shutdown notifier instance.
   * @param limits A (possibly empty) list without null entries of not-yet-started resource limits.
   */
  public ResourceLimitChecker(ShutdownManager shutdownManager, List<ResourceLimit> limits) {
    checkNotNull(shutdownManager);
    this.limits = limits;
    if (limits.isEmpty() || shutdownManager.getNotifier().shouldShutdown()) {
      // limits are irrelevant
      thread = null;

    } else {
      thread =
          Concurrency.newDaemonThread(
              "Resource limit checker", new ResourceLimitCheckRunnable(shutdownManager, limits));
    }
  }

  /**
   * Actually start enforcing the limits and use the current point in time as the start time of the
   * limit. May be called only once. In order to support per-thread limits, this should be called
   * from the thread that will do the work and should be limited.
   */
  public void start() {
    if (thread != null) {
      checkState(!thread.isAlive());
      for (ResourceLimit limit : limits) {
        // This is the only place where we call a limit from a different thread than the usual
        // thread, which is fine because it happens before thread.start(), so there is a happens-
        // before relationship.
        limit.start(Thread.currentThread());
      }
      thread.start();
    }
  }

  /** Cancel enforcing the limits (without triggering the stop request if not done before). */
  public void cancel() {
    if (thread != null) {
      thread.interrupt();
    }
  }

  /**
   * Create an instance of this class from some configuration options. The returned instance is not
   * started yet, {@link #start()} still needs to be called.
   */
  public static ResourceLimitChecker fromConfiguration(
      Configuration config, LogManager logger, ShutdownManager shutdownManager)
      throws InvalidConfigurationException {

    ResourceLimitOptions options = new ResourceLimitOptions();
    config.inject(options);

    ImmutableList.Builder<ResourceLimit> limits = ImmutableList.builder();
    if (options.walltime.compareTo(TimeSpan.empty()) >= 0) {
      limits.add(WalltimeLimit.create(options.walltime));
    }
    boolean cpuTimeLimitSet = options.cpuTime.compareTo(TimeSpan.empty()) >= 0;
    if (options.cpuTimeRequired.compareTo(TimeSpan.empty()) >= 0) {
      if (!options.cpuTimeRequired.equals(options.cpuTime)) {
        if (!cpuTimeLimitSet) {
          throw new InvalidConfigurationException(
              "CPU time limit was not specified but is required to be explicitly set to "
                  + options.cpuTimeRequired
                  + " in this configuration.");
        } else {
          throw new InvalidConfigurationException(
              "CPU time limit was set to "
                  + options.cpuTime
                  + "  but is required to be explicitly set to "
                  + options.cpuTimeRequired
                  + " in this configuration.");
        }
      }
    }
    if (cpuTimeLimitSet) {
      try {
        limits.add(ProcessCpuTimeLimit.create(options.cpuTime));
      } catch (JMException e) {
        logger.logDebugException(e, "Querying cpu time failed");
        logger.log(
            Level.WARNING,
            "Your Java VM does not support measuring the cpu time, cpu time threshold disabled.");
      }
    }

    handleThreadTimeLimit(options, limits, logger);

    ImmutableList<ResourceLimit> limitsList = limits.build();
    if (!limitsList.isEmpty()) {
      logger.log(
          Level.INFO,
          "Using the following resource limits:",
          Joiner.on(", ").join(Lists.transform(limitsList, ResourceLimit::getName)));
    }
    return new ResourceLimitChecker(shutdownManager, limitsList);
  }

  /**
   * Creates a thread-CPU-time-limit if the options specify one. TODO: add info about relative
   * limits.
   *
   * @param options {@link ResourceLimitOptions} used to retrieve the time-limits from.
   * @param limits {@link Builder} for the possibly created {@link ResourceLimit} to be put into.
   * @throws InvalidConfigurationException for invalid combinations of thread and relative thread
   *     time configuration options.
   */
  private static void handleThreadTimeLimit(
      ResourceLimitOptions options, ImmutableList.Builder<ResourceLimit> limits, LogManager pLogger)
      throws InvalidConfigurationException {

    Optional<TimeSpan> maximumThreadTime = Optional.empty();
    Optional<TimeSpan> minimumThreadTime = Optional.empty();
    if (options.threadTimeMax.compareTo(TimeSpan.empty()) > 0) {
      maximumThreadTime = Optional.of(options.threadTimeMax);
    }
    if (options.threadTimeMin.compareTo(TimeSpan.empty()) > 0) {
      minimumThreadTime = Optional.of(options.threadTimeMin);
    }

    if (maximumThreadTime.isPresent()
        && minimumThreadTime.isPresent()
        && options.threadTimeMin.compareTo(options.threadTimeMax) > 0) {
      throw new InvalidConfigurationException(
          "Invalid Configuration: the minimum thread time-limit \"time.cpu.thread.minimum\" can not"
              + " be larger than the maximum time-limit \"time.cpu.thread.maximum\"!");
    }

    if (options.threadCpuTimeInPercentOfTotal > 100.0) {
      throw new InvalidConfigurationException(
          "Invalid Configuration: the thread time limit can not be more than 100%."
              + " \"limit.time.cpu.thread.factorOfTotalCpuTime\" is "
              + options.threadCpuTimeInPercentOfTotal
              + "%.");
    } else if (options.threadCpuTimeInPercentOfTotal <= 0.0) {
      throw new InvalidConfigurationException(
          "Invalid Configuration: the thread time limit can not be less or equal to 0% of the total"
              + " CPU time-limit. \"limit.time.cpu.thread.factorOfTotalCpuTime\" is "
              + options.threadCpuTimeInPercentOfTotal
              + "%.");
    }

    double threadTimeFactor = options.threadCpuTimeInPercentOfTotal / 100.0;

    if (options.threadTime.compareTo(TimeSpan.empty()) >= 0) {
      // Legacy system
      limits.add(
          ThreadCpuTimeLimit.create(options.threadTime, maximumThreadTime, minimumThreadTime));
      if (options.threadCpuTimeInPercentOfTotal != 100.00) {
        pLogger.log(
            Level.WARNING,
            "Found conflicting options \"limit.time.cpu.thread\" with setting "
                + options.threadTime
                + " and \"limit.time.cpu.thread.factor\" with setting "
                + options.threadCpuTimeInPercentOfTotal
                + "%, using option \"limit.time.cpu.thread\""
                + " with setting "
                + options.threadTime);
      }
    }

    // TODO: the 2 cases below are just split for more accurate info given in the command line.
    //  We could just use the same creation method and ignore factor == 1.0 cases.
    // Unlimited thread time (within min and max) as no CPU time-limit is set
    if (options.cpuTime.compareTo(TimeSpan.empty()) >= 0 && threadTimeFactor != 1.0) {
      limits.add(
          ThreadCpuTimeLimit.createByFactorOfTotalCpuTime(
              threadTimeFactor, options.cpuTime, maximumThreadTime, minimumThreadTime));
    } else if (maximumThreadTime.isPresent() || minimumThreadTime.isPresent()) {
      // Default threadTimeFactor is 1, so just CPU time
      limits.add(ThreadCpuTimeLimit.create(options.cpuTime, maximumThreadTime, minimumThreadTime));
    }
  }

  /**
   * Create an instance of this class with specific CPU time limit. The returned instance is not
   * started yet.
   */
  public static ResourceLimitChecker createCpuTimeLimitChecker(
      LogManager logger, ShutdownManager shutdownManager, TimeSpan cpuTime) {

    if (cpuTime.compareTo(TimeSpan.empty()) <= 0) {
      return new ResourceLimitChecker(shutdownManager, ImmutableList.of());
    }

    try {
      ResourceLimit cpuTimeLimitChecker = ProcessCpuTimeLimit.create(cpuTime);
      logger.log(Level.INFO, "Using " + cpuTimeLimitChecker.getName());
      return new ResourceLimitChecker(shutdownManager, ImmutableList.of(cpuTimeLimitChecker));
    } catch (JMException e) {
      logger.log(
          Level.WARNING,
          "Your Java VM does not support measuring the cpu time, cpu time threshold disabled.");
    }
    return new ResourceLimitChecker(shutdownManager, ImmutableList.of());
  }

  @Options(prefix = "limits")
  private static class ResourceLimitOptions {

    @Option(
        secure = true,
        name = "time.wall",
        description =
            "Limit for wall time used by CPAchecker (use seconds or specify a unit; -1 for"
                + " infinite)")
    @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = -1)
    private TimeSpan walltime = TimeSpan.ofNanos(-1);

    @Option(
        secure = true,
        name = "time.cpu",
        description =
            "Limit for cpu time used by CPAchecker (use seconds or specify a unit; -1 for"
                + " infinite)")
    @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = -1)
    private TimeSpan cpuTime = TimeSpan.ofNanos(-1);

    @Option(
        secure = true,
        name = "time.cpu::required",
        description =
            "Enforce that the given CPU time limit is set as the value of limits.time.cpu.")
    @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = -1)
    private TimeSpan cpuTimeRequired = TimeSpan.ofNanos(-1);

    @Option(
        secure = true,
        name = "time.cpu.thread",
        description =
            "Limit for thread cpu time used by CPAchecker. This option will in general not work"
                + " when multi-threading is used in more than one place, use only with great"
                + " caution! (use seconds or specify a unit; -1 for infinite)")
    @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = -1)
    private TimeSpan threadTime = TimeSpan.ofNanos(-1);

    @Option(
        secure = true,
        name = "time.cpu.thread.minimum",
        description =
            "Minimum limit for thread cpu time used by CPAchecker. I.e. iff the thread CPU time"
                + " limit calculated with \"time.cpu.thread.factor\" is less than the limit"
                + " specified in this option, the limit of this option is used instead. This option"
                + " will in general not work when multi-threading is used in more than one place,"
                + " use only with great caution! (use seconds or specify a unit)")
    @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
    private TimeSpan threadTimeMin = TimeSpan.ofNanos(0);

    @Option(
        secure = true,
        name = "time.cpu.thread.maximum",
        description =
            "Maximum limit for thread cpu time used by CPAchecker. I.e. iff the thread CPU time"
                + " limit calculated with \"time.cpu.thread.factor\" is larger than this option,"
                + " the limit specified in this option is used instead. This option will in general"
                + " not work when multi-threading is used in more than one place, use only with"
                + " great caution! (use seconds or specify a unit; -1 for infinite)")
    @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = -1)
    private TimeSpan threadTimeMax = TimeSpan.ofNanos(-1);

    @Option(
        secure = true,
        name = "time.cpu.thread.factorOfTotalCpuTime",
        description =
            "Thread time-limit in percent (%) of total CPU time-limit \"limit.time.cpu\" used by"
                + " CPAchecker. Example: using this option with value 15.6 leads to 15.6% of 900s"
                + " \"limits.time.cpu\" = 140s thread CPU time limit used. A maximum and minimum"
                + " can be set using the options \"time.cpu.thread.maximum\" and"
                + " \"time.cpu.thread.minimum\" respectively. The value of this option may not be"
                + " greater than 100 or less or equal to 0.")
    private double threadCpuTimeInPercentOfTotal = 100.00;
  }

  private static class ResourceLimitCheckRunnable implements Runnable {

    // All times in this class are nanoseconds,
    // as long as the variable name does not indicate otherwise.

    // A minimal interval of measuring granularity.
    // We never check the limits more often than this.
    // This could be made configurable if necessary.
    private static final long PRECISION = TimeUnit.MILLISECONDS.toNanos(500);

    private final ShutdownManager shutdownManager;

    private final ImmutableList<ResourceLimit> limits;

    ResourceLimitCheckRunnable(ShutdownManager pShutdownManager, List<ResourceLimit> pLimits) {
      shutdownManager = checkNotNull(pShutdownManager);
      limits = ImmutableList.copyOf(pLimits);
      checkArgument(!limits.isEmpty());
    }

    @Override
    public void run() {
      ShutdownRequestListener interruptThreadOnShutdown = interruptCurrentThreadOnShutdown();
      shutdownManager.getNotifier().registerAndCheckImmediately(interruptThreadOnShutdown);

      // Here we keep track of the next time we need to check each limit.
      final long[] timesOfNextCheck = new long[limits.size()];

      while (true) {
        final long currentTime = System.nanoTime();

        // Check limits
        int i = 0;
        for (ResourceLimit limit : limits) {
          if (currentTime < timesOfNextCheck[i]) {
            i++;
            continue;
          }

          // Check if expired
          final long currentValue = limit.getCurrentMeasurementValue();
          if (limit.isExceeded(currentValue)) {
            String reason = String.format("The %s has elapsed.", limit.getName());
            shutdownManager.requestShutdown(reason);
            return;
          }

          // Determine when to do the next check.
          // A negative of zero value is ignored here
          // because we anyway wait at least for PRECISION nanoseconds.
          final long nanosToNextCheck = limit.nanoSecondsToNextCheck(currentValue);
          timesOfNextCheck[i] = currentTime + nanosToNextCheck;

          i++;
        }

        // Sleep until next check
        long timeOfNextCheck = Longs.min(timesOfNextCheck);
        long nanosToSleep = Math.max(timeOfNextCheck - currentTime, PRECISION);
        long millisToSleep = LongMath.divide(nanosToSleep, 1000 * 1000, RoundingMode.UP);

        try {
          Thread.sleep(millisToSleep);
        } catch (InterruptedException e) {
          // Cancel requested by ResourceLimitChecker#cancel()
          shutdownManager.getNotifier().unregister(interruptThreadOnShutdown);
          return;
        }
      }
    }
  }
}
