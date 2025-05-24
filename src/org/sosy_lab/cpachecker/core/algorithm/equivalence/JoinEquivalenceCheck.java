// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.equivalence;

import com.google.common.collect.FluentIterable;
import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class JoinEquivalenceCheck implements EquivalenceCheck {

  private final Solver solver;

  public JoinEquivalenceCheck(Solver pSolver) {
    solver = pSolver;
  }

  @Override
  public EquivalenceData isEquivalent(List<BooleanFormula> original, List<BooleanFormula> mutant)
      throws InterruptedException, SolverException {
    BooleanFormulaManagerView bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    boolean check = solver.implies(bmgr.or(original), bmgr.or(mutant));
    int checkedSafe = check ? 1 : 0;
    int checkedUnsafe = check ? 0 : 1;
    int falseOrig =
        FluentIterable.from(original)
            .filter(solver.getFormulaManager().getBooleanFormulaManager()::isFalse)
            .size();
    int falseMutant =
        FluentIterable.from(mutant)
            .filter(solver.getFormulaManager().getBooleanFormulaManager()::isFalse)
            .size();
    return new EquivalenceData(
        solver.implies(bmgr.or(original), bmgr.or(mutant)),
        original.size(),
        mutant.size(),
        1,
        checkedSafe,
        checkedUnsafe,
        0,
        falseOrig,
        falseMutant);
  }
}
