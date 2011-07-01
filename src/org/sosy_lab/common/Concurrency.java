/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Helper methods for concurrency related things.
 */
public class Concurrency {

  private Concurrency() { }

  /**
   * Wait uninterruptibly until an ExecutorService has shutdown.
   * It also ensures full memory visibility of everything that was done in
   * the callables.
   *
   * Interrupting the thread will have no effect, but this method
   * will set the thread's interrupted flag in this case.
   */
  public static void waitForTermination(ExecutorService executor) {
    boolean interrupted = Thread.interrupted();

    while (!executor.isTerminated()) {
      try {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      } catch (InterruptedException _) {
        interrupted = true;
      }
    }

    // now all tasks have terminated

    // restore interrupted flag
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
