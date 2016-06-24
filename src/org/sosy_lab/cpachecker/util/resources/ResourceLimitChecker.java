/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.ShutdownNotifier.interruptCurrentThreadOnShutdown;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.LongMath;
import com.google.common.primitives.Longs;

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

import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.management.JMException;

/**
 * This class handles one or more limits for the usage of external resources.
 * It does not contain code for any specific limits,
 * but instead uses instances of {@link ResourceLimit}.
 * It periodically checks whether any of the given limits is exceeded,
 * and signals this via an instance of {@link ShutdownNotifier}.
 */
public final class ResourceLimitChecker {

  private final Thread thread;

  /**
   * Create a new instance with a list of limits to check and a {@link ShutdownNotifier}
   * that gets notified when a limit is exceeded.
   * It is safe (but useless) to call this constructor with an empty list of limits,
   * limits that have already been exceeded, or a shutdown notifier that has
   * already been triggered.
   * @param shutdownManager A non-null shutdown notifier instance.
   * @param limits A (possibly empty) list without null entries of resource limits.
   */
  public ResourceLimitChecker(ShutdownManager shutdownManager, List<ResourceLimit> limits) {
    checkNotNull(shutdownManager);
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
   * Actually start enforcing the limits.
   * May be called only once.
   */
  public void start() {
    if (thread != null) {
      thread.start();
    }
  }

  /**
   * Cancel enforcing the limits (without triggering the stop request if not done before).
   */
  public void cancel() {
    if (thread != null) {
      thread.interrupt();
    }
  }

  /**
   * Create an instance of this class from some configuration options.
   * The returned instance is not started yet.
   */
  public static ResourceLimitChecker fromConfiguration(
      Configuration config, LogManager logger, ShutdownManager shutdownManager)
      throws InvalidConfigurationException {

    ResourceLimitOptions options = new ResourceLimitOptions();
    config.inject(options);

    ImmutableList.Builder<ResourceLimit> limits = ImmutableList.builder();
    if (options.walltime.compareTo(TimeSpan.empty()) >= 0) {
      limits.add(WalltimeLimit.fromNowOn(options.walltime));
    }
    if (options.cpuTime.compareTo(TimeSpan.empty()) >= 0) {
      try {
        limits.add(ProcessCpuTimeLimit.fromNowOn(options.cpuTime));
      } catch (JMException e) {
        logger.logDebugException(e, "Querying cpu time failed");
        logger.log(Level.WARNING, "Your Java VM does not support measuring the cpu time, cpu time threshold disabled.");
      }
    }

    ImmutableList<ResourceLimit> limitsList = limits.build();
    if (!limitsList.isEmpty()) {
      logger.log(
          Level.INFO,
          "Using the following resource limits:",
          Joiner.on(", ").join(Lists.transform(limitsList, ResourceLimit::getName)));
    }
    return new ResourceLimitChecker(shutdownManager, limitsList);
  }

  @Options(prefix="limits")
  private static class ResourceLimitOptions {

    @Option(secure=true, name="time.wall",
        description="Limit for wall time used by CPAchecker (use seconds or specify a unit; -1 for infinite)")
    @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
        defaultUserUnit=TimeUnit.SECONDS,
        min=-1)
    private TimeSpan walltime = TimeSpan.ofNanos(-1);

    @Option(secure=true, name="time.cpu",
        description="Limit for cpu time used by CPAchecker (use seconds or specify a unit; -1 for infinite)")
    @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
        defaultUserUnit=TimeUnit.SECONDS,
        min=-1)
    private TimeSpan cpuTime = TimeSpan.ofNanos(-1);

  }

  private static class ResourceLimitCheckRunnable implements Runnable {

    // All times in this class are nanoseconds,
    // as long as the variable name does not indicate otherwise.

    // A minimal interval of measuring granularity.
    // We never check the limits more often than this.
    // This could be made configurable if necessary.
    private final static long PRECISION = TimeUnit.MILLISECONDS.toNanos(500);

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
          final long currentValue = limit.getCurrentValue();
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
        long nanosToSleep = Math.max(timeOfNextCheck-currentTime, PRECISION);
        long millisToSleep = LongMath.divide(nanosToSleep, 1000*1000, RoundingMode.UP);

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
