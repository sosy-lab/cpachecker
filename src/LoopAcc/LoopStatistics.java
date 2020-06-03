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

public class LoopStatistics implements Statistics {

  ArrayList<LoopData> loopList;
  private final String NAME = "LoopStatistics ";

  public LoopStatistics(ArrayList<LoopData> loopList) {
    this.loopList = loopList;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    int i = 1;
    for (LoopData loop : loopList) {
      pOut.println("Loop " + i + " : ");
      pOut.println("Startnode of the loop is: " + loop.getLoopStart());
      pOut.println("Endnode of the loop is: " + loop.getLoopEnd());
      pOut.println("Looptype: " + loop.getLoopType());
      pOut.println("Loopcondition: " + loop.getCondition());
      pOut.println("Failed loop state: " + loop.getFaileState());
      pOut.println("Is it a loop in a loop: " + loop.getLoopInLoop());
      pOut.println("Amount of paths in loop: " + loop.getAmountOfPaths());
      pOut.println("Nodes in loop: " + loop.getNodesInLoop().toString());
      pOut.println("IO-Variables: " + loop.getInputsOutputs());
      pOut.println("Outputvariables: " + loop.getOutputs().toString());
      pOut.println("Can loop be accelerated: " + loop.getCanBeAccelerated() + "\n");

      i++;
    }
  }

  @Override
  public @Nullable String getName() {
    // TODO Auto-generated method stub
    return NAME;
  }

}
