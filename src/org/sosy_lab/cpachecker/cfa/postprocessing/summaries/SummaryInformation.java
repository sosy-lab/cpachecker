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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class SummaryInformation {

  private Map<StrategiesEnum, CFANode> strategiesToNodes = new HashMap<>();
  private Map<CFANode, StrategiesEnum> nodesToStrategies = new HashMap<>();
  private Map<CFANode, Map<String, CExpression>> variableDeclarations = new HashMap<>();
  private Set<StrategyInterface> strategies = new HashSet<>();
  protected StrategyFactory factory;

  private static SummaryInformation singletonInstance = new SummaryInformation();

  private SummaryInformation() {}

  public static SummaryInformation getSummaryInformation() {
    return singletonInstance;
  }

  public void addNodeForStrategy(StrategiesEnum strategy, CFANode node) {
    strategiesToNodes.put(strategy, node);
    nodesToStrategies.put(node, strategy);
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

  public Set<StrategyInterface> getStrategies() {
    return strategies;
  }

  public void setFactory(StrategyFactory pFactory) {
    factory = pFactory;
  }

  public StrategyFactory getFactory() {
    return factory;
  }
}
