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


public class InvariantsMergeOperator implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException,
      InterruptedException {
    InvariantsState state1 = (InvariantsState) pState1;
    InvariantsState state2 = (InvariantsState) pState2;
    InvariantsPrecision precision = (InvariantsPrecision) pPrecision;
    if (!environmentsEqualWithRespectToInterestingVariables(state1, state2, precision)) {
      return state2;
    }
    return state1.join(state2, precision);
  }

  private boolean environmentsEqualWithRespectToInterestingVariables(InvariantsState pState1, InvariantsState pState2, InvariantsPrecision pPrecision) {
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
