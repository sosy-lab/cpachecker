// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.witnessjoiner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class WitnessJoinerTransferRelation extends AbstractSingleWrapperTransferRelation {

  protected WitnessJoinerTransferRelation(TransferRelation pWrapped) {
    super(pWrapped);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    WitnessJoinerState pred = ((WitnessJoinerState) pState);

    AbstractState wrappedState = pred.getWrappedState();

    Collection<? extends AbstractState> successors;
    successors = transferRelation.getAbstractSuccessors(wrappedState, pPrecision);

    if (successors.isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableList.Builder<WitnessJoinerState> wrappedSuccessors = ImmutableList.builder();
    for (AbstractState absElement : successors) {
      wrappedSuccessors.add(new WitnessJoinerState(absElement));
    }

    return wrappedSuccessors.build();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    WitnessJoinerState pred = ((WitnessJoinerState) pState);

    AbstractState wrappedState = pred.getWrappedState();

    Collection<? extends AbstractState> successors;
    successors = transferRelation.getAbstractSuccessorsForEdge(wrappedState, pPrecision, pCfaEdge);

    if (successors.isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableList.Builder<WitnessJoinerState> wrappedSuccessors = ImmutableList.builder();
    for (AbstractState absElement : successors) {
      wrappedSuccessors.add(new WitnessJoinerState(absElement));
    }

    return wrappedSuccessors.build();
  }
}
