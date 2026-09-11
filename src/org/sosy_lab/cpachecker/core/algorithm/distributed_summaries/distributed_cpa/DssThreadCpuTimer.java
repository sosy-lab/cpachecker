// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.time.Tickers;

/**
 * Measures the CPU time consumed by a single owning thread. Can be started and stopped multiple
 * times to accumulate time only across meaningful operations.
 */
public class DssThreadCpuTimer {

  private long sum;
  private long lastStart;
  private final String name;
  private boolean running;

  public DssThreadCpuTimer(String pName) {
    sum = 0;
    lastStart = 0;
    name = pName;
    running = false;
  }

  public void start() {
    checkState(!running, "Timer has already been started.");
    lastStart = Tickers.getCurrentThreadCputime().read();
    running = true;
  }

  public void stop() {
    checkState(running, "Timer needs to be started first.");
    sum += Tickers.getCurrentThreadCputime().read() - lastStart;
    running = false;
  }

  public long nanos() {
    return sum;
  }

  public String getName() {
    return name;
  }
}
