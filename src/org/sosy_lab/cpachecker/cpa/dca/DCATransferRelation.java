// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dca;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class DCATransferRelation extends SingleEdgeTransferRelation {

  private AutomatonTransferRelation automatonTR;

  public DCATransferRelation(AutomatonTransferRelation pAutomatonTransferRelation) {
    automatonTR = pAutomatonTransferRelation;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    checkArgument(pState instanceof DCAState);

    DCAState state = (DCAState) pState;
    List<Set<AutomatonState>> listOfStates = new ArrayList<>();
    for (AutomatonState compositeState : state.getCompositeStates()) {
      ImmutableSet<AutomatonState> successorsStates =
          ImmutableSet.copyOf(
              automatonTR.getAbstractSuccessorsForEdge(compositeState, pPrecision, pCfaEdge));
      if (successorsStates.isEmpty()) {
        return ImmutableSet.of();
      }
      listOfStates.add(successorsStates);
    }
    Set<List<AutomatonState>> cartesianProduct = Sets.cartesianProduct(listOfStates);

    ImmutableSet<AutomatonState> buechiSuccessorsStates =
        ImmutableSet.copyOf(
            automatonTR.getAbstractSuccessorsForEdge(state.getBuechiState(), pPrecision, pCfaEdge));
    ImmutableSet.Builder<DCAState> builder = ImmutableSet.<DCAState>builder();
    for (List<AutomatonState> productStates : cartesianProduct) {
      for (AutomatonState buechiSuccessorState : buechiSuccessorsStates) {
        builder.add(new DCAState(buechiSuccessorState, productStates));
      }
    }
    return builder.build();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    checkArgument(pState instanceof DCAState);

    List<Set<AutomatonState>> listOfStates = new ArrayList<>();
    DCAState state = (DCAState) pState;
    for (AutomatonState s : state.getCompositeStates()) {
      Set<AutomatonState> strengthenResult =
          ImmutableSet.copyOf(automatonTR.strengthen(s, pOtherStates, pCfaEdge, pPrecision));
      if (strengthenResult.isEmpty()) {
        return ImmutableSet.of();
      }
      listOfStates.add(strengthenResult);
    }
    Set<List<AutomatonState>> cartesianProduct = Sets.cartesianProduct(listOfStates);

    Set<AutomatonState> buechiStrengthenResults =
        ImmutableSet.copyOf(
            automatonTR.strengthen(state.getBuechiState(), pOtherStates, pCfaEdge, pPrecision));
    ImmutableSet.Builder<DCAState> builder = ImmutableSet.<DCAState>builder();
    for (List<AutomatonState> productStates : cartesianProduct) {
      for (AutomatonState buechiStrenghtenState : buechiStrengthenResults) {
        builder.add(new DCAState(buechiStrenghtenState, productStates));
      }
    }
    return builder.build();
  }
}
