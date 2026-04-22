// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicateStateCoverageOperator implements CoverageOperator {

  private final Solver solver;

  public PredicateStateCoverageOperator(Solver pSolver) {
    solver = pSolver;
  }

  /**
   * Check whether state1 is subsumed by state2, i.e., whether state1 implies state2. The formulas
   * used for the implication check encode all information transported by the
   * PredicateAbstractState. Even pointer information is included in the abstraction formulas.
   *
   * @param state1 First abstract state
   * @param state2 Second abstract state
   * @return True if state1 is subsumed by state2
   */
  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    PredicateAbstractState predicateState1 = (PredicateAbstractState) state1;
    PredicateAbstractState predicateState2 = (PredicateAbstractState) state2;
    BooleanFormula formula1;
    BooleanFormula formula2;
    if (predicateState1.isAbstractionState() && predicateState2.isAbstractionState()) {
      formula1 = predicateState1.getAbstractionFormula().asFormula();
      formula2 = predicateState2.getAbstractionFormula().asFormula();
    } else if (!predicateState1.isAbstractionState() && !predicateState2.isAbstractionState()) {
      formula1 = predicateState1.getPathFormula().getFormula();
      formula2 = predicateState2.getPathFormula().getFormula();
    } else {
      return false;
    }
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
