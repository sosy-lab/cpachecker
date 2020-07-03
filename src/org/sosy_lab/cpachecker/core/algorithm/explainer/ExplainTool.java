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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;

/**
 * This Class Explains the Differences Between 2 Executions
 */
public class ExplainTool {

  /**
   * Takes a CE and a Safe Path and Prints the Differences between them
   *
   * @param counterexample   The counterexample that we want to examine
   * @param closestExecution The closest successful found execution
   * @param logger           For Printing
   */
  public static void ExplainDeltas(
      List<CFAEdge> counterexample,
      List<CFAEdge> closestExecution,
      LogManager logger) {
    logger.log(Level.INFO, "Explain Tool Started");
    counterexample = cleanPath(counterexample);
    closestExecution = cleanPath(closestExecution);
    List<CFAEdge> deltas_ce = new ArrayList<>();
    List<CFAEdge> deltas_sp = new ArrayList<>();

    for (int i = 0; i < counterexample.size(); i++) {
      if (!closestExecution.contains(counterexample.get(i))) {
        deltas_ce.add(counterexample.get(i));
      }
    }

    for (int i = 0; i < closestExecution.size(); i++) {
      if (!counterexample.contains(closestExecution.get(i))) {
        deltas_sp.add(closestExecution.get(i));
      }
    }
    logger.log(Level.INFO, "COUNTEREXAMPLE DIFFERENCES");
    for (int i = 0; i < deltas_ce.size(); i++) {
      logger.log(Level.INFO,
          deltas_ce.get(i).getLineNumber() + ": " + deltas_ce.get(i).getDescription());
    }
    logger.log(Level.INFO, "-------------------------------------------");
    logger.log(Level.INFO, "CLOSEST SUCCESSFUL EXECUTION DIFFERENCES");
    for (int i = 0; i < deltas_sp.size(); i++) {
      logger.log(Level.INFO,
          deltas_sp.get(i).getLineNumber() + ": " + deltas_sp.get(i).getDescription());
    }

  }

  /**
   * Filters the Path to STOP at the "__VERIFIER_ASSERT" Node
   *
   * @param path The path that we want to filter
   * @return the same path but without the nodes after the Assertion
   */
  private static List<CFAEdge> cleanPath(List<CFAEdge> path) {
    List<CFAEdge> flow = path;
    List<CFAEdge> filteredEdges = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(flow.get(i).getCode());
        if (code.size() > 0) {
          if (code.get(0).equals("__VERIFIER_assert")) {
            filteredEdges.add(flow.get(i));
            return filteredEdges;
          }
        }
      }
      filteredEdges.add(flow.get(i));
    }
    return filteredEdges;
  }

}
