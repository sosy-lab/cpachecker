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
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.SolverException;

public class ValueViolationConditionOperator implements ViolationConditionOperator {
  private final MachineModel machineModel;
  private final BlockNode blockNode;
  private final ValueAnalysisCPA valueCPA;
  private final boolean runSymExec;

  public ValueViolationConditionOperator(
      MachineModel pMachineModel,
      boolean pRunSymExec,
      BlockNode pBlockNode,
      ValueAnalysisCPA pValueCPA) {
    machineModel = pMachineModel;
    blockNode = pBlockNode;
    runSymExec = pRunSymExec;
    valueCPA = pValueCPA;
  }

  public Optional<AbstractState> computeViolationConditionValueAnalysis(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws CPATransferException, InterruptedException, SolverException {
    PathFormulaManagerImpl pfmgr = valueCPA.getBlockStrengtheningOperator().getPfmgr();
    PathFormula pathFormula;
    if (pPreviousCondition.isEmpty()) {
      pathFormula = pfmgr.makeEmptyPathFormula();
    } else {
      ValueAnalysisState previousCondition =
          AbstractStates.extractStateByType(
              pPreviousCondition.orElseThrow(), ValueAnalysisState.class);
      assert previousCondition != null;
      pathFormula = previousCondition.getViolationCondition();
      if (pathFormula == null) {
        pathFormula = pfmgr.makeEmptyPathFormula();
      }
    }
    for (CFAEdge cfaEdge : pARGPath.getFullPath().reverse()) {
      pathFormula = pfmgr.makeAnd(pathFormula, cfaEdge);
    }
    if (blockNode.isRoot()
        && valueCPA.getBlockStrengtheningOperator().getSolver().isUnsat(pathFormula.getFormula())) {
      return Optional.empty();
    }
    ValueAnalysisState state = new ValueAnalysisState(machineModel);
    state.setViolationCondition(pathFormula);
    return Optional.of(state);
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws CPATransferException, InterruptedException, SolverException {
    if (!runSymExec) {
      return computeViolationConditionValueAnalysis(pARGPath, pPreviousCondition);
    }

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
      if (edge instanceof AStatementEdge aStatementEdge
          && aStatementEdge.getStatement() instanceof CAssignment cAssignment) {
        if (cAssignment.getLeftHandSide() instanceof CIdExpression id)
          declared.add(id.getDeclaration().getQualifiedName());
      }
    }

    ValueAnalysisState violationValue =
        AbstractStates.extractStateByType(pARGPath.getLastState(), ValueAnalysisState.class);
    for (Entry<MemoryLocation, ValueAndType> variable : violationValue.getConstants()) {
      if (!violationCondition.contains(variable.getKey())
          && !declared.contains(variable.getKey().getQualifiedName())
          && !accessedVariables
              .get(blockNode.getId())
              .containsKey(variable.getKey().getQualifiedName())) {
        violationCondition.assignConstant(
            variable.getKey(), variable.getValue().getValue(), variable.getValue().getType());
      }
    }
    return Optional.of(violationCondition);
  }

  private static ValueAnalysisState getViolationValueState(
      ARGState pState, MachineModel pMachineModel) {

    BlockState blockState = AbstractStates.extractStateByType(pState, BlockState.class);
    assert (blockState != null && blockState.isTarget());

    if (blockState.getViolationConditions().isEmpty()) {
      return new ValueAnalysisState(pMachineModel);
    }
    AbstractState violation = blockState.getViolationConditions().getFirst();
    ValueAnalysisState violationValue =
        AbstractStates.extractStateByType(violation, ValueAnalysisState.class);
    assert violationValue != null;
    return violationValue;
  }
}
