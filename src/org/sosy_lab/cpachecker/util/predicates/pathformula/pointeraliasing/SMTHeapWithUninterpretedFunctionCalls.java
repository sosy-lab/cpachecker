// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/** SMT heap representation with uninterpreted function calls. */
class SMTHeapWithUninterpretedFunctionCalls implements SMTMultipleAssignmentHeap {

  private final FunctionFormulaManagerView ffmgr;
  private final FormulaManagerView formulaManager;

  SMTHeapWithUninterpretedFunctionCalls(FormulaManagerView pFormulaManager) {
    formulaManager = pFormulaManager;
    ffmgr = formulaManager.getFunctionFormulaManager();
  }

  @Override
  public <I extends Formula, E extends Formula> BooleanFormula makePointerAssignments(
      String targetName,
      FormulaType<?> pTargetType,
      int oldIndex,
      int newIndex,
      List<SMTAddressValue<I, E>> assignments) {
    BooleanFormula result = formulaManager.getBooleanFormulaManager().makeTrue();
    for (SMTAddressValue<I, E> assignment : assignments) {
      I address = assignment.address();
      E value = assignment.value();
      FormulaType<E> targetType = formulaManager.getFormulaType(value);
      checkArgument(pTargetType.equals(targetType));
      final Formula lhs =
          ffmgr.declareAndCallUninterpretedFunction(targetName, newIndex, targetType, address);
      final BooleanFormula assignmentFormula = formulaManager.assignment(lhs, value);
      result = formulaManager.makeAnd(result, assignmentFormula);
    }
    return result;
  }

  @Override
  public <I extends Formula, E extends Formula> BooleanFormula makeQuantifiedPointerAssignment(
      String targetName,
      FormulaType<?> pTargetType,
      int oldIndex,
      int newIndex,
      final BooleanFormula condition,
      final SMTAddressValue<I, E> assignment) {
    // TODO: implement
    throw new UnsupportedOperationException(
        "Quantified pointer assignment not implemented for heap with uninterpreted function calls"
            + " yet");
  }

  @Override
  public <I extends Formula, E extends Formula> E makePointerDereference(
      String targetName, FormulaType<E> targetType, I address) {
    return ffmgr.declareAndCallUF(targetName, targetType, address);
  }

  @Override
  public <I extends Formula, V extends Formula> V makePointerDereference(
      String targetName, FormulaType<V> targetType, int ssaIndex, I address) {
    return ffmgr.declareAndCallUninterpretedFunction(targetName, ssaIndex, targetType, address);
  }

  @Override
  public <E extends Formula> BooleanFormula makeIdentityPointerAssignment(
      String targetName, FormulaType<E> targetType, int oldIndex, int nwIndex) {
    // the retainment assignments will be performed afterwards by AssignmentHandler
    return formulaManager.getBooleanFormulaManager().makeTrue();
  }
}
