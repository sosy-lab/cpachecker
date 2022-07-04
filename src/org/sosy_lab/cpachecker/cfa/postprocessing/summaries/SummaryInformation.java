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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.util.CFAUtils;
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

  private Map<StrategiesEnum, CFANode> strategiesToNodes = new HashMap<>();
  // TODO: Refactor the nodes with incoming strategies to also consider Multiple incoming
  // strategies. This should only happen with the base strategy so it is currently not a constraint
  private Map<CFANode, StrategiesEnum> nodesWithIncomingStrategies = new HashMap<>();
  private Map<CFANode, Set<StrategiesEnum>> nodesWithOutgoingStrategies = new HashMap<>();
  private Map<CFANode, GhostCFA> startNodeGhostCFAToGhostCFA = new HashMap<>();
  private Map<CFANode, GhostCFA> startNodeOriginalCFAToGhostCFA = new HashMap<>();
  private Map<CFANode, Map<String, CExpression>> variableDeclarations = new HashMap<>();
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

  public void addNodeForStrategy(StrategiesEnum strategy, CFANode node) {
    strategiesToNodes.put(strategy, node);
    nodesWithIncomingStrategies.put(node, strategy);
    for (int i = 0; i < node.getNumEnteringEdges(); i++) {
      CFAEdge e = node.getEnteringEdge(i);
      Set<StrategiesEnum> set = new HashSet<>();
      if (nodesWithOutgoingStrategies.containsKey(e.getPredecessor())) {
        set.addAll(nodesWithOutgoingStrategies.get(e.getPredecessor()));
      }
      set.add(strategy);
      nodesWithOutgoingStrategies.put(e.getPredecessor(), set);
    }
  }

  public void addGhostCFA(GhostCFA ghostCFA) {
    startNodeGhostCFAToGhostCFA.put(ghostCFA.getStartGhostCfaNode(), ghostCFA);
    startNodeOriginalCFAToGhostCFA.put(ghostCFA.getStartOriginalCfaNode(), ghostCFA);
    for (CFANode n : ghostCFA.getAllNodes()) {
      this.addNodeForStrategy(StrategiesEnum.BASE, n);
      this.unallowedStrategiesForNode.put(n, new HashSet<>());
    }

    addNodeForStrategy(ghostCFA.getStrategy(), ghostCFA.getStartGhostCfaNode());
  }

  public StrategiesEnum getStrategyForEdge(CFAEdge edge) {
    if (nodesWithIncomingStrategies.get(edge.getPredecessor()) == StrategiesEnum.BASE) {
      return nodesWithIncomingStrategies.get(edge.getSuccessor());
    } else {
      return nodesWithIncomingStrategies.get(edge.getPredecessor());
    }
  }

  public StrategiesEnum getStrategyForNode(CFANode node) {
    return nodesWithIncomingStrategies.get(node);
  }

  public Map<String, CExpression> getVariableDeclarationsForNode(CFANode node) {
    return variableDeclarations.get(node);
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

    for (CFANode n : pCfa.getAllNodes()) {
      this.addNodeForStrategy(StrategiesEnum.BASE, n);
      this.unallowedStrategiesForNode.put(n, new HashSet<>());
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

  public Set<StrategiesEnum> getUnallowedStrategiesForNode(CFANode node) {
    return this.unallowedStrategiesForNode.get(node);
  }

  public void addUnallowedStrategiesForNode(CFANode node, StrategiesEnum strategy) {
    unallowedStrategiesForNode.computeIfAbsent(node, x -> new HashSet<>()).add(strategy);
  }

  public List<StrategiesEnum> getAllowedStrategies(CFANode node) {
    List<StrategiesEnum> availableStrategies =
        new ArrayList<>(CFAUtils.successorsOf(node).transform(this::getStrategyForNode).toSet());
    availableStrategies.removeAll(getUnallowedStrategiesForNode(node));
    List<StrategiesEnum> allowedStrategies =
        new ArrayList<>(getTransferSummaryStrategy().filter(availableStrategies));
    return allowedStrategies;
  }

  public Set<CFANode> getDistinctNodesWithStrategies(Set<StrategiesEnum> ignoreStrategies) {
    Set<CFANode> nodes = new HashSet<>();
    for (Entry<CFANode, StrategiesEnum> e : this.nodesWithIncomingStrategies.entrySet()) {
      if (!ignoreStrategies.contains(e.getValue())) {
        if (e.getKey().getNumEnteringEdges() == 1) {
          // The predecessor Nodes are those for which the Strategies are calculated with the
          // entering
          // function
          nodes.add(e.getKey().getEnteringEdge(0).getPredecessor());
        }
      }
    }
    return nodes;
  }

  public Set<CFANode> getDistinctNodesWithStrategiesWithoutDissallowed(
      Set<StrategiesEnum> pIgnoreStrategies) {
    Set<CFANode> nodes = new HashSet<>();
    for (Entry<CFANode, StrategiesEnum> e : this.nodesWithIncomingStrategies.entrySet()) {
      Set<StrategiesEnum> ignoreStrategies = new HashSet<>(pIgnoreStrategies);
      ignoreStrategies.addAll(this.getUnallowedStrategiesForNode(e.getKey()));
      if (!ignoreStrategies.contains(e.getValue())) {
        if (e.getKey().getNumEnteringEdges() == 1) {
          // The predecessor Nodes are those for which the Strategies are calculated with the
          // entering
          // function
          nodes.add(e.getKey().getEnteringEdge(0).getPredecessor());
        }
      }
    }
    return nodes;
  }
}
