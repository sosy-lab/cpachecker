// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.loopInformation;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/** Class that returns important statistics for loops */
public class LoopStatistics implements Statistics {

  private final String NAME;
  private TimeSpan timeToAnalyze;
  private LoopData loopData;

  public LoopStatistics(LoopData pLoopData) {
    NAME = "Loop " + pLoopData.getLoopStart().toString();
    timeToAnalyze = pLoopData.getAnalyzeTime();
    loopData = pLoopData;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("Startnode:                    " + loopData.getLoopStart());
    pOut.println("Endnode:                      " + loopData.getLoopEnd());
    pOut.println("Looptype:                     " + loopData.getLoopType());
    pOut.println("Innerloop:                    " + loopData.getInnerLoop());
    pOut.println("Loopcondition:                " + loopData.getCondition());
    pOut.println("Failed loop state:            " + loopData.getFaileState());
    pOut.println("Amount of paths in loop:      " + loopData.getAmountOfPaths());
    pOut.println("Nodes in loop:                " + loopData.getNodesInLoop());
    pOut.println("IO-Variables:                 " + loopData.getInputsOutputs());
    pOut.println("Outputvariables:              " + loopData.getOutputs());
    pOut.println("Can loop be accelerated:      " + loopData.getCanBeAccelerated());
    pOut.println("Time to analyze loop in ms:   " + timeToAnalyze);
  }

  @Override
  public @Nullable String getName() {
    return NAME;
  }
}
