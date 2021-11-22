// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;

public class SummaryFilter {


  private SummaryInformation summaryInformation;
  private StrategyDependencyInterface strategyDependencies;

  public SummaryFilter(
      SummaryInformation pSummaryInformation, StrategyDependencyInterface pStrategyDependencies) {
    strategyDependencies = pStrategyDependencies;
    summaryInformation = pSummaryInformation;

  }

  public List<CFAEdge> getOutgoingEdges(CFANode node) {
    List<StrategiesEnum> availableStrategies = new ArrayList<>();
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      availableStrategies.add(
          summaryInformation.getStrategyForNode(node.getLeavingEdge(i).getSuccessor()));
    }
    List<StrategiesEnum> filteredStrategies = strategyDependencies.filter(availableStrategies);
    List<CFAEdge> filteredLeavingEdges = new ArrayList<>();
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      if (filteredStrategies.contains(
          summaryInformation.getStrategyForNode(node.getLeavingEdge(i).getSuccessor()))) {
        filteredLeavingEdges.add(node.getLeavingEdge(i));
      }
    }
    return filteredLeavingEdges;
  }

  public List<CFAEdge> getEdgesForStrategies(List<CFAEdge> edges, Set<StrategiesEnum> strategies) {
    List<CFAEdge> newEdges = new ArrayList<>();
    for (CFAEdge e : edges) {
      if (strategies.contains(summaryInformation.getStrategyForEdge(e))) {
        newEdges.add(e);
      }
    }
    return newEdges;
  }
}
