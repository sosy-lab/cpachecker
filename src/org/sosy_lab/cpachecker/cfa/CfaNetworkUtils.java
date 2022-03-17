// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public final class CfaNetworkUtils {

  private CfaNetworkUtils() {}

  public static Optional<FunctionExitNode> getFunctionExitNode(
      CfaNetwork pCfaNetwork, FunctionEntryNode pFunctionEntryNode) {

    Set<CFANode> waitlisted = new HashSet<>(ImmutableList.of(pFunctionEntryNode));
    Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

    while (!waitlist.isEmpty()) {

      CFANode node = waitlist.remove();

      if (node instanceof FunctionExitNode) {
        return Optional.of((FunctionExitNode) node);
      }

      for (CFAEdge outEdge : pCfaNetwork.outEdges(node)) {
        if (!(outEdge instanceof FunctionCallEdge)) {
          CFANode successor = pCfaNetwork.incidentNodes(outEdge).target();
          if (waitlisted.add(successor)) {
            waitlist.add(successor);
          }
        }
      }
    }

    return Optional.empty();
  }

  public static Optional<FunctionSummaryEdge> getFunctionSummaryEdge(
      CfaNetwork pCfaNetwork, FunctionCallEdge pFunctionCallEdge) {

    CFANode functionCallEdgePredecessor = pCfaNetwork.incidentNodes(pFunctionCallEdge).source();

    for (CFAEdge outEdge : pCfaNetwork.outEdges(functionCallEdgePredecessor)) {
      if (outEdge instanceof FunctionSummaryEdge) {
        return Optional.of((FunctionSummaryEdge) outEdge);
      }
    }

    return Optional.empty();
  }

  public static Optional<FunctionSummaryEdge> getFunctionSummaryEdge(
      CfaNetwork pCfaNetwork, FunctionReturnEdge pFunctionReturnEdge) {

    CFANode functionReturnEdgeSuccessor = pCfaNetwork.incidentNodes(pFunctionReturnEdge).target();

    for (CFAEdge inEdge : pCfaNetwork.inEdges(functionReturnEdgeSuccessor)) {
      if (inEdge instanceof FunctionSummaryEdge) {
        return Optional.of((FunctionSummaryEdge) inEdge);
      }
    }

    return Optional.empty();
  }

  public static Optional<FunctionEntryNode> getFunctionEntryNode(
      CfaNetwork pCfaNetwork, FunctionSummaryEdge pFunctionSummaryEdge) {

    CFANode functionSummaryEdgePredecessor =
        pCfaNetwork.incidentNodes(pFunctionSummaryEdge).source();

    for (CFANode successor : pCfaNetwork.successors(functionSummaryEdgePredecessor)) {
      if (successor instanceof FunctionEntryNode) {
        return Optional.of((FunctionEntryNode) successor);
      }
    }

    return Optional.empty();
  }
}
