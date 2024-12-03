// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.SolverException;

public class TPAAbstractDomain implements AbstractDomain {

  private final PredicateAbstractionManager mgr;
  private final boolean symbolicCoverageCheck;
  private final PredicateStatistics statistics;

  private final TimerWrapper coverageCheckTimer;
  private final TimerWrapper bddCoverageCheckTimer;
  private final TimerWrapper symbolicCoverageCheckTimer;

  public TPAAbstractDomain(
      PredicateAbstractionManager pPredAbsManager,
      boolean pSymbolicCoverageCheck,
      PredicateStatistics pStatistics) {
    mgr = pPredAbsManager;
    symbolicCoverageCheck = pSymbolicCoverageCheck;
    statistics = pStatistics;

    coverageCheckTimer = statistics.coverageCheckTimer.getNewTimer();
    bddCoverageCheckTimer = statistics.bddCoverageCheckTimer.getNewTimer();
    symbolicCoverageCheckTimer = statistics.symbolicCoverageCheckTimer.getNewTimer();
  }

  @Override
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    coverageCheckTimer.start();
    try {
      TPAAbstractState s1 = (TPAAbstractState) state1;
      TPAAbstractState s2 = (TPAAbstractState) state2;

      if (s1.isAbstractionState() && s2.isAbstractionState()) {
        bddCoverageCheckTimer.start();

        // If s1's aF is the same or covered by s2's aF
        boolean isCovered = mgr.checkCoverage(s1.getAbstractionFormula(), s2.getAbstractionFormula());

        bddCoverageCheckTimer.stop();
        return isCovered;

      } else if (s2.isAbstractionState()){ // s1 is NonAbstractionState
        if (symbolicCoverageCheck) {
          symbolicCoverageCheckTimer.start();

          boolean isCovered =
              mgr.checkCoverage(
                  s1.getAbstractionFormula(), s1.getPathFormula(), s2.getAbstractionFormula());

          symbolicCoverageCheckTimer.stop();
          return isCovered;
        } else {
          return false;
        }
      } else if (s1.isAbstractionState()){ // s2 is NonAbstractionState
        return false;
      } else {
          if (s1.getPathFormula().equals(s2.getPathFormula())) {
            return true;
          }

          // only the fast check which returns true if a merge occurred for this element
          return s1.getMergedInto() == s2;
      }

    } catch (SolverException pE) {
      throw new RuntimeException(pE);
    } finally {
      coverageCheckTimer.stop();
    }
  }

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("Can't join 2 AbstractStates");
  }

}
