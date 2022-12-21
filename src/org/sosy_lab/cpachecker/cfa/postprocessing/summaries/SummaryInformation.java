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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cpa.location.LocationPrecision;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/*
 * The Information related to the Summaries.
 * This information will be used in order to change the transfer relation of the location CPA on the fly.
 * The information is encoded in the Nodes. This means that to know if you're entering a
 * strategy you need to check the successor of the edge with which you're entering it.
 *
 * This also contains the Informations needed in order to build the strategies.
 */
public class SummaryInformation {

  private Map<CFANode, GhostCFA> startNodeGhostCFAToGhostCFA = new HashMap<>();
  private Map<CFANode, List<GhostCFA>> startNodeOriginalCFAToGhostCFAs = new HashMap<>();
  private Map<CFANode, Loop> nodeToLoopStructure = new HashMap<>();
  private Set<Strategy> strategies = new HashSet<>();
  private StrategyFactory factory;
  private final StrategyDependency summaryCreationStrategy;
  private final StrategyDependency summaryTransferStrategy;

  public SummaryInformation(
      CFA pCfa,
      StrategyDependency pCreationSummaryStrategy,
      StrategyDependency pTransferSummaryStrategy) {
    summaryCreationStrategy = pCreationSummaryStrategy;
    summaryTransferStrategy = pTransferSummaryStrategy;
    this.addCfaInformations(pCfa);
  }

  public void addGhostCFA(GhostCFA ghostCFA) {
    startNodeGhostCFAToGhostCFA.put(ghostCFA.getStartGhostCfaNode(), ghostCFA);
    if (startNodeOriginalCFAToGhostCFAs.containsKey(ghostCFA.getStartOriginalCfaNode())) {
      startNodeOriginalCFAToGhostCFAs.get(ghostCFA.getStartOriginalCfaNode()).add(ghostCFA);
    } else {
      List<GhostCFA> ghostCFAs = new ArrayList<>();
      ghostCFAs.add(ghostCFA);
      startNodeOriginalCFAToGhostCFAs.put(ghostCFA.getStartOriginalCfaNode(), ghostCFAs);
    }
  }

  public void addStrategy(Strategy strategy) {
    strategies.add(strategy);
  }

  public void addCfaInformations(CFA pCfa) {
    Optional<LoopStructure> optionalLoopStructure = pCfa.getLoopStructure();
    if (optionalLoopStructure.isPresent()) {
      for (Loop loop : optionalLoopStructure.orElseThrow().getAllLoops()) {
        for (CFANode node : loop.getLoopHeads()) {
          nodeToLoopStructure.put(node, loop);
        }
      }
    }
  }

  public Set<Strategy> getStrategies() {
    return strategies;
  }

  public void setFactory(StrategyFactory pFactory) {
    factory = pFactory;
  }

  public StrategyFactory getFactory() {
    return factory;
  }

  public Optional<Loop> getLoop(CFANode node) {
    Loop loop = nodeToLoopStructure.get(node);
    if (Objects.isNull(loop)) {
      return Optional.empty();
    }
    return Optional.of(loop);
  }

  public StrategyDependency getCreationSummaryStrategy() {
    return this.summaryCreationStrategy;
  }

  public StrategyDependency getTransferSummaryStrategy() {
    return this.summaryTransferStrategy;
  }

  public Optional<GhostCFA> getBestAllowedStrategy(CFANode pNode, LocationPrecision pPrec) {
    List<StrategiesEnum> availableStrategies =
        FluentIterable.from(
                this.startNodeOriginalCFAToGhostCFAs.getOrDefault(pNode, new ArrayList<>()))
            .filter(g -> !pPrec.getForbiddenStrategies().contains(g))
            .transform(g -> g.getStrategy())
            .toList();
    List<StrategiesEnum> possibleStrategies =
        this.summaryTransferStrategy.filter(availableStrategies);

    for (GhostCFA g: this.startNodeOriginalCFAToGhostCFAs.getOrDefault(pNode, new ArrayList<>())) {
      if (possibleStrategies.contains(g.getStrategy())) {
        return Optional.of(g);
      }
    }

    return Optional.empty();
  }

  public List<GhostCFA> getAvailableStrategies(CFANode node) {
    return startNodeOriginalCFAToGhostCFAs.getOrDefault(node, new ArrayList<>());
  }
}
