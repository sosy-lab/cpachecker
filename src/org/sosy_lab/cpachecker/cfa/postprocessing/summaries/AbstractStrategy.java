// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;

public abstract class AbstractStrategy implements StrategyInterface {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final SummaryInformation summaryInformation;
  private StrategyDependencyInterface strategyDependencies;
  private AExpressionFactory expressionFactory;
  protected SummaryFilter summaryFilter;

  protected AbstractStrategy(
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface strategyDependencies,
      CFA pCfa) {
    this.shutdownNotifier = pShutdownNotifier;
    this.logger = pLogger;
    this.strategyDependencies = strategyDependencies;
    this.summaryInformation = pCfa.getSummaryInformation().orElseThrow();
    this.setExpressionFactory(new AExpressionFactory());
    this.summaryFilter = new SummaryFilter(this.summaryInformation, this.strategyDependencies);
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return false;
  }

  public AExpressionFactory getExpressionFactory() {
    return expressionFactory;
  }

  public void setExpressionFactory(AExpressionFactory pExpressionFactory) {
    expressionFactory = pExpressionFactory;
  }
}
