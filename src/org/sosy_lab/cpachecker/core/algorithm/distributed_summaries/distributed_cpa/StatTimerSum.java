// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class StatTimerSum extends StatTimer {

  private final List<StatTimer> timers;

  public enum StatTimerType {
    COMBINE,
    SERIALIZE,
    DESERIALIZE,
    PROCEED_F,
    PROCEED_B
  }

  public StatTimerSum(String pTitle) {
    super(pTitle);
    timers = new ArrayList<>();
  }

  public void register(StatTimer pStatTimer) {
    timers.add(pStatTimer);
  }

  @Override
  public int getUpdateCount() {
    return timers.stream().mapToInt(timer -> timer.getUpdateCount()).sum();
  }

  @Override
  public String toString() {
    return getConsumedTime().formatAs(TimeUnit.MINUTES)
        + " ("
        + getConsumedTime().formatAs(TimeUnit.SECONDS)
        + ")";
  }

  @Override
  public TimeSpan getConsumedTime() {
    return timers.stream()
        .map(t -> t.getConsumedTime())
        .reduce(TimeSpan::sum)
        .orElse(super.getConsumedTime());
  }

  @Override
  public TimeSpan getMaxTime() {
    return timers.stream()
        .map(t -> t.getMaxTime())
        .max(Comparator.naturalOrder())
        .orElse(super.getMaxTime());
  }
}
