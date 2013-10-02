/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.core.ShutdownNotifier.interruptCurrentThreadOnShutdown;

import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.ShutdownNotifier.ShutdownRequestListener;

import com.google.common.collect.ImmutableList;
import com.google.common.math.LongMath;
import com.google.common.primitives.Longs;

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
   * @param shutdownNotifier A non-null shutdown notifier instance.
   * @param limits A (possibly empty) list without null entries of resource limits.
   */
  public ResourceLimitChecker(ShutdownNotifier shutdownNotifier, List<ResourceLimit> limits) {
    checkNotNull(shutdownNotifier);
    if (limits.isEmpty() || shutdownNotifier.shouldShutdown()) {
      // limits are irrelevant
      thread = null;

    } else {
      Runnable runnable = new ResourceLimitCheckRunnable(shutdownNotifier, limits);

      thread = new Thread(runnable, "Resource limit checker");
      thread.setDaemon(true);
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

  private static class ResourceLimitCheckRunnable implements Runnable {

    // All times in this class are nanoseconds,
    // as long as the variable name does not indicate otherwise.

    // A minimal interval of measuring granularity.
    // We never check the limits more often than this.
    // This could be made configurable if necessary.
    private final static long PRECISION = TimeUnit.MILLISECONDS.toNanos(500);

    private final ShutdownNotifier toNotify;

    private final ImmutableList<ResourceLimit> limits;

    ResourceLimitCheckRunnable(ShutdownNotifier pToNotify, List<ResourceLimit> pLimits) {
      toNotify = checkNotNull(pToNotify);
      limits = ImmutableList.copyOf(pLimits);
      checkArgument(!limits.isEmpty());
    }

    @Override
    public void run() {
      ShutdownRequestListener interruptThreadOnShutdown = interruptCurrentThreadOnShutdown();
      toNotify.registerAndCheckImmediately(interruptThreadOnShutdown);

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
            toNotify.requestShutdown(reason);
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
          toNotify.unregister(interruptThreadOnShutdown);
          return;
        }
      }
    }
  }
}
