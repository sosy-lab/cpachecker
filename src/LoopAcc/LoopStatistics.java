/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package LoopAcc;

import java.io.PrintStream;
import java.util.ArrayList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * Class that returns important statistics for loops
 */
public class LoopStatistics implements Statistics {

  ArrayList<LoopData> loopList;
  private final String NAME = "LoopStatistics ";

  private ArrayList<String[]> loopStartAndEnd;
  private ArrayList<String[]> loopType;
  private ArrayList<String[]> loopCondition;
  private ArrayList<String[]> failedState;
  private ArrayList<String[]> pathsNumber;
  private ArrayList<String[]> loopNodes;
  private ArrayList<String[]> ioVariables;
  private ArrayList<String[]> oVariables;
  private ArrayList<String[]> accelerationPossible;

  public LoopStatistics(ArrayList<LoopData> loopList) {
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
        String[] x =
            {"L" + (i + 1), loopList.get(i).getLoopStart().toString(),
                loopList.get(i).getLoopEnd().toString()};
        loopStartAndEnd.add(x);
        String[] y =
            {"L" + (i + 1), loopList.get(i).getLoopType(), "" + loopList.get(i).getLoopInLoop()};
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

  private String ArrayToString(ArrayList<String[]> x) {
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
