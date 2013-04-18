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
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Class with some helper methods for doing Impact-like refinements.
 */
class ImpactUtils {

  private ImpactUtils() {}

  /**
   * Create an AbstractionFormula instance without actually computing an abstraction.
   * @param uninstantiatedStateFormula The state formula (without SSA indices).
   * @param instantiatedStateFormula The state formula (with SSA indices matching those of blockFormula).
   * @param blockFormula The block formula.
   * @param fmgr The formula manager.
   * @return An AbstractionFormula instance with a SymbolicRegion
   */
  private static AbstractionFormula createFakeAbstraction(
      BooleanFormula uninstantiatedStateFormula,
      BooleanFormula instantiatedStateFormula,
      PathFormula blockFormula, FormulaManagerView fmgr) {

    return new AbstractionFormula(fmgr,
        new SymbolicRegionManager.SymbolicRegion(fmgr.getBooleanFormulaManager(), uninstantiatedStateFormula),
        uninstantiatedStateFormula, instantiatedStateFormula, blockFormula);
  }

  /**
   * Given a state and a valid interpolant for this state,
   * strengthen the state by conjunctively adding the interpolant.
   * This method does nothing if the interpolant is already implied
   * by the state's state formula.
   * @param itp The interpolant (with indices matching those of the state's abstraction).
   * @param s The abstract state.
   * @param fmgr The formula manager.
   * @param solver The SMT solver.
   * @param predAbsMgr The predicate abstraction manager.
   * @return True if the state was actually changed
   * (i.e., the interpolant was not already implied by the state's state formula).
   */
  static boolean strengthenStateWithInterpolant(final BooleanFormula itp,
      final ARGState s, final FormulaManagerView fmgr,
      final PredicateAbstractionManager predAbsMgr) {

    final PredicateAbstractState predState = AbstractStates.extractStateByType(s, PredicateAbstractState.class);

    final BooleanFormula uninstantiatedItp = fmgr.uninstantiate(itp);
    AbstractionFormula newAbstraction = ImpactUtils.createFakeAbstraction(uninstantiatedItp, itp, predState.getAbstractionFormula().getBlockFormula(), fmgr);

    boolean isNewItp = !predAbsMgr.checkCoverage(predState.getAbstractionFormula(), newAbstraction);

    if (isNewItp) {
      newAbstraction = predAbsMgr.makeAnd(predState.getAbstractionFormula(), newAbstraction);
      predState.setAbstraction(newAbstraction);
    }
    return isNewItp;
  }
}
