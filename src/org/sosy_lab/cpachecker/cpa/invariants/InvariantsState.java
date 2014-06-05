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

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
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
import org.sosy_lab.cpachecker.cpa.invariants.formula.StateEqualsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Union;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

/**
 * Instances of this class represent states in the light-weight invariants analysis.
 */
public class InvariantsState implements AbstractState, FormulaReportingState {

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private static final FormulaDepthCountVisitor<CompoundInterval> FORMULA_DEPTH_COUNT_VISITOR = new FormulaDepthCountVisitor<>();

  /**
   * A visitor used to split boolean conjunction formulas up into the conjuncted clauses
   */
  private static final SplitConjunctionsVisitor<CompoundInterval> SPLIT_CONJUNCTIONS_VISITOR =
      new SplitConjunctionsVisitor<>();

    /**
   * A visitor used to evaluate formulas as exactly as possible.
   */
  public static final FormulaEvaluationVisitor<CompoundInterval> EVALUATION_VISITOR =
      new FormulaCompoundStateEvaluationVisitor();

  /**
   * A visitor that, like the formula evaluation visitor, is used to evaluate formulas, but far less exact to allow for convergence.
   */
  public static final FormulaEvaluationVisitor<CompoundInterval> ABSTRACTION_VISITOR = new FormulaAbstractionVisitor();

  /**
   * The constant formula representing TOP
   */
  private static final InvariantsFormula<CompoundInterval> TOP = CompoundIntervalFormulaManager.INSTANCE
      .asConstant(CompoundInterval.top());

  /**
   * The constant formula representing BOTTOM
   */
  private static final InvariantsFormula<CompoundInterval> BOTTOM = CompoundIntervalFormulaManager.INSTANCE
      .asConstant(CompoundInterval.bottom());

  /**
   * The environment currently known to the state.
   */
  private final NonRecursiveEnvironment environment;

  /**
   * The variables selected for this analysis.
   */
  private final VariableSelection<CompoundInterval> variableSelection;

  private final Map<String, CType> variableTypes;

  /**
   * A flag indicating whether or not to use bit vectors for representing states.
   */
  private final boolean useBitvectors;

  private final PartialEvaluator partialEvaluator;

  private final MachineModel machineModel;

  private Iterable<InvariantsFormula<CompoundInterval>> environmentAsAssumptions;

  private volatile int hash = 0;

  public InvariantsState(boolean pUseBitvectors,
      VariableSelection<CompoundInterval> pVariableSelection,
      MachineModel pMachineModel,
      InvariantsState pInvariant) {
    this.environment = pInvariant.environment;
    this.partialEvaluator = pInvariant.partialEvaluator;
    this.useBitvectors = pUseBitvectors;
    this.variableSelection = pVariableSelection;
    this.variableTypes = pInvariant.variableTypes;
    this.machineModel = pMachineModel;
  }

  /**
   * Creates a new invariants state with just a value for the flag indicating
   * whether or not to use bit vectors for representing states, a selection of
   * variables, the set of visited edges and a precision.
   *
   * @param pUseBitvectors the flag indicating whether or not to use bit vectors for representing states.
   * @param pVariableSelection the selected variables.
   * @param pMachineModel the machine model used.
   */
  public InvariantsState(boolean pUseBitvectors,
      VariableSelection<CompoundInterval> pVariableSelection,
      MachineModel pMachineModel) {
    this.environment = new NonRecursiveEnvironment();
    this.partialEvaluator = new PartialEvaluator(this.environment);
    this.useBitvectors = pUseBitvectors;
    this.variableSelection = pVariableSelection;
    this.variableTypes = new HashMap<>();
    this.machineModel = pMachineModel;
  }

