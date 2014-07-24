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
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


public class InvariantsMergeOperator implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException,
      InterruptedException {
    InvariantsState state1 = (InvariantsState) pState1;
    InvariantsState state2 = (InvariantsState) pState2;
    InvariantsPrecision precision = (InvariantsPrecision) pPrecision;
    boolean isMergeAllowed = isMergeAllowed(state1, state2, precision);
    Set<String> wideningTargets = state1.determineAbstractionStrategy(precision).determineWideningTargets(state2.determineAbstractionStrategy(precision));
    state1 = state1.widen(state2, precision, wideningTargets);
    if (state1 != pState1 && definitelyImplies(state2, reduceToGivenVariables(reduceToInterestingVariables(state1, precision), Sets.difference(state1.getEnvironment().keySet(), wideningTargets)))) {
      isMergeAllowed = true;
    }
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
    for (InvariantsFormula<CompoundInterval> assumption : pState2.getEnvironmentAsAssumptions()) {
      if (!pState1.definitelyImplies(assumption)) {
        return false;
      }
    }
    return true;
  }

  private static InvariantsState reduceToInterestingVariables(InvariantsState pState, InvariantsPrecision pPrecision) {
    return reduceToGivenVariables(pState, pPrecision.getInterestingVariables());
  }

  private static InvariantsState reduceToGivenVariables(InvariantsState pState, Iterable<? extends String> pVariables) {
    InvariantsState result = pState;
    for (String variableName : pState.getEnvironment().keySet()) {
      if (!Iterables.contains(pVariables, variableName)) {
        result = result.clear(variableName);
      }
    }
    return result;
  }

  private static boolean environmentsEqualWithRespectToInterestingVariables(InvariantsState pState1, InvariantsState pState2, InvariantsPrecision pPrecision) {
    Set<String> checkedVariables = new HashSet<>();
    Queue<String> waitlist = new ArrayDeque<>(pPrecision.getInterestingVariables());
    Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> environment1 = pState1.getEnvironment();
    Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> environment2 = pState2.getEnvironment();
    CollectVarsVisitor<CompoundInterval> collectVarsVisitor = new CollectVarsVisitor<>();
    while (!waitlist.isEmpty()) {
      String variableName = waitlist.poll();
      if (checkedVariables.add(variableName)) {
        InvariantsFormula<CompoundInterval> left = environment1.get(variableName);
        InvariantsFormula<CompoundInterval> right = environment2.get(variableName);
        if (left != right && (left == null || !left.equals(right))) {
          return false;
        }
        if (left != null) {
          waitlist.addAll(left.accept(collectVarsVisitor));
        }
      }
    }
    return true;
  }

}
