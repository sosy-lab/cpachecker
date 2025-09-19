// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedAnalysisSequence.SequenceBoundAnalysis;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class SequenceTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;

  SequenceTransferRelation(CFA pCFA, LogManager pLogger) {
    checkNotNull(pCFA, "CFA instance needed to create LoopBoundCPA");
    this.logger = pLogger;
  }

  @Override
  public Collection<? extends SequenceState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, final CFAEdge pCfaEdge)
      throws CPATransferException {

    logger.logf(Level.FINE, "Processing edge %s, current State is %s", pCfaEdge, pState);

    SequenceState state = (SequenceState) pState;
    if (pCfaEdge instanceof AssumeEdge assumeEdge) {
      if (state.thisEdgeShouldBeTaken(assumeEdge)) {
        return Collections.singleton(state.takeEdge(pCfaEdge));
      } else {
        return ImmutableSet.of();
      }
    }

    return Collections.singleton(state.copy());
  }

  //  @Override
  //  public Collection<? extends AbstractState> strengthen(
  //      AbstractState state,
  //      Iterable<AbstractState> otherStates,
  //      @Nullable CFAEdge cfaEdge,
  //      Precision precision)
  //      throws CPATransferException, InterruptedException {
  //    assert state instanceof SequenceState;
  //    SequenceState rsState = (SequenceState) state;
  //    return Collections.singleton(rsState);
  //  }
}
