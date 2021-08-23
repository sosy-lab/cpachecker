// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.execution;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.AbstractStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;

public class ConcolicExecutionStrategy extends AbstractStrategy {

  public ConcolicExecutionStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    // TODO Execute the Loop symbolically and concretely to get the values after the loop.
    // A heuristic needs to be determined in order to decide when to apply this and when not.
    // See Comments on Conrete Execution for some ideas on how to implement this.
    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return true;
  }

}
