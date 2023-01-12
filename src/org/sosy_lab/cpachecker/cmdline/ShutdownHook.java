// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cmdline;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.ShutdownManager;

/**
 * This class is a thread which should be registered as a VM shutdown hook. It serves two purposes:
 *
 * <ul>
 *   <li>It allows us to handle the user pressing Ctrl+C: The JVM will start the shutdown hook in
 *       this case and it will call {@link ShutdownManager#requestShutdown(String)}.
 *   <li>It blocks the JVM from terminating before the main thread had the chance to finish.
 * </ul>
 *
 * <p>The first goal is only desired while the main analysis is running, not during statistics: If
 * we have come that far, we prefer writing them out fully. So we provide a flag to disable this
 * behavior.
 *
 * <p>The second goal is only partially desired while the main analysis is running: We do want to
 * give the main thread with the main analysis the chance to terminate properly, but only within
 * some grace period. If it is hanging for longer (e.g., within native code that does not respect
 * our shutdown manager), we want to terminate forcefully. So we provide a method to disable and
 * stop this shutdown hook immediately.
 */
class ShutdownHook extends Thread {

  private final Thread mainThread;
  private final ShutdownManager shutdownManager;

  // These two fields indicate whether the respective feature is enabled, not whether we should
  // actually do it right now.
  // Their values are monotonic (true -> false).
  private volatile boolean requestShutdown = true;
  private volatile boolean blockJvmExit = true;

  /**
   * Create a shutdown hook. This constructor needs to be called from the thread in which CPAchecker
   * is run. It will block the JVM from existing as long as that thread is executing.
   */
  public ShutdownHook(ShutdownManager pShutdownManager) {
    super("Shutdown Hook");
    shutdownManager = checkNotNull(pShutdownManager);

    mainThread = Thread.currentThread();
  }

  /**
   * Completely disable this shutdown hook. It will neither issue shutdown requests nor block the
   * JVM from exiting, and it will terminate itself as soon as possible if it is already running.
   */
  public void disableAndStop() {
    // Inverse order of operations in run() is used here to be safe against races.
    // But actually only interrupt() needs to come last.
    blockJvmExit = false;
    requestShutdown = false;
    interrupt(); // in case it is already running
  }

  /**
   * Prevent this shutdown hook from issuing shutdown requests. It will still try to block them JVM
   * from exiting as long as the main thread is running.
   */
  public void disableShutdownRequests() {
    requestShutdown = false;
  }

  // We want to use Thread.stop() to force the main thread to stop
  // when interrupted by the user.
  @SuppressWarnings("ThreadJoinLoop") // interrupt is used on purpose by disableAndStop()
  @Override
  public void run() {

    if (requestShutdown && mainThread.isAlive()) {
      // probably the user pressed Ctrl+C
      shutdownManager.requestShutdown(
          "The JVM is shutting down, probably because Ctrl+C was pressed.");
    }

    if (blockJvmExit) {
      // Keep this thread alive to that the main thread has the chance to
      // print the statistics.
      // (This thread should be the only thing that prevents the JVM
      // from immediate termination.)
      try {
        mainThread.join();
      } catch (InterruptedException expected) {
        // expected
      }
    }
  }
}
