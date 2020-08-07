// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
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
  public static void explainDeltas(
      List<CFAEdge> counterexample,
      List<CFAEdge> closestExecution,
      LogManager logger) {
    logger.log(Level.INFO, "Explain Tool Started");
    counterexample = cleanPath(counterexample);
    closestExecution = cleanPath(closestExecution);
    List<CFAEdge> deltasCe = new ArrayList<>();
    List<CFAEdge> deltasSp = new ArrayList<>();

    for (int i = 0; i < counterexample.size(); i++) {
      if (!closestExecution.contains(counterexample.get(i))) {
        deltasCe.add(counterexample.get(i));
      }
    }

    for (int i = 0; i < closestExecution.size(); i++) {
      if (!counterexample.contains(closestExecution.get(i))) {
        deltasSp.add(closestExecution.get(i));
      }
    }
    logger.log(Level.INFO, "COUNTEREXAMPLE DIFFERENCES");
    for (int i = 0; i < deltasCe.size(); i++) {
      logger.log(Level.INFO,
          deltasCe.get(i).getLineNumber() + ": " + deltasCe.get(i).getDescription());
    }
    logger.log(Level.INFO, "-------------------------------------------");
    logger.log(Level.INFO, "CLOSEST SUCCESSFUL EXECUTION DIFFERENCES");
    for (int i = 0; i < deltasSp.size(); i++) {
      logger.log(Level.INFO,
          deltasSp.get(i).getLineNumber() + ": " + deltasSp.get(i).getDescription());
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

    //TODO: Review: filteredEdges contain all the Edges of a path up until the Node that VERIFIES the spec
    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(flow.get(i).getCode());
        if (!code.isEmpty()) {
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
