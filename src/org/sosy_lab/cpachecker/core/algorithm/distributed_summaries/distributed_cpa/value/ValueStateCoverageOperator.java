// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class ValueStateCoverageOperator implements CoverageOperator {

  private final Solver solver;

  public ValueStateCoverageOperator(Solver pSolver) {
    solver = pSolver;
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    BooleanFormula formula1 =
        ((ValueAnalysisState) state1).getFormulaApproximation(solver.getFormulaManager());
    BooleanFormula formula2 =
        ((ValueAnalysisState) state2).getFormulaApproximation(solver.getFormulaManager());

    try {
      return solver.implies(formula1, formula2);
    } catch (SolverException e) {
      throw new CPAException("Solver encountered an issue when calculating implication.", e);
    }
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
