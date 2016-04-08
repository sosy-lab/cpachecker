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
package org.sosy_lab.cpachecker.cmdline;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.ShutdownManager;


/**
 * This class is a thread which should be registered as a VM shutdown hook.
 * It will try to stop the analysis when the user presses Ctrl+C.
 */
class ShutdownHook extends Thread {

  private final Thread mainThread;
  private final ShutdownManager shutdownManager;

  private volatile boolean enabled = true;

  /**
   * Create a shutdown hook. This constructor needs to be called from the
   * thread in which CPAchecker is run.
   */
  public ShutdownHook(ShutdownManager pShutdownManager) {
    super("Shutdown Hook");
    shutdownManager = checkNotNull(pShutdownManager);

    mainThread = Thread.currentThread();
  }

  public void disableAndStop() {
    enabled = false;
    this.interrupt(); // in case it is already running
  }

  public void disable() {
    enabled = false;
  }

  // We want to use Thread.stop() to force the main thread to stop
  // when interrupted by the user.
  @Override
  public void run() {

    if (enabled && mainThread.isAlive()) {
      // probably the user pressed Ctrl+C
      shutdownManager.requestShutdown(
          "The JVM is shutting down, probably because Ctrl+C was pressed.");

      // Keep this thread alive to that the main thread has the chance to
      // print the statistics.
      // (This thread should be the only thing that prevents the JVM
      // from immediate termination.)
      try {
        mainThread.join();
      } catch (InterruptedException e) {}
    }
  }
}