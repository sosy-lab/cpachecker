// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicateAbstractDomain implements AbstractDomain {

  private final PredicateAbstractionManager mgr;
  private final BooleanFormulaManagerView bmgr;
  private final boolean symbolicCoverageCheck;
  private final PredicateStatistics statistics;

  private final TimerWrapper coverageCheckTimer;
  private final TimerWrapper bddCoverageCheckTimer;
  private final TimerWrapper symbolicCoverageCheckTimer;

  public PredicateAbstractDomain(
      PredicateAbstractionManager pPredAbsManager,
      boolean pSymbolicCoverageCheck,
      PredicateStatistics pStatistics,
      BooleanFormulaManagerView pBmanager) {
    mgr = pPredAbsManager;
    symbolicCoverageCheck = pSymbolicCoverageCheck;
    statistics = pStatistics;
    bmgr = pBmanager;

    coverageCheckTimer = statistics.coverageCheckTimer.getNewTimer();
    bddCoverageCheckTimer = statistics.bddCoverageCheckTimer.getNewTimer();
    symbolicCoverageCheckTimer = statistics.symbolicCoverageCheckTimer.getNewTimer();
  }

  @Override
  public boolean isLessOrEqual(AbstractState element1,
                                       AbstractState element2) throws CPAException, InterruptedException {
    coverageCheckTimer.start();
    try {

      if (element1 instanceof PredicateProjectedState
          && element2 instanceof PredicateProjectedState) {

        PredicateProjectedState e1 = (PredicateProjectedState) element1;
        PredicateProjectedState e2 = (PredicateProjectedState) element2;

        BooleanFormula abs1 = e1.getGuard();
        BooleanFormula abs2 = e2.getGuard();
        if (!bmgr.isTrue(abs2) && bmgr.isTrue(abs1)) {
          return false;
        }
        BooleanFormula implication = bmgr.implication(abs1, abs2);
        BooleanFormula negation = bmgr.not(implication);
        try {
          if (!mgr.unsat(negation)) {
            return false;
          }
        } catch (SolverException | InterruptedException e) {
          return false;
        }

        AbstractEdge edge1 = e1.getAbstractEdge();
        AbstractEdge edge2 = e2.getAbstractEdge();

        if (edge1 instanceof PredicateAbstractEdge && edge2 instanceof PredicateAbstractEdge) {

          PredicateAbstractEdge pEdge1 = (PredicateAbstractEdge) edge1;
          PredicateAbstractEdge pEdge2 = (PredicateAbstractEdge) edge2;

          if (pEdge2 == PredicateAbstractEdge.getHavocEdgeInstance()) {
            return true;
          } else if (pEdge1 == PredicateAbstractEdge.getHavocEdgeInstance()) {
            return false;
          }

          if (pEdge2.getFormulas().containsAll(pEdge1.getFormulas())) {
            return true;
          }
          return false;

        } else {
          return edge1.getClass() == edge2.getClass();
        }

      } else if (element1 instanceof PredicateAbstractState
          && element2 instanceof PredicateAbstractState) {

        PredicateAbstractState e1 = (PredicateAbstractState) element1;
        PredicateAbstractState e2 = (PredicateAbstractState) element2;

        // TODO time statistics (previously in formula manager)
        /*
         * long start = System.currentTimeMillis(); entails(f1, f2); long end =
         * System.currentTimeMillis(); stats.bddCoverageCheckMaxTime =
         * Math.max(stats.bddCoverageCheckMaxTime, (end - start)); stats.bddCoverageCheckTime +=
         * (end - start); ++stats.numCoverageChecks;
         */

        if (e1.isAbstractionState() && e2.isAbstractionState()) {
          bddCoverageCheckTimer.start();

          // if e1's predicate abstraction entails e2's pred. abst.
          boolean result =
              mgr.checkCoverage(e1.getAbstractionFormula(), e2.getAbstractionFormula());

          bddCoverageCheckTimer.stop();
          return result;

        } else if (e2.isAbstractionState()) {
          if (symbolicCoverageCheck) {
            symbolicCoverageCheckTimer.start();

            boolean result =
                mgr.checkCoverage(
                    e1.getAbstractionFormula(),
                    e1.getPathFormula(),
                    e2.getAbstractionFormula());

            symbolicCoverageCheckTimer.stop();
            return result;

          } else {
            return false;
          }

        } else if (e1.isAbstractionState()) {
          return false;

        } else {

          if (e1.getPathFormula().equals(e2.getPathFormula())) {
            return true;
          }
          // only the fast check which returns true if a merge occurred for this element
          return e1.getMergedInto() == e2;
        }
      } else {
        throw new UnsupportedOperationException("Should not be possible");
      }

    } catch (SolverException e) {
      throw new CPAException("Solver Exception", e);
    } finally {
      coverageCheckTimer.stop();
    }
  }

  @Override
  public AbstractState join(AbstractState pElement1,
      AbstractState pElement2) throws CPAException {
    throw new UnsupportedOperationException();
  }
}