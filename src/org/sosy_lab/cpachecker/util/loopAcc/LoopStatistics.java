// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.loopAcc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/** Class that returns important statistics for loops */
public class LoopStatistics implements Statistics {

  List<LoopData> loopList;
  private final String NAME = "LoopStatistics ";

  private List<String[]> loopStartAndEnd;
  private List<String[]> loopType;
  private List<String[]> loopCondition;
  private List<String[]> failedState;
  private List<String[]> pathsNumber;
  private List<String[]> loopNodes;
  private List<String[]> ioVariables;
  private List<String[]> oVariables;
  private List<String[]> accelerationPossible;

  public LoopStatistics(List<LoopData> loopList) {
    this.loopList = loopList;
    usefulLoopStatistics();
  }

  /**
   * Method that collects the data from all the loops in multiple arraylists to be used in a
   * statisticsoutput
   */
  private void usefulLoopStatistics() {
    loopStartAndEnd = new ArrayList<>();
    loopType = new ArrayList<>();
    loopCondition = new ArrayList<>();
    failedState = new ArrayList<>();
    pathsNumber = new ArrayList<>();
    loopNodes = new ArrayList<>();
    ioVariables = new ArrayList<>();
    oVariables = new ArrayList<>();
    accelerationPossible = new ArrayList<>();

    for (int i = 0; i < loopList.size(); i++) {
      if (!loopList.isEmpty()) {
        String[] x = {
          "L" + (i + 1),
          loopList.get(i).getLoopStart().toString(),
          loopList.get(i).getLoopEnd().toString()
        };
        loopStartAndEnd.add(x);
        String[] y = {
          "L" + (i + 1), loopList.get(i).getLoopType(), "" + loopList.get(i).getLoopInLoop()
        };
        loopType.add(y);
        String[] loopCond = {"L" + (i + 1), loopList.get(i).getCondition()};
        loopCondition.add(loopCond);
        if (loopList.get(i).getFaileState() != null) {
          String[] failed = {"L" + (i + 1), loopList.get(i).getFaileState().toString()};
          failedState.add(failed);
        }
        String[] paths = {"L" + (i + 1), "" + loopList.get(i).getAmountOfPaths()};
        pathsNumber.add(paths);
        String[] nodes = {"L" + (i + 1), loopList.get(i).getNodesInLoop().toString()};
        loopNodes.add(nodes);
        String[] io = {"L" + (i + 1), loopList.get(i).getInputsOutputs().toString()};
        ioVariables.add(io);
        String[] o = {"L" + (i + 1), loopList.get(i).getOutputs().toString()};
        oVariables.add(o);
        String[] acc = {"L" + (i + 1), "" + loopList.get(i).getCanBeAccelerated()};
        accelerationPossible.add(acc);
      }
    }
  }

  private String ArrayToString(List<String[]> x) {
    String ergebnis = "";
    for (String[] sa : x) {
      ergebnis += "[";
      for (String value : sa) {
        ergebnis += (value + ",");
      }
      ergebnis += "],";
    }
    return ergebnis;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("Startnode and Endnode:" + ArrayToString(loopStartAndEnd));
    pOut.println("Looptype and Innerloop:" + ArrayToString(loopType));
    pOut.println("Loopcondition:" + ArrayToString(loopCondition));
    pOut.println("Failed loop state:" + ArrayToString(failedState));
    pOut.println("Amount of paths in loop:" + ArrayToString(pathsNumber));
    pOut.println("Nodes in loop:" + ArrayToString(loopNodes));
    pOut.println("IO-Variables:" + ArrayToString(ioVariables));
    pOut.println("Outputvariables:" + ArrayToString(oVariables));
    pOut.println("Can loop be accelerated:" + ArrayToString(accelerationPossible));
  }

  @Override
  public @Nullable String getName() {
    // TODO Auto-generated method stub
    return NAME;
  }
}
