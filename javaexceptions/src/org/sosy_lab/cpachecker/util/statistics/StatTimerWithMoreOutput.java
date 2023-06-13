// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import java.util.concurrent.TimeUnit;

public class StatTimerWithMoreOutput extends StatTimer {

  public StatTimerWithMoreOutput(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public StatTimerWithMoreOutput(String pTitle) {
    super(StatKind.SUM, pTitle);
  }

  @Override
  public String toString() {
    return String.format(
        "%s (max: %s, count: %s)",
        super.toString(), getMaxTime().formatAs(TimeUnit.SECONDS), getUpdateCount());
  }
}
