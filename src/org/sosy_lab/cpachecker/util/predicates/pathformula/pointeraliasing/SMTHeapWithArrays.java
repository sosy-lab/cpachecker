// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/** SMT heap representation with multiple arrays. */
class SMTHeapWithArrays implements SMTHeap {

  private final ArrayFormulaManagerView afmgr;
  private final FormulaManagerView formulaManager;
  private final FormulaType<?> pointerType;

  SMTHeapWithArrays(FormulaManagerView pFormulaManager, FormulaType<?> pPointerType) {
    formulaManager = pFormulaManager;
    afmgr = formulaManager.getArrayFormulaManager();
    pointerType = pPointerType;
  }

  @Override
  public <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      String targetName,
      FormulaType<?> pTargetType,
      int oldIndex,
      int newIndex,
      I address,
      E value) {
    FormulaType<E> targetType = formulaManager.getFormulaType(value);
    checkArgument(pTargetType.equals(targetType));
    FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(pointerType.equals(addressType));
    final ArrayFormula<I, E> oldFormula =
        afmgr.makeArray(targetName, oldIndex, addressType, targetType);
    final ArrayFormula<I, E> arrayFormula =
        afmgr.makeArray(targetName, newIndex, addressType, targetType);
    return formulaManager.makeEqual(arrayFormula, afmgr.store(oldFormula, address, value));
  }

  @Override
  public <I extends Formula, E extends Formula> BooleanFormula makeQuantifiedPointerAssignment(
      final String targetName,
      final FormulaType<?> pTargetType,
      final int oldIndex,
      final int newIndex,
      final I address,
      final BooleanFormula condition,
      final E value) {

    // handle the in the same fashion as if the select was an uninterpreted function

    FormulaType<E> targetType = formulaManager.getFormulaType(value);
    checkArgument(pTargetType.equals(targetType));
    FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(pointerType.equals(addressType));

    final ArrayFormula<I, E> oldFormula =
        afmgr.makeArray(targetName, oldIndex, addressType, targetType);
    final ArrayFormula<I, E> newFormula =
        afmgr.makeArray(targetName, newIndex, addressType, targetType);

    Formula oldRead = afmgr.select(oldFormula, address);
    Formula newRead = afmgr.select(newFormula, address);

    // forall i . (c and a'[i] = v) or ((not c) and a'[i] = a[i])
    // the quantifier is constructed outside
    // i ... address
    // c ... condition
    // a[i] ... oldRead
    // a'[i] ... newRead
    // v ... value

    BooleanFormulaManagerView bfmgr = formulaManager.getBooleanFormulaManager();

    // c and a'[i] = v
    BooleanFormula applyAssignmentFormula = formulaManager.assignment(newRead, value);
    BooleanFormula applyAssignmentCase = bfmgr.and(condition, applyAssignmentFormula);

    // (not c) and a'[i] = a[i]
    BooleanFormula retainAssignmentFormula = formulaManager.assignment(newRead, oldRead);
    BooleanFormula retainAssignmentCase = bfmgr.and(bfmgr.not(condition), retainAssignmentFormula);

    BooleanFormula result = bfmgr.or(applyAssignmentCase, retainAssignmentCase);

    return result;
  }

  @Override
  public <E extends Formula> BooleanFormula makeIdentityPointerAssignment(
      final String targetName,
      final FormulaType<E> pTargetType,
      final int oldIndex,
      final int newIndex) {
    final ArrayFormula<?, E> oldFormula =
        afmgr.makeArray(targetName, oldIndex, pointerType, pTargetType);
    final ArrayFormula<?, E> newFormula =
        afmgr.makeArray(targetName, newIndex, pointerType, pTargetType);
    return formulaManager.makeEqual(newFormula, oldFormula);
  }

  @Override
  public <I extends Formula, E extends Formula> E makePointerDereference(
      String targetName, FormulaType<E> targetType, I address) {
    final FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(pointerType.equals(addressType));

    final ArrayFormula<I, E> arrayFormula = afmgr.makeArray(targetName, addressType, targetType);
    return afmgr.select(arrayFormula, address);
  }

  @Override
  public <I extends Formula, V extends Formula> V makePointerDereference(
      String targetName, FormulaType<V> targetType, int ssaIndex, I address) {
    final FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(pointerType.equals(addressType));
    final ArrayFormula<I, V> arrayFormula =
        afmgr.makeArray(targetName, ssaIndex, addressType, targetType);
    return afmgr.select(arrayFormula, address);
  }
}
