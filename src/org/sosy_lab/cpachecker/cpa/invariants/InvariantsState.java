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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaAbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ReplaceVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToRationalFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;

import com.google.common.base.Joiner;

public class InvariantsState implements AbstractState, FormulaReportingState {

  private static final InvariantsFormula<CompoundState> TOP = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.top());

  private static final InvariantsFormula<CompoundState> BOTTOM = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.bottom());

  private final int evaluationThreshold;

  private final Map<String, InvariantsFormula<CompoundState>> environment;

  private final Set<InvariantsFormula<CompoundState>> assumptions;

  private final Map<String, Integer> remainingEvaluations;

  private FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  private FormulaEvaluationVisitor<CompoundState> abstractionVisitor;

  public InvariantsState(int pEvaluationThreshold) {
    this.evaluationThreshold = pEvaluationThreshold;
    this.remainingEvaluations = new HashMap<>();
    this.assumptions = new HashSet<>();
    this.environment = new HashMap<>();
  }

  public static InvariantsState copy(InvariantsState pToCopy) {
    return from(pToCopy.remainingEvaluations, pToCopy.evaluationThreshold,
        pToCopy.assumptions, pToCopy.environment);
  }

  public static InvariantsState from(Map<String, Integer> pRemainingEvaluations,
      int pEvaluationThreshold,
      Collection<InvariantsFormula<CompoundState>> pInvariants,
      Map<String, InvariantsFormula<CompoundState>> pEnvironment) {
    InvariantsState result = new InvariantsState(pEvaluationThreshold);
    if (!result.assumeInternal(pInvariants, result.getFormulaResolver())) {
      return null;
    }
    result.remainingEvaluations.putAll(pRemainingEvaluations);
    result.putEnvironmentValuesInternal(pEnvironment);
    return result;
  }

  private boolean assumeInternal(Collection<InvariantsFormula<CompoundState>> pAssumptions,
      FormulaEvaluationVisitor<CompoundState> evaluationVisitor) {
    for (InvariantsFormula<CompoundState> invariant : pAssumptions) {
      if (!assumeInternal(invariant, evaluationVisitor)) {
        return false;
      }
    }
    return true;
  }

  private void putEnvironmentValuesInternal(Map<String, InvariantsFormula<CompoundState>> pEnvironment) {
    for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : pEnvironment.entrySet()) {
      putEnvironmentValueInternal(entry.getKey(), entry.getValue());
    }
  }

  private void putEnvironmentValueInternal(String pKey, InvariantsFormula<CompoundState> pValue) {
    InvariantsFormula<CompoundState> value = pValue.accept(PartialEvaluator.INSTANCE);
    if (value.equals(TOP)) {
      this.environment.remove(pKey);
    } else {
      this.environment.put(pKey, pValue.accept(PartialEvaluator.INSTANCE));
    }
  }

  public Map<String, Integer> getRemainingEvaluations() {
    return Collections.unmodifiableMap(remainingEvaluations);
  }

  public Map<? extends String, ? extends InvariantsFormula<CompoundState>> getEnvironment() {
    return Collections.unmodifiableMap(environment);
  }

  public InvariantsState assign(String varName, InvariantsFormula<CompoundState> value) {
    /*
     * A variable is newly assigned, so the appearances of this variable
     * in any previously collected invariants (including its new new value)
     * have to be resolved with the variable's previous value.
     */
    // TODO limit exact evaluations to avoid interpretations; add an "edge" parameter
    InvariantsFormulaManager fmgr = InvariantsFormulaManager.INSTANCE;
    InvariantsFormula<CompoundState> variable = fmgr.asVariable(varName);
    InvariantsFormula<CompoundState> previousValue = getEnvironmentValue(varName);
    ReplaceVisitor<CompoundState> replaceVisitor = new ReplaceVisitor<>(variable, previousValue);
    InvariantsFormula<CompoundState> newValue = value.accept(replaceVisitor).accept(PartialEvaluator.INSTANCE);
    InvariantsState result = new InvariantsState(this.evaluationThreshold);
    for (InvariantsFormula<CompoundState> assumption : this.assumptions) {
      // Try to add the invariant; if it turns out that it is false, the state is bottom
      if (!result.assumeInternal(assumption.accept(replaceVisitor),
          getFormulaResolver())) {
        return null;
      }
    }
    for (Map.Entry<String, InvariantsFormula<CompoundState>> environmentEntry : this.environment.entrySet()) {
      if (!environmentEntry.getKey().equals(varName)) {
        InvariantsFormula<CompoundState> newEnvValue =
            environmentEntry.getValue().accept(replaceVisitor);
        result.putEnvironmentValueInternal(environmentEntry.getKey(), newEnvValue);
      }
    }
    result.putEnvironmentValueInternal(varName, newValue);
    result.remainingEvaluations.putAll(this.remainingEvaluations);
    return result;
  }

  private InvariantsFormula<CompoundState> getEnvironmentValue(String varName) {
    InvariantsFormula<CompoundState> environmentValue = this.environment.get(varName);
    if (environmentValue == null) {
      return TOP;
    }
    return environmentValue;
  }

  public FormulaEvaluationVisitor<CompoundState> getFormulaResolver(String edge) {
    if (mayEvaluate(edge)) {
      decreaseRemainingEvaluations(edge);
      return getFormulaResolver();
    }
    if (this.abstractionVisitor != null) {
      return this.abstractionVisitor;
    }
    return this.abstractionVisitor = new FormulaAbstractionVisitor(this.environment);
  }

  private FormulaEvaluationVisitor<CompoundState> getFormulaResolver() {
    if (this.evaluationVisitor != null) {
      return this.evaluationVisitor;
    }
    return this.evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(this.environment);
  }

  private boolean assumeInternal(InvariantsFormula<CompoundState> pAssumption,
      FormulaEvaluationVisitor<CompoundState> evaluationVisitor) {
    InvariantsFormula<CompoundState> assumption = pAssumption.accept(PartialEvaluator.INSTANCE);
    if (assumption.equals(TOP)) {
      return true;
    }
    if (assumption.equals(BOTTOM) || this.assumptions.contains(InvariantsFormulaManager.INSTANCE.logicalNot(assumption).accept(PartialEvaluator.INSTANCE))) {
      return false;
    }
    CompoundState invariantEvaluation = assumption.accept(evaluationVisitor);
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (invariantEvaluation.isDefinitelyFalse() || invariantEvaluation.equals(BOTTOM)) {
      return false;
    }
    // If the invariant evaluates to true, it adds no value
    if (invariantEvaluation.isDefinitelyTrue()) {
      return true;
    }
    this.assumptions.add(assumption);
    return true;
  }

  public InvariantsState assume(InvariantsFormula<CompoundState> pInvariant, String edge) {
    InvariantsFormula<CompoundState> invariant = pInvariant.accept(PartialEvaluator.INSTANCE);
    if (invariant instanceof Constant<?>) {
      CompoundState value = ((Constant<CompoundState>) invariant).getValue();
      // An invariant evaluating to false represents an unreachable state; it can never be fulfilled
      if (value.isDefinitelyFalse()) {
        return null;
      }
      // An invariant representing nothing more than "true" or "maybe true" adds no information
      return this;
    }
    InvariantsState result = copy(this);
    if (result != null) {
      if (!result.assumeInternal(invariant, getFormulaResolver(edge))) {
        return null;
      }
    }
    return result;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManager pManager) {
    FormulaEvaluationVisitor<CompoundState> evaluationVisitor = getFormulaResolver();
    ToBooleanFormulaVisitor toBooleanFormulaVisitor =
        new ToBooleanFormulaVisitor(pManager, evaluationVisitor);
    ToRationalFormulaVisitor toRationalFormulaVisitor =
        toBooleanFormulaVisitor.getToRationalFormulaVisitor();
    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    RationalFormulaManager nfmgr = pManager.getRationalFormulaManager();
    BooleanFormula result = bfmgr.makeBoolean(true);

    for (Entry<String, InvariantsFormula<CompoundState>> entry
        : environment.entrySet()) {
      RationalFormula var = nfmgr.makeVariable(entry.getKey());
      InvariantsFormula<CompoundState> value = entry.getValue();
      RationalFormula valueFormula = value.accept(toRationalFormulaVisitor);
      if (valueFormula != null ) {
        result = bfmgr.and(result, nfmgr.equal(var, valueFormula));
      }
      CompoundState compoundState = value.accept(evaluationVisitor);
      for (SimpleInterval interval : compoundState.getIntervals()) {
        if (interval.isSingleton()) {
          RationalFormula bound = nfmgr.makeNumber(interval.getLowerBound().longValue());
          BooleanFormula f = nfmgr.equal(var, bound);
          result = bfmgr.and(result, f);
        } else {
          if (interval.hasLowerBound()) {
            RationalFormula bound = nfmgr.makeNumber(interval.getLowerBound().longValue());
            BooleanFormula f = nfmgr.greaterOrEquals(var, bound);
            result = bfmgr.and(result, f);
          }
          if (interval.hasUpperBound()) {
            RationalFormula bound = nfmgr.makeNumber(interval.getUpperBound().longValue());
            BooleanFormula f = nfmgr.lessOrEquals(var, bound);
            result = bfmgr.and(result, f);
          }
        }
      }
    }
    for (InvariantsFormula<CompoundState> invariant : this.assumptions) {
      BooleanFormula invariantFormula = invariant.accept(toBooleanFormulaVisitor);
      if (invariantFormula != null) {
        result = bfmgr.and(result, invariantFormula);
      }
    }
    return result;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof InvariantsState)) {
      return false;
    }
    InvariantsState other = (InvariantsState) pObj;
    return environment.equals(other.environment)
        && assumptions.equals(other.assumptions);
  }

  @Override
  public int hashCode() {
    return environment.hashCode();
  }

  @Override
  public String toString() {
    return String.format("Environment: %s; Invariants: %s",
        Joiner.on(", ").withKeyValueSeparator("=").join(environment),
        Joiner.on(", ").join(assumptions));
  }

  public int getEvaluationThreshold() {
    return this.evaluationThreshold;
  }

  private int getRemainingEvaluations(String edge) {
    Integer remainingEvaluations = this.remainingEvaluations.get(edge);
    return remainingEvaluations == null ? this.evaluationThreshold : remainingEvaluations;
  }

  public boolean mayEvaluate(String edge) {
    return getRemainingEvaluations(edge) > 0;
  }

  public void decreaseRemainingEvaluations(String edge) {
    int remainingEvaluations = getRemainingEvaluations(edge);
    if (remainingEvaluations > 0) {
      this.remainingEvaluations.put(edge, remainingEvaluations - 1);
    }
  }

  public Set<InvariantsFormula<CompoundState>> getInvariants() {
    return Collections.unmodifiableSet(this.assumptions);
  }

}
