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
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.AExpressionsFactory;

public abstract class AbstractStrategy implements StrategyInterface {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final SummaryInformation summaryInformation;
  private StrategyDependencyInterface strategyDependencies;
  private AExpressionsFactory expressionFactory;

  protected AbstractStrategy(
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface strategyDependencies,
      CFA pCfa) {
    this.shutdownNotifier = pShutdownNotifier;
    this.logger = pLogger;
    this.strategyDependencies = strategyDependencies;
    this.summaryInformation = pCfa.getSummaryInformation().get();
    this.setExpressionFactory(new AExpressionsFactory());
  }

  protected CAssumeEdge overwriteStartEndStateEdge(
      CAssumeEdge edge, boolean truthAssignment, CFANode startNode, CFANode endNode) {
    return new CAssumeEdge(
        edge.getDescription(),
        FileLocation.DUMMY,
        startNode,
        endNode,
        edge.getExpression(),
        truthAssignment);
  }

  protected CStatementEdge overwriteStartEndStateEdge(
      CStatementEdge edge, CFANode startNode, CFANode endNode) {
    return new CStatementEdge(
        edge.getRawStatement(), edge.getStatement(), FileLocation.DUMMY, startNode, endNode);
  }

  protected List<CFAEdge> getOutgoingEdges(CFANode node) {
    List<StrategiesEnum> availableStrategies = new ArrayList<>();
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      availableStrategies.add(summaryInformation.getStrategyForNode(node.getLeavingEdge(i).getSuccessor()));
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

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return false;
  }

  public AExpressionsFactory getExpressionFactory() {
    return expressionFactory;
  }

  public void setExpressionFactory(AExpressionsFactory pExpressionFactory) {
    expressionFactory = pExpressionFactory;
  }
}
