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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;

enum InvariantsDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public InvariantsState join(AbstractState pElement1, AbstractState pElement2) {
    InvariantsState element1 = (InvariantsState) pElement1;
    InvariantsState element2 = (InvariantsState) pElement2;

    if (isLessOrEqual(element1, element2)) { return element2; }

    Map<String, Integer> resultRemainingEvaluations =
        new HashMap<>(element1.getRemainingEvaluations());
    for (Entry<String, Integer> entry : element2.getRemainingEvaluations().entrySet()) {
      Integer leftValue = resultRemainingEvaluations.get(entry.getKey());
      if (leftValue != null) {
        resultRemainingEvaluations.put(entry.getKey(), Math.min(leftValue, entry.getValue()));
      } else {
        resultRemainingEvaluations.put(entry.getKey(), entry.getValue());
      }
    }

    Map<? extends String, ? extends InvariantsFormula<CompoundState>> environment1 = element1.getEnvironment();
    Map<? extends String, ? extends InvariantsFormula<CompoundState>> environment2 = element2.getEnvironment();
    Map<String, InvariantsFormula<CompoundState>> resultEnvironment =
        new HashMap<>(element1.getEnvironment().size());

    for (Entry<? extends String, ? extends InvariantsFormula<CompoundState>> entry : environment2.entrySet()) {
      InvariantsFormula<CompoundState> leftValue = environment1.get(entry.getKey());
      if (leftValue != null) {
        InvariantsFormula<CompoundState> value = InvariantsFormulaManager.INSTANCE.union(leftValue, entry.getValue());
        value = value.accept(PartialEvaluator.INSTANCE, InvariantsState.EVALUATION_VISITOR);
        resultEnvironment.put(entry.getKey(), value);
      }
    }

    Set<InvariantsFormula<CompoundState>> resultCandidateAssumptions =
        new HashSet<>();
    resultCandidateAssumptions.addAll(element1.getCandidateAssumptions());
    resultCandidateAssumptions.addAll(element2.getCandidateAssumptions());

    int newThreshold = Math.min(element1.getEvaluationThreshold(), element2.getEvaluationThreshold());

    Set<InvariantsFormula<CompoundState>> resultAssumptions = new HashSet<>();

    if (!element1.getAssumptions().isEmpty() && !element2.getAssumptions().isEmpty()) {
      Iterator<InvariantsFormula<CompoundState>> leftAssumptionIterator = element1.getAssumptions().iterator();
      InvariantsFormula<CompoundState> leftTotalAssumption = leftAssumptionIterator.next();
      while (leftAssumptionIterator.hasNext()) {
        leftTotalAssumption = InvariantsFormulaManager.INSTANCE.logicalAnd(leftTotalAssumption, leftAssumptionIterator.next());
      }
      Iterator<InvariantsFormula<CompoundState>> rightAssumptionIterator = element2.getAssumptions().iterator();
      InvariantsFormula<CompoundState> rightTotalAssumption = rightAssumptionIterator.next();
      while (rightAssumptionIterator.hasNext()) {
        rightTotalAssumption = InvariantsFormulaManager.INSTANCE.logicalAnd(rightTotalAssumption, rightAssumptionIterator.next());
      }
      resultAssumptions.add(InvariantsFormulaManager.INSTANCE.logicalOr(leftTotalAssumption, rightTotalAssumption));
    }


    assert element1.getUseBitvectors() == element2.getUseBitvectors();

    InvariantsState result = InvariantsState.from(resultRemainingEvaluations,
        newThreshold, resultAssumptions, resultEnvironment,
        resultCandidateAssumptions, element1.getUseBitvectors());
    if (result == null) {
      return result;
    }
    if (result.equals(element2)) {
      return element2;
    }
    if (result.equals(element1)) {
      return element1;
    }
    return result;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {
    if (pElement1 == pElement2 || pElement1 != null && pElement1.equals(pElement2)) { return true; }
    if (pElement1 == null || pElement2 == null) {
      return false;
    }
    /*InvariantsState leftState = (InvariantsState) pElement1;
    InvariantsState rightState = (InvariantsState) pElement2;
    if (!leftState.getAssumptions().containsAll(rightState.getAssumptions())) {
      return false;
    }
    Map<? extends String, ? extends InvariantsFormula<CompoundState>> leftEnvironment = leftState.getEnvironment();
    for (Entry<? extends String, ? extends InvariantsFormula<CompoundState>> rightEnvElement : rightState.getEnvironment().entrySet()) {
      String rightVarName = rightEnvElement.getKey();
      InvariantsFormula<CompoundState> leftVarFormula = leftEnvironment.get(rightVarName);
      if (leftVarFormula == null) {
        return false;
      }
      InvariantsFormula<CompoundState> rightVarFormula = rightEnvElement.getValue();
      CompoundState leftVarValue = leftVarFormula.accept(new FormulaCompoundStateEvaluationVisitor(), leftState.getEnvironment());
      CompoundState rightVarValue = rightVarFormula.accept(new FormulaCompoundStateEvaluationVisitor(), rightState.getEnvironment());
      if (!rightVarValue.contains(leftVarValue)) {
        return false;
      }
    }
    return true;
    */
    return false;
  }

}
