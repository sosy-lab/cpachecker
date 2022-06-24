// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.AbstractStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public class LoopStrategy extends AbstractStrategy {

  protected static final CSimpleType SIGNED_LONG_INT = CNumericTypes.SIGNED_LONG_INT;
  protected StrategiesEnum strategyEnum;

  protected LoopStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      StrategiesEnum pStrategyEnum,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);
    strategyEnum = pStrategyEnum;
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    return Optional.empty();
  }

  protected final Optional<CFANode> determineLoopHead(final CFANode loopStartNode) {
    List<CFAEdge> filteredOutgoingEdges =
        this.summaryFilter.getEdgesForStrategies(
            loopStartNode.getLeavingEdges(),
            new HashSet<>(Arrays.asList(StrategiesEnum.BASE, this.strategyEnum)));

    if (filteredOutgoingEdges.size() != 1) {
      return Optional.empty();
    }

    if (!filteredOutgoingEdges.get(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopHead = filteredOutgoingEdges.get(0).getSuccessor();
    return Optional.of(loopHead);
  }
}
