// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.k3toformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.LanguageToSmtConverter.VARIABLE_FIRST_ASSIGNMENT;

import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

public class SSAHandler {

  /** Produces a fresh new SSA index for an assignment and updates the SSA map. */
  public static int makeFreshIndex(String name, K3Type type, SSAMapBuilder ssa) {
    int idx = getFreshIndex(name, ssa);
    ssa.setIndex(name, type, idx);
    return idx;
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand side of an
   * assignment. This method does not handle scoping and the NON_DET_VARIABLE!
   */
  public static Formula makeFreshVariable(
      String name, K3Type type, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    int useIndex = makeFreshIndex(name, type, ssa);

    Formula result = fmgr.makeVariable(type.toFormulaType(), name, useIndex);

    return result;
  }

  /**
   * Produces a fresh new SSA index for an assignment, but does _not_ update the SSA map. Usually
   * you should use {@link #makeFreshIndex(String, K3Type, SSAMapBuilder)} instead, because using
   * variables with indices that are not stored in the SSAMap is not a good idea (c.f. the comment
   * inside getIndex()). If you use this method, you need to make sure to update the SSAMap
   * correctly.
   */
  public static int getFreshIndex(String name, SSAMapBuilder ssa) {
    // TODO: Check that the variable for its type has been declared before?
    // checkSsaSavedType(name, type, ssa.getType(name));
    int idx = ssa.getFreshIndex(name);
    if (idx <= 0) {
      idx = VARIABLE_FIRST_ASSIGNMENT;
    }
    return idx;
  }
}
