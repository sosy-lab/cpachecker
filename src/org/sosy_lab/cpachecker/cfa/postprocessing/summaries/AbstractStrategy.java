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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;

public abstract class AbstractStrategy implements Strategy {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final SummaryInformation summaryInformation;
  protected final StrategyDependency strategyDependencies;
  protected SummaryFilter summaryFilter;

  protected AbstractStrategy(
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      StrategyDependency strategyDependencies,
      CFA pCfa) {
    this.shutdownNotifier = pShutdownNotifier;
    this.logger = pLogger;
    this.strategyDependencies = strategyDependencies;
    this.summaryInformation = pCfa.getSummaryInformation().orElseThrow();
    this.summaryFilter = new SummaryFilter(this.strategyDependencies);
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    return Optional.empty();
  }
}
