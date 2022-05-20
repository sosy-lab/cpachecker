// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cmdline;

import com.google.common.base.Joiner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.annotations.SuppressForbidden;
import org.sosy_lab.common.log.LogManager;

/**
 * This class implements a mechanism to forcefully terminate CPAchecker
 * and even the whole JVM some time after a shutdown request was received
 * and the analysis did not terminate gracefully.
 */
class ForceTerminationOnShutdown implements Runnable {

  // static state, currently only one instance can be alive
  // (which is fully sufficient given that all instance would just kill the JVM)
  // We need this instance to be able to cancel the forced termination.
  private static final AtomicReference<Thread> forceTerminationOnShutdownThread = new AtomicReference<>();

  private static final AtomicBoolean canceled = new AtomicBoolean();

  // Time that a shutdown may last before we kill the program.
  private static final int SHUTDOWN_GRACE_PERIOD = 10; // seconds
  private static final int SHUTDOWN_GRACE_PERIOD_2 = 1; // seconds

  private final LogManager logger;
  private final Thread mainThread;
  private final ShutdownHook shutdownHook;

  private ForceTerminationOnShutdown(LogManager pLogger, Thread pMainThread,
      ShutdownHook pShutdownHook) {
    logger = pLogger;
    mainThread = pMainThread;
    shutdownHook = pShutdownHook;
  }

  /**
   * Create a {@link ShutdownRequestListener} that will kill the current thread
   * and the JVM after some time.
   * When doing so, it will disable the given shutdown hook.
   */
  static ShutdownRequestListener createShutdownListener(
      final LogManager logger, final ShutdownHook shutdownHook) {
    final Thread mainThread = Thread.currentThread();
    return new ShutdownRequestListener() {

      @Override
      public void shutdownRequested(final String pReason) {
        if (forceTerminationOnShutdownThread.get() != null) {
          logger.log(
              Level.WARNING,
              "Shutdown requested",
              "(" + pReason + "),",
              "but there is already a thread waiting to terminate the JVM.");
          return;
        }
        if (canceled.get()) {
          return;
        }

        logger.log(
            Level.WARNING, "Shutdown requested", "(" + pReason + "),", "waiting for termination.");
        Thread t =
            Concurrency.newDaemonThread(
                "ForceTerminationOnShutdown",
                new ForceTerminationOnShutdown(logger, mainThread, shutdownHook));
        boolean success = forceTerminationOnShutdownThread.compareAndSet(null, t);
        if (success) {
          t.start();
        }
        // Otherwise a second instance of such a thread was created in the meantime,
        // we do not need to start our's.
      }
    };
  }

  /**
   * If a shutdown request was signalled,
   * and we are currently waiting to kill the JVM after some time,
   * this method cancels the pending termination process,
   * so that no action will be done.
   */
  static void cancelPendingTermination() {
    canceled.set(true);
    Thread t = forceTerminationOnShutdownThread.getAndSet(null);
    if (t != null) {
      t.interrupt();
    }
  }

  @SuppressWarnings({"deprecation", "DeprecatedThreadMethods"})
  @SuppressForbidden("need to call Thread.stop")
  @SuppressFBWarnings("DM_EXIT")
  @Override
  public void run() {
    // This thread may be killed at any time by the JVM because it is a daemon thread.
    // Interrupts signal that we should abort.

    try {
      TimeUnit.SECONDS.sleep(SHUTDOWN_GRACE_PERIOD);
    } catch (InterruptedException e) {
      return; // Cancel termination
    }
    if (canceled.get()) {
      return;
    }
    logger.log(Level.WARNING, "Shutdown was requested but CPAchecker is still running after",
        SHUTDOWN_GRACE_PERIOD + "s, forcing immediate termination now.");

    if (mainThread.isAlive()) {
      logger.log(Level.INFO, "For your information: CPAchecker is currently hanging at\n",
          Joiner.on('\n').join(mainThread.getStackTrace()), "\n");

    } else {
      logger.log(Level.INFO, "For your information: CPAchecker is currently hanging because the following threads did not yet terminate:\n",
          buildLiveThreadInfo());
    }

    logger.flush();

    // Now we need to kill the JVM.

    // If the main thread hangs in Java code, this will work:
    mainThread.stop();
    try {
      TimeUnit.SECONDS.sleep(SHUTDOWN_GRACE_PERIOD_2);
    } catch (InterruptedException e) {
      return;
    }
    if (canceled.get()) {
      return;
    }

    // If we come here, stop() did not work,
    // probably because the main thread hangs in native code.
    // System.exit() will work, but there is a ShutdownHook
    // that blocks it until the main thread terminates,
    // so we have to disable that ShutdownHook.

    shutdownHook.disableAndStop();
    System.exit(0);
  }

  /**
   * Create a short summary of all running non-daemon threads
   * (those threads are prevent JVM termination).
   */
  private StringBuilder buildLiveThreadInfo() {
    Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
    StringBuilder output = new StringBuilder();
    for (Map.Entry<Thread, StackTraceElement[]> threadInfo : traces.entrySet()) {
      Thread thread = threadInfo.getKey();
      StackTraceElement[] trace = threadInfo.getValue();
      if (thread.isAlive() && !thread.isDaemon()) {
        output.append(thread);
        if (trace.length > 0) {
            output.append(" at " + trace[0]);
        }
        output.append("\n");
      }
    }
    return output;
  }
}
