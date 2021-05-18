// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LoopSummaryTransferRelation extends AbstractLoopSummaryTransferRelation<CPAException> {

  @SuppressWarnings("unused")
  private final AlgorithmFactory algorithmFactory;

  @SuppressWarnings("unused")
  private final LoopSummaryCPAStatistics stats;

  public LoopSummaryTransferRelation(
      LoopSummaryCPA loopSummaryCpa,
      ShutdownNotifier pShutdownNotifier,
      AlgorithmFactory pFactory,
      List<StrategyInterface> strategies,
      int pLookaheadamntnodes,
      int pLookaheaditerations,
      CFA pCfa) {
    super(
        loopSummaryCpa,
        pShutdownNotifier,
        strategies,
        pLookaheadamntnodes,
        pLookaheaditerations,
        pCfa);
    algorithmFactory = pFactory;
    stats = loopSummaryCpa.getStatistics();
  }
}
