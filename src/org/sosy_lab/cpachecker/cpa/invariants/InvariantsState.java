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
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaAbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PushAssumptionToEnvironmentVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ReplaceVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.StateEqualsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/**
 * Instances of this class represent states in the light-weight invariants analysis.
 */
public class InvariantsState implements AbstractState, FormulaReportingState {

  /**
   * A visitor used to split boolean conjunction formulas up into the conjuncted clauses
   */
  private static final SplitConjunctionsVisitor<CompoundState> SPLIT_CONJUNCTIONS_VISITOR =
      new SplitConjunctionsVisitor<>();

  /**
   * A visitor used to evaluate formulas as exactly as possible.
   */
  private static final FormulaEvaluationVisitor<CompoundState> EVALUATION_VISITOR =
      new FormulaCompoundStateEvaluationVisitor();

  /**
   * A visitor that, like the formula evaluation visitor, is used to evaluate formulas, but far less exact to allow for convergence.
   */
  private static final FormulaEvaluationVisitor<CompoundState> ABSTRACTION_VISITOR = new FormulaAbstractionVisitor();

  /**
   * The constant formula representing TOP
   */
  private static final InvariantsFormula<CompoundState> TOP = InvariantsFormulaManager.INSTANCE
      .asConstant(CompoundState.top());

  /**
   * The constant formula representing BOTTOM
   */
  private static final InvariantsFormula<CompoundState> BOTTOM = InvariantsFormulaManager.INSTANCE
      .asConstant(CompoundState.bottom());

  /**
   * The number of times an edge is evaluated exactly before switching to the formula abstraction visitor.
   */
  private final int evaluationThreshold;

  /**
   * The environment currently known to the state.
   */
  private final Map<String, InvariantsFormula<CompoundState>> environment;

  /**
   * The currently made assumptions.
   */
  private final Set<InvariantsFormula<CompoundState>> assumptions;

  /**
   * A set of assumptions that might be helpful to make, but are not necessarily valid for the current state.
   */
  private final Set<InvariantsFormula<CompoundState>> candidateAssumptions;

  /**
   * The number of times each edge may still be evaluated exactly before switching to the formula abstraction visitor. An edge not recorded in this map is counted as having <code>this.evaluationThreshold</code> exact evaluations left.
   */
  private final Map<String, Integer> remainingEvaluations;

  /**
   * A flag indicating whether or not to use bit vectors for representing states.
   */
  private final boolean useBitvectors;

  private final IdentityMap identityMap;

  /**
   * Creates a new pristine invariants state with just a value for the evaluation threshold and the flag indicating whether or not to use bit vectors for representing states.
   *
   * @param pEvaluationThreshold the number of times an edge is evaluated exactly before switching to the formula abstraction visitor.
   * @param pUseBitvectors the flag indicating whether or not to use bit vectors for representing states.
   */
  public InvariantsState(int pEvaluationThreshold,
      boolean pUseBitvectors) {
    this(pEvaluationThreshold, pUseBitvectors, new IdentityMap());
  }

  /**
   * Creates a new pristine invariants state with just a value for the evaluation threshold and the flag indicating whether or not to use bit vectors for representing states.
   *
   * @param pEvaluationThreshold the number of times an edge is evaluated exactly before switching to the formula abstraction visitor.
   * @param pUseBitvectors the flag indicating whether or not to use bit vectors for representing states.
   * @param pIdentityMap a map used to store state identities, used to avoid keeping multiple equal states.
   */
  public InvariantsState(int pEvaluationThreshold,
      boolean pUseBitvectors, IdentityMap pIdentityMap) {
    this.evaluationThreshold = pEvaluationThreshold;
    this.remainingEvaluations = new HashMap<>();
    this.assumptions = new HashSet<>();
    this.environment = new HashMap<>();
    this.candidateAssumptions = new HashSet<>();
    this.useBitvectors = pUseBitvectors;
    this.identityMap = pIdentityMap;
  }

  /**
   * Creates a copy of the given state.
   *
   * @param pToCopy the state to copy.
   * @return a copy of the given state.
   */
  public static InvariantsState copy(InvariantsState pToCopy) {
    return from(pToCopy.remainingEvaluations, pToCopy.evaluationThreshold,
        pToCopy.assumptions, pToCopy.environment, pToCopy.candidateAssumptions,
        pToCopy.useBitvectors, pToCopy.identityMap);
  }

