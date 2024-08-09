// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DSSMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedPredicateStateOperator implements ProceedOperator {

  private final Solver solver;

  public ProceedPredicateStateOperator(Solver pSolver) {
    solver = pSolver;
  }

  @Override
  public DSSMessageProcessing processForward(AbstractState pState) {
    return DSSMessageProcessing.proceed();
  }

  @Override
  public DSSMessageProcessing processBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    PredicateAbstractState predicateAbstractState = (PredicateAbstractState) pState;
    BooleanFormula formula;
    if (predicateAbstractState.isAbstractionState()) {
      formula = predicateAbstractState.getAbstractionFormula().asFormula();
    } else {
      formula = predicateAbstractState.getPathFormula().getFormula();
    }
    if (solver.isUnsat(formula)) {
      return DSSMessageProcessing.stop();
    }
    return DSSMessageProcessing.proceed();
  }
}
