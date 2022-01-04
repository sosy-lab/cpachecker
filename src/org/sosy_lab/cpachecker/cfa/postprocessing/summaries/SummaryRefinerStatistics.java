// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class SummaryRefinerStatistics implements Statistics {

  private Integer doubleRefinementsMade = 0;

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("Number of Double Refinements:          " + doubleRefinementsMade);
  }

  public void increaseDoubleRefinements() {
    this.doubleRefinementsMade += 1;
  }

  @Override
  public String getName() {
    return "Summary refiner statistics";
  }
}
