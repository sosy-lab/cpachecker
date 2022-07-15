// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicatePCCStopOperator implements StopOperator {

  private final PredicateAbstractionManager paMgr;
  private final PathFormulaManager pMgr;
  private final Solver solver;

  private final AbstractionFormula trueAbs;

  public PredicatePCCStopOperator(
      PathFormulaManager pPfmgr, PredicateAbstractionManager pPredAbsManager, Solver pSolver) {
    paMgr = pPredAbsManager;
    pMgr = pPfmgr;
    solver = pSolver;

    trueAbs = paMgr.makeTrueAbstractionFormula(null);
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    PredicateAbstractState e1 = (PredicateAbstractState) pState;

    for (AbstractState reachedState : pReached) {
      PredicateAbstractState e2 = (PredicateAbstractState) reachedState;

      try {
        if (isCoveredBy(e1, e2)) {
          return true;
        }
      } catch (SolverException e) {
        throw new CPAException("Solver Failure", e);
      }
    }
    return false;
  }

  private boolean isCoveredBy(final PredicateAbstractState e1, final PredicateAbstractState e2)
      throws InterruptedException, SolverException {
    if (e1.isAbstractionState() && e2.isAbstractionState()) {
      return paMgr.checkCoverage(e1.getAbstractionFormula(), e2.getAbstractionFormula());

    } else if (e2.isAbstractionState()) {
      return paMgr.checkCoverage(
          e1.getAbstractionFormula(), e1.getPathFormula(), e2.getAbstractionFormula());

    } else if (e1.isAbstractionState()) {
      return false;

    } else {
      if (e1.getAbstractionFormula() == e2.getAbstractionFormula()) {
        BooleanFormula implicationTest =
            pMgr.buildImplicationTestAsUnsat(e1.getPathFormula(), e2.getPathFormula());
        return solver.isUnsat(
            solver.getFormulaManager().makeAnd(trueAbs.asInstantiatedFormula(), implicationTest));
      }
      return false;
    }
  }
}
