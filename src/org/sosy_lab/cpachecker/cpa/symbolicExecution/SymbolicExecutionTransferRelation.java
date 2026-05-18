// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.symbolicExecution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class SymbolicExecutionTransferRelation extends SingleEdgeTransferRelation {

  private final TransferRelation valueAnalysisTransferRelation;
  private final TransferRelation constraintsTransferRelation;

  public SymbolicExecutionTransferRelation(
      TransferRelation pValueAnalysisTransferRelation,
      TransferRelation pConstraintsTransferRelation) {
    valueAnalysisTransferRelation = pValueAnalysisTransferRelation;
    constraintsTransferRelation = pConstraintsTransferRelation;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    ImmutableList.Builder<SymbolicExecutionState> symbolicStates = ImmutableList.builder();
    SymbolicExecutionState symbolicExecutionState = (SymbolicExecutionState) state;
    for (AbstractState valueState :
        valueAnalysisTransferRelation.getAbstractSuccessorsForEdge(
            symbolicExecutionState.valueAnalysisState(), precision, cfaEdge)) {
      for (AbstractState constraintsState :
          constraintsTransferRelation.getAbstractSuccessorsForEdge(
              symbolicExecutionState.constraintsState(), precision, cfaEdge)) {
        symbolicStates.add(
            new SymbolicExecutionState(
                (ValueAnalysisState) valueState, (ConstraintsState) constraintsState));
      }
    }
    return symbolicStates.build();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    SymbolicExecutionState symbolicExecutionState = (SymbolicExecutionState) state;
    Collection<? extends AbstractState> valueStates =
        valueAnalysisTransferRelation.strengthen(
            symbolicExecutionState.valueAnalysisState(),
            Iterables.concat(
                otherStates, ImmutableList.of(symbolicExecutionState.constraintsState())),
            cfaEdge,
            precision);
    Collection<? extends AbstractState> constraintStates =
        constraintsTransferRelation.strengthen(
            symbolicExecutionState.valueAnalysisState(),
            Iterables.concat(
                otherStates, ImmutableList.of(symbolicExecutionState.valueAnalysisState())),
            cfaEdge,
            precision);
    ImmutableList.Builder<SymbolicExecutionState> strengthenedStates = ImmutableList.builder();
    for (AbstractState valueState : valueStates) {
      for (AbstractState constraintState : constraintStates) {
        strengthenedStates.add(
            new SymbolicExecutionState(
                (ValueAnalysisState) valueState, (ConstraintsState) constraintState));
      }
    }
    return strengthenedStates.build();
  }
}
