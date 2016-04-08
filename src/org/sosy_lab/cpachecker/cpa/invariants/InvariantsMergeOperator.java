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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


public class InvariantsMergeOperator implements MergeOperator {

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException,
      InterruptedException {
    InvariantsState state1 = (InvariantsState) pState1;
    InvariantsState state2 = (InvariantsState) pState2;
    InvariantsPrecision precision = (InvariantsPrecision) pPrecision;
    boolean isMergeAllowed = isMergeAllowed(state1, state2, precision);
    AbstractionState abstractionState1 = state1.determineAbstractionState(precision);
    AbstractionState abstractionState2 = state2.determineAbstractionState(precision);
    Set<MemoryLocation> wideningTargets = abstractionState1.determineWideningTargets(abstractionState2);
    wideningTargets = wideningTargets == null ? state1.getEnvironment().keySet() : wideningTargets;
    Set<BooleanFormula<CompoundInterval>> wideningHints = Sets.union(abstractionState1.getWideningHints(), abstractionState2.getWideningHints());
    state1 = state1.widen(state2, precision, wideningTargets, wideningHints);
    isMergeAllowed = isMergeAllowed
        || (state1 != pState1 && definitelyImplies(
            state2,
            reduceToGivenVariables(
                reduceToInterestingVariables(state1, precision),
                Sets.difference(state1.getEnvironment().keySet(), wideningTargets))));
    InvariantsState result = state2;
    if (isMergeAllowed) {
      result = state1.join(state2, precision);
    }
    return result;
  }

  private static boolean isMergeAllowed(InvariantsState pState1, InvariantsState pState2, InvariantsPrecision pPrecision) {
    return environmentsEqualWithRespectToInterestingVariables(pState1, pState2, pPrecision)
        || definitelyImplies(pState2, reduceToInterestingVariables(pState1, pPrecision));
  }

  private static boolean definitelyImplies(InvariantsState pState1, InvariantsState pState2) {
    for (BooleanFormula<CompoundInterval> assumption : pState2.getEnvironmentAsAssumptions()) {
      if (!pState1.definitelyImplies(assumption)) {
        return false;
      }
    }
    return true;
  }

  private static InvariantsState reduceToInterestingVariables(InvariantsState pState, InvariantsPrecision pPrecision) {
    return reduceToGivenVariables(pState, pPrecision.getInterestingVariables());
  }

  private static InvariantsState reduceToGivenVariables(final InvariantsState pState, final Iterable<? extends MemoryLocation> pVariables) {
    return pState.clearAll(new Predicate<MemoryLocation>() {

      @Override
      public boolean apply(MemoryLocation pMemoryLocation) {
        return !Iterables.contains(pVariables, pMemoryLocation);
      }
    });
  }

  private static boolean environmentsEqualWithRespectToInterestingVariables(InvariantsState pState1, InvariantsState pState2, InvariantsPrecision pPrecision) {
    Set<MemoryLocation> checkedVariables = new HashSet<>();
    Queue<MemoryLocation> waitlist = new ArrayDeque<>(pPrecision.getInterestingVariables());
    Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> environment1 = pState1.getEnvironment();
    Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> environment2 = pState2.getEnvironment();
    while (!waitlist.isEmpty()) {
      MemoryLocation memoryLocation = waitlist.poll();
      if (checkedVariables.add(memoryLocation)) {
        NumeralFormula<CompoundInterval> left = environment1.get(memoryLocation);
        NumeralFormula<CompoundInterval> right = environment2.get(memoryLocation);
        if (left != right && (left == null || !left.equals(right))) {
          return false;
        }
        if (left != null) {
          waitlist.addAll(left.accept(COLLECT_VARS_VISITOR));
        }
      }
    }
    return true;
  }

}