  /**
   * Creates a new state from the given state properties.
   *
   * @param pEnvironment the current environment.
   * @param pUseBitvectors a flag indicating whether or not to use bit vectors to represent states.
   * @param pVariableSelection the selected variables.
   * @param pMachineModel the machine model used.
   * @param pVariableTypes the types of the variables.
   *
   * @return a new state from the given state properties.
   */
  private static InvariantsState from(Map<String, InvariantsFormula<CompoundInterval>> pEnvironment,
      boolean pUseBitvectors,
      VariableSelection<CompoundInterval> pVariableSelection,
      MachineModel pMachineModel,
      Map<String, CType> pVariableTypes) {
    InvariantsState result = new InvariantsState(pUseBitvectors, pVariableSelection, pMachineModel);
    result.environment.putAll(pEnvironment);
    result.variableTypes.putAll(pVariableTypes);
    return result;
  }

  public InvariantsState setType(String pVarName, CType pType) {
    if (pType.equals(variableTypes.get(pVarName))) {
      return this;
    }
    InvariantsState result = from(environment, useBitvectors, variableSelection, machineModel, variableTypes);
    result.variableTypes.put(pVarName, pType);
    return result;
  }

  public InvariantsState setTypes(Map<String, CType> pVarTypes) {
    boolean allContained = true;
    for (Map.Entry<String, CType> entry : pVarTypes.entrySet()) {
      if (!entry.getValue().equals(variableTypes.get(entry.getKey()))) {
        allContained = false;
        break;
      }
    }
    if (allContained) {
      return this;
    }
    InvariantsState result = from(environment, useBitvectors, variableSelection, machineModel, variableTypes);
    result.variableTypes.putAll(pVarTypes);
    return result;
  }

  public InvariantsState assignArray(String pArray, InvariantsFormula<CompoundInterval> pSubscript, InvariantsFormula<CompoundInterval> pValue, CFAEdge pEdge) {
    FormulaEvaluationVisitor<CompoundInterval> fev = getFormulaResolver(pEdge);
    CompoundInterval value = pSubscript.accept(fev, this.environment);
    if (value.isSingleton()) { // Exact subscript value is known
      return assign(pArray + "[" + value.getValue() + "]", pValue, pEdge, true);
    } else { // Multiple subscript values are possible: All possible subscript targets are now unknown
      InvariantsState result = this;
      for (String varName : this.environment.keySet()) {
        String prefix = pArray + "[";
        if (varName.startsWith(prefix)) {
          String subscriptValueStr = varName.replace(prefix, "").replaceAll("].*", "");
          if (subscriptValueStr.equals("*") || value.contains(new BigInteger(subscriptValueStr))) {
            result = result.assign(varName, TOP, pEdge, true);
          }
        }
      }
      return result;
    }
  }