  /**
   * Creates a new state from the given state properties.
   *
   * @param pRemainingEvaluations the remaining exact evaluations for each edge.
   * @param pEvaluationThreshold the maximum number of exact evaluations for each edge.
   * @param pAssumptions the current assumptions.
   * @param pEnvironment the current environment.
   * @param pCandidateAssumptions a set of assumptions that might be helpful to be made, but need not hold for the current state.
   * @param pUseBitvectors a flag indicating whether or not to use bit vectors to represent states.
   * @param pIdentityMap a map used to store state identities, used to avoid keeping multiple equal states.
   * @return a new state from the given state properties.
   */
  public static InvariantsState from(Map<String, Integer> pRemainingEvaluations,
      int pEvaluationThreshold,
      Collection<InvariantsFormula<CompoundState>> pAssumptions,
      Map<String, InvariantsFormula<CompoundState>> pEnvironment,
      Set<InvariantsFormula<CompoundState>> pCandidateAssumptions,
      boolean pUseBitvectors,
      IdentityMap pIdentityMap) {
    InvariantsState result = new InvariantsState(pEvaluationThreshold, pUseBitvectors, pIdentityMap);
    if (!result.assumeInternal(pAssumptions, result.getFormulaResolver())) { return null; }
    result.remainingEvaluations.putAll(pRemainingEvaluations);
    result.putEnvironmentValuesInternal(pEnvironment);
    result.candidateAssumptions.addAll(pCandidateAssumptions);
    return pIdentityMap.get(result);
  }

  /**
   * Checks whether or not the given variable name is a renamed variable name.
   *
   * @param pVarName the variable name to check.
   * @return <code>true</code> if the given variable name is a renamed variable name, <code>false</code> otherwise.
   */
  private static boolean isPrevVarName(String pVarName) {
    return pVarName.startsWith("__prev_");
  }

  /**
   * Renames the given variable name.
   *
   * @param varName the variable name to rename.
   * @return the renamed variable name.
   */
  private static String renameVariable(String varName) {
    return "__prev_" + varName;
  }

