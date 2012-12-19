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

import java.util.Set;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
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
   * Safely remove a port of the ARG which has been proved as completely
   * unreachable. This method takes care of the coverage relationships of the
   * removed nodes, re-adding covered nodes to the waitlist if necessary.
   * @param rootOfInfeasiblePart The root of the subtree to remove.
   * @param pReached The reached set.
   */
  static void removeInfeasiblePartofARG(ARGState rootOfInfeasiblePart,
      ARGReachedSet pReached) {
    Set<ARGState> infeasibleSubtree = rootOfInfeasiblePart.getSubgraph();

    uncover(infeasibleSubtree, pReached);

    for (ARGState removedNode : infeasibleSubtree) {
      removedNode.removeFromARG();
    }
    pReached.asReachedSet().removeAll(infeasibleSubtree);
  }

  /**
   * Try covering an ARG state by other states in the reached set.
   * If successful, also mark the subtree below this state as covered,
   * which means that all states in this subtree do not cover any states anymore.
   * @param v The state which should be covered if possible.
   * @param pReached The reached set.
   * @param argCpa The ARG CPA.
   * @return whether the covering was successful
   * @throws CPAException
   */
  static boolean cover(ARGState v, ARGReachedSet pReached, ARGCPA argCpa) throws CPAException {
    assert v.mayCover();
    ReachedSet reached = pReached.asReachedSet();

    argCpa.getStopOperator().stop(v, reached.getReached(v), reached.getPrecision(v));
    // ignore return value of stop, because it will always be false

    if (v.isCovered()) {
      reached.removeOnlyFromWaitlist(v);

      Set<ARGState> subtree = v.getSubgraph();

      // first, uncover all necessary states

      uncover(subtree, pReached);

      // second, clean subtree of covered element
      subtree.remove(v); // but no not clean v itself

      for (ARGState childOfV : subtree) {
        // each child of v is now not covered directly anymore
        if (childOfV.isCovered()) {
          childOfV.uncover();
        }

        reached.removeOnlyFromWaitlist(childOfV);

        childOfV.setHasCoveredParent(true);
      }

      for (ARGState childOfV : subtree) {
        // each child of v now doesn't cover anything anymore
        assert childOfV.getCoveredByThis().isEmpty();
        assert !childOfV.mayCover();
      }

      assert !reached.getWaitlist().contains(v.getSubgraph());
      return true;
    }
    return false;
  }

  /**
   * Uncover all states which are covered by a state in a given subtree of the ARG.
   * After this method returns, all the states in the subtree do not cover any
   * states anymore.
   * @param subtree The set of states of which to remove coverings.
   * @param reached The reached set.
   */
  static void uncover(Set<ARGState> subtree, ARGReachedSet reached) {
    Set<ARGState> coveredStates = ARGUtils.getCoveredBy(subtree);
    for (ARGState coveredState : coveredStates) {
      // uncover each previously covered state
      reached.uncover(coveredState);
    }
    assert ARGUtils.getCoveredBy(subtree).isEmpty() : "Subtree of covered node still covers other elements";
  }

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
