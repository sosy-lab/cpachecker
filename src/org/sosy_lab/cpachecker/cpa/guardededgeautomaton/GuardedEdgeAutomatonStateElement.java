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
package org.sosy_lab.cpachecker.cpa.guardededgeautomaton;

import java.util.ArrayList;

import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPGuard;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public abstract class GuardedEdgeAutomatonStateElement implements
    GuardedEdgeAutomatonElement, IGuardedEdgeAutomatonStateElement, Targetable {

  private final NondeterministicFiniteAutomaton.State mAutomatonState;
  private final boolean mIsFinalState;

  public GuardedEdgeAutomatonStateElement(NondeterministicFiniteAutomaton.State pState, boolean pIsFinalState) {
    mAutomatonState = pState;
    mIsFinalState = pIsFinalState;
  }

  @Override
  public boolean isTarget() {
    return isFinalState();
  }

  @Override
  public final boolean isFinalState() {
    return mIsFinalState;
  }

  @Override
  public final NondeterministicFiniteAutomaton.State getAutomatonState() {
    return mAutomatonState;
  }

  public static GuardedEdgeAutomatonStateElement create(NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge pEdge, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    NondeterministicFiniteAutomaton.State lAutomatonState = pEdge.getTarget();

    GuardedEdgeLabel lLabel = pEdge.getLabel();

    boolean lIsFinalState = pAutomaton.getFinalStates().contains(lAutomatonState);

    if (lLabel.hasGuards()) {
      ArrayList<ECPPredicate> lPredicates = new ArrayList<ECPPredicate>(lLabel.getNumberOfGuards());

      for (ECPGuard lGuard : lLabel) {
        assert(lGuard instanceof ECPPredicate);

        lPredicates.add((ECPPredicate)lGuard);
      }

      return new GuardedEdgeAutomatonPredicateElement(lAutomatonState, lPredicates, lIsFinalState);
    }
    else {
      return new GuardedEdgeAutomatonStandardElement(lAutomatonState, lIsFinalState);
    }
  }

  @Override
  public String toString() {
    return "GuardedEdgeAutomatonStateElement[" + mAutomatonState.toString() + "]@" + hashCode();
  }

}
