// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class LoopSummaryTransferRelation extends AbstractSingleWrapperTransferRelation {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("unused")
  private final LoopSummaryCPAStatistics stats;

  protected LoopSummaryTransferRelation(
      AbstractLoopSummaryCPA pLoopSummaryCPA, ShutdownNotifier pShutdownNotifier) {
    super(pLoopSummaryCPA.getWrappedCpa().getTransferRelation());
    stats = pLoopSummaryCPA.getStatistics();
    logger = pLoopSummaryCPA.getLogger();
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException("Unimplemented");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState pState, final Precision pPrecision)
      throws InterruptedException, CPATransferException {

    return transferRelation.getAbstractSuccessors(pState, pPrecision);
    /*
    Optional<Collection<? extends AbstractState>> summarizedState =
        applyStrategyIfAlreadyApplied(pState, pPrecision, transferRelation);
    while (summarizedState.isEmpty()) {
      List<CFAEdge> removedEdges = new ArrayList<>();
      int i = 0;
      while (i < AbstractStates.extractLocation(pState).getNumLeavingEdges()) {
        // Remove Edges of Other Strategies in order for the Strategy Calculation to work with the
        // Original CFA
        CFAEdge currentEdge = AbstractStates.extractLocation(pState).getLeavingEdge(i);
        if (isGhostEdge(currentEdge)) {
          removedEdges.add(currentEdge);
          AbstractStates.extractLocation(pState)
              .removeLeavingEdge(removedEdges.get(removedEdges.size() - 1));
        } else {
          i += 1;
        }
      }
      summarizedState =
          strategies
              .get(((LoopSummaryPrecision) pPrecision).getStrategyCounter())
              .summarizeLoopState(
                  pState, ((LoopSummaryPrecision) pPrecision).getPrecision(), transferRelation);
      // Reinsert Removed Edges
      for (CFAEdge e : removedEdges) {
        AbstractStates.extractLocation(pState).addLeavingEdge(e);
      }
      if (summarizedState.isEmpty()) {
        // If the Strategy cannot be applied we see if the next Strategy was someday applied, else
        // we see if we can apply it and generate the ghost CFA
        ((LoopSummaryPrecision) pPrecision).updateStrategy();
        summarizedState = this.applyStrategyIfAlreadyApplied(pState, pPrecision, transferRelation);
      }
    }

    stats.incrementStrategyUsageCount(
        strategies
            .get(((LoopSummaryPrecision) pPrecision).getStrategyCounter())
            .getClass()
            .getSimpleName());

    ((LoopSummaryPrecision) pPrecision)
        .setLoopHead(
            ((LoopSummaryPrecision) pPrecision).getStrategyCounter() != this.baseStrategyPosition);
    return summarizedState.orElseThrow();*/
  }
}
