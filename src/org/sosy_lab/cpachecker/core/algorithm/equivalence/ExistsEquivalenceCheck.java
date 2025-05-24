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
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class ExistsEquivalenceCheck implements EquivalenceCheck {

  private final Solver solver;

  public ExistsEquivalenceCheck(Solver pSolver) {
    solver = pSolver;
  }

  @Override
  public EquivalenceData isEquivalent(List<BooleanFormula> original, List<BooleanFormula> mutant)
      throws InterruptedException, SolverException {
    int originalSize = original.size();
    int checkedSafe = 0;
    int checkedUnsafe = 0;
    int falseOrig =
        FluentIterable.from(original)
            .filter(solver.getFormulaManager().getBooleanFormulaManager()::isFalse)
            .size();
    int falseMutant =
        FluentIterable.from(mutant)
            .filter(solver.getFormulaManager().getBooleanFormulaManager()::isFalse)
            .size();
    for (BooleanFormula origFormula : original) {
      if (solver.getFormulaManager().getBooleanFormulaManager().isFalse(origFormula)) {
        continue;
      }
      boolean foundImplication = false;
      for (BooleanFormula mutantFormula : mutant) {
        if (solver.implies(origFormula, mutantFormula)) {
          foundImplication = true;
          break;
        }
      }
      if (!foundImplication) {
        checkedUnsafe++;
        break;
      } else {
        checkedSafe++;
      }
    }
    return new EquivalenceData(
        checkedSafe + falseOrig == originalSize,
        originalSize,
        mutant.size(),
        originalSize,
        checkedSafe,
        checkedUnsafe,
        originalSize - checkedSafe - checkedUnsafe,
        falseOrig,
        falseMutant);
  }
}
