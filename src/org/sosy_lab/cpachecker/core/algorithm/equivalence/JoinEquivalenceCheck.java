// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.equivalence;

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class JoinEquivalenceCheck implements EquivalenceCheck {

  private final Solver solver;

  public JoinEquivalenceCheck(Solver pSolver) {
    // Constructor implementation
    solver = pSolver;
  }

  @Override
  public boolean isEquivalent(List<BooleanFormula> original, List<BooleanFormula> mutant)
      throws InterruptedException, SolverException {
    BooleanFormulaManagerView bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    return solver.implies(bmgr.or(original), bmgr.or(mutant));
  }
}
