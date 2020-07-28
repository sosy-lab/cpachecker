// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sl;

import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.Formula;

/**
 * This class is used to pass relevant arguments to the {@link ExpressionToFormulaVisitor}.
 */
public class SLCheckAllocationDelegate implements PointerTargetSetBuilder {

  private final Solver solver;
  private final SLState state;

  public SLCheckAllocationDelegate(Solver pSolver, SLState pState) {
    solver = pSolver;
    state = pState;
  }

  @Override
  public PointerTargetSet build() {
    return PointerTargetSet.emptyPointerTargetSet();
  }

  public Solver getSolver() {
    return solver;
  }

  public SLState getState() {
    return state;
  }

  /**
   * Checks whether the given location is allocated in the memory of the state the delegate was
   * instantiated with.
   *
   * @param pLoc The location to be checked.
   * @return The location in the memory or null.
   */
  public Formula checkAllocation(Formula pLoc) {
    return null;
  }

}
