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

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CompatibilityCheck;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;


public class PredicateCompatibilityCheck implements CompatibilityCheck {

  private final Solver solver;
  private final BooleanFormulaManager mngr;

  public PredicateCompatibilityCheck(Solver s) {
    solver = s;
    mngr = solver.getFormulaManager().getBooleanFormulaManager();
  }

  @Override
  public boolean compatible(AbstractState pState, InferenceObject pObject) {
    if (pObject == EmptyInferenceObject.getInstance()) {
      return true;
    }
    PredicateAbstractState state = (PredicateAbstractState) pState;
    PredicateInferenceObject object = (PredicateInferenceObject) pObject;

    BooleanFormula stateFormula = state.getAbstractionFormula().asFormula();
    BooleanFormula objectFormula = object.getGuard();

    BooleanFormula f = mngr.and(stateFormula, objectFormula);

    try {
      return !solver.isUnsat(f);

    } catch (SolverException | InterruptedException e) {
      Preconditions.checkArgument(false);
    }
    return false;
  }

}
