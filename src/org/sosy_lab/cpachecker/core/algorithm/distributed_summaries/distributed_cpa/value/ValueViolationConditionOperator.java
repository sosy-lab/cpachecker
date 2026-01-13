// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueViolationConditionOperator implements ViolationConditionOperator {
  public final MachineModel machineModel;
  public final BlockNode blockNode;

  public ValueViolationConditionOperator(MachineModel pMachineModel, BlockNode pBlockNode) {
    machineModel = pMachineModel;
    blockNode = pBlockNode;
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition) {

    ImmutableList<ARGState> states = pARGPath.asStatesList();
    assert !states.isEmpty();

    AbstractState entry = states.getFirst().getWrappedState();
    ValueAnalysisState violationCondition = new ValueAnalysisState(machineModel);

    if (!(entry instanceof CompositeState compositeEntry))
      return Optional.of(new ValueAnalysisState(machineModel));
    for (AbstractState state : compositeEntry.getWrappedStates()) {
      if (state instanceof ValueAnalysisState valueState) violationCondition = valueState;
    }
    Set<String> declared = new HashSet<>();
    for (CFAEdge edge : pARGPath.getFullPath()) {
      if (edge instanceof ADeclarationEdge aDeclarationEdge) {
        declared.add(aDeclarationEdge.getDeclaration().getQualifiedName());
      }
    }
    ValueAnalysisState violationValue = getViolationValueState(states.getLast(), machineModel);
    for (Map.Entry<MemoryLocation, ValueAndType> variable : violationValue.getConstants()) {
      if (!violationCondition.contains(variable.getKey())
          && !declared.contains(variable.getKey().getQualifiedName())
          && !DeserializeValueAnalysisStateOperator.accessedVariables
              .get(blockNode.getId())
              .containsKey(variable.getKey().getQualifiedName()))
        violationCondition.assignConstant(
            variable.getKey(), variable.getValue().getValue(), variable.getValue().getType());
    }
    return Optional.of(violationCondition);
  }

  private static ValueAnalysisState getViolationValueState(
      ARGState pState, MachineModel pMachineModel) {

    if (!(pState.getWrappedState() instanceof CompositeState cS))
      return new ValueAnalysisState(pMachineModel);

    for (AbstractState state : cS.getWrappedStates()) {
      if (state instanceof BlockState blockState && blockState.isTarget()) {
        AbstractState violation = blockState.getViolationConditions().getFirst();
        if (!(violation instanceof ARGState argState)
            || !(argState.getWrappedState() instanceof CompositeState compositeViolation))
          return new ValueAnalysisState(pMachineModel);

        for (AbstractState wrappedState : compositeViolation.getWrappedStates()) {
          if (wrappedState instanceof ValueAnalysisState valueState) return valueState;
        }
      }
    }
    return new ValueAnalysisState(pMachineModel);
  }
}