  /**
   * Creates a new state representing the given assignment applied to the current state.
   *
   * @param pVarName the name of the variable being assigned.
   * @param pValue the new value of the variable.
   * @param pEdge the edge containing the assignment.
   * @return a new state representing the given assignment applied to the current state.
   */
  public InvariantsState assign(String pVarName, InvariantsFormula<CompoundState> pValue, String pEdge) {
    Preconditions.checkNotNull(pValue);

    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    Variable<CompoundState> variable = ifm.asVariable(pVarName);

    // Optimization: If the value being assigned is equivalent to the value already stored, do nothing
    if (variable.accept(new StateEqualsVisitor(getFormulaResolver(), this.environment), pValue)) {
      return this;
    }

    final InvariantsState result = new InvariantsState(evaluationThreshold, useBitvectors, identityMap);
    if (!mayEvaluate(pEdge)) {
      /*
       * No more evaluations allowed!
       *
       * Create a completely new state with the old assumptions and variables, but
       * rename the assigned variables.
       */
      FormulaEvaluationVisitor<CompoundState> evaluationVisitor = getFormulaResolver(pEdge);
      String renamedVariableName = renameVariable(pVarName);
      Variable<CompoundState> renamedVariable = ifm.asVariable(renamedVariableName);
      ReplaceVisitor<CompoundState> renamer = new ReplaceVisitor<>(variable, renamedVariable);
      InvariantsFormula<CompoundState> renamedValue = getEnvironmentValue(renamedVariableName);
      ReplaceVisitor<CompoundState> renamedEvaluater = new ReplaceVisitor<>(renamedVariable, renamedValue);
      InvariantsFormula<CompoundState> oldValue = getEnvironmentValue(pVarName);
      for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : this.environment.entrySet()) {
        String envVarName = entry.getKey();
        if (!envVarName.equals(renamedVariableName)) {
          InvariantsFormula<CompoundState> envVarValue = entry.getValue();
          // Evaluate the old renamed variable and rename the (as yet unrenamed) actual variable
          envVarValue = envVarValue.accept(renamedEvaluater).accept(renamer).accept(PartialEvaluator.INSTANCE, evaluationVisitor);
          result.putEnvironmentValueInternal(pVarName, envVarValue);
        }
      }
      if (oldValue != null) {
        result.putEnvironmentValueInternal(pVarName, oldValue.accept(renamer).accept(PartialEvaluator.INSTANCE, evaluationVisitor));
      }

      for (InvariantsFormula<CompoundState> assumption : this.assumptions) {
        InvariantsFormula<CompoundState> modifiedAssumption = assumption.accept(renamedEvaluater).accept(renamer).accept(PartialEvaluator.INSTANCE, evaluationVisitor);
        // result.assumeInternal(assumption.accept(renamedEvaluater).accept(renamer).accept(PartialEvaluator.INSTANCE, evaluationVisitor), evaluationVisitor);
        boolean ass = result.assumeInternal(modifiedAssumption, evaluationVisitor);
        if (!ass) {
          result.assumeInternal(modifiedAssumption, evaluationVisitor);
          return null;
        }
      }

      result.putEnvironmentValueInternal(pVarName, pValue.accept(renamedEvaluater).accept(renamer));

      // Check if any candidate assumptions can be made for the new state
      for (InvariantsFormula<CompoundState> candidate : this.candidateAssumptions) {
          /*
           * Only assume the formula, if we are sure that it holds. While this does obviously not
           * add any new information to the state, candidate assumptions often provide more
           * compact abstract representations of relevant information that can be retained while dropping
           * more concrete information.
           */
          if (candidate.accept(getFormulaResolver(), result.getEnvironment()).isDefinitelyTrue()) {
            assert result.assumeInternal(candidate, getFormulaResolver());
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
      ReplaceVisitor<CompoundState> replaceVisitor;
      InvariantsFormula<CompoundState> previousValue = getEnvironmentValue(pVarName);
      replaceVisitor = new ReplaceVisitor<>(variable, previousValue);
      InvariantsFormula<CompoundState> newSubstitutedValue =
          pValue.accept(replaceVisitor).accept(PartialEvaluator.INSTANCE, getFormulaResolver(pEdge));

      for (Map.Entry<String, InvariantsFormula<CompoundState>> environmentEntry : this.environment.entrySet()) {
        if (!environmentEntry.getKey().equals(pVarName)) {
          InvariantsFormula<CompoundState> newEnvValue =
              environmentEntry.getValue().accept(replaceVisitor);
          result.putEnvironmentValueInternal(environmentEntry.getKey(), newEnvValue);
        }
      }
      result.putEnvironmentValueInternal(pVarName, newSubstitutedValue);

      // Try to add the invariant; if it turns out that it is false, the state is bottom
      if (!updateAssumptions(result, replaceVisitor, pValue, pVarName)) { return null; }

    }

    result.remainingEvaluations.putAll(this.remainingEvaluations);
    result.removeUnusedRenamedVariables();

    return this.identityMap.get(result);
  }

  /**
   * Removes unused renamed variables from the state.
   */
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

  /**
   * Gets the environment as a set equations of the variables with their values.
   *
   * @return the environment as a set equations of the variables with their values.
   */
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

  /**
   * When an assignment to the state is made, all assumptions of this state must be added to the given
   * state after applying the given replace visitor, used to adjust them to be correct in the new
   * environment, to those assumptions.
   *
   * @param pTargetState the state to add the assumptions to.
   * @param pReplaceVisitor the replace visitor used to transform the assumptions correct in this state to assumptions correct in the new state.
   * @param pNewValue the new value of the assigned variable.
   * @param pVarName the name of the assigned variable.
   * @return <code>true</code> if the transfer of assumptions results in a valid state, <code>false</code> if it is bottom.
   */
  private boolean updateAssumptions(InvariantsState pTargetState, ReplaceVisitor<CompoundState> pReplaceVisitor,
      InvariantsFormula<CompoundState> pNewValue, String pVarName) {
    FormulaEvaluationVisitor<CompoundState> resolver = getFormulaResolver();
    for (InvariantsFormula<CompoundState> oldAssumption : this.assumptions) {
      // Try to add the assumption; if it turns out that it is false, the state is bottom
      if (!pTargetState.assumeInternal(oldAssumption.accept(pReplaceVisitor),
          resolver)) { return false; }

    }
    // Check if any candidate assumptions can be made for the new state
    Map<String, InvariantsFormula<CompoundState>> whatIfEnvironment = new HashMap<>(pTargetState.getEnvironment());
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    whatIfEnvironment.put(pVarName, ifm.asConstant(pNewValue.accept(resolver, getEnvironment())));
    for (InvariantsFormula<CompoundState> candidate : this.candidateAssumptions) {
      /*
       * Only assume the formula, if we are sure that it holds. While this does obviously not
       * add any new information to the state, candidate assumptions often provide more
       * compact abstract representations of relevant information that can be retained while dropping
       * more concrete information.
       */
      if (candidate.accept(resolver, whatIfEnvironment).isDefinitelyTrue()) {
        assert pTargetState.assumeInternal(candidate, resolver);
      }
    }
    return true;
  }

  /**
   * Gets the value of the variable with the given name from the environment.
   * @param varName the name of the variable.
   * @return the value of the variable with the given name from the environment.
   */
  private InvariantsFormula<CompoundState> getEnvironmentValue(String varName) {
    InvariantsFormula<CompoundState> environmentValue = this.environment.get(varName);
    if (environmentValue == null) { return TOP; }
    return environmentValue;
  }

  /**
   * Gets a formula resolver for the given edge. Until the number of remaining evaluations
   * for this edge reaches zero, each call to this method will decrease that count and an
   * exact evaluation formula resolver will be returned; afterwards, a less exact abstraction
   * evaluation visitor will be returned.
   *
   * @param edge the edge the resolver will be used for.
   * @return a formula resolver for the given edge.
   */
  public FormulaEvaluationVisitor<CompoundState> getFormulaResolver(String edge) {
    if (mayEvaluate(edge)) {
      decreaseRemainingEvaluations(edge);
      return getFormulaResolver();
    }
    return ABSTRACTION_VISITOR;
  }

  /**
   * Gets an exact formula evaluation visitor.
   *
   * @return an exact formula evaluation visitor.
   */
  private FormulaEvaluationVisitor<CompoundState> getFormulaResolver() {
    return EVALUATION_VISITOR;
  }

  /**
   * Inserts the given environment into this environment.
   *
   * @param pEnvironment the environment to insert into this environment.
   */
  private void putEnvironmentValuesInternal(Map<String, InvariantsFormula<CompoundState>> pEnvironment) {
    for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : pEnvironment.entrySet()) {
      putEnvironmentValueInternal(entry.getKey(),
          entry.getValue().accept(PartialEvaluator.INSTANCE, getFormulaResolver()));
    }
  }

