/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * Interface abstraction for smt heap accesses.
 */
public interface SMTHeap {

  /**
   * Create a formula that represents an assignment to a value via a pointer.
   * @param targetName The name of the pointer access symbol as returned by {@link MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param pTargetType The formula type of the value
   * @param oldIndex The old SSA index for targetName
   * @param newIndex The new SSA index for targetName
   * @param address The address where the value should be written
   * @param value The value to write
   * @return A formula representing an assignment of the form {@code targetName@newIndex[address] = value}
   */
  <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      final String targetName,
      final FormulaType<?> pTargetType,
      final int oldIndex,
      final int newIndex,
      final I address,
      final E value);
  /**
   * Make a formula that represents a pointer access.
   * @param targetName The name of the pointer access symbol as returned by {@link MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param targetType The formula type of the value
   * @param ssaIndex The SSA index for targetName
   * @param address The address to access
   * @return A formula representing {@code targetName@ssaIndex[address]}
   */
  <I extends Formula, E extends Formula> E makePointerDereference(
      final String targetName,
      final FormulaType<E> targetType,
      final I address);


  /**
   * Make a formula that represents a pointer access.
   * @param targetName The name of the pointer access symbol as returned by {@link MemoryRegionManager#getPointerAccessName(MemoryRegion)}
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
