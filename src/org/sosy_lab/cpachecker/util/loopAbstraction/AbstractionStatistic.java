// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.loopAbstraction;

import java.io.PrintStream;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class AbstractionStatistic implements Statistics {

  private TimeSpan timeToAbstract;
  private static final String name = "Time taken to abstract loops";

  public AbstractionStatistic(TimeSpan timeToAbstract) {
    this.timeToAbstract = timeToAbstract;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    if (timeToAbstract != null) {
      pOut.println("Time to abstract loops:" + timeToAbstract);
    } else {
      pOut.println("Time to abstract loops:");
    }
  }

  @Override
  public String getName() {
    return name;
  }
}