  /**
   * Puts the given environment element into this environment.
   *
   * @param pVariableName the variable name.
   * @param pValue the value of the variable.
   */
  private void putEnvironmentValueInternal(String pVariableName, InvariantsFormula<CompoundState> pValue) {
    FormulaEvaluationVisitor<CompoundState> evaluator = getFormulaResolver();
    InvariantsFormula<CompoundState> value = pValue.accept(PartialEvaluator.INSTANCE, evaluator);
    if (value.equals(TOP)) {
      this.environment.remove(pVariableName);
    } else {
      this.environment.put(pVariableName, pValue.accept(PartialEvaluator.INSTANCE, evaluator));
    }
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
      //this.candidateAssumptions.addAll(assumption.accept(new GuessAssumptionVisitor()));
      this.candidateAssumptions.add(assumption);
    }
  }

  /**
   * Makes the given assumptions for this state and checks if this state is still valid.
   *
   * @param pAssumptions the assumptions to be made.
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions' correctness.
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>false</code> otherwise.
   */
  private boolean assumeInternal(Collection<InvariantsFormula<CompoundState>> pAssumptions,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    for (InvariantsFormula<CompoundState> invariant : pAssumptions) {
      if (!assumeInternal(invariant, pEvaluationVisitor)) { return false; }
    }
    return true;
  }

  /**
   * Makes the given assumption for this state and checks if this state is still valid.
   *
   * @param pAssumption the assumption to be made.
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions' correctness.
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>false</code> otherwise.
   */
  private boolean assumeInternal(InvariantsFormula<CompoundState> pAssumption,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> assumption = pAssumption.accept(PartialEvaluator.INSTANCE, pEvaluationVisitor);
    // If there are multiple assumptions combined with &&, split them up
    List<InvariantsFormula<CompoundState>> assumptionParts = assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) { return assumeInternal(assumptionParts, pEvaluationVisitor); }
    // If the assumption is top, it adds no value
    if (assumption.equals(TOP)) { return true; }
    assumeCandidate(assumption);

    // If the assumption is an obvious contradiction, it cannot be validly
    // assumed
    if (assumption.equals(BOTTOM)) { return false; }

    CompoundState invariantEvaluation = assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (invariantEvaluation.isDefinitelyFalse() || invariantEvaluation.isBottom()) { return false; }
    // If the invariant evaluates to true, it adds no value for now, but it
    // can later be used as a suggestion - unless it is a constant
    if (invariantEvaluation.isDefinitelyTrue() && assumption instanceof Constant<?>) { return true; }

    PushAssumptionToEnvironmentVisitor patev =
        new PushAssumptionToEnvironmentVisitor(pEvaluationVisitor, this.environment);
    if (!assumption.accept(patev, CompoundState.logicalTrue())) {
      assert !invariantEvaluation.isDefinitelyTrue();
      return false;
    }
    // Check all assumptions once more after the environment changed
    if (isDefinitelyFalse(assumption, pEvaluationVisitor)) { return false; }
    for (InvariantsFormula<CompoundState> oldAssumption : this.assumptions) {
      if (isDefinitelyFalse(oldAssumption, pEvaluationVisitor)) { return false; }
    }

    this.assumptions.add(assumption);

    return true;
  }

  /**
   * Checks if the given assumption is definitely false for this state.
   * @param pAssumption the assumption to evaluate.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate the assumption within this state's environment.
   * @return <code>true</code> if the given assumption does definitely not hold for this state's environment, <code>false</code> if it might.
   */
  private boolean isDefinitelyFalse(InvariantsFormula<CompoundState> pAssumption,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    return pAssumption.accept(pEvaluationVisitor, getEnvironment()).isDefinitelyFalse();
  }

  /**
   * Creates a new state representing the given assumption made in the context of the current state.
   *
   * @param pAssumption the assumption made.
   * @param pEdge the edge the assumption is made on.
   * @return a new state representing the given assumption made in the context of the current state.
   */
  public InvariantsState assume(InvariantsFormula<CompoundState> pAssumption, String pEdge) {
    FormulaEvaluationVisitor<CompoundState> evaluator = getFormulaResolver(pEdge);
    InvariantsFormula<CompoundState> invariant = pAssumption.accept(PartialEvaluator.INSTANCE, evaluator);
    if (invariant instanceof Constant<?>) {
      CompoundState value = ((Constant<CompoundState>) invariant).getValue();
      // An invariant evaluating to false represents an unreachable state; it can never be fulfilled
      if (value.isDefinitelyFalse()) { return null; }
      // An invariant representing nothing more than "true" or "maybe true" adds no information
      return this;
    }
    InvariantsState result = copy(this);
    if (result != null) {
      if (!result.assumeInternal(invariant, evaluator)) { return null; }
      result.removeUnusedRenamedVariables();
    }
    return this.identityMap.get(result);
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
    FormulaEvaluationVisitor<CompoundState> evaluationVisitor = getFormulaResolver();
    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeBoolean(true);
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    ToFormulaVisitor<CompoundState, BooleanFormula> toBooleanFormulaVisitor =
        ToBooleanFormulaVisitor.getVisitor(pManager, evaluationVisitor, useBitvectors);

    for (Entry<String, InvariantsFormula<CompoundState>> entry : environment.entrySet()) {
      InvariantsFormula<CompoundState> var = ifm.asVariable(entry.getKey());
      InvariantsFormula<CompoundState> value = entry.getValue();
      InvariantsFormula<CompoundState> equation = ifm.equal(var, value);
      BooleanFormula equationFormula = equation.accept(toBooleanFormulaVisitor, getEnvironment());
      if (equationFormula != null) {
        result = bfmgr.and(result, equationFormula);
      }
      CompoundState compoundState = value.accept(evaluationVisitor, getEnvironment());
      InvariantsFormula<CompoundState> evaluatedValue = ifm.asConstant(compoundState);
      if (!evaluatedValue.equals(value)) {
        equation = ifm.equal(var, evaluatedValue);
        equationFormula = equation.accept(toBooleanFormulaVisitor, getEnvironment());
        if (equationFormula != null) {
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
    if (pObj == this) { return true; }
    if (!(pObj instanceof InvariantsState)) { return false; }
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
    return String.format("Environment: %s; Assumptions: %s",
        Joiner.on(", ").withKeyValueSeparator("=").join(environment),
        Joiner.on(", ").join(assumptions));
  }

  /**
   * Gets the maximum number of times each edge may be evaluated exactly before switching to
   * less exact abstraction.
   *
   * @return the maximum number of times each edge may be evaluated exactly before switching to
   * less exact abstraction.
   */
  public int getEvaluationThreshold() {
    return this.evaluationThreshold;
  }

  /**
   * Gets the number of remaining times the given edge will be evaluated exactly before switching
   * to less exact abstraction.
   *
   * @param edge the edge in question.
   * @return the number of remaining times the given edge will be evaluated exactly before switching
   * to less exact abstraction.
   */
  private int getRemainingEvaluations(String edge) {
    Integer remainingEvaluations = this.remainingEvaluations.get(edge);
    return remainingEvaluations == null ? this.evaluationThreshold : remainingEvaluations;
  }

  /**
   * Checks whether or not the given edge may be evaluated exactly any further.
   *
   * @param edge the edge to evaluate.
   * @return <code>true</code> if the given edge has any exact evaluations left, <code>false</code>
   * otherwise.
   */
  public boolean mayEvaluate(String edge) {
    return getRemainingEvaluations(edge) > 0;
  }

  /**
   * Decreases the allowed remaining exact evaluations for the given edge.
   *
   * @param edge the edge to evaluate.
   */
  public void decreaseRemainingEvaluations(String edge) {
    int remainingEvaluations = getRemainingEvaluations(edge);
    if (remainingEvaluations > 0) {
      this.remainingEvaluations.put(edge, remainingEvaluations - 1);
    }
  }

  /**
   * Gets the assumptions made in this state.
   *
   * @return the assumptions made in this state.
   */
  public Set<InvariantsFormula<CompoundState>> getAssumptions() {
    return Collections.unmodifiableSet(this.assumptions);
  }

  /**
   * Gets the allowed remaining evaluations for each edge.
   *
   * @return the allowed remaining evaluations for each edge.
   */
  public Map<String, Integer> getRemainingEvaluations() {
    return Collections.unmodifiableMap(remainingEvaluations);
  }

  /**
   * Gets the environment of this state.
   *
   * @return the environment of this state.
   */
  public Map<? extends String, ? extends InvariantsFormula<CompoundState>> getEnvironment() {
    return Collections.unmodifiableMap(environment);
  }


  /**
   * Gets the set of possibly useful assumptions recorded in this state (that do not necessary hold for this state).
   * @return the set of possibly useful assumptions recorded in this state (that do not necessary hold for this state).
   */
  public Collection<? extends InvariantsFormula<CompoundState>> getCandidateAssumptions() {
    return Collections.unmodifiableSet(candidateAssumptions);
  }

  /**
   * Gets the flag indicating whether or not to use bit vectors to represent the states.
   *
   * @return the flag indicating whether or not to use bit vectors to represent the states.
   */
  public boolean getUseBitvectors() {
    return useBitvectors;
  }

  /**
   * Gets the identity map of this cluster of states.
   *
   * @return the identity map of this cluster of states.
   */
  public IdentityMap getIdentityMap() {
    return this.identityMap;
  }

  /**
   * Instances of this class wrap invariants state identity maps.
   */
  public static class IdentityMap {

    /**
     * The actual identity map.
     */
    private Map<InvariantsState, InvariantsState> identityMap = new HashMap<>();

    /**
     * Gets the identity state of the given state.
     *
     * @param pKey the state to get the identity for.
     * @return the identity state of the given state.
     */
    private InvariantsState get(InvariantsState pKey) {
      InvariantsState value = this.identityMap.get(pKey);
      if (value != null) {
        return value;
      }
      this.identityMap.put(pKey, pKey);
      return pKey;
    }

  }

}
