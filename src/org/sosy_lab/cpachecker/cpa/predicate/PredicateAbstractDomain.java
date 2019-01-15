/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicateAbstractDomain implements AbstractDomain {

  private final PredicateAbstractionManager mgr;
  private final boolean symbolicCoverageCheck;
  private final PredicateStatistics statistics;

  private final TimerWrapper coverageCheckTimer;
  private final TimerWrapper bddCoverageCheckTimer;
  private final TimerWrapper symbolicCoverageCheckTimer;

  public PredicateAbstractDomain(
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
  public boolean isLessOrEqual(AbstractState element1,
                                       AbstractState element2) throws CPAException, InterruptedException {
    coverageCheckTimer.start();
    try {

    PredicateAbstractState e1 = (PredicateAbstractState)element1;
    PredicateAbstractState e2 = (PredicateAbstractState)element2;

    // TODO time statistics (previously in formula manager)
    /*
  long start = System.currentTimeMillis();
  entails(f1, f2);
  long end = System.currentTimeMillis();
  stats.bddCoverageCheckMaxTime = Math.max(stats.bddCoverageCheckMaxTime,
      (end - start));
  stats.bddCoverageCheckTime += (end - start);
  ++stats.numCoverageChecks;
     */

    if (e1.isAbstractionState() && e2.isAbstractionState()) {
        bddCoverageCheckTimer.start();

      // if e1's predicate abstraction entails e2's pred. abst.
      boolean result = mgr.checkCoverage(e1.getAbstractionFormula(), e2.getAbstractionFormula());

        bddCoverageCheckTimer.stop();
      return result;

    } else if (e2.isAbstractionState()) {
      if (symbolicCoverageCheck) {
          symbolicCoverageCheckTimer.start();

        boolean result = mgr.checkCoverage(e1.getAbstractionFormula(), e1.getPathFormula(), e2.getAbstractionFormula());

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