// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

public abstract class SSAIndexProvider<T extends Type> {

  // Index that is used to read from variables that were not assigned yet
  protected static final int VARIABLE_UNINITIALIZED = 1;

  // Index to be used for first assignment to a variable (must be higher than
  // VARIABLE_UNINITIALIZED!)
  protected static final int VARIABLE_FIRST_ASSIGNMENT = 2;

  /** Produces a fresh new SSA index for an assignment and updates the SSA map. */
  protected int makeFreshIndex(String name, T type, SSAMapBuilder ssa) {
    int idx = getFreshIndex(name, type, ssa);
    ssa.setIndex(name, type, idx);
    return idx;
  }

  /**
   * Produces a fresh new SSA index for an assignment, but does _not_ update the SSA map. Usually
   * you should use {@link #makeFreshIndex(String, Type, SSAMapBuilder)} instead, because using
   * variables with indices that are not stored in the SSAMap is not a good idea (c.f. the comment
   * inside getIndex()). If you use this method, you need to make sure to update the SSAMap
   * correctly.
   */
  @SuppressWarnings("unused") // The parameter in the middle is there only to prohibit subclassing
  protected int getFreshIndex(String name, T pType, SSAMapBuilder ssa) {
    int idx = ssa.getFreshIndex(name);
    if (idx <= 0) {
      idx = LanguageToSmtConverter.VARIABLE_FIRST_ASSIGNMENT;
    }
    return idx;
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there is none, it
   * creates one with the value 1.
   *
   * <p>Note that this not check whether the variable has always the same type. It is the caller's
   * responsibility to ensure that.
   *
   * @return the index of the variable
   */
  public int getExistingOrNewIndex(String name, T type, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(name);
    if (idx <= 0) {
      idx = LanguageToSmtConverter.VARIABLE_UNINITIALIZED;

      // It is important to store the index in the variable here.
      // If getIndex() was called with a specific name,
      // this means that name@idx will appear in formulas.
      // Thus, we need to make sure that calls to FormulaManagerView.instantiate()
      // will also add indices for this name,
      // which it does exactly if the name is in the SSAMap.
      ssa.setIndex(name, type, idx);
    }

    return idx;
  }
}
