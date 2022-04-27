// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;

public class StatTimer extends AbstractStatValue {

  private final Timer timer = new Timer();

  public StatTimer(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public StatTimer(String pTitle) {
    super(StatKind.SUM, pTitle);
  }

  public void start() {
    timer.start();
  }

  public void stop() {
    timer.stop();
  }

  @Override
  public int getUpdateCount() {
    return timer.getNumberOfIntervals();
  }

  @Override
  public String toString() {
    return timer.toString();
  }

  public TimeSpan getConsumedTime() {
    return timer.getSumTime();
  }

  public TimeSpan getMaxTime() {
    return timer.getMaxTime();
  }
}
