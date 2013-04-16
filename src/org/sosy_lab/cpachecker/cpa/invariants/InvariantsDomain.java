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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;

import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;

enum InvariantsDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public InvariantsState join(AbstractState pElement1, AbstractState pElement2) {
    InvariantsState element1 = (InvariantsState)pElement1;
    InvariantsState element2 = (InvariantsState)pElement2;

    if (element1.equals(element2)) {
      return element2;
    }

    MapDifference<String, Integer> remainingEvaluationDifferences =
        Maps.difference(element1.getRemainingEvaluations(), element2.getRemainingEvaluations());
    Map<String, Integer> resultRemainingEvaluations =
        new HashMap<>(element1.getRemainingEvaluations().size());
    resultRemainingEvaluations.putAll(remainingEvaluationDifferences.entriesInCommon());
    resultRemainingEvaluations.putAll(remainingEvaluationDifferences.entriesOnlyOnLeft());
    resultRemainingEvaluations.putAll(remainingEvaluationDifferences.entriesOnlyOnRight());
    for (Entry<String, ValueDifference<Integer>> entry : remainingEvaluationDifferences.entriesDiffering().entrySet()) {
      ValueDifference<Integer> difference = entry.getValue();
      int newValue = Math.min(difference.leftValue(), difference.rightValue());
      resultRemainingEvaluations.put(entry.getKey(), newValue);
    }

    MapDifference<String, InvariantsFormula<CompoundState>> environmentDifferences =
        Maps.difference(element1.getEnvironment(), element2.getEnvironment());
    Map<String, InvariantsFormula<CompoundState>> resultEnvironment =
        new HashMap<>(element1.getEnvironment().size());
    resultEnvironment.putAll(environmentDifferences.entriesInCommon());
    for (Entry<String, ValueDifference<InvariantsFormula<CompoundState>>> entry : environmentDifferences.entriesDiffering().entrySet()) {
      InvariantsFormula<CompoundState> newValue =
          InvariantsFormulaManager.INSTANCE.union(
              entry.getValue().leftValue(), entry.getValue().rightValue());
      resultEnvironment.put(entry.getKey(), newValue);
    }

    Set<InvariantsFormula<CompoundState>> resultInvariants =
        new HashSet<>(element1.getInvariants());
    resultInvariants.retainAll(element2.getInvariants());

    int newThreshold = Math.min(element1.getEvaluationThreshold(), element2.getEvaluationThreshold());

    return InvariantsState.from(resultRemainingEvaluations,
        newThreshold, resultInvariants, resultEnvironment);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {
    // check whether element 1 (or left) contains more information than element 2 (or right)
    InvariantsState element1 = (InvariantsState)pElement1;
    InvariantsState element2 = (InvariantsState)pElement2;

    MapDifference<String, InvariantsFormula<CompoundState>> differences =
        Maps.difference(element1.getEnvironment(), element2.getEnvironment());

    if (differences.areEqual()) {
      return element2.getInvariants().containsAll(element1.getInvariants());
    }

    if (!differences.entriesOnlyOnRight().isEmpty()) {
      // right knows more, so it is not greater than left
      return false;
    }

    for (ValueDifference<InvariantsFormula<CompoundState>> valueDifference : differences.entriesDiffering().values()) {
      if (valueDifference.leftValue() instanceof Constant<?> && valueDifference.rightValue() instanceof Constant<?>) {
        Constant<CompoundState> leftValue = (Constant<CompoundState>) valueDifference.leftValue();
        Constant<CompoundState> rightValue = (Constant<CompoundState>) valueDifference.rightValue();
        if (!rightValue.getValue().contains(leftValue.getValue())) {
          return false;
        }
      } else {
        // TODO better check if right value contains left value?
        return false;/*
        if (!values.rightValue().contains(values.leftValue())) {
          // right is more specific, so it has more information
          return false;
        }*/
      }
    }

    return element2.getInvariants().containsAll(element1.getInvariants());
  }

}
