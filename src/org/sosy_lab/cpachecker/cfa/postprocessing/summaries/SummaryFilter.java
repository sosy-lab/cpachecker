// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;

public class SummaryFilter {

  private SummaryInformation summaryInformation;
  private StrategyDependency strategyDependencies;

  public SummaryFilter(
      SummaryInformation pSummaryInformation, StrategyDependency pStrategyDependencies) {
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
    return FluentIterable.from(edges).filter(x -> filter(x, strategies)).toList();
  }

  public boolean filter(CFAEdge edge, Set<StrategiesEnum> strategies) {
    return strategies.contains(summaryInformation.getStrategyForEdge(edge));
  }
}
