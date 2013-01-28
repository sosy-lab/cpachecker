/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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


import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Class with some helper methods for doing Impact-like refinements.
 */
class ImpactUtils {

  private ImpactUtils() {}

  /**
   * Extract the (uninstantiated) state formula from an ARG state.
   */
  static BooleanFormula getStateFormula(ARGState pARGState) {
    return AbstractStates.extractStateByType(pARGState, PredicateAbstractState.class).getAbstractionFormula().asFormula();
  }

  /**
   * Conjunctively add a formula to the state formula of an ARG state.
   * @param f The (uninstantiated) formula to add.
   * @param argState The state where to add the formula.
   * @param fmgr The formula manager.
   */
  static void addFormulaToState(BooleanFormula f, ARGState argState, FormulaManagerView fmgr) {
    PredicateAbstractState predState = AbstractStates.extractStateByType(argState, PredicateAbstractState.class);
    AbstractionFormula af = predState.getAbstractionFormula();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    BooleanFormula newFormula = bfmgr.and(f, af.asFormula());
    BooleanFormula instantiatedNewFormula = fmgr.instantiate(newFormula, predState.getPathFormula().getSsa());
    AbstractionFormula newAF = new AbstractionFormula(fmgr, new SymbolicRegionManager.SymbolicRegion(bfmgr, newFormula), newFormula, instantiatedNewFormula, af.getBlockFormula());
    predState.setAbstraction(newAF);
  }

}
