// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.symbolic_execution;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.symbolicExecution.SymbolicExecutionCPA;
import org.sosy_lab.cpachecker.cpa.symbolicExecution.SymbolicExecutionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.SolverException;

public class SymbolicExecutionViolationConditionOperator implements ViolationConditionOperator {
  BlockNode blockNode;
  SymbolicExecutionCPA cpa;
  SymbolicExecutionState initialState;

  public SymbolicExecutionViolationConditionOperator(
      BlockNode pBlockNode, SymbolicExecutionCPA pCpa, SymbolicExecutionState pInitialState) {
    blockNode = pBlockNode;
    cpa = pCpa;
    initialState = pInitialState;
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {

    // Simulate the path on an empty state
    // SymbolicExecutionState lastState = initialState;
    SymbolicExecutionState lastState =
        AbstractStates.extractStateByType(pARGPath.getLastState(), SymbolicExecutionState.class);
    /*for (CFAEdge edge : pARGPath.getFullPath()) {
          Set<AbstractState> nextStates =
              new HashSet<>(
                  cpa.getTransferRelation()
                      .getAbstractSuccessorsForEdge(lastState, SingletonPrecision.getInstance(), edge));
          Preconditions.checkArgument(nextStates.size() == 1);
          AbstractState successor = Objects.requireNonNull(Iterables.getOnlyElement(nextStates));

          nextStates.clear();
          nextStates.addAll(
              cpa.getTransferRelation()
                  .strengthen(successor, ImmutableSet.of(), edge, SingletonPrecision.getInstance()));
          Preconditions.checkArgument(nextStates.size() == 1);

          lastState =
              (SymbolicExecutionState) Objects.requireNonNull(Iterables.getOnlyElement(nextStates));
        }

        // Strengthen the last state with the violation condition
        BlockState blockVC =
            AbstractStates.extractStateByType(pARGPath.getLastState(), BlockState.class);
        lastState =
            ((SymbolicExecutionTransferRelation) cpa.getTransferRelation())
                .strengthenWithBlockState(
                    lastState,
                    blockVC,
                    pARGPath.getFullPath().getLast(),
                    SingletonPrecision.getInstance());
    */
    // Add the initial assignment to the VC
    SymbolicExecutionState firstState =
        AbstractStates.extractStateByType(pARGPath.getFirstState(), SymbolicExecutionState.class);
    ValueAnalysisState violationCondition =
        new ValueAnalysisState(firstState.valueAnalysisState().getMachineModel());
    for (Map.Entry<MemoryLocation, ValueAndType> variable :
        firstState.valueAnalysisState().getConstants()) {
      violationCondition.assignConstant(
          variable.getKey(), variable.getValue().getValue(), variable.getValue().getType());
    }

    // Add also the state of the unused variables from the violation state,
    // but do not consider those that were declared on this path.
    Set<String> declared = getDeclared(pARGPath);

    for (Entry<MemoryLocation, ValueAndType> variable :
        lastState.valueAnalysisState().getConstants()) {
      if (!violationCondition.contains(variable.getKey())
          && !declared.contains(variable.getKey().getQualifiedName())
          && !DeserializeSymbolicExecutionStateOperator.accessedVariables
              .get(blockNode.getId())
              .containsKey(variable.getKey().getQualifiedName())) {
        violationCondition.assignConstant(
            variable.getKey(), variable.getValue().getValue(), variable.getValue().getType());
      }
    }
    return Optional.of(
        new SymbolicExecutionState(violationCondition, lastState.constraintsState()));
  }

  private static @NonNull Set<String> getDeclared(ARGPath pARGPath) {
    Set<String> declared = new HashSet<>();
    for (CFAEdge edge : pARGPath.getFullPath()) {
      if (edge instanceof ADeclarationEdge aDeclarationEdge) {
        declared.add(aDeclarationEdge.getDeclaration().getQualifiedName());
      }
      if (edge instanceof AStatementEdge aStatementEdge
          && aStatementEdge.getStatement() instanceof CAssignment cAssignment) {
        if (cAssignment.getLeftHandSide() instanceof CIdExpression id) {
          declared.add(id.getDeclaration().getQualifiedName());
        }
      }
    }
    return declared;
  }
}
