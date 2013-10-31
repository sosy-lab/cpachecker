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
package org.sosy_lab.cpachecker.cmdline;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier.ShutdownRequestListener;

import com.google.common.base.Joiner;

/**
 * This class implements a mechanism to forcefully terminate CPAchecker
 * and even the whole JVM some time after a shutdown request was received
 * and the analysis did not terminate gracefully.
 */
class ForceTerminationOnShutdown implements Runnable {

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
          logger.log(Level.WARNING, "Shutdown requested",
              "(" + pReason + "),",
              "waiting for termination.");
          Thread t = new Thread(new ForceTerminationOnShutdown(logger,
                                                               mainThread,
                                                               shutdownHook),
                                "ForceTerminationOnShutdown");
          t.setDaemon(true);
          t.start();
        }
      };
  }

  @SuppressWarnings("deprecation")
  @Override
  public void run() {
    // This thread may be killed at any time by the JVM because it is a daemon thread.

    try {
      TimeUnit.SECONDS.sleep(SHUTDOWN_GRACE_PERIOD);
    } catch (InterruptedException e) {
      return; // Cancel termination
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
