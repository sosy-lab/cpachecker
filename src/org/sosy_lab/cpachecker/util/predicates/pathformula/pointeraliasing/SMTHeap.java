// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/** Interface abstraction for SMT heap accesses. */
interface SMTHeap {

  record SMTAddressValue<I extends Formula, E extends Formula>(I address, E value) {}

  /**
   * Create a formula that represents an assignment to a value via a pointer.
   *
   * <p>The assignment may not contain encoded quantified variables. Use {@link
   * #makeQuantifiedPointerAssignment(String, FormulaType, int, int, BooleanFormula,
   * SMTAddressValue)} instead if it does.
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param pTargetType The formula type of the value
   * @param oldIndex The old SSA index for targetName
   * @param newIndex The new SSA index for targetName
   * @param assignment The assignment, which may not contain encoded quantified variables
   * @return A formula representing assignment of the form {@code targetName@newIndex[address] =
   *     value}
   */
  <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      final String targetName,
      final FormulaType<?> pTargetType,
      final int oldIndex,
      final int newIndex,
      final SMTAddressValue<I, E> assignment);

  /**
   * Create a formula that represents a conditional assignment to a value via a pointer, with the
   * possibility of using quantified variables encoded in the formulas for address, condition, and
   * value to set.
   *
   * <p>Since the formulas may contain encoded quantified variables, it may not be possible to
   * perform the assignment using the theory used for standard {@link #makePointerAssignment(String,
   * FormulaType, int, int, SMTAddressValue)}. Specifically, for the theory of arrays, it is not
   * possible to perform a write to quantified address as the meaning would be "value is stored to
   * one of the selected locations" instead of proper "value is stored to each address as
   * aplicable".
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param pTargetType The formula type of the value
   * @param oldIndex The old SSA index for targetName
   * @param newIndex The new SSA index for targetName
   * @param condition The condition upon which the value is assigned to the address, otherwise, the
   *     previous value is retained. May contain encoded quantified variables.
   * @param assignment The combination of address to assign to and the value to assign. May contain
   *     encoded quantified variables, therefore actually "pointing to multiple addresses".
   * @return A formula representing the assignments.
   */
  <I extends Formula, E extends Formula> BooleanFormula makeQuantifiedPointerAssignment(
      final String targetName,
      final FormulaType<?> pTargetType,
      final int oldIndex,
      final int newIndex,
      final BooleanFormula condition,
      final SMTAddressValue<I, E> assignment);

  /**
   * Create a formula that represents an assignment from old SSA index to new SSA index without
   * changing the value. Useful for creating conditional assignments.
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param pTargetType The formula type of the value
   * @param oldIndex The old SSA index for targetName
   * @param newIndex The new SSA index for targetName
   * @return A formula representing an assignment of the form {@code targetName@newIndex[address] =
   *     value}
   */
  <E extends Formula> BooleanFormula makeIdentityPointerAssignment(
      final String targetName,
      final FormulaType<E> pTargetType,
      final int oldIndex,
      final int newIndex);

  /**
   * Make a formula that represents a pointer access.
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param targetType The formula type of the value
   * @param address The address to access
   * @return A formula representing {@code targetName@ssaIndex[address]}
   */
  <I extends Formula, E extends Formula> E makePointerDereference(
      final String targetName, final FormulaType<E> targetType, final I address);

  /**
   * Make a formula that represents a pointer access.
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param targetType The formula type of the value
   * @param address The address to access
   * @return A formula representing {@code targetName[address]}
   */
  <I extends Formula, V extends Formula> V makePointerDereference(
      final String targetName,
      final FormulaType<V> targetType,
      final int ssaIndex,
      final I address);
}
