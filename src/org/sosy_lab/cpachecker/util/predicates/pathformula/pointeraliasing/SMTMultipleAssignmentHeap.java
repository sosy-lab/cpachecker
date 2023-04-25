// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * Interface abstraction for SMT heap accesses, supporting assignment to multiple addresses at once.
 *
 * <p>The intent behind this interface is to provide a way for {@link SMTHeapWithByteArray} to
 * delegate assignments to another backing heap. Since there may be multiple assignments to the
 * backing heap needed for each assignment to {@link SMTHeapWithByteArray} and we do not want to
 * create a new SSA index for each one, the backing heap must implement this interface.
 */
interface SMTMultipleAssignmentHeap extends SMTHeap {

  /**
   * Create a formula that represents multiple assignments to a value via a pointer.
   *
   * <p>The assignments may not contain encoded quantified variables. Each address may be assigned
   * to at most once.
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param pTargetType The formula type of the value
   * @param oldIndex The old SSA index for targetName
   * @param newIndex The new SSA index for targetName
   * @param assignments The assignments, each one combining the address where the value should be
   *     written and the value to write
   * @return A formula representing assignments of the form {@code targetName@newIndex[address] =
   *     value}
   */
  <I extends Formula, E extends Formula> BooleanFormula makePointerAssignments(
      final String targetName,
      final FormulaType<?> pTargetType,
      final int oldIndex,
      final int newIndex,
      final List<SMTAddressValue<I, E>> assignments);

  /** {@inheritDoc} */
  @Override
  default <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      final String targetName,
      final FormulaType<?> pTargetType,
      final int oldIndex,
      final int newIndex,
      final SMTAddressValue<I, E> assignment) {
    return makePointerAssignments(
        targetName, pTargetType, oldIndex, newIndex, ImmutableList.of(assignment));
  }
}
