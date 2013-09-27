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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CanExtractVariableRelationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundStateFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaAbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaDepthCountVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PushAssumptionToEnvironmentVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ReplaceVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitDisjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.StateEqualsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Union;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.invariants.formula.variablerelations.VariableEQ;
import org.sosy_lab.cpachecker.cpa.invariants.formula.variablerelations.VariableLT;
import org.sosy_lab.cpachecker.cpa.invariants.formula.variablerelations.VariableRelation;
import org.sosy_lab.cpachecker.cpa.invariants.formula.variablerelations.VariableRelationSet;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.RationalFormulaManagerView;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

/**
 * Instances of this class represent states in the light-weight invariants analysis.
 */
public class InvariantsState implements AbstractState, FormulaReportingState {

  private static final FormulaDepthCountVisitor<CompoundState> FORMULA_DEPTH_COUNT_VISITOR = new FormulaDepthCountVisitor<>();

  /**
   * A visitor used to split boolean conjunction formulas up into the conjuncted clauses
   */
  private static final SplitConjunctionsVisitor<CompoundState> SPLIT_CONJUNCTIONS_VISITOR =
      new SplitConjunctionsVisitor<>();

    /**
     * A visitor used to split boolean conjunction formulas up into the conjuncted clauses
     */
    private static final SplitDisjunctionsVisitor<CompoundState> SPLIT_DISJUNCTIONS_VISITOR =
        new SplitDisjunctionsVisitor<>();

  /**
   * A visitor used to evaluate formulas as exactly as possible.
   */
  public static final FormulaEvaluationVisitor<CompoundState> EVALUATION_VISITOR =
      new FormulaCompoundStateEvaluationVisitor();

  /**
   * A visitor that, like the formula evaluation visitor, is used to evaluate formulas, but far less exact to allow for convergence.
   */
  public static final FormulaEvaluationVisitor<CompoundState> ABSTRACTION_VISITOR = new FormulaAbstractionVisitor();

  /**
   * The constant formula representing TOP
   */
  private static final InvariantsFormula<CompoundState> TOP = CompoundStateFormulaManager.INSTANCE
      .asConstant(CompoundState.top());

  /**
   * The constant formula representing BOTTOM
   */
  private static final InvariantsFormula<CompoundState> BOTTOM = CompoundStateFormulaManager.INSTANCE
      .asConstant(CompoundState.bottom());

  /**
   * The environment currently known to the state.
   */
  private final NonRecursiveEnvironment environment;

  /**
   * The currently made assumptions.
   */
  private final VariableRelationSet<CompoundState> assumptions;

  /**
   * The edges already visited.
   */
  private final Set<CFAEdge> visitedEdges;

  /**
   * The variables selected for this analysis.
   */
  private final VariableSelection<CompoundState> variableSelection;

  private final Map<String, CType> types;

  /**
   * A flag indicating whether or not to use bit vectors for representing states.
   */
  private final boolean useBitvectors;

  private final PartialEvaluator partialEvaluator;

  private final ImmutableSet<CFAEdge> relevantEdges;

  private final ImmutableSet<InvariantsFormula<CompoundState>> interestingAssumptions;

  private final ImmutableSet<String> interestingVariables;

  private final Set<InvariantsFormula<CompoundState>> collectedInterestingAssumptions;

  private Collection<InvariantsFormula<CompoundState>> environmentAndAssumptions = null;

  /**
   * Creates a new pristine invariants state with just a value for the flag indicating whether
   * or not to use bit vectors for representing states and a variable selection.
   *
   * @param pUseBitvectors the flag indicating whether or not to use bit vectors for representing states.
   */
  public InvariantsState(boolean pUseBitvectors, VariableSelection<CompoundState> pVariableSelection) {
    this(pUseBitvectors, pVariableSelection, null, ImmutableSet.<InvariantsFormula<CompoundState>>of(), ImmutableSet.<String>of());
  }

  /**
   * Creates a new pristine invariants state with just a value for the flag indicating whether
   * or not to use bit vectors for representing states and a variable selection.
   *
   * @param pUseBitvectors the flag indicating whether or not to use bit vectors for representing states.
   */
  public InvariantsState(boolean pUseBitvectors, VariableSelection<CompoundState> pVariableSelection,
      ImmutableSet<CFAEdge> pRelevantEdges, ImmutableSet<InvariantsFormula<CompoundState>> pInterestingAssumptions,
      ImmutableSet<String> pInterestingVariables) {
    this.visitedEdges = new HashSet<>();
    this.environment = new NonRecursiveEnvironment();
    this.assumptions = new VariableRelationSet<>();
    this.partialEvaluator = new PartialEvaluator(this.environment);
    this.useBitvectors = pUseBitvectors;
    this.variableSelection = pVariableSelection;
    this.relevantEdges = pRelevantEdges;
    this.types = new HashMap<>();
    this.interestingAssumptions = pInterestingAssumptions;
    this.interestingVariables = pInterestingVariables;
    this.collectedInterestingAssumptions = new HashSet<>();
  }

