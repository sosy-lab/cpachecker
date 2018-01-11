/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;


public class PredicateStopForInferenceObjects implements StopOperator {

  private final BooleanFormulaManagerView mngr;
  private final PredicateAbstractionManager amngr;

  public PredicateStopForInferenceObjects(BooleanFormulaManagerView pMngr,
      PredicateAbstractionManager pAbstractionManager) {
    mngr = pMngr;
    amngr = pAbstractionManager;
  }

  @Override
  public boolean stop(AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision) throws CPAException, InterruptedException {
    PredicateInferenceObject target = (PredicateInferenceObject) pState;

    for (AbstractState state : pReached) {
      PredicateInferenceObject object = (PredicateInferenceObject) state;
      if (object.getAction().containsAll(target.getAction())) {
        BooleanFormula abs1 = target.getGuard();
        BooleanFormula abs2 = object.getGuard();
        if (mngr.isTrue(abs2)) {
          return true;
        }
        if (mngr.isTrue(abs1)) {
          return false;
        }
        BooleanFormula implication = mngr.implication(abs1, abs2);
        BooleanFormula negation = mngr.not(implication);
        try {
          if (amngr.unsat(negation)) {
            //Covered
            return true;
          }
        } catch (SolverException | InterruptedException e) {
          return false;
        }
      }
    }

    return false;
  }

}
