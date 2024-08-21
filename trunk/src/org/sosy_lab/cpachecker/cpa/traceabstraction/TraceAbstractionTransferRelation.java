// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

class TraceAbstractionTransferRelation extends AbstractSingleWrapperTransferRelation {

  TraceAbstractionTransferRelation(TransferRelation pDelegateTransferRelation) {
    super(pDelegateTransferRelation);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    TraceAbstractionState taState = (TraceAbstractionState) pState;

    Collection<? extends AbstractState> delegateSuccessorStates =
        getWrappedTR()
            .getAbstractSuccessorsForEdge(taState.getWrappedState(), pPrecision, pCfaEdge);

    verify(delegateSuccessorStates.size() == 1);

    // The TraceAbstraction needs more information from other CPA-states before
    // it can compute the correct successor state.
    // Until then we let the delegate compute its successor and return it with the predicates
    // from the previous TAState (the PredicateTR is expected to only return a single successor
    // state)
    return ImmutableList.of(
        taState.withWrappedState(Iterables.getOnlyElement(delegateSuccessorStates)));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    TraceAbstractionState taState = (TraceAbstractionState) pState;

    Collection<? extends AbstractState> delegateStrengthenedStates =
        getWrappedTR().strengthen(taState.getWrappedState(), pOtherStates, pCfaEdge, pPrecision);
    verify(delegateStrengthenedStates.size() == 1);

    return ImmutableList.of(
        taState.withWrappedState(Iterables.getOnlyElement(delegateStrengthenedStates)));
  }

  private TransferRelation getWrappedTR() {
    return Iterables.getOnlyElement(super.getWrappedTransferRelations());
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException(
        "The "
            + getClass().getSimpleName()
            + " expects to be called with a CFA edge supplied"
            + " and does not support configuration where it needs to"
            + " return abstract states for any CFA edge.");
  }
}
