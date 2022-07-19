// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

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
  private Map<CFANode, GhostCFA> startNodeOriginalCFAToGhostCFA = new HashMap<>();
  private Map<CFANode, Loop> nodeToLoopStructure = new HashMap<>();
  private Set<Strategy> strategies = new HashSet<>();
  private StrategyFactory factory;
  private final StrategyDependency summaryCreationStrategy;
  private final StrategyDependency summaryTransferStrategy;
  private Map<CFANode, Set<StrategiesEnum>> unallowedStrategiesForNode = new HashMap<>();

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
    startNodeOriginalCFAToGhostCFA.put(ghostCFA.getStartOriginalCfaNode(), ghostCFA);
    for (CFANode n : ghostCFA.getAllNodes()) {
      // this.addNodeForStrategy(StrategiesEnum.BASE, n);
      this.unallowedStrategiesForNode.put(n, new HashSet<>());
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

  public List<StrategiesEnum> getChosenStrategies(CFANode node, LocationPrecision prec) {
    List<StrategiesEnum> availableStrategies =
        new ArrayList<>(SummaryUtils.getAvailableStrategies(node));
    availableStrategies.removeAll(prec.getUnallowedStrategies());
    List<StrategiesEnum> allowedStrategies =
        new ArrayList<>(getTransferSummaryStrategy().filter(availableStrategies));
    return allowedStrategies;
  }
}
