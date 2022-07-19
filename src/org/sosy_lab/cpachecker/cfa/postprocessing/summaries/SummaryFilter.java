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

  private StrategyDependency strategyDependencies;

  public SummaryFilter(StrategyDependency pStrategyDependencies) {
    strategyDependencies = pStrategyDependencies;
  }

  public List<CFAEdge> getFilteredOutgoingEdges(CFANode node) {
    Set<StrategiesEnum> availableStrategies = SummaryUtils.getAvailableStrategies(node);

    List<StrategiesEnum> filteredStrategies =
        strategyDependencies.filter(new ArrayList<>(availableStrategies));

    return FluentIterable.from(node.getLeavingEdges())
        .filter(e -> filteredStrategies.contains(SummaryUtils.getStrategyForEdge(e)))
        .toList();
  }

  /**
   * Filters in order such that only the edges corresponding to the strategies remain
   *
   * @param edges The edges to be filtered
   * @param presentStrategies the strategies which should remain present
   * @return the filtered edges
   */
  public List<CFAEdge> filterStrategies(
      List<CFAEdge> edges, Set<StrategiesEnum> presentStrategies) {
    return FluentIterable.from(edges)
        .filter(e -> presentStrategies.contains(SummaryUtils.getStrategyForEdge(e)))
        .toList();
  }

  public List<CFAEdge> filterStrategies(CFANode node, Set<StrategiesEnum> presentStrategies) {
    return filterStrategies(node.getLeavingEdges(), presentStrategies);
  }

  public List<CFAEdge> filterStrategies(CFAEdge e, Set<StrategiesEnum> presentStrategies) {
    List<CFAEdge> edges = new ArrayList<>();
    edges.add(e);
    return filterStrategies(edges, presentStrategies);
  }
}
