// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageCPA;
import org.sosy_lab.cpachecker.cpa.coverage.PredicateCoverageCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.coverage.report.FilePredicateCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;

public class CoverageUtility {

  public static boolean isNodeConsidered(CFANode node, Iterable<AbstractState> reached) {
    for (AbstractState state : reached) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      if (argState != null) {
        LocationState locState =
            AbstractStates.extractStateByType(argState.getWrappedState(), LocationState.class);
        if (locState != null) {
          CFANode locationNode = locState.getLocationNode();
          List<CFANode> locationComboNodes = extractBlockNodes(locationNode);
          for (var comboNode : locationComboNodes) {
            if (node.toString().equals(comboNode.toString())) {
              return true;
            }
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
          List<CFANode> locationComboNodes = extractBlockNodes(locationNode);
          for (CFANode node : locationComboNodes) {
            double visitedNumber = nodesVisitedMap.getOrDefault(node, 0.0);
            nodesVisitedMap.put(node, visitedNumber + 1.0);
          }
        }
      }
    }
    double maxVisited =
        nodesVisitedMap.values().stream().max(Comparator.naturalOrder()).orElse(0.0);
    nodesVisitedMap.replaceAll((k, v) -> v /= maxVisited);
    return nodesVisitedMap;
  }

  private static List<CFANode> extractBlockNodes(CFANode locationNode) {
    List<CFAEdge> currentComboEdge = null;
    List<CFANode> nodes = new ArrayList<>();
    CFANode currentNode = locationNode;
    nodes.add(locationNode);
    do {
      if (currentNode.isLoopStart()
          || (currentNode.getNumEnteringEdges() != 1)
          || (currentNode.getNumLeavingEdges() != 1)
          || (currentComboEdge != null
              && !currentNode.equals(
                  currentComboEdge.get(currentComboEdge.size() - 1).getSuccessor()))
          || (currentNode.getLeavingEdge(0).getEdgeType() == CFAEdgeType.CallToReturnEdge)
          || (currentNode.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge)) {
        currentComboEdge = null;
        if (nodes.size() > 1) {
          nodes.remove(nodes.size() - 1);
        }
      } else {
        CFAEdge leavingEdge = currentNode.getLeavingEdge(0);
        if (currentComboEdge == null) {
          currentComboEdge = new ArrayList<>();
        }
        currentComboEdge.add(leavingEdge);
        currentNode = leavingEdge.getSuccessor();
        nodes.add(currentNode);
      }
    } while (currentComboEdge != null);
    return nodes;
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

  public static CoverageData extractTimeDependentCoverageData(ConfigurableProgramAnalysis cpa) {
    CoverageData coverageData = new CoverageData();
    boolean usedPredicateCoverageCPA = false;

    if (cpa instanceof ARGCPA) {
      ImmutableList<ConfigurableProgramAnalysis> cpas = ((ARGCPA) cpa).getWrappedCPAs();
      for (var compositeCPA : cpas) {
        if (compositeCPA instanceof CompositeCPA) {
          ImmutableList<ConfigurableProgramAnalysis> wrappedCPAs =
              ((CompositeCPA) compositeCPA).getWrappedCPAs();
          for (var wrappedCPA : wrappedCPAs) {
            if (wrappedCPA instanceof PredicateCoverageCPA) {
              usedPredicateCoverageCPA = true;
              coverageData = ((PredicateCoverageCPA) wrappedCPA).getCoverageData();
            }
          }
          for (var wrappedCPA : wrappedCPAs) {
            if (wrappedCPA instanceof CoverageCPA) {
              if (usedPredicateCoverageCPA) {
                TimeDependentCoverageType type = TimeDependentCoverageType.Visited;
                TimeDependentCoverageData data =
                    ((CoverageCPA) wrappedCPA).getCoverageData().getTDCGHandler().getData(type);
                Map<String, FilePredicateCoverageStatistics> predicateStatistics =
                    coverageData.getPredicateStatistics();
                ((CoverageCPA) wrappedCPA)
                    .getCoverageData()
                    .setPredicateStatistics(predicateStatistics);
                coverageData.setInfosPerFile(
                    ((CoverageCPA) wrappedCPA).getCoverageData().getInfosPerFile());
                coverageData.getTDCGHandler().addData(type, data);
              } else {
                coverageData = ((CoverageCPA) wrappedCPA).getCoverageData();
              }
            }
          }
        }
      }
    }
    return coverageData;
  }

  public static CoverageData getCoverageDataFromReachedSet(UnmodifiableReachedSet pReached) {
    CoverageData coverageData = new CoverageData();
    if (pReached instanceof PartitionedReachedSet) {
      ConfigurableProgramAnalysis cpa = ((PartitionedReachedSet) pReached).getCPA();
      coverageData = CoverageUtility.extractTimeDependentCoverageData(cpa);
    }
    return coverageData;
  }

  public static boolean coversLine(CFAEdge pEdge) {
    FileLocation loc = pEdge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      // dummy location
      return false;
    }
    if (pEdge instanceof ADeclarationEdge
        && (((ADeclarationEdge) pEdge).getDeclaration() instanceof AFunctionDeclaration)) {
      // Function declarations span the complete body, this is not desired.
      return false;
    }
    return true;
  }
}
