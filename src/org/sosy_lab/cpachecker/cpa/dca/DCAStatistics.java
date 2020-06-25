// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dca;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisStatistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

class DCAStatistics extends LassoAnalysisStatistics {

  @SuppressWarnings("unused")
  private final DCACPA dcaCpa;

  final Timer refinementTotalTimer = new Timer();

  public DCAStatistics(DCACPA pCpa) {
    dcaCpa = pCpa;
  }

  @Override
  @Nullable
  public String getName() {
    return "DCARefiner";
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("  Refinment total time:      " + refinementTotalTimer);
    pOut.println();
  }
}