  /**
   * Creates a copy of the given state.
   *
   * @param pToCopy the state to copy.
   * @return a copy of the given state.
   */
  public static InvariantsState copy(InvariantsState pToCopy) {
    return from(pToCopy.visitedEdges, pToCopy.assumptions,
        pToCopy.environment, pToCopy.useBitvectors, pToCopy.variableSelection,
        pToCopy.relevantEdges, pToCopy.types, pToCopy.interestingAssumptions,
        pToCopy.collectedInterestingAssumptions, pToCopy.interestingVariables);
  }

  /**
   * Creates a new state from the given state properties.
   *
   * @param pVisitedEdges the edges already visited previously.
   * @param pAssumptions the current assumptions.
   * @param pVariableRelations the currently known relations between variables.
   * @param pEnvironment the current environment.
   * @param pUseBitvectors a flag indicating whether or not to use bit vectors to represent states.
   * @param pInterestingAssumptions
   * @return a new state from the given state properties.
   */
  public static InvariantsState from(Set<CFAEdge> pVisitedEdges, Set<? extends InvariantsFormula<CompoundState>> pAssumptions,
      Map<String, InvariantsFormula<CompoundState>> pEnvironment,
      boolean pUseBitvectors, VariableSelection<CompoundState> pVariableSelection,
      ImmutableSet<CFAEdge> pRelevantEdges, Map<String, CType> pTypes,
      ImmutableSet<InvariantsFormula<CompoundState>> pInterestingAssumptions,
      Set<InvariantsFormula<CompoundState>> pCollectedInterestingAssumptions,
      ImmutableSet<String> pInterestingVariables) {
    InvariantsState result = new InvariantsState(pUseBitvectors, pVariableSelection, pRelevantEdges, pInterestingAssumptions, pInterestingVariables);
    if (!result.assumeInternal(pAssumptions, result.getFormulaResolver())) { return null; }
    if (!result.assumeInternal(pCollectedInterestingAssumptions, result.getFormulaResolver())) { return null; }
    result.environment.putAll(pEnvironment);
    result.visitedEdges.addAll(pVisitedEdges);
    result.types.putAll(pTypes);
    return result;
  }

  public InvariantsState assignArray(String pArray, InvariantsFormula<CompoundState> pSubscript, InvariantsFormula<CompoundState> pValue, CFAEdge pEdge) {
    FormulaEvaluationVisitor<CompoundState> fev = getFormulaResolver(pEdge);
    // Edge is already counted by formula resolver access
    boolean ignoreEdge = mayEvaluate(pEdge);
    CompoundState value = pSubscript.accept(fev, this.environment);
    if (value.isSingleton()) { // Exact subscript value is known
      return assign(pArray + "[" + value.getValue() + "]", pValue, pEdge, ignoreEdge);
    } else { // Multiple subscript values are possible: All possible subscript targets are now unknown
      InvariantsState result = this;
      for (String varName : this.environment.keySet()) {
        String prefix = pArray + "[";
        if (varName.startsWith(prefix)) {
          String subscriptValueStr = varName.replace(prefix, "").replace("]", "");
          if (subscriptValueStr.equals("*") || value.contains(new BigInteger(subscriptValueStr))) {
            result = result.assign(varName, TOP, pEdge, ignoreEdge);
          }
        }
      }
      return result;
    }
  }

