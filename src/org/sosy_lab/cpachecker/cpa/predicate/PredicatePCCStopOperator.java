/*
i *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicatePCCStopOperator implements StopOperator {

  private final PredicateAbstractionManager paMgr;
  private final PathFormulaManager pMgr;

  private final AbstractionFormula trueAbs;

  public PredicatePCCStopOperator(
      PathFormulaManager pPfmgr, PredicateAbstractionManager pPredAbsManager) {
    paMgr = pPredAbsManager;
    pMgr = pPfmgr;

    trueAbs = paMgr.makeTrueAbstractionFormula(null);
  }

  @Override
  public boolean stop(AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    PredicateAbstractState e1 = (PredicateAbstractState) pState;

    for (AbstractState reachedState : pReached) {
      PredicateAbstractState e2 = (PredicateAbstractState) reachedState;

      try {
        if (isCoveredBy(e1, e2)) { return true; }
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
      return paMgr.checkCoverage(e1.getAbstractionFormula(), e1.getPathFormula(), e2.getAbstractionFormula());

    } else if (e1.isAbstractionState()) {
      return false;

    } else {
      if (e1.getAbstractionFormula() == e2.getAbstractionFormula()) {
        PathFormula pF = e1.getPathFormula();
        return paMgr.unsat(trueAbs, new PathFormula(pMgr.buildImplicationTestAsUnsat(pF, e2.getPathFormula()),
            pF.getSsa(), pF.getPointerTargetSet(), pF.getLength()));
      }
      return false;
    }
  }
}
