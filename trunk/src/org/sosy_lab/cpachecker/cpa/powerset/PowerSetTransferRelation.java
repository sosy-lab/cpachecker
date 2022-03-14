// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.powerset;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class PowerSetTransferRelation extends SingleEdgeTransferRelation {

  private final TransferRelation wrapperTransfer;

  public PowerSetTransferRelation(final TransferRelation pTransferRelation) {
    wrapperTransfer = pTransferRelation;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    PowerSetState state = (PowerSetState) pState;
    Set<AbstractState> successors =
        Sets.newHashSetWithExpectedSize(state.getWrappedStates().size());

    for (AbstractState wrappedState : state.getWrappedStates()) {
      successors.addAll(
          wrapperTransfer.getAbstractSuccessorsForEdge(wrappedState, pPrecision, pCfaEdge));
    }

    return ImmutableSet.of(new PowerSetState(successors));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {

    PowerSetState setStates = (PowerSetState) state;
    Set<AbstractState> newStates =
        Sets.newHashSetWithExpectedSize(setStates.getWrappedStates().size());

    boolean changed = false;

    for (AbstractState stateInSet : setStates.getWrappedStates()) {
      Collection<? extends AbstractState> strengtheningRes =
          wrapperTransfer.strengthen(
              stateInSet, Collections.singletonList(stateInSet), cfaEdge, precision);
      if (strengtheningRes != null && !strengtheningRes.isEmpty()) {
        changed = true;
        newStates.addAll(strengtheningRes);
      }
    }

    return changed ? ImmutableSet.of(new PowerSetState(newStates)) : ImmutableSet.of();
  }
}