  public InvariantsState assign(String pVarName, InvariantsFormula<CompoundInterval> pValue, CFAEdge pEdge) {
    InvariantsState result = this;
    for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : this.environment.entrySet()) {
      String varName = entry.getKey();
      if (varName.startsWith(pVarName + "->") || varName.startsWith(pVarName + ".")) {
        result = result.assign(varName, TOP, pEdge);
      }
    }
    if (pValue instanceof Variable<?>) {
      String valueVarName = ((Variable<?>) pValue).getName();
      if (valueVarName.startsWith(pVarName + "->") || valueVarName.startsWith(pVarName + ".")) {
        return assign(pVarName, TOP, pEdge);
      }
      String pointerDerefPrefix = valueVarName + "->";
      String nonPointerDerefPrefix = valueVarName + ".";
      for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : this.environment.entrySet()) {
        if (entry.getKey().startsWith(pointerDerefPrefix)) {
          String suffix = entry.getKey().substring(pointerDerefPrefix.length());
          result = result.assign(pVarName + "->" + suffix, CompoundIntervalFormulaManager.INSTANCE.asVariable(entry.getKey()), pEdge);
        } else if (entry.getKey().startsWith(nonPointerDerefPrefix)) {
          String suffix = entry.getKey().substring(nonPointerDerefPrefix.length());
          result = result.assign(pVarName + "." + suffix, CompoundIntervalFormulaManager.INSTANCE.asVariable(entry.getKey()), pEdge);
        }
      }
      return result.assign(pVarName, pValue, pEdge, false);
    }
    return result.assign(pVarName, pValue, pEdge, false);
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
  private InvariantsState assign(String pVarName, InvariantsFormula<CompoundInterval> pValue, CFAEdge pEdge, boolean pIgnoreEdge) {
    Preconditions.checkNotNull(pValue);

    // Check if the assigned variable is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection = this.variableSelection.acceptAssignment(pVarName, pValue);
    if (newVariableSelection == null) {
      // Ensure that no information about the irrelevant assigned variable is retained
      Map<String, InvariantsFormula<CompoundInterval>> newEnvironment = this.environment;
      if (this.environment.containsKey(pVarName)) {
        newEnvironment = new HashMap<>(this.environment);
        newEnvironment.remove(pVarName);
      }
      if (this.environment == newEnvironment) {
        return this;
      }
      return from(newEnvironment,
          useBitvectors, variableSelection,
          machineModel,
          variableTypes);
    }

    CompoundIntervalFormulaManager ifm = CompoundIntervalFormulaManager.INSTANCE;
    Variable<CompoundInterval> variable = ifm.asVariable(pVarName);
    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>();

    // Optimization: If the value being assigned is equivalent to the value already stored, do nothing
    if (getEnvironmentValue(pVarName).equals(pValue)
        && (pValue instanceof Variable<?> || pValue instanceof Constant<?> && ((Constant<CompoundInterval>) pValue).getValue().isSingleton())
        || variable.accept(new StateEqualsVisitor(getFormulaResolver(), this.environment), pValue)) {
      return this;
    }

    // Avoid self-assignments if an equivalent alternative is available
    if (pValue.accept(containsVarVisitor, pVarName)) {
      InvariantsFormula<CompoundInterval> varValue = environment.get(pVarName);
      boolean isVarValueConstant = varValue instanceof Constant && ((Constant<CompoundInterval>) varValue).getValue().isSingleton();
      InvariantsFormula<CompoundInterval> alternative = varValue;
      if (!(alternative instanceof Variable)) {
        alternative = null;
        for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : environment.entrySet()) {
          InvariantsFormula<CompoundInterval> value = entry.getValue();
          if (!entry.getKey().equals(pVarName)
              && (value.equals(variable) || isVarValueConstant && value.equals(varValue))) {
            alternative = CompoundIntervalFormulaManager.INSTANCE.asVariable(entry.getKey());
            break;
          }
        }
      }
      if (alternative != null) {
        pValue = pValue.accept(new ReplaceVisitor<>(variable, alternative));
      }
      CompoundInterval value = pValue.accept(EVALUATION_VISITOR, environment);
      if (value.isSingleton()) {
        for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : environment.entrySet()) {
          InvariantsFormula<CompoundInterval> v = entry.getValue();
          if (v instanceof Constant && value.equals(((Constant<CompoundInterval>) v).getValue())) {
            pValue = CompoundIntervalFormulaManager.INSTANCE.asVariable(entry.getKey());
            break;
          }
        }
      }
    }


    InvariantsFormula<CompoundInterval> previousValue = getEnvironmentValue(pVarName);
    FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor = getFormulaResolver(pEdge);

    /*
     * A variable is newly assigned, so the appearances of this variable
     * in any previously collected assumptions (including its new value)
     * have to be resolved with the variable's previous value.
     */
    ReplaceVisitor<CompoundInterval> replaceVisitor = new ReplaceVisitor<>(variable, previousValue);

    // Try without widening first
    InvariantsState unwidened =
        assignInternal(pVarName, pValue, pEdge, newVariableSelection, EVALUATION_VISITOR, replaceVisitor);
    InvariantsState result = unwidened;

    // If widening is required, do so
    if (!evaluationVisitor.equals(EVALUATION_VISITOR)) {
      result = assignInternal(pVarName, pValue, pEdge, newVariableSelection, evaluationVisitor, replaceVisitor);

      // If this state covers the unwidened result, use this state as widening
      if (unwidened.isLessThanOrEqualTo(this) && !result.isLessThanOrEqualTo(this)) {
        return this;
      }
    }

    if (equals(result)) {
      return this;
    }
    return result;
  }

  private InvariantsState assignInternal(String pVarName, InvariantsFormula<CompoundInterval> pValue, CFAEdge pEdge,
      VariableSelection<CompoundInterval> newVariableSelection,
      FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor, ReplaceVisitor<CompoundInterval> replaceVisitor) {
    final InvariantsState result = new InvariantsState(useBitvectors, newVariableSelection, machineModel);
    result.variableTypes.putAll(variableTypes);

    for (Map.Entry<String, InvariantsFormula<CompoundInterval>> environmentEntry : this.environment.entrySet()) {
      if (!environmentEntry.getKey().equals(pVarName)) {
        InvariantsFormula<CompoundInterval> newEnvValue =
            environmentEntry.getValue().accept(replaceVisitor);
        result.environment.put(environmentEntry.getKey(), newEnvValue);
      }
    }
    result.environment.put(pVarName, pValue.accept(replaceVisitor));
    return result;
  }

  /**
   * Gets a state that has no information about the program and the same
   * information about the analysis as this state.
   *
   * @return a state that has no information about the program and the same
   * information about the analysis as this state.
   */
  public InvariantsState clear() {
    if (environment.isEmpty()) {
      return this;
    }
    return new InvariantsState(useBitvectors, variableSelection, machineModel);
  }

  /**
   * Gets the environment as a set equations of the variables with their values.
   *
   * @return the environment as a set equations of the variables with their values.
   */
  private Iterable<InvariantsFormula<CompoundInterval>> getEnvironmentAsAssumptions() {
    if (this.environmentAsAssumptions == null) {
      Set<InvariantsFormula<CompoundInterval>> environmentalAssumptions = new HashSet<>();
      CompoundIntervalFormulaManager ifm = CompoundIntervalFormulaManager.INSTANCE;

      List<InvariantsFormula<CompoundInterval>> atomic = new ArrayList<>(1);
      Deque<InvariantsFormula<CompoundInterval>> toCheck = new ArrayDeque<>(1);
      for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : this.environment.entrySet()) {
        InvariantsFormula<CompoundInterval> variable = ifm.asVariable(entry.getKey());

        atomic.clear();
        toCheck.clear();

        toCheck.add(entry.getValue());
        while (!toCheck.isEmpty()) {
          InvariantsFormula<CompoundInterval> current = toCheck.poll();
          if (current instanceof Union<?>) {
            Union<CompoundInterval> union = (Union<CompoundInterval>) current;
            toCheck.add(union.getOperand1());
            toCheck.add(union.getOperand2());
          } else {
            atomic.add(current);
          }
        }
        assert !atomic.isEmpty();
        Iterator<InvariantsFormula<CompoundInterval>> iterator = atomic.iterator();
        InvariantsFormula<CompoundInterval> equation = ifm.equal(variable, iterator.next());
        while (iterator.hasNext()) {
          equation = ifm.logicalOr(equation, ifm.equal(variable, iterator.next()));
        }

        environmentalAssumptions.add(equation);
      }
      environmentAsAssumptions = environmentalAssumptions;
    }
    return environmentAsAssumptions;
  }

  /**
   * Gets the value of the variable with the given name from the environment.
   * @param pVarName the name of the variable.
   * @return the value of the variable with the given name from the environment.
   */
  private InvariantsFormula<CompoundInterval> getEnvironmentValue(String pVarName) {
    InvariantsFormula<CompoundInterval> environmentValue = this.environment.get(pVarName);
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
  public FormulaEvaluationVisitor<CompoundInterval> getFormulaResolver(CFAEdge pEdge) {
    return getFormulaResolver();
  }

  /**
   * Gets an exact formula evaluation visitor.
   *
   * @return an exact formula evaluation visitor.
   */
  private FormulaEvaluationVisitor<CompoundInterval> getFormulaResolver() {
    return EVALUATION_VISITOR;
  }

  /**
   * Makes the given assumptions for this state and checks if this state is still valid.
   *
   * @param pAssumptions the assumptions to be made.
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions' correctness.
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>false</code> otherwise.
   */
  private boolean assumeInternal(Collection<? extends InvariantsFormula<CompoundInterval>> pAssumptions,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    for (InvariantsFormula<CompoundInterval> assumption : pAssumptions) {
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
  private boolean assumeInternal(InvariantsFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    InvariantsFormula<CompoundInterval> assumption = pAssumption.accept(this.partialEvaluator, pEvaluationVisitor);
    // If there are multiple assumptions combined with &&, split them up
    List<InvariantsFormula<CompoundInterval>> assumptionParts = assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) { return assumeInternal(assumptionParts, pEvaluationVisitor); }
    // If the assumption is top, it adds no value
    if (assumption.equals(TOP)) { return true; }

    if (assumption instanceof Constant<?>) {
      return !((Constant<CompoundInterval>) assumption).getValue().isDefinitelyFalse();
    }

    // If the assumption is an obvious contradiction, it cannot be validly assumed
    if (assumption.equals(BOTTOM)) { return false; }

    CompoundInterval assumptionEvaluation = assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (assumptionEvaluation.isDefinitelyFalse() || assumptionEvaluation.isBottom()) { return false; }
    // If the invariant evaluates to true, it adds no value for now
    if (assumptionEvaluation.isDefinitelyTrue()) { return true; }

    // If exact evaluation is enabled or the expression relates a maximum of one variable
    // to constants, then environment information may be gained
    if (!(pEvaluationVisitor instanceof FormulaAbstractionVisitor)
        || assumption.accept(COLLECT_VARS_VISITOR).size() <= 1) {
      PushAssumptionToEnvironmentVisitor patev =
          new PushAssumptionToEnvironmentVisitor(pEvaluationVisitor, this.environment);
      if (!assumption.accept(patev, CompoundInterval.logicalTrue())) {
        assert !assumptionEvaluation.isDefinitelyTrue();
        return false;
      }
      // Check all the assumption once more after the environment changed
      if (isDefinitelyFalse(assumption, pEvaluationVisitor)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if the given assumption is definitely false for this state.
   * @param pAssumption the assumption to evaluate.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate the assumption within this state's environment.
   * @return <code>true</code> if the given assumption does definitely not hold for this state's environment, <code>false</code> if it might.
   */
  private boolean isDefinitelyFalse(InvariantsFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return pAssumption.accept(pEvaluationVisitor, getEnvironment()).isDefinitelyFalse();
  }

  public InvariantsState assume(InvariantsFormula<CompoundInterval> pAssumption, CFAEdge pEdge) {
    // Check if at least one of the involved variables is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection = this.variableSelection.acceptAssumption(pAssumption);
    if (newVariableSelection == null) {
      return this;
    }
    FormulaEvaluationVisitor<CompoundInterval> evaluator = getFormulaResolver(pEdge);
    InvariantsFormula<CompoundInterval> assumption = pAssumption.accept(this.partialEvaluator, evaluator);
    if (assumption instanceof Constant<?>) {
      CompoundInterval value = ((Constant<CompoundInterval>) assumption).getValue();
      // An assumption evaluating to false represents an unreachable state; it can never be fulfilled
      if (value.isDefinitelyFalse()) { return null; }
      // An assumption representing nothing more than "true" or "maybe true" adds no information
      return this;
    }

    InvariantsState result = from(environment, useBitvectors,
        newVariableSelection, machineModel, variableTypes);
    if (result != null) {
      if (!result.assumeInternal(assumption, evaluator)) { return null; }
      if (equalsState(result)) {
        return this;
      }
    }
    return result;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
    FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor = getFormulaResolver();
    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeBoolean(true);
    ToFormulaVisitor<CompoundInterval, BooleanFormula> toBooleanFormulaVisitor =
        ToBooleanFormulaVisitor.getVisitor(pManager, evaluationVisitor, useBitvectors, machineModel, variableTypes);

    final Predicate<String> acceptVariable = new Predicate<String>() {

      @Override
      public boolean apply(@Nullable String pInput) {
        return pInput != null && !pInput.contains("*");
      }

    };

    final Predicate<InvariantsFormula<CompoundInterval>> acceptFormula = new Predicate<InvariantsFormula<CompoundInterval>>() {

      @Override
      public boolean apply(@Nullable InvariantsFormula<CompoundInterval> pInput) {
        return pInput != null
            && !pInput.equals(TOP)
            && FluentIterable.from(pInput.accept(COLLECT_VARS_VISITOR)).allMatch(acceptVariable);
      }

    };

    for (InvariantsFormula<CompoundInterval> assumption : getEnvironmentAsAssumptions()) {
      if (acceptFormula.apply(assumption)) {
        BooleanFormula assumptionFormula = assumption.accept(toBooleanFormulaVisitor, getEnvironment());
        if (assumptionFormula != null) {
          result = bfmgr.and(result, assumptionFormula);
        }
      }
    }
    return result;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) { return true; }
    if (!(pObj instanceof InvariantsState)) { return false; }
    return equalsState((InvariantsState) pObj);
  }

  private boolean equalsState(InvariantsState pOther) {
    return pOther != null && environment.equals(pOther.environment);
  }

  @Override
  public int hashCode() {
    int result = hash;
    if (result == 0) {
      result = 17;
      result = 31 * result + environment.hashCode();
      hash = result;
    }
    return result;
  }

  @Override
  public String toString() {
    return Joiner.on(", ").withKeyValueSeparator("=").join(environment);
  }

  /**
   * Gets the environment of this state.
   *
   * @return the environment of this state.
   */
  public Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> getEnvironment() {
    return Collections.unmodifiableMap(environment);
  }

  /**
   * Gets the flag indicating whether or not to use bit vectors to represent the states.
   *
   * @return the flag indicating whether or not to use bit vectors to represent the states.
   */
  private boolean getUseBitvectors() {
    return useBitvectors;
  }

  public boolean isLessThanOrEqualTo(InvariantsState pElement2) {
    if (pElement2 == this) { return true; }
    if (pElement2 == null) {
      return false;
    }
    // Perform the implication check (if this state definitely implies the other one, it is less than or equal to it)
    for (InvariantsFormula<CompoundInterval> rightAssumption : pElement2.getEnvironmentAsAssumptions()) {
      if (!definitelyImplies(rightAssumption)) {
        return false;
      }
    }
    return true;
  }

  public boolean definitelyImplies(InvariantsFormula<CompoundInterval> pFormula) {
    return CompoundIntervalFormulaManager.definitelyImplies(this.environment, pFormula);
  }

  private InvariantsState widen(InvariantsState pOlderState, InvariantsPrecision pPrecision) {
    InvariantsState result = from(environment, useBitvectors, variableSelection, machineModel, variableTypes);
    Map<String, InvariantsFormula<CompoundInterval>> toDo = new HashMap<>();
    for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : this.environment.entrySet()) {
      String varName = entry.getKey();
      InvariantsFormula<CompoundInterval> currentFormula = entry.getValue();
      if (currentFormula.accept(FORMULA_DEPTH_COUNT_VISITOR) > pPrecision.getMaximumFormulaDepth()) {
        InvariantsFormula<CompoundInterval> oldFormula = pOlderState.getEnvironmentValue(varName);
        if (oldFormula != null && !currentFormula.equals(oldFormula)) {
          InvariantsFormula<CompoundInterval> newValueFormula =
          CompoundIntervalFormulaManager.INSTANCE.union(
              currentFormula.accept(this.partialEvaluator, EVALUATION_VISITOR),
              oldFormula.accept(pOlderState.partialEvaluator, EVALUATION_VISITOR)).accept(new PartialEvaluator(), EVALUATION_VISITOR);
          if (newValueFormula.accept(FORMULA_DEPTH_COUNT_VISITOR) > pPrecision.getMaximumFormulaDepth()) {
            result.environment.put(varName, newValueFormula);
            toDo.put(varName, newValueFormula);
          }
        }
      }
    }
    if (toDo.isEmpty()) {
      return this;
    }
    for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : toDo.entrySet()) {
      String varName = entry.getKey();
      InvariantsFormula<CompoundInterval> newValueFormula = entry.getValue();
      CompoundInterval simpleExactValue = newValueFormula.accept(EVALUATION_VISITOR, result.environment);
      if (simpleExactValue.isSingleton()) {
        result.environment.put(varName, CompoundIntervalFormulaManager.INSTANCE.asConstant(simpleExactValue));
      } else {
        InvariantsFormula<CompoundInterval> oldFormula = pOlderState.getEnvironmentValue(varName);
        InvariantsFormula<CompoundInterval> currentFormula = getEnvironmentValue(varName);
        CompoundInterval oldExactValue = oldFormula.accept(EVALUATION_VISITOR, pOlderState.environment);
        CompoundInterval currentExactValue = currentFormula.accept(EVALUATION_VISITOR, environment);
        final CompoundInterval newValue;
        if (oldExactValue.equals(currentExactValue)) {
          newValue = currentExactValue;
        } else if (oldExactValue.lessEqual(currentExactValue).isDefinitelyTrue()) {
          newValue = oldExactValue.extendToPositiveInfinity();
        } else if (oldExactValue.greaterEqual(currentExactValue).isDefinitelyTrue()) {
          newValue = oldExactValue.extendToNegativeInfinity();
        } else {
          newValue = result.getEnvironmentValue(varName).accept(ABSTRACTION_VISITOR, result.environment);
        }
        result.environment.put(varName, CompoundIntervalFormulaManager.INSTANCE.asConstant(newValue));
      }
    }
    if (equals(result)) {
      return this;
    }
    return result;
  }

  public InvariantsState join(InvariantsState pState2, InvariantsPrecision pPrecision) {
    Preconditions.checkArgument(pState2.useBitvectors == useBitvectors);

    InvariantsState state1 = widen(pState2, pPrecision);
    InvariantsState state2 = pState2;

    InvariantsState result;

    if (isLessThanOrEqualTo(state2)) {
      result = state2;
    } else if (state2.isLessThanOrEqualTo(state1)) {
      result = state1;
    } else {

      Map<String, InvariantsFormula<CompoundInterval>> resultEnvironment = new NonRecursiveEnvironment();

      // Get some basic information by joining the environments
      for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : state1.environment.entrySet()) {
        String varName = entry.getKey();
        InvariantsFormula<CompoundInterval> rightFormula = state2.environment.get(varName);
        if (rightFormula != null) {
          InvariantsFormula<CompoundInterval> newValueFormula =
              CompoundIntervalFormulaManager.INSTANCE.union(
                  entry.getValue().accept(state1.partialEvaluator, EVALUATION_VISITOR),
                  rightFormula.accept(state2.partialEvaluator, EVALUATION_VISITOR)).accept(new PartialEvaluator(), EVALUATION_VISITOR);
          resultEnvironment.put(varName, newValueFormula);
        }
      }

      VariableSelection<CompoundInterval> resultVariableSelection = state1.variableSelection.join(state2.variableSelection);

      Map<String, CType> variableTypes = new HashMap<>(state1.variableTypes);
      variableTypes.putAll(state2.variableTypes);

      result = InvariantsState.from(resultEnvironment, state1.getUseBitvectors(), resultVariableSelection,
          machineModel, variableTypes);

      if (result != null) {
        if (result.equalsState(state1)) {
          result = state1;
        }
      }
    }
    return result;
  }

  static interface EdgeBasedAbstractionStrategy {

    public boolean useAbstraction(CFAEdge pEdge);

    public EdgeBasedAbstractionStrategy addVisitedEdge(CFAEdge pEdge);

    public EdgeBasedAbstractionStrategy join(EdgeBasedAbstractionStrategy pStrategy);

  }

  static interface AbstractEdgeBasedAbstractionStrategyFactory {

    public EdgeBasedAbstractionStrategy getAbstractionStrategy();

  }

  private static enum BasicAbstractionStrategies implements EdgeBasedAbstractionStrategy {

    ALWAYS {

      @Override
      public boolean useAbstraction(CFAEdge pEdge) {
        return true;
      }

      @Override
      public EdgeBasedAbstractionStrategy addVisitedEdge(CFAEdge pEdge) {
        return this;
      }

      @Override
      public EdgeBasedAbstractionStrategy join(EdgeBasedAbstractionStrategy pStrategy) {
        return this;
      }

    },

    NEVER {

      @Override
      public boolean useAbstraction(CFAEdge pEdge) {
        return false;
      }

      @Override
      public EdgeBasedAbstractionStrategy addVisitedEdge(CFAEdge pEdge) {
        return this;
      }

      @Override
      public EdgeBasedAbstractionStrategy join(EdgeBasedAbstractionStrategy pStrategy) {
        if (pStrategy == this) {
          return this;
        }
        return pStrategy.join(this);
      }

    };

  }

  static enum EdgeBasedAbstractionStrategyFactories implements AbstractEdgeBasedAbstractionStrategyFactory {

    ALWAYS {

      @Override
      public EdgeBasedAbstractionStrategy getAbstractionStrategy() {
        return BasicAbstractionStrategies.ALWAYS;
      }

    },

    VISITED_EDGES {

      @Override
      public EdgeBasedAbstractionStrategy getAbstractionStrategy() {
        class VisitedEdgesBasedAbstractionStrategy implements EdgeBasedAbstractionStrategy {

          private final ImmutableSet<CFAEdge> visitedEdges;

          private VisitedEdgesBasedAbstractionStrategy() {
            this(ImmutableSet.<CFAEdge>of());
          }

          private VisitedEdgesBasedAbstractionStrategy(ImmutableSet<CFAEdge> pVisitedEdges) {
            this.visitedEdges = pVisitedEdges;
          }

          @Override
          public boolean useAbstraction(CFAEdge pEdge) {
            return visitedEdges.contains(pEdge);
          }

          @Override
          public EdgeBasedAbstractionStrategy addVisitedEdge(CFAEdge pEdge) {
            if (visitedEdges.contains(pEdge)) {
              return this;
            }
            return new VisitedEdgesBasedAbstractionStrategy(ImmutableSet.<CFAEdge>builder().addAll(visitedEdges).add(pEdge).build());
          }

          @Override
          public EdgeBasedAbstractionStrategy join(EdgeBasedAbstractionStrategy pStrategy) {
            if (pStrategy == BasicAbstractionStrategies.NEVER || pStrategy == this) {
              return this;
            }
            if (pStrategy instanceof VisitedEdgesBasedAbstractionStrategy) {
              VisitedEdgesBasedAbstractionStrategy other = (VisitedEdgesBasedAbstractionStrategy) pStrategy;
              if (this.visitedEdges == other.visitedEdges || other.visitedEdges.containsAll(this.visitedEdges)) {
                return other;
              }
              if (this.visitedEdges.containsAll(other.visitedEdges)) {
                return this;
              }
              final ImmutableSet<CFAEdge> edges =
                  ImmutableSet.<CFAEdge>builder().addAll(visitedEdges).addAll(other.visitedEdges).build();
              return new VisitedEdgesBasedAbstractionStrategy(edges);
            }
            return BasicAbstractionStrategies.ALWAYS;
          }

          @Override
          public boolean equals(Object pO) {
            if (this == pO) {
              return true;
            }
            if (pO instanceof VisitedEdgesBasedAbstractionStrategy) {
              return visitedEdges.equals(((VisitedEdgesBasedAbstractionStrategy) pO).visitedEdges);
            }
            return false;
          }

          @Override
          public int hashCode() {
            return visitedEdges.hashCode();
          }

        }
        return new VisitedEdgesBasedAbstractionStrategy();
      }

    },

    NEVER {

      @Override
      public EdgeBasedAbstractionStrategy getAbstractionStrategy() {
        return BasicAbstractionStrategies.NEVER;
      }

    };

  }

}
