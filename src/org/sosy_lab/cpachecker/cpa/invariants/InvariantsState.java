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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.DefinitelyStillHoldsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExposeVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaAbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PushAssumptionToEnvironmentVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ReplaceVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToRationalFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.WeakeningVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;

import com.google.common.base.Joiner;

public class InvariantsState implements AbstractState, FormulaReportingState {

  private static final SplitConjunctionsVisitor<CompoundState> SPLIT_CONJUNCTIONS_VISITOR = new SplitConjunctionsVisitor<>();

  private static final FormulaEvaluationVisitor<CompoundState> EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

  private static final FormulaEvaluationVisitor<CompoundState> ABSTRACTION_VISITOR = new FormulaAbstractionVisitor();

  private static final PartialEvaluator PARTIAL_EVALUATION_VISITOR = new PartialEvaluator(EVALUATION_VISITOR);

  private static final PartialEvaluator PARTIAL_ABSTRACTION_VISITOR = new PartialEvaluator(ABSTRACTION_VISITOR);

  private static final WeakeningVisitor<CompoundState> WEAKENING_VISITOR = new WeakeningVisitor<>();

  private static final InvariantsFormula<CompoundState> TOP = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.top());

  private static final InvariantsFormula<CompoundState> BOTTOM = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.bottom());

  private final int evaluationThreshold;

  private final Map<String, InvariantsFormula<CompoundState>> environment;

  private final Set<InvariantsFormula<CompoundState>> assumptions;

  private final Map<String, Integer> remainingEvaluations;

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

  public InvariantsState assign(String pVarName, InvariantsFormula<CompoundState> pValue, String pEdge) {
    /*
     * A variable is newly assigned, so the appearances of this variable
     * in any previously collected invariants (including its new new value)
     * have to be resolved with the variable's previous value.
     */
    InvariantsFormulaManager fmgr = InvariantsFormulaManager.INSTANCE;
    InvariantsFormula<CompoundState> variable = fmgr.asVariable(pVarName);
    ReplaceVisitor<CompoundState> replaceVisitor;
    InvariantsFormula<CompoundState> previousValue = getEnvironmentValue(pVarName);
    InvariantsState result = new InvariantsState(this.evaluationThreshold);
    replaceVisitor = new ReplaceVisitor<>(variable, previousValue);
    InvariantsFormula<CompoundState> newSubstitutedValue = pValue.accept(replaceVisitor).accept(getPartialEvaluator(pEdge), getEnvironment());

    for (InvariantsFormula<CompoundState> assumption : this.assumptions) {
      // Try to add the invariant; if it turns out that it is false, the state is bottom
      if (!updateInvariant(result, assumption, replaceVisitor, pValue, pVarName)) {
        return null;
      }
    }
    for (Map.Entry<String, InvariantsFormula<CompoundState>> environmentEntry : this.environment.entrySet()) {
      if (!environmentEntry.getKey().equals(pVarName)) {
        InvariantsFormula<CompoundState> newEnvValue =
            environmentEntry.getValue().accept(replaceVisitor);
        result.putEnvironmentValueInternal(environmentEntry.getKey(), newEnvValue);
      }
    }
    result.putEnvironmentValueInternal(pVarName, newSubstitutedValue);
    result.remainingEvaluations.putAll(this.remainingEvaluations);
    return result;
  }

  private boolean updateInvariant(InvariantsState pTargetState, InvariantsFormula<CompoundState> pOldAssumption, ReplaceVisitor<CompoundState> pReplaceVisitor, InvariantsFormula<CompoundState> pNewValue, String pVarName) {
    FormulaEvaluationVisitor<CompoundState> resolver = getFormulaResolver();
    // Try to add the invariant; if it turns out that it is false, the state is bottom
    if (!pTargetState.assumeInternal(pOldAssumption.accept(pReplaceVisitor),
        resolver)) {
      return false;
    }
    ExposeVarVisitor<CompoundState> exposeVarVisitor = new ExposeVarVisitor<>(pVarName, getEnvironment());
    InvariantsFormula<CompoundState> oldAssumption = pOldAssumption.accept(exposeVarVisitor);
    InvariantsFormula<CompoundState> newValue = pNewValue.accept(exposeVarVisitor);
    ContainsVarVisitor<CompoundState> cvv = new ContainsVarVisitor<>(pVarName);
    if (oldAssumption.accept(cvv) && newValue.accept(cvv)) {
      DefinitelyStillHoldsVisitor dshv = new DefinitelyStillHoldsVisitor(newValue, resolver, cvv);
      Map<String, InvariantsFormula<CompoundState>> whatIfEnvironment = new HashMap<>(getEnvironment());
      InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
      whatIfEnvironment.put(pVarName, ifm.asConstant(newValue.accept(getFormulaResolver(), getEnvironment())));
      if (oldAssumption.accept(dshv, whatIfEnvironment)) {
        return pTargetState.assumeInternal(pOldAssumption, resolver);
      }
      InvariantsFormula<CompoundState> weakened = oldAssumption.accept(WEAKENING_VISITOR);
      if (!weakened.equals(oldAssumption) && weakened.accept(dshv, whatIfEnvironment)) {
        return pTargetState.assumeInternal(weakened, resolver);
      }
    }
    return true;
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
    return ABSTRACTION_VISITOR;
  }

  private FormulaEvaluationVisitor<CompoundState> getFormulaResolver() {
    return EVALUATION_VISITOR;
  }

  private PartialEvaluator getPartialEvaluator(String pEdge) {
    if (mayEvaluate(pEdge)) {
      decreaseRemainingEvaluations(pEdge);
      return getPartialEvaluator();
    }
    return PARTIAL_ABSTRACTION_VISITOR;
  }

  private PartialEvaluator getPartialEvaluator() {
    return PARTIAL_EVALUATION_VISITOR;
  }

  private void putEnvironmentValuesInternal(Map<String, InvariantsFormula<CompoundState>> pEnvironment) {
    for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : pEnvironment.entrySet()) {
      putEnvironmentValueInternal(entry.getKey(), entry.getValue().accept(getPartialEvaluator(), getEnvironment()));
    }
  }

  private void putEnvironmentValueInternal(String pKey, InvariantsFormula<CompoundState> pValue) {
    InvariantsFormula<CompoundState> value = pValue.accept(getPartialEvaluator(), getEnvironment());
    if (value.equals(TOP)) {
      this.environment.remove(pKey);
    } else {
      this.environment.put(pKey, pValue.accept(getPartialEvaluator(), getEnvironment()));
    }
  }

  private boolean assumeInternal(Collection<InvariantsFormula<CompoundState>> pAssumptions,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    for (InvariantsFormula<CompoundState> invariant : pAssumptions) {
      if (!assumeInternal(invariant, pEvaluationVisitor)) {
        return false;
      }
    }
    return true;
  }

  private boolean assumeInternal(InvariantsFormula<CompoundState> pAssumption,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> assumption = pAssumption.accept(getPartialEvaluator(), getEnvironment());
    // If there are multiple assumptions combined with &&, split them up
    List<InvariantsFormula<CompoundState>> assumptionParts = assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) {
      return assumeInternal(assumptionParts, pEvaluationVisitor);
    }
    if (assumption.equals(TOP)) {
      return true;
    }
    if (assumption.equals(BOTTOM) || this.assumptions.contains(InvariantsFormulaManager.INSTANCE.logicalNot(assumption).accept(getPartialEvaluator(), getEnvironment()))) {
      return false;
    }
    CompoundState invariantEvaluation = assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (invariantEvaluation.isDefinitelyFalse() || invariantEvaluation.equals(BOTTOM)) {
      return false;
    }
    // If the invariant evaluates to true, it adds no value
    if (invariantEvaluation.isDefinitelyTrue()) {
      return true;
    }
    PushAssumptionToEnvironmentVisitor patev =
        new PushAssumptionToEnvironmentVisitor(pEvaluationVisitor, this.environment);
    if (!assumption.accept(patev, CompoundState.logicalTrue())) {
      return false;
    }

    this.assumptions.add(assumption);
    return true;
  }

  public InvariantsState assume(InvariantsFormula<CompoundState> pInvariant, String pEdge) {
    PartialEvaluator partialEvaluator = getPartialEvaluator(pEdge);
    InvariantsFormula<CompoundState> invariant = pInvariant.accept(partialEvaluator, getEnvironment());
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
      if (!result.assumeInternal(invariant, partialEvaluator.getEvaluationVisitor())) {
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
      RationalFormula valueFormula = value.accept(toRationalFormulaVisitor, getEnvironment());
      if (valueFormula != null ) {
        result = bfmgr.and(result, nfmgr.equal(var, valueFormula));
      }
      CompoundState compoundState = value.accept(evaluationVisitor, getEnvironment());
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
      BooleanFormula invariantFormula = invariant.accept(toBooleanFormulaVisitor, getEnvironment());
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

  public Map<String, Integer> getRemainingEvaluations() {
    return Collections.unmodifiableMap(remainingEvaluations);
  }

  public Map<? extends String, ? extends InvariantsFormula<CompoundState>> getEnvironment() {
    return Collections.unmodifiableMap(environment);
  }

}
