// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class SummaryInformation {

  private Map<StrategiesEnum, CFANode> strategiesToNodes = new HashMap<>();
  private Map<CFANode, StrategiesEnum> nodesToStrategies = new HashMap<>();
  private Map<CFANode, GhostCFA> startNodeGhostCFAToGhostCFA = new HashMap<>();
  private Map<CFANode, GhostCFA> startNodeOriginalCFAToGhostCFA = new HashMap<>();
  private Map<CFANode, Map<String, CExpression>> variableDeclarations = new HashMap<>();
  private Map<CFANode, Loop> nodeToLoopStructure = new HashMap<>();
  private Set<StrategyInterface> strategies = new HashSet<>();
  private StrategyFactory factory;

  public SummaryInformation() {}

  public void addNodeForStrategy(StrategiesEnum strategy, CFANode node) {
    strategiesToNodes.put(strategy, node);
    nodesToStrategies.put(node, strategy);
  }

  public void addGhostCFA(GhostCFA ghostCFA) {
    startNodeGhostCFAToGhostCFA.put(ghostCFA.getStartGhostCfaNode(), ghostCFA);
    startNodeOriginalCFAToGhostCFA.put(ghostCFA.getStartOriginalCfaNode(), ghostCFA);
  }

  public StrategiesEnum getStrategyForNode(CFANode node) {
    return nodesToStrategies.get(node);
  }

  public Map<String, CExpression> getVariableDeclarationsForNode(CFANode node) {
    return variableDeclarations.get(node);
  }

  public void addStrategy(StrategyInterface strategy) {
    strategies.add(strategy);
  }

  public void addCfaInformations(CFA pCfa) {
    Optional<LoopStructure> optionalLoopStructure = pCfa.getLoopStructure();
    if (!optionalLoopStructure.isEmpty()) {
      for (Loop loop : optionalLoopStructure.get().getAllLoops()) {
        for (CFANode node : loop.getLoopHeads()) {
          nodeToLoopStructure.put(node, loop);
        }
      }
    }
  }

  public Set<StrategyInterface> getStrategies() {
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
}