  public InvariantsState assign(String pVarName, InvariantsFormula<CompoundState> pValue, CFAEdge pEdge) {
    if (pValue instanceof Variable<?>) {
      InvariantsState result = this;
      String valueVarName = ((Variable<?>) pValue).getName();
      String pointerDerefPrefix = valueVarName + "->";
      String nonPointerDerefPrefix = valueVarName + ".";
      for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : this.environment.entrySet()) {
        if (entry.getKey().startsWith(pointerDerefPrefix)) {
          String suffix = entry.getKey().substring(pointerDerefPrefix.length());
          result = result.assign(pVarName + "->" + suffix, CompoundStateFormulaManager.INSTANCE.asVariable(entry.getKey()), pEdge);
        } else if (entry.getKey().startsWith(nonPointerDerefPrefix)) {
          String suffix = entry.getKey().substring(nonPointerDerefPrefix.length());
          result = result.assign(pVarName + "." + suffix, CompoundStateFormulaManager.INSTANCE.asVariable(entry.getKey()), pEdge);
        }
      }
      return result.assign(pVarName, pValue, pEdge, false);
    }
    return assign(pVarName, pValue, pEdge, false);
  }

  /**
   * Creates a new state representing the given assignment applied to the current state.
   *
   * @param pVarName the name of the variable being assigned.
   * @param pValue the new value of the variable.
   * @param pEdge the edge containing the assignment.
   * @param pIgnoreEdge flag indicating whether or not to evaluate without checking the remaining evaluations for the edge
   * @return a new state representing the given assignment applied to the current state.
   */
  private InvariantsState assign(String pVarName, InvariantsFormula<CompoundState> pValue, CFAEdge pEdge, boolean pIgnoreEdge) {
    Preconditions.checkNotNull(pValue);

    // Check if the assigned variable is selected (newVariableSelection != null)
    VariableSelection<CompoundState> newVariableSelection = this.variableSelection.acceptAssignment(pVarName, pValue);
    if (newVariableSelection == null) {
      // Ensure that no information about the irrelevant assigned variable is retained
      Map<String, InvariantsFormula<CompoundState>> newEnvironment = this.environment;
      if (this.environment.containsKey(pVarName)) {
        newEnvironment = new HashMap<>(this.environment);
        newEnvironment.remove(pVarName);
      }
      boolean assumptionsChanged = false;
      Set<InvariantsFormula<CompoundState>> newAssumptions = new HashSet<>();
      for (InvariantsFormula<CompoundState> assumption : this.assumptions) {
        if (!assumption.accept(new ContainsVarVisitor<CompoundState>(), pVarName)) {
          newAssumptions.add(assumption);
        } else {
          assumptionsChanged = true;
        }
      }
      for (InvariantsFormula<CompoundState> assumption : this.collectedInterestingAssumptions) {
        if (!assumption.accept(new ContainsVarVisitor<CompoundState>(), pVarName)) {
          newAssumptions.add(assumption);
        } else {
          assumptionsChanged = true;
        }
      }
      if (this.environment == newEnvironment && !assumptionsChanged) {
        return this;
      } else {
        return from(visitedEdges, newAssumptions, newEnvironment,
            useBitvectors, variableSelection, relevantEdges, types, interestingAssumptions,
            Collections.<InvariantsFormula<CompoundState>>emptySet(),
            interestingVariables);
      }
    }

    CompoundStateFormulaManager ifm = CompoundStateFormulaManager.INSTANCE;
    Variable<CompoundState> variable = ifm.asVariable(pVarName);

    // Optimization: If the value being assigned is equivalent to the value already stored, do nothing
    if (getEnvironmentValue(pVarName).equals(pValue) || variable.accept(new StateEqualsVisitor(getFormulaResolver(), this.environment), pValue)) {
      return this;
    }

    final InvariantsState result = new InvariantsState(useBitvectors, newVariableSelection, relevantEdges, interestingAssumptions, interestingVariables);

    /*
     * A variable is newly assigned, so the appearances of this variable
     * in any previously collected assumptions (including its new value)
     * have to be resolved with the variable's previous value.
     */
    ReplaceVisitor<CompoundState> replaceVisitor;
    InvariantsFormula<CompoundState> previousValue = getEnvironmentValue(pVarName);
    FormulaEvaluationVisitor<CompoundState> evaluationVisitor = getFormulaResolver(pEdge);
    if (!mayEvaluate(pEdge) && previousValue instanceof Union<?>) {
      Union<CompoundState> union = (Union<CompoundState>) previousValue;
      if (union.getOperand1() instanceof Union<?>
          || union.getOperand2() instanceof Union<?>) {
        previousValue = CompoundStateFormulaManager.INSTANCE.asConstant(previousValue.accept(evaluationVisitor, environment));
      }
    }
    replaceVisitor = new ReplaceVisitor<>(variable, previousValue);
    InvariantsFormula<CompoundState> newSubstitutedValue =
        pValue.accept(replaceVisitor).accept(this.partialEvaluator, evaluationVisitor);

    for (Map.Entry<String, InvariantsFormula<CompoundState>> environmentEntry : this.environment.entrySet()) {
      if (!environmentEntry.getKey().equals(pVarName)) {
        InvariantsFormula<CompoundState> newEnvValue =
            environmentEntry.getValue().accept(replaceVisitor);
        result.environment.put(environmentEntry.getKey(), trim(newEnvValue));
      }
    }
    result.environment.put(pVarName, trim(newSubstitutedValue));

    // Try to add the assumptions; if it turns out that they are false, the state is bottom
    if (!updateAssumptions(result, replaceVisitor, pValue, pVarName, pEdge)) { return null; }

    result.visitedEdges.addAll(visitedEdges);
    result.assumeInternal(CompoundStateFormulaManager.INSTANCE.equal(variable, pValue), getFormulaResolver(pEdge));

    if (!result.collectInterestingAssumptions(CompoundStateFormulaManager.INSTANCE.equal(variable, pValue.accept(replaceVisitor)))) {
      return null;
    }

    // Forbid further exact evaluations of this edge
    result.visitedEdges.add(pEdge);
    result.types.putAll(types);

    if (equals(result)) {
      return this;
    }
    return result;
  }

  private InvariantsFormula<CompoundState> trim(InvariantsFormula<CompoundState> pFormula) {
    if (pFormula.accept(FORMULA_DEPTH_COUNT_VISITOR) > 4) {
      return CompoundStateFormulaManager.INSTANCE.asConstant(
          pFormula.accept(EVALUATION_VISITOR, environment));
    }
    return pFormula;
  }

  /**
   * Gets the environment as a set equations of the variables with their values.
   *
   * @return the environment as a set equations of the variables with their values.
   */
  public Set<InvariantsFormula<CompoundState>> getEnvironmentAsAssumptions() {
    Set<InvariantsFormula<CompoundState>> environmentalAssumptions = new HashSet<>();
    CompoundStateFormulaManager ifm = CompoundStateFormulaManager.INSTANCE;
    for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : this.environment.entrySet()) {
      InvariantsFormula<CompoundState> variable = ifm.asVariable(entry.getKey());
      InvariantsFormula<CompoundState> equation = ifm.equal(variable, entry.getValue());
      environmentalAssumptions.add(equation);
    }
    return environmentalAssumptions;
  }

  public Iterable<InvariantsFormula<CompoundState>> getAssumptionsAndEnvironment() {
    return Iterables.concat(this.assumptions, this.collectedInterestingAssumptions, new Iterable<InvariantsFormula<CompoundState>>() {

      private Iterable<InvariantsFormula<CompoundState>> lazyInner = null;

      @Override
      public Iterator<InvariantsFormula<CompoundState>> iterator() {
        if (lazyInner == null) {
          lazyInner = getEnvironmentAsAssumptions();
        }
        return lazyInner.iterator();
      }

    });
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
      InvariantsFormula<CompoundState> pNewValue, String pVarName, CFAEdge pEdge) {
    FormulaEvaluationVisitor<CompoundState> resolver = getFormulaResolver(pEdge);
    for (InvariantsFormula<CompoundState> oldAssumption : this.assumptions) {
      // Try to add the assumption; if it turns out that it is false, the assumption is bottom
      if (!pTargetState.assumeInternal(oldAssumption.accept(pReplaceVisitor),
          resolver)) {
        return false;
      }
    }
    for (InvariantsFormula<CompoundState> oldAssumption : this.collectedInterestingAssumptions) {
      // Try to add the assumption; if it turns out that it is false, the assumption is bottom
      if (!pTargetState.assumeInternal(oldAssumption.accept(pReplaceVisitor),
          resolver)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Gets the value of the variable with the given name from the environment.
   * @param pVarName the name of the variable.
   * @return the value of the variable with the given name from the environment.
   */
  private InvariantsFormula<CompoundState> getEnvironmentValue(String pVarName) {
    InvariantsFormula<CompoundState> environmentValue = this.environment.get(pVarName);
    if (environmentValue == null) { return TOP; }
    return environmentValue;
  }

  /**
   * Gets a formula resolver for the given edge. If the edge has not yet been visited,
   * an exact evaluation formula resolver will be returned; afterwards, a less exact abstraction
   * evaluation visitor will be returned.
   *
   * @param pEdge the edge the resolver will be used for.
   * @return a formula resolver for the given edge.
   */
  public FormulaEvaluationVisitor<CompoundState> getFormulaResolver(CFAEdge pEdge) {
    if (mayEvaluate(pEdge)) {
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
   * Makes the given assumptions for this state and checks if this state is still valid.
   *
   * @param pAssumptions the assumptions to be made.
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions' correctness.
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>false</code> otherwise.
   */
  private boolean assumeInternal(Collection<? extends InvariantsFormula<CompoundState>> pAssumptions,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    for (InvariantsFormula<CompoundState> assumption : pAssumptions) {
      if (!assumeInternal(assumption, pEvaluationVisitor)) { return false; }
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
    InvariantsFormula<CompoundState> assumption = pAssumption.accept(this.partialEvaluator, pEvaluationVisitor);
    // If there are multiple assumptions combined with &&, split them up
    List<InvariantsFormula<CompoundState>> assumptionParts = assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) { return assumeInternal(assumptionParts, pEvaluationVisitor); }
    // If the assumption is top, it adds no value
    if (assumption.equals(TOP)) { return true; }

    if (assumption instanceof Constant<?>) {
      return !((Constant<CompoundState>) assumption).getValue().isDefinitelyFalse();
    }

    // If the assumption is an obvious contradiction, it cannot be validly assumed
    if (assumption.equals(BOTTOM)) { return false; }

    if (!collectInterestingAssumptions(assumption)) {
      return false;
    }

    CompoundState assumptionEvaluation = assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (assumptionEvaluation.isDefinitelyFalse() || assumptionEvaluation.isBottom()) { return false; }
    // If the invariant evaluates to true, it adds no value for now
    if (assumptionEvaluation.isDefinitelyTrue()) { return true; }

    if (!(pEvaluationVisitor instanceof FormulaAbstractionVisitor)) {
      PushAssumptionToEnvironmentVisitor patev =
          new PushAssumptionToEnvironmentVisitor(pEvaluationVisitor, this.environment);
      if (!assumption.accept(patev, CompoundState.logicalTrue())) {
        assert !assumptionEvaluation.isDefinitelyTrue();
        return false;
      }
      // Check all assumptions once more after the environment changed
      if (isDefinitelyFalse(assumption, pEvaluationVisitor)) { return false; }
      for (InvariantsFormula<CompoundState> oldAssumption : this.assumptions) {
        if (isDefinitelyFalse(oldAssumption, pEvaluationVisitor)) { return false; }
      }

      // Check again if there is any more value to gain from the assumption after extracting environment information
      assumption = assumption.accept(this.partialEvaluator, EVALUATION_VISITOR);
      if (assumption.accept(EVALUATION_VISITOR, this.environment).isDefinitelyTrue()) {
        // No more value to gain
        return true;
      }
    }

    extractVariableRelations(assumption, EVALUATION_VISITOR, this.assumptions);

    return true;
  }

  private void extractVariableRelations(InvariantsFormula<CompoundState> pFormula, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor,
      VariableRelationSet<CompoundState> pVariableRelationSet) {
    List<InvariantsFormula<CompoundState>> conjunctiveParts = pFormula.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (conjunctiveParts.size() > 1) {
      for (InvariantsFormula<CompoundState> conjunctivePart : conjunctiveParts) {
        extractVariableRelations(conjunctivePart, pEvaluationVisitor, pVariableRelationSet);
      }
      return;
    }
    List<InvariantsFormula<CompoundState>> disjunctiveParts = pFormula.accept(SPLIT_DISJUNCTIONS_VISITOR);
    if (disjunctiveParts.size() > 1) {
      VariableRelationSet<CompoundState> union = new VariableRelationSet<>();
      VariableRelationSet<CompoundState> partRelations = new VariableRelationSet<>();
      for (InvariantsFormula<CompoundState> disjunctivePart : disjunctiveParts) {
        partRelations.clear();
        extractVariableRelations(disjunctivePart, pEvaluationVisitor, partRelations);
        union.uniteWith(partRelations);
      }
      pVariableRelationSet.refineBy(union);
      return;
    }
    Set<String> variables = pFormula.accept(new CollectVarsVisitor<CompoundState>());
    if (variables.size() == 2 && pFormula.accept(new CanExtractVariableRelationVisitor(Collections.unmodifiableMap(environment)), pEvaluationVisitor)) {
      Iterator<String> variableIterator = variables.iterator();
      String var1Name = variableIterator.next();
      String var2Name = variableIterator.next();
      Map<String, InvariantsFormula<CompoundState>> tmpEnvironment = new HashMap<>();
      tmpEnvironment.put(var1Name, CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.singleton(0)));
      if (pFormula.accept(new PushAssumptionToEnvironmentVisitor(pEvaluationVisitor, tmpEnvironment), CompoundState.logicalTrue())) {
        InvariantsFormula<CompoundState> var2RelativeValue = tmpEnvironment.get(var2Name);
        if (var2RelativeValue != null) {
          CompoundState relationSignum = var2RelativeValue.accept(pEvaluationVisitor, tmpEnvironment).signum();
          if (!relationSignum.isBottom()) {
            Variable<CompoundState> var1 = CompoundStateFormulaManager.INSTANCE.asVariable(var1Name);
            Variable<CompoundState> var2 = CompoundStateFormulaManager.INSTANCE.asVariable(var2Name);
            VariableRelation<CompoundState> relation = null;
            if (relationSignum.containsZero()) {
              relation = new VariableEQ<>(var1, var2);
            }
            if (relationSignum.containsNegative()) {
              VariableLT<CompoundState> gt = new VariableLT<>(var2, var1);
              relation = relation == null ? gt : relation.union(gt);
            }
            if (relationSignum.containsPositive()) {
              VariableLT<CompoundState> lt = new VariableLT<>(var1, var2);
              relation = relation == null ? lt : relation.union(lt);
            }
            if (relation != null && !pVariableRelationSet.contains(relation)) {
              pVariableRelationSet.add(relation);
            }
          }
        }
      }
    }
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

  public InvariantsState assume(InvariantsFormula<CompoundState> pAssumption, CFAEdge pEdge) {
    // Check if at least one of the involved variables is selected (newVariableSelection != null)
    VariableSelection<CompoundState> newVariableSelection = this.variableSelection.acceptAssumption(pAssumption);
    if (newVariableSelection == null) {
      return this;
    }
    FormulaEvaluationVisitor<CompoundState> evaluator = getFormulaResolver(pEdge);
    InvariantsFormula<CompoundState> assumption = pAssumption.accept(this.partialEvaluator, evaluator);
    if (assumption instanceof Constant<?>) {
      CompoundState value = ((Constant<CompoundState>) assumption).getValue();
      // An assumption evaluating to false represents an unreachable state; it can never be fulfilled
      if (value.isDefinitelyFalse()) { return null; }
      // An assumption representing nothing more than "true" or "maybe true" adds no information
      return this;
    }
    if (getAssumptions().contains(assumption)) {
      return this;
    }
    InvariantsState result = from(visitedEdges, assumptions, environment, useBitvectors,
        newVariableSelection, relevantEdges, types, interestingAssumptions, collectedInterestingAssumptions,
        interestingVariables);
    if (result != null) {
      result.visitedEdges.add(pEdge);
      if (!result.assumeInternal(assumption, evaluator)) { return null; }
      if (equals(result)) {
        return this;
      }
    }
    return result;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
    FormulaEvaluationVisitor<CompoundState> evaluationVisitor = getFormulaResolver();
    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeBoolean(true);
    ToFormulaVisitor<CompoundState, BooleanFormula> toBooleanFormulaVisitor =
        ToBooleanFormulaVisitor.getVisitor(pManager, evaluationVisitor, useBitvectors);

    List<InvariantsFormula<CompoundState>> assumptions = new ArrayList<>();
    assumptions.addAll(collectedInterestingAssumptions);
    assumptions.addAll(this.assumptions);

    for (InvariantsFormula<CompoundState> assumption : getAssumptionsAndEnvironment()) {
      BooleanFormula assumptionFormula = assumption.accept(toBooleanFormulaVisitor, getEnvironment());
      if (assumptionFormula != null) {
        result = bfmgr.and(result, assumptionFormula);
      }
    }
    for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : this.environment.entrySet()) {
      InvariantsFormula<CompoundState> valueFormula = entry.getValue();
      if (valueFormula.equals(TOP)) {
        continue;
      }
      String varName = entry.getKey();
      CType type = types.get(varName);
      BooleanFormula formula = null;
      if (type instanceof CSimpleType) {
        CSimpleType simpleType = (CSimpleType) type;
        simpleType.getType();
        if (simpleType.getType().equals(org.sosy_lab.cpachecker.cfa.types.c.CBasicType.BOOL)) {
          formula = bfmgr.equivalence(bfmgr.makeVariable(varName), valueFormula.accept(toBooleanFormulaVisitor, getEnvironment()));
        }
      }
      if (formula == null) {
        formula = CompoundStateFormulaManager.INSTANCE.equal(CompoundStateFormulaManager.INSTANCE.asVariable(varName), valueFormula).accept(toBooleanFormulaVisitor, getEnvironment());
      }
      if (formula != null) {
        result = bfmgr.and(result, formula);
      }
    }

    // Apply type information
    RationalFormulaManagerView rfmgr = pManager.getRationalFormulaManager();
    for (Map.Entry<String, CType> typeMapping : types.entrySet()) {
      if (typeMapping.getValue() instanceof CSimpleType) {
        CSimpleType type = (CSimpleType) typeMapping.getValue();
        if (type.isUnsigned()) {
          BooleanFormula typeFormula = rfmgr.greaterOrEquals(rfmgr.makeVariable(typeMapping.getKey()), rfmgr.makeNumber(0));
          result = bfmgr.and(result, typeFormula);
        }
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
        && assumptions.equals(other.assumptions)
        && collectedInterestingAssumptions.equals(other.collectedInterestingAssumptions);
  }

  @Override
  public int hashCode() {
    return environment.hashCode();
  }

  @Override
  public String toString() {
    return String.format("Environment: %s; Assumptions: %s",
        Joiner.on(", ").withKeyValueSeparator("=").join(environment),
        Joiner.on(", ").join(FluentIterable.from(Iterables.concat(assumptions, collectedInterestingAssumptions)).toSet()));
  }

  /**
   * Checks whether or not the given edge may be evaluated exactly any further.
   *
   * @param edge the edge to evaluate.
   * @return <code>true</code> if the given edge has any exact evaluations left, <code>false</code>
   * otherwise.
   */
  public boolean mayEvaluate(CFAEdge edge) {
    return !this.visitedEdges.contains(edge);
  }

  /**
   * Gets the assumptions made in this state.
   *
   * @return the assumptions made in this state.
   */
  public Set<? extends InvariantsFormula<CompoundState>> getAssumptions() {
    return Collections.unmodifiableSet(this.assumptions);
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
   * Gets the flag indicating whether or not to use bit vectors to represent the states.
   *
   * @return the flag indicating whether or not to use bit vectors to represent the states.
   */
  public boolean getUseBitvectors() {
    return useBitvectors;
  }

  public boolean isLessThanOrEqualTo(InvariantsState pElement2) {
    if (pElement2 == this) { return true; }
    if (pElement2 == null) {
      return false;
    }
    // Only states with equal information regarding interesting assumptions and environment may be combined
    if (!collectedInterestingAssumptions.equals(pElement2.collectedInterestingAssumptions)) {
      return false;
    }
    // Environments must be equal at least with respect to interesting variables
    if (!environmentsEqualWithRespectToInterestingVariables(pElement2)) {
      return false;
    }
    // Only two checks remain to test for equality:
    if (assumptions.equals(pElement2.assumptions)
        && environment.equals(pElement2.environment)) {
      return true;
    }
    // Perform the implication check (if this state definitely implies the other one, it is less than or equal to it)
    if (environmentAndAssumptions == null) {
      environmentAndAssumptions = FluentIterable.from(getAssumptionsAndEnvironment()).toSet();
    }
    for (InvariantsFormula<CompoundState> rightAssumption : pElement2.getAssumptionsAndEnvironment()) {
      if (!CompoundStateFormulaManager.definitelyImplies(environmentAndAssumptions, rightAssumption, this.environment)) {
        return false;
      }
    }
    return true;
  }

  private boolean environmentsEqualWithRespectToInterestingVariables(InvariantsState pElement2) {
    for (String interestingVariable : interestingVariables) {
      InvariantsFormula<CompoundState> left = environment.get(interestingVariable);
      InvariantsFormula<CompoundState> right = pElement2.environment.get(interestingVariable);
      if (left != right && (left == null || !left.equals(right))) {
        return false;
      }
    }
    return true;
  }

  public InvariantsState join(InvariantsState pElement2) {
    return join(pElement2, false);
  }

  public InvariantsState join(InvariantsState pElement2, boolean forceJoin) {
    Preconditions.checkArgument(pElement2.useBitvectors == useBitvectors);
    Preconditions.checkArgument(pElement2.relevantEdges == relevantEdges);
    Preconditions.checkArgument(pElement2.interestingAssumptions == interestingAssumptions);

    InvariantsState element1 = this;
    InvariantsState element2 = pElement2;

    InvariantsState result;

    if (isLessThanOrEqualTo(element2)
        || !forceJoin && !collectedInterestingAssumptions.equals(element2.collectedInterestingAssumptions)
        || !forceJoin && !environmentsEqualWithRespectToInterestingVariables(pElement2)) {
      result = element2;
    } else if (element2.isLessThanOrEqualTo(element1)) {
      result = element1;
    } else {
      final Set<CFAEdge> resultVisitedEdges;
      if (element1.visitedEdges.isEmpty()) {
        resultVisitedEdges = element2.visitedEdges;
      } else if (element2.visitedEdges.isEmpty()) {
        resultVisitedEdges = element1.visitedEdges;
      } else {
        resultVisitedEdges = new HashSet<>(element1.visitedEdges);
        resultVisitedEdges.addAll(element2.visitedEdges);
      }

      Map<String, InvariantsFormula<CompoundState>> resultEnvironment = new NonRecursiveEnvironment();
      Set<InvariantsFormula<CompoundState>> resultAssumptions = new HashSet<>();

      // Get some basic information by joining the environments
      for (Map.Entry<String, InvariantsFormula<CompoundState>> entry : element1.environment.entrySet()) {
        String varName = entry.getKey();
        InvariantsFormula<CompoundState> rightFormula = element2.environment.get(varName);
        if (rightFormula != null) {
          InvariantsFormula<CompoundState> newValueFormula =
              CompoundStateFormulaManager.INSTANCE.union(
                  entry.getValue().accept(element1.partialEvaluator, EVALUATION_VISITOR),
                  rightFormula.accept(element2.partialEvaluator, EVALUATION_VISITOR)).accept(new PartialEvaluator(), EVALUATION_VISITOR);
          resultEnvironment.put(varName,
              newValueFormula);
        }
      }

      // Make assumptions
      VariableRelationSet<CompoundState> leftRelations = new VariableRelationSet<>(element1.assumptions);
      VariableRelationSet<CompoundState> rightRelations = new VariableRelationSet<>(element2.assumptions);
      VariableRelationSet<CompoundState> resultRelations = new VariableRelationSet<>(leftRelations);
      resultRelations.uniteWith(element2.assumptions);
      leftRelations.removeAll(resultRelations);
      rightRelations.removeAll(resultRelations);
      resultAssumptions.addAll(resultRelations);

      Iterator<? extends InvariantsFormula<CompoundState>> leftAssumptionIterator = leftRelations.iterator();
      Iterator<? extends InvariantsFormula<CompoundState>> rightAssumptionIterator = rightRelations.iterator();
      // Apply "or" to the two remaining sets of assumptions
      if (leftAssumptionIterator.hasNext() && rightAssumptionIterator.hasNext()) {

        InvariantsFormula<CompoundState> leftTotalAssumption = leftAssumptionIterator.next();
        while (leftAssumptionIterator.hasNext()) {
          leftTotalAssumption = CompoundStateFormulaManager.INSTANCE.logicalAnd(leftTotalAssumption, leftAssumptionIterator.next());
        }
        InvariantsFormula<CompoundState> rightTotalAssumption = rightAssumptionIterator.next();
        while (rightAssumptionIterator.hasNext()) {
          rightTotalAssumption = CompoundStateFormulaManager.INSTANCE.logicalAnd(rightTotalAssumption, rightAssumptionIterator.next());
        }

        Set<InvariantsFormula<CompoundState>> newDisjunctiveClauses = new HashSet<>();

        newDisjunctiveClauses.addAll(leftTotalAssumption.accept(SPLIT_DISJUNCTIONS_VISITOR));
        newDisjunctiveClauses.addAll(rightTotalAssumption.accept(SPLIT_DISJUNCTIONS_VISITOR));
        InvariantsFormula<CompoundState> newAssumption =
            CompoundStateFormulaManager.INSTANCE.logicalOr(leftTotalAssumption, rightTotalAssumption);
        resultAssumptions.add(newAssumption);
      }

      VariableSelection<CompoundState> resultVariableSelection = element1.variableSelection.join(element2.variableSelection);


      MapDifference<String, CType> difference = Maps.difference(element1.types, element2.types);

      result = InvariantsState.from(resultVisitedEdges, resultAssumptions,
          resultEnvironment, element1.getUseBitvectors(), resultVariableSelection,
          element1.relevantEdges, difference.entriesInCommon(), interestingAssumptions,
          collectedInterestingAssumptions, interestingVariables);
    }
    if (result != null) {
      if (result.equals(element2)) {
        result = element2;
      } else if (result.equals(element1)) {
        result = element1;
      }
    }
    return result;
  }

  public boolean isRelevant(CFAEdge pCfaEdge) {
    return relevantEdges == null || relevantEdges.contains(pCfaEdge);
  }

  public InvariantsState putType(String pVarName, CType pType) {
    CType storedType = this.types.get(pVarName);
    if (storedType == pType || storedType != null && storedType.equals(pType)) {
      return this;
    }
    InvariantsState result = copy(this);
    result.types.put(pVarName, pType);
    return result;
  }

  /**
   * Collects any "interesting" assumptions holding for this state, considering
   * the given assumption as true.
   *
   * @param pAssumption the additional assumption to consider.
   * @return <code>true</code> if the state is not obviously unsound afterwards.
   */
  private boolean collectInterestingAssumptions(InvariantsFormula<CompoundState> pAssumption) {
    if (interestingAssumptions.isEmpty()) {
      return true;
    }
    Collection<InvariantsFormula<CompoundState>> informationBase = new ArrayList<>();
    FluentIterable.from(getAssumptionsAndEnvironment()).copyInto(informationBase);
    informationBase.add(pAssumption);
    for (InvariantsFormula<CompoundState> interestingAssumption : interestingAssumptions) {
      InvariantsFormula<CompoundState> negatedInterestingAssumption = CompoundStateFormulaManager.INSTANCE.logicalNot(interestingAssumption);
      if (!collectedInterestingAssumptions.contains(interestingAssumption) && CompoundStateFormulaManager.definitelyImplies(informationBase, interestingAssumption)) {
        collectedInterestingAssumptions.add(interestingAssumption);
        if (collectedInterestingAssumptions.contains(negatedInterestingAssumption)) {
          return false;
        }
        assumeInternal(interestingAssumption, ABSTRACTION_VISITOR);
      } else if (!collectedInterestingAssumptions.contains(negatedInterestingAssumption) && CompoundStateFormulaManager.definitelyImplies(informationBase, negatedInterestingAssumption)) {
        collectedInterestingAssumptions.add(negatedInterestingAssumption);
        if (collectedInterestingAssumptions.contains(interestingAssumption)) {
          return false;
        }
        assumeInternal(negatedInterestingAssumption, ABSTRACTION_VISITOR);
      }
    }
    return true;
  }

}
