// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.symbolicExecution;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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

    // Some states can strengthen the CS and VS separately
    Set<AbstractState> strengthenSeparately = new HashSet<>();

    for (AbstractState strengtheningState : otherStates) {
      if (strengtheningState instanceof BlockState blockState) {
        SymbolicExecutionState newState =
            strengthenWithBlockState(symbolicExecutionState, blockState, cfaEdge, precision);
        if (newState == null) {
          return ImmutableSet.of();
        }
        symbolicExecutionState = newState;
      } else {
        strengthenSeparately.add(strengtheningState);
      }
    }
    Collection<? extends AbstractState> valueStates =
        valueAnalysisTransferRelation.strengthen(
            symbolicExecutionState.valueAnalysisState(),
            Iterables.concat(
                strengthenSeparately, ImmutableList.of(symbolicExecutionState.constraintsState())),
            cfaEdge,
            precision);
    Collection<? extends AbstractState> constraintStates =
        constraintsTransferRelation.strengthen(
            symbolicExecutionState.constraintsState(),
            Iterables.concat(
                strengthenSeparately,
                ImmutableList.of(symbolicExecutionState.valueAnalysisState())),
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

  public SymbolicExecutionState strengthenWithBlockState(
      final SymbolicExecutionState pStateToStrengthen,
      final BlockState pBlockState,
      final CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    if (!pBlockState.isTarget()) {
      return pStateToStrengthen;
    }

    List<? extends @NonNull AbstractState> violations = pBlockState.getViolationConditions();

    SymbolicExecutionState violation =
        AbstractStates.extractStateByType(violations.getFirst(), SymbolicExecutionState.class);
    Preconditions.checkNotNull(violation);
    return strengthenWithSymbolicState(pStateToStrengthen, violation, pCfaEdge, pPrecision);
  }

  public SymbolicExecutionState strengthenWithSymbolicState(
      final SymbolicExecutionState pStateToStrengthen,
      final SymbolicExecutionState pViolation,
      final CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    SymbolicExecutionState newViolation =
        pViolation.renameIDs(pStateToStrengthen.getSymbolicIdentifiers());

    List<Constraint> addConstraints =
        ValueAnalysisState.compareInConstraint(
            pStateToStrengthen.valueAnalysisState(), newViolation.valueAnalysisState());
    addConstraints.addAll(newViolation.constraintsState());

    ConstraintsState newConstraintsState = new ConstraintsState(new HashSet<>(addConstraints));

    // Call strengthen of CS with CS
    Collection<? extends AbstractState> newCS =
        constraintsTransferRelation.strengthen(
            pStateToStrengthen.constraintsState(),
            ImmutableList.of(newConstraintsState),
            pCfaEdge,
            pPrecision);
    if (newCS.isEmpty()) {
      return null;
    }
    Preconditions.checkArgument(newCS.size() == 1);

    ValueAnalysisState newVS =
        new ValueAnalysisState(pStateToStrengthen.valueAnalysisState().getMachineModel());
    for (Entry<MemoryLocation, ValueAndType> entry :
        pStateToStrengthen.valueAnalysisState().getConstants()) {
      newVS.assignConstant(entry.getKey(), entry.getValue().getValue(), entry.getValue().getType());
    }

    for (Entry<MemoryLocation, ValueAndType> entry :
        newViolation.valueAnalysisState().getConstants()) {
      if (!pStateToStrengthen.valueAnalysisState().contains(entry.getKey())) {
        newVS.assignConstant(
            entry.getKey(), entry.getValue().getValue(), entry.getValue().getType());
      }
    }

    // return new SE
    return new SymbolicExecutionState(newVS, (ConstraintsState) Iterables.getOnlyElement(newCS));
  }
}
