// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value.DeserializeValueAnalysisStateOperator.accessedVariables;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.AbstractStates;
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

    ValueAnalysisState entryValueState =
        AbstractStates.extractStateByType(pARGPath.getFirstState(), ValueAnalysisState.class);
    assert entryValueState != null;

    // Add the initial assignment to the VC
    ValueAnalysisState violationCondition = new ValueAnalysisState(machineModel);
    for (Map.Entry<MemoryLocation, ValueAndType> variable : entryValueState.getConstants()) {
      violationCondition.assignConstant(
          variable.getKey(), variable.getValue().getValue(), variable.getValue().getType());
    }

    // Add also the state of the unused variables from the violation state,
    // but do not consider those that were declared in this block.
    // TODO: store this instead of recomputing each time
    Set<String> declared = new HashSet<>();
    for (CFAEdge edge : pARGPath.getFullPath()) {
      if (edge instanceof ADeclarationEdge aDeclarationEdge) {
        declared.add(aDeclarationEdge.getDeclaration().getQualifiedName());
      }
    }

    ValueAnalysisState violationValue =
        getViolationValueState(pARGPath.getLastState(), machineModel);
    for (Entry<MemoryLocation, ValueAndType> variable : violationValue.getConstants()) {
      if (!violationCondition.contains(variable.getKey())
          && !declared.contains(variable.getKey().getQualifiedName())
          && !accessedVariables
              .get(blockNode.getId())
              .containsKey(variable.getKey().getQualifiedName()))
        violationCondition.assignConstant(
            variable.getKey(), variable.getValue().getValue(), variable.getValue().getType());
    }
    return Optional.of(violationCondition);
  }

  private static ValueAnalysisState getViolationValueState(
      ARGState pState, MachineModel pMachineModel) {

    BlockState blockState = AbstractStates.extractStateByType(pState, BlockState.class);
    assert (blockState != null && blockState.isTarget());

    if (blockState.getViolationConditions().isEmpty()) return new ValueAnalysisState(pMachineModel);
    AbstractState violation = blockState.getViolationConditions().getFirst();
    ValueAnalysisState violationValue =
        AbstractStates.extractStateByType(violation, ValueAnalysisState.class);
    assert violationValue != null;
    return violationValue;
  }
}
