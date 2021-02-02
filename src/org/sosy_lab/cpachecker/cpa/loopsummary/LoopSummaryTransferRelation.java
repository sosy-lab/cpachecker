// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.ArrayList;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.strategyInterface;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class LoopSummaryTransferRelation extends AbstractLoopSummaryTransferRelation<CPAException> {

  @SuppressWarnings("unused")
  private final AlgorithmFactory algorithmFactory;

  @SuppressWarnings("unused")
  private final LoopSummaryCPAStatistics stats;

  public LoopSummaryTransferRelation(
      LoopSummaryCPA loopSummaryCpa,
      ShutdownNotifier pShutdownNotifier,
      AlgorithmFactory pFactory,
      ArrayList<strategyInterface> strategies) {
    super(loopSummaryCpa, pShutdownNotifier, strategies);
    algorithmFactory = pFactory;
    stats = loopSummaryCpa.getStatistics();
  }

  @Override
  protected Collection<? extends AbstractState> getWrappedTransferSuccessor(
      final ARGState pState, final Precision pPrecision, final CFANode node)
      throws CPATransferException, InterruptedException {

    final Collection<? extends AbstractState> result =
        transferRelation.getAbstractSuccessors(pState, pPrecision);
    return result;
  }


}
