/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;


public class CFAStatistics implements Statistics {

  private final CFA cfa;

  public CFAStatistics(CFA pCfa) {
    this.cfa = pCfa;
  }

  private static class FunctionCfaStat {
    public int numberOfNodes = 0;
    public int numberOfEdges = 0;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    Map<String,FunctionCfaStat> functionCfaStats = new HashMap<String, CFAStatistics.FunctionCfaStat>();
    int[] numberOfEdgesByType = new int[CFAEdgeType.values().length];
    int numberOfCfaEdges = 0;
    int numberOfBranches = 0;
    for (CFANode node : cfa.getAllNodes()) {
      String functionName = node.getFunctionName();
      FunctionCfaStat functionStat = functionCfaStats.get(functionName);
      if (functionStat == null) {
        functionStat = new FunctionCfaStat();
        functionCfaStats.put(functionName, functionStat);
      }

      functionStat.numberOfNodes++;
      int numberOfLeavingEdges = node.getNumLeavingEdges();
      if (numberOfLeavingEdges > 1) {
          numberOfBranches++;
      }
      for (int i=0; i<numberOfLeavingEdges; i++) {
        CFAEdge outgoingEdge = node.getLeavingEdge(i);
        numberOfEdgesByType[outgoingEdge.getEdgeType().ordinal()]++;
        numberOfCfaEdges++;
        functionStat.numberOfEdges++;
      }
    }

    double edgeNodeFractionSum = 0.0f;
    for (String functionName: functionCfaStats.keySet()) {
      FunctionCfaStat functionStat = functionCfaStats.get(functionName);
      if (functionStat.numberOfNodes > 0) {
        edgeNodeFractionSum += functionStat.numberOfEdges / functionStat.numberOfNodes;
      }
    }

    pOut.println("Number of functions:          " + cfa.getNumberOfFunctions());
    pOut.println("Number of CFA nodes:          " + cfa.getAllNodes().size());
    pOut.println("Number of CFA edges:          " + numberOfCfaEdges);
    pOut.println("Number of CFA branches:       " + numberOfBranches);
    pOut.println("Edge node fraction :          " + edgeNodeFractionSum);
    pOut.println("Cyclomatic complexity:        " + (numberOfCfaEdges -  cfa.getAllNodes().size() + 2));


    for (CFAEdgeType type : CFAEdgeType.values()) {
      pOut.println(String.format("%-30s %d", "Number of " + type.toString() + ":", numberOfEdgesByType[type.ordinal()]));
    }
  }

  @Override
  public String getName() {
    return "CFA";
  }

}
