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

import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExposeVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaAbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.GuessAssumptionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PushAssumptionToEnvironmentVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ReplaceVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Joiner;

public class InvariantsState implements AbstractState, FormulaReportingState {

  private static final SplitConjunctionsVisitor<CompoundState> SPLIT_CONJUNCTIONS_VISITOR = new SplitConjunctionsVisitor<>();

  private static final FormulaEvaluationVisitor<CompoundState> EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

  private static final FormulaEvaluationVisitor<CompoundState> ABSTRACTION_VISITOR = new FormulaAbstractionVisitor();

  private static final InvariantsFormula<CompoundState> TOP = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.top());

  private static final InvariantsFormula<CompoundState> BOTTOM = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.bottom());

  private final int evaluationThreshold;

  private final Map<String, InvariantsFormula<CompoundState>> environment;

  private final Set<InvariantsFormula<CompoundState>> assumptions;

  private final Set<InvariantsFormula<CompoundState>> candidateAssumptions;

  private final Map<String, Integer> remainingEvaluations;

  private final boolean useBitvectors;

  public InvariantsState(int pEvaluationThreshold,
      boolean pUseBitvectors) {
    this.evaluationThreshold = pEvaluationThreshold;
    this.remainingEvaluations = new HashMap<>();
    this.assumptions = new HashSet<>();
    this.environment = new HashMap<>();
    this.candidateAssumptions = new HashSet<>();
    this.useBitvectors = pUseBitvectors;
  }

  public static InvariantsState copy(InvariantsState pToCopy) {
    return from(pToCopy.remainingEvaluations, pToCopy.evaluationThreshold,
        pToCopy.assumptions, pToCopy.environment, pToCopy.candidateAssumptions,
        pToCopy.useBitvectors);
  }

  public static InvariantsState from(Map<String, Integer> pRemainingEvaluations,
      int pEvaluationThreshold,
      Collection<InvariantsFormula<CompoundState>> pInvariants,
      Map<String, InvariantsFormula<CompoundState>> pEnvironment,
      Set<InvariantsFormula<CompoundState>> pCandidateAssumptions,
      boolean pUseBitvectors) {
    InvariantsState result = new InvariantsState(pEvaluationThreshold, pUseBitvectors);
    if (!result.assumeInternal(pInvariants, result.getFormulaResolver())) {
      return null;
    }
    result.remainingEvaluations.putAll(pRemainingEvaluations);
    result.putEnvironmentValuesInternal(pEnvironment);
    result.candidateAssumptions.addAll(pCandidateAssumptions);
    return result;
  }

  private static boolean isPrevVarName(String pVarName) {
    return pVarName.startsWith("__prev_");
  }

  private static final boolean useOptimization = Boolean.parseBoolean("true");

  private InvariantsFormula<CompoundState> exposeRenamed(InvariantsFormula<CompoundState> pFormula) {
    InvariantsFormula<CompoundState> formula = pFormula;
    ExposeVarVisitor<CompoundState> exposer = new ExposeVarVisitor<>(getEnvironment());
    for (String varName : this.environment.keySet()) {
      if (isPrevVarName(varName)) {
        formula = formula.accept(exposer, varName);
      }
    }
    return formula;
  }

  private InvariantsFormula<CompoundState> resolveRenamed(InvariantsFormula<CompoundState> pFormula) {
    InvariantsFormula<CompoundState> formula = pFormula;
    CollectVarsVisitor<CompoundState> varCollector = new CollectVarsVisitor<>();
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    for (String varName : formula.accept(varCollector)) {
      if (isPrevVarName(varName)) {
        InvariantsFormula<CompoundState> renamedVariable = ifm.asVariable(varName);
        InvariantsFormula<CompoundState> renamedVariableValue = getEnvironmentValue(varName);
        ReplaceVisitor<CompoundState> renamedVarResolver = new ReplaceVisitor<>(renamedVariable, renamedVariableValue);
        formula = formula.accept(renamedVarResolver);
      }
    }
    return formula;
  }

  private static InvariantsFormula<CompoundState> renameVariables(InvariantsFormula<CompoundState> pFormula) {
    InvariantsFormula<CompoundState> formula = pFormula;
    CollectVarsVisitor<CompoundState> varCollector = new CollectVarsVisitor<>();
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    for (String varName : formula.accept(varCollector)) {
      assert !isPrevVarName(varName);
      InvariantsFormula<CompoundState> variable = ifm.asVariable(varName);
      InvariantsFormula<CompoundState> renamedVariable = ifm.asVariable(renameVariable(varName));
      ReplaceVisitor<CompoundState> renamedVarResolver = new ReplaceVisitor<>(variable, renamedVariable);
      formula = formula.accept(renamedVarResolver);
    }
    return formula;
  }

  private static String renameVariable(String varName) {
    return "__prev_" + varName;
  }

  public InvariantsState assign(String pVarName, InvariantsFormula<CompoundState> pValue, String pEdge) {

    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    final InvariantsState result;
    if (useOptimization && !mayEvaluate(pEdge)) {
      /*
       * No more evaluations allowed!
       *
       * Create a completely new state with the old assumptions, but rename all
       * normal variables and initialize the environment with a mapping of the
       * variable names to the renamed variables.
       *
       * This deliberately omits almost all previous environment information but
       * allows for recording exactly how a loop modifies the environment.
       */
      result = new InvariantsState(evaluationThreshold, useBitvectors);
      // Transfer environment and record detailed values in a shadow environment
      Map<String, InvariantsFormula<CompoundState>> shadowEnvironment = new HashMap<>();
      for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : this.environment.entrySet()) {
        String varName = entry.getKey();
        // Transfer only original variables, not remained ones
        if (!isPrevVarName(varName)) {
          InvariantsFormula<CompoundState> oldValue = entry.getValue();
          // Expose all renamed variables
          oldValue = exposeRenamed(oldValue);
          // Resolve all occurrences of renamed variables in the value
          oldValue = resolveRenamed(oldValue);
          // Rename all original variables in the value
          oldValue = renameVariables(oldValue);
          // Put the resolved value as the value of the renamed variable into the environment
          String renamedVariableName = renameVariable(varName);
          final FormulaEvaluationVisitor<CompoundState> evaluator = getFormulaResolver();
          oldValue = oldValue.accept(PartialEvaluator.INSTANCE, evaluator);
          result.environment.put(renamedVariableName, TOP);
          shadowEnvironment.put(renamedVariableName, oldValue);
          // Add a reference of the new value to the old value
          InvariantsFormula<CompoundState> renamedVariable = ifm.asVariable(renamedVariableName);
          result.environment.put(varName, renamedVariable);
        }
      }
      // Handle the actual assignment
      InvariantsFormula<CompoundState> newValue = pValue;
      // Resolve all occurrences of renamed variables in the value
      newValue = resolveRenamed(newValue);
      // Rename all original variables in the value
      newValue = renameVariables(newValue);
      /*
       * Put the assignment value (that now refers to only renamed variables of
       * the new environment) as the value of the original variable it is
       * assigned to.
       */
      newValue = newValue.accept(PartialEvaluator.INSTANCE, getFormulaResolver(pEdge));
      result.environment.put(pVarName, newValue);

      // Refine the shadow environment by the current result environment
      for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : result.environment.entrySet()) {
        String key = entry.getKey();
        InvariantsFormula<CompoundState> value = entry.getValue();
        if (!shadowEnvironment.containsKey(key)) {
          shadowEnvironment.put(key, value);
        }
      }

      // Transfer assumptions
      for (InvariantsFormula<CompoundState> assumption : this.assumptions) {
        InvariantsFormula<CompoundState> modifiedAssumption = assumption;
        modifiedAssumption = resolveRenamed(modifiedAssumption);
        modifiedAssumption = renameVariables(modifiedAssumption);
        modifiedAssumption = modifiedAssumption.accept(PartialEvaluator.INSTANCE, getFormulaResolver());
        assert result.assumeInternal(modifiedAssumption, getFormulaResolver());
        for (InvariantsFormula<CompoundState> guessedAssumption : modifiedAssumption.accept(new GuessAssumptionVisitor())) {
          CompoundState evaluated = guessedAssumption.accept(getFormulaResolver(), shadowEnvironment);
          if (evaluated.isDefinitelyTrue()) {
            assert result.assumeInternal(guessedAssumption, getFormulaResolver());
          }
        }
        for (InvariantsFormula<CompoundState> guessedAssumption : assumption.accept(new GuessAssumptionVisitor())) {
          CompoundState evaluated = guessedAssumption.accept(getFormulaResolver(), shadowEnvironment);
          if (evaluated.isDefinitelyTrue()) {
            assert result.assumeInternal(guessedAssumption, getFormulaResolver());
          }
        }
      }
      for (InvariantsFormula<CompoundState> candidate : this.candidateAssumptions) {
        for (InvariantsFormula<CompoundState> guessedAssumption : candidate.accept(new GuessAssumptionVisitor())) {
          CompoundState evaluated = guessedAssumption.accept(getFormulaResolver(), shadowEnvironment);
          if (evaluated.isDefinitelyTrue()) {
            assert result.assumeInternal(guessedAssumption, getFormulaResolver());
          }
        }
      }

      // Forbid further exact evaluations of this edge
      result.remainingEvaluations.put(pEdge, 0);
    } else {
      /*
       * A variable is newly assigned, so the appearances of this variable
       * in any previously collected invariants (including its new new value)
       * have to be resolved with the variable's previous value.
       */
      InvariantsFormula<CompoundState> variable = ifm.asVariable(pVarName);
      ReplaceVisitor<CompoundState> replaceVisitor;
      InvariantsFormula<CompoundState> previousValue = getEnvironmentValue(pVarName);
      result = new InvariantsState(evaluationThreshold, useBitvectors);
      replaceVisitor = new ReplaceVisitor<>(variable, previousValue);
      InvariantsFormula<CompoundState> newSubstitutedValue = pValue.accept(replaceVisitor).accept(PartialEvaluator.INSTANCE, getFormulaResolver(pEdge));

      for (Map.Entry<String, InvariantsFormula<CompoundState>> environmentEntry : this.environment.entrySet()) {
        if (!environmentEntry.getKey().equals(pVarName)) {
          InvariantsFormula<CompoundState> newEnvValue =
              environmentEntry.getValue().accept(replaceVisitor);
          result.putEnvironmentValueInternal(environmentEntry.getKey(), newEnvValue);
        }
      }
      result.putEnvironmentValueInternal(pVarName, newSubstitutedValue);

      // Try to add the invariant; if it turns out that it is false, the state is bottom
      if (!updateInvariants(result, replaceVisitor, pValue, pVarName)) {
        return null;
      }

    }

    result.remainingEvaluations.putAll(this.remainingEvaluations);
    result.removeUnusedRenamedVariables();
    return result;
  }

  private void removeUnusedRenamedVariables() {
    // Collect all used variables used in the environment
    Set<String> usedVars = new HashSet<>();
    CollectVarsVisitor<CompoundState> cvv = new CollectVarsVisitor<>();
    for (Map.Entry<String, InvariantsFormula<CompoundState>> envEntry : environment.entrySet()) {
      String var = envEntry.getKey();
      if (!isPrevVarName(var)) {
        InvariantsFormula<CompoundState> value = envEntry.getValue();
        usedVars.addAll(value.accept(cvv));
        usedVars.add(var);
      }
    }
    // Fix-point iteration over assumptions
    {
      int size = -1;
      while (size < usedVars.size()) {
        size = usedVars.size();
        for (InvariantsFormula<CompoundState> assumption : this.assumptions) {
          Set<String> assumptionVars = assumption.accept(cvv);
          for (String assumptionVar : assumptionVars) {
            if (!isPrevVarName(assumptionVar) || usedVars.contains(assumptionVar)) {
              usedVars.addAll(assumptionVars);
              break;
            }
          }
        }
      }
    }
    // Collect all renamed variables that are unused
    Collection<String> varsToRemove = new ArrayList<>();
    for (String var : environment.keySet()) {
      if (isPrevVarName(var) && !usedVars.contains(var)) {
        varsToRemove.add(var);
      }
    }
    // Remove the variables from the environment
    for (String var : varsToRemove) {
      environment.remove(var);
    }
    Collection<InvariantsFormula<CompoundState>> assumptionsToRemove = new ArrayList<>();
    ContainsVarVisitor<CompoundState> containsVarVisitor = new ContainsVarVisitor<>();
    for (InvariantsFormula<CompoundState> assumption : this.assumptions) {
      for (String var : varsToRemove) {
        if (assumption.accept(containsVarVisitor, var)) {
          assumptionsToRemove.add(assumption);
          break;
        }
      }
    }
    this.assumptions.removeAll(assumptionsToRemove);
  }

  public Set<InvariantsFormula<CompoundState>> getEnvironmentAsAssumptions() {
    Set<InvariantsFormula<CompoundState>> environmentalAssumptions = new HashSet<>();
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : this.environment.entrySet()) {
      InvariantsFormula<CompoundState> variable = ifm.asVariable(entry.getKey());
      InvariantsFormula<CompoundState> equation = ifm.equal(variable, entry.getValue());
      environmentalAssumptions.add(equation);
    }
    return environmentalAssumptions;
  }

  private boolean updateInvariants(InvariantsState pTargetState, ReplaceVisitor<CompoundState> pReplaceVisitor, InvariantsFormula<CompoundState> pNewValue, String pVarName) {
    FormulaEvaluationVisitor<CompoundState> resolver = getFormulaResolver();
    for (InvariantsFormula<CompoundState> oldAssumption : this.assumptions) {
      // Try to add the invariant; if it turns out that it is false, the state is bottom
      if (!pTargetState.assumeInternal(oldAssumption.accept(pReplaceVisitor),
          resolver)) {
        return false;
      }

    }
    Map<String, InvariantsFormula<CompoundState>> whatIfEnvironment = new HashMap<>(pTargetState.getEnvironment());
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    whatIfEnvironment.put(pVarName, ifm.asConstant(pNewValue.accept(resolver, getEnvironment())));
    for (InvariantsFormula<CompoundState> candidate : this.candidateAssumptions) {
      if (candidate.accept(resolver, whatIfEnvironment).isDefinitelyTrue()) {
        assert pTargetState.assumeInternal(candidate, resolver);
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

  private void putEnvironmentValuesInternal(Map<String, InvariantsFormula<CompoundState>> pEnvironment) {
    for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : pEnvironment.entrySet()) {
      putEnvironmentValueInternal(entry.getKey(), entry.getValue().accept(PartialEvaluator.INSTANCE, getFormulaResolver()));
    }
  }

  private void putEnvironmentValueInternal(String pKey, InvariantsFormula<CompoundState> pValue) {
    FormulaEvaluationVisitor<CompoundState> evaluator = getFormulaResolver();
    InvariantsFormula<CompoundState> value = pValue.accept(PartialEvaluator.INSTANCE, evaluator);
    if (value.equals(TOP)) {
      this.environment.remove(pKey);
    } else {
      this.environment.put(pKey, pValue.accept(PartialEvaluator.INSTANCE, evaluator));
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

  /**
   * Adds the given assumption as a candidate assumption. Candidate assumptions
   * are not part of the actual state but are rather hints for assumptions that
   * could be guessed at a later point.
   *
   * @param pAssumption the assumption to add to the candidates.
   */
  private void assumeCandidate(InvariantsFormula<CompoundState> pAssumption) {
    InvariantsFormula<CompoundState> assumption = pAssumption.accept(PartialEvaluator.INSTANCE, getFormulaResolver());
    if (!this.candidateAssumptions.contains(assumption)
        && (assumption.accept(new CollectVarsVisitor<CompoundState>()).size() > 0)) {
      this.candidateAssumptions.addAll(assumption.accept(new GuessAssumptionVisitor()));
    }
  }

  private boolean assumeInternal(InvariantsFormula<CompoundState> pAssumption,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> assumption = pAssumption.accept(PartialEvaluator.INSTANCE, pEvaluationVisitor);
    // If there are multiple assumptions combined with &&, split them up
    List<InvariantsFormula<CompoundState>> assumptionParts = assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) {
      return assumeInternal(assumptionParts, pEvaluationVisitor);
    }
    // If the assumption is top, it adds no value
    if (assumption.equals(TOP)) {
      return true;
    }
    assumeCandidate(assumption);

    // If the assumption is an obvious contradiction, it cannot be validly
    // assumed
    if (assumption.equals(BOTTOM)
        || this.assumptions.contains(
            InvariantsFormulaManager.INSTANCE.logicalNot(assumption).accept(
                PartialEvaluator.INSTANCE, pEvaluationVisitor))) {
      return false;
    }

    CompoundState invariantEvaluation = assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (invariantEvaluation.isDefinitelyFalse() || invariantEvaluation.isBottom()) {
      return false;
    }
    // If the invariant evaluates to true, it adds no value for now, but it
    // can later be used as a suggestion - unless it is a constant
    if (invariantEvaluation.isDefinitelyTrue() && assumption instanceof Constant<?>) {
      return true;
    }

    PushAssumptionToEnvironmentVisitor patev =
        new PushAssumptionToEnvironmentVisitor(pEvaluationVisitor, this.environment);
    if (!assumption.accept(patev, CompoundState.logicalTrue())) {
      assert !invariantEvaluation.isDefinitelyTrue();
      return false;
    }
    // Check all assumptions once more after the environment changed
    if (isDefinitelyFalse(assumption, pEvaluationVisitor)) {
      return false;
    }
    for (InvariantsFormula<CompoundState> oldAssumption : this.assumptions) {
      if (isDefinitelyFalse(oldAssumption, pEvaluationVisitor)) {
        return false;
      }
    }

    this.assumptions.add(assumption);

    return true;
  }

  private boolean isDefinitelyFalse(InvariantsFormula<CompoundState> pAssumption, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    return pAssumption.accept(pEvaluationVisitor, getEnvironment()).isDefinitelyFalse();
  }

  public InvariantsState assume(InvariantsFormula<CompoundState> pInvariant, String pEdge) {
    FormulaEvaluationVisitor<CompoundState> evaluator = getFormulaResolver(pEdge);
    InvariantsFormula<CompoundState> invariant = pInvariant.accept(PartialEvaluator.INSTANCE, evaluator);
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
      if (!result.assumeInternal(invariant, evaluator)) {
        return null;
      }
      result.removeUnusedRenamedVariables();
    }
    return result;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManager pManager) {
    FormulaEvaluationVisitor<CompoundState> evaluationVisitor = getFormulaResolver();
    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeBoolean(true);
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    ToFormulaVisitor<CompoundState, BooleanFormula> toBooleanFormulaVisitor =
        ToBooleanFormulaVisitor.getVisitor(pManager, evaluationVisitor, useBitvectors);

    for (Entry<String, InvariantsFormula<CompoundState>> entry
        : environment.entrySet()) {
      InvariantsFormula<CompoundState> var = ifm.asVariable(entry.getKey());
      InvariantsFormula<CompoundState> value = entry.getValue();
      InvariantsFormula<CompoundState> equation = ifm.equal(var, value);
      BooleanFormula equationFormula = equation.accept(toBooleanFormulaVisitor, getEnvironment());
      if (equationFormula != null ) {
        result = bfmgr.and(result, equationFormula);
      }
      CompoundState compoundState = value.accept(evaluationVisitor, getEnvironment());
      InvariantsFormula<CompoundState> evaluatedValue = ifm.asConstant(compoundState);
      if (!evaluatedValue.equals(value)) {
        equation = ifm.equal(var, evaluatedValue);
        equationFormula = equation.accept(toBooleanFormulaVisitor, getEnvironment());
        if (equationFormula != null ) {
          result = bfmgr.and(result, equationFormula);
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
    }
    if (!(pObj instanceof InvariantsState)) {
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

  public Set<InvariantsFormula<CompoundState>> getAssumptions() {
    return Collections.unmodifiableSet(this.assumptions);
  }

  public Map<String, Integer> getRemainingEvaluations() {
    return Collections.unmodifiableMap(remainingEvaluations);
  }

  public Map<? extends String, ? extends InvariantsFormula<CompoundState>> getEnvironment() {
    return Collections.unmodifiableMap(environment);
  }

  public Collection<? extends InvariantsFormula<CompoundState>> getCandidateAssumptions() {
    return Collections.unmodifiableSet(candidateAssumptions);
  }

  public boolean getUseBitvectors() {
    return useBitvectors;
  }

}
