// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class CoverageUtility {

  public static boolean isNodeConsidered(CFANode node, Iterable<AbstractState> reached) {
    String cfaNodeId = node.toString();
    for (AbstractState state : reached) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      if (argState != null) {
        LocationState locState =
            AbstractStates.extractStateByType(argState.getWrappedState(), LocationState.class);
        if (locState != null) {
          String argConsideredNodeId = locState.getLocationNode().toString();
          if (cfaNodeId.equals(argConsideredNodeId)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static Set<CFANode> getVisitedNodes(Iterable<AbstractState> reached, CFA cfa) {
    Set<CFANode> consideredNodes = new LinkedHashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      if (CoverageUtility.isNodeConsidered(node, reached)) {
        consideredNodes.add(node);
      }
    }
    return consideredNodes;
  }

  public static Map<CFANode, Double> getNodesVisitedMap(Iterable<AbstractState> reached) {
    Map<CFANode, Double> nodesVisitedMap = new HashMap<>();
    for (AbstractState state : reached) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      if (argState != null) {
        LocationState locState =
            AbstractStates.extractStateByType(argState.getWrappedState(), LocationState.class);
        if (locState != null) {
          CFANode locationNode = locState.getLocationNode();
          double visitedNumber = nodesVisitedMap.getOrDefault(locationNode, 0.0);
          nodesVisitedMap.put(locationNode, visitedNumber + 1.0);
        }
      }
    }
    double maxVisited =
        nodesVisitedMap.values().stream().max(Comparator.naturalOrder()).orElse(0.0);
    nodesVisitedMap.replaceAll((k, v) -> v /= maxVisited);
    return nodesVisitedMap;
  }

  public static void addIndirectlyCoveredNodes(Set<CFANode> nodes) {
    boolean hasChanged;
    Set<Integer> alreadyProcessedNodes = new HashSet<>();
    do {
      hasChanged = false;
      Set<CFANode> newConsideredNodes = new HashSet<>();
      for (CFANode node : nodes) {
        if (alreadyProcessedNodes.contains(node.getNodeNumber())) {
          continue;
        }
        alreadyProcessedNodes.add(node.getNodeNumber());
        for (int i = 0; i < node.getNumEnteringEdges(); i++) {
          CFANode preNode = node.getEnteringEdge(i).getPredecessor();
          if (!nodes.contains(preNode)) {
            hasChanged = true;
            newConsideredNodes.add(preNode);
          }
        }
      }
      nodes.addAll(newConsideredNodes);
    } while (hasChanged);
  }
}
