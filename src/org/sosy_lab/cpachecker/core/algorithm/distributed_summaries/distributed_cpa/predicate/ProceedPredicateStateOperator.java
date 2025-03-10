// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedPredicateStateOperator implements ProceedOperator {

  private final Solver solver;

  public ProceedPredicateStateOperator(Solver pSolver) {
    solver = pSolver;
  }

  @Override
  public DssMessageProcessing processForward(AbstractState pState) {
    return DssMessageProcessing.proceed();
  }

  @Override
  public DssMessageProcessing processBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    PredicateAbstractState predicateAbstractState =
        Objects.requireNonNull(
            AbstractStates.extractStateByType(pState, PredicateAbstractState.class));
    BooleanFormula formula;
    if (predicateAbstractState.isAbstractionState()) {
      formula = predicateAbstractState.getAbstractionFormula().asFormula();
    } else {
      formula = predicateAbstractState.getPathFormula().getFormula();
    }
    if (solver.isUnsat(formula)) {
      return DssMessageProcessing.stop();
    }
    return DssMessageProcessing.proceed();
  }
}
