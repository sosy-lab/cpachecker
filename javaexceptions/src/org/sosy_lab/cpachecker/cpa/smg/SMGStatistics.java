// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGIsLessOrEqual;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class SMGStatistics implements Statistics {

  final StatCounter abstractions = new StatCounter("Number of abstraction computations");
  final StatTimer totalAbstraction = new StatTimer("Total time for abstraction computation");

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    put(pOut, 0, SMGIsLessOrEqual.isLEQTimer);
    put(pOut, 1, SMGIsLessOrEqual.globalsTimer);
    put(pOut, 1, SMGIsLessOrEqual.stackTimer);
    put(pOut, 1, SMGIsLessOrEqual.heapTimer);
    put(pOut, 0, abstractions);
    put(pOut, 0, totalAbstraction);
  }

  @Override
  public @Nullable String getName() {
    return "SMGCPA";
  }
}
