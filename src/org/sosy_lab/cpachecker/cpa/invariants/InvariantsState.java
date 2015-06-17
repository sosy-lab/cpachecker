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
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Exclusion;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

/**
 * Instances of this class represent states in the light-weight invariants analysis.
 */
public class InvariantsState implements AbstractState, FormulaReportingState,
    LatticeAbstractState<InvariantsState> {

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

  private static final Predicate<? super String> IS_UNSUPPORTED_VARIABLE = new Predicate<String>() {

      @Override
      public boolean apply(String pArg0) {
        return pArg0 == null || pArg0.contains("[");
      }};

  private final Predicate<InvariantsFormula<CompoundInterval>> implies = new Predicate<InvariantsFormula<CompoundInterval>>() {

    @Override
    public boolean apply(InvariantsFormula<CompoundInterval> pArg0) {
      return definitelyImplies(pArg0);
    }

  };

  /**
   * The environment currently known to the state.
   */
  private final NonRecursiveEnvironment environment;

  /**
   * The variables selected for this analysis.
   */
  private final VariableSelection<CompoundInterval> variableSelection;

  private final PersistentSortedMap<String, CType> variableTypes;

  private final PartialEvaluator partialEvaluator;

  private final MachineModel machineModel;

  private final AbstractionState abstractionState;

  private Iterable<InvariantsFormula<CompoundInterval>> environmentAsAssumptions;

  private volatile int hash = 0;

  public InvariantsState(VariableSelection<CompoundInterval> pVariableSelection,
      MachineModel pMachineModel,
      InvariantsState pInvariant,
      AbstractionState pAbstractionState) {
    this.environment = pInvariant.environment;
    this.partialEvaluator = pInvariant.partialEvaluator;
    this.variableSelection = pVariableSelection;
    this.variableTypes = pInvariant.variableTypes;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
  }

  /**
   * Creates a new invariants state with a selection of
   * variables, and the machine model used.
   *
   * @param pVariableSelection the selected variables.
   * @param pMachineModel the machine model used.
   */
  public InvariantsState(VariableSelection<CompoundInterval> pVariableSelection,
      MachineModel pMachineModel,
      AbstractionState pAbstractionState) {
    this.environment = NonRecursiveEnvironment.of();
    this.partialEvaluator = new PartialEvaluator(this.environment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = PathCopyingPersistentTreeMap.of();
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
  }

  /**
   * Creates a new invariants state with the given data, reusing the given
   * instance of the environment without copying.
   *
   * @param pVariableSelection the selected variables.
   * @param pMachineModel the machine model used.
   * @param pAbstractionState the abstraction state.
   * @param pEnvironment the environment. This instance is reused and not copied.
   * @param pVariableTypes the variable types.
   */
  private InvariantsState(VariableSelection<CompoundInterval> pVariableSelection,
      MachineModel pMachineModel,
      AbstractionState pAbstractionState,
      NonRecursiveEnvironment pEnvironment,
      PersistentSortedMap<String, CType> pVariableTypes) {
    this.environment = pEnvironment;
    this.partialEvaluator = new PartialEvaluator(this.environment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = pVariableTypes;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
  }

  /**
   * Creates a new invariants state with a selection of variables, the machine
   * model used, the given variable types and the given abstraction state.
   *
   * @param pVariableSelection the selected variables.
   * @param pMachineModel the machine model used.
   * @param pVariableTypes the variable types.
   * @param pAbstractionState the abstraction state.
   */
  private InvariantsState(Map<String, InvariantsFormula<CompoundInterval>> pEnvironment,
      VariableSelection<CompoundInterval> pVariableSelection,
      MachineModel pMachineModel,
      PersistentSortedMap<String, CType> pVariableTypes,
      AbstractionState pAbstractionState) {
    this.environment = NonRecursiveEnvironment.copyOf(pEnvironment);
    this.partialEvaluator = new PartialEvaluator(pEnvironment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = pVariableTypes;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
  }

  private AbstractionState determineAbstractionState(AbstractionState pMasterState) {
    AbstractionState state = pMasterState;
    if (state.getClass() == abstractionState.getClass()) {
      state = abstractionState.join(state);
    }
    return state;
  }

  public AbstractionState determineAbstractionState(InvariantsPrecision pPrecision) {
    return determineAbstractionState(
        pPrecision.getAbstractionStateFactory()
        .from(abstractionState));
  }

  public InvariantsState updateAbstractionState(InvariantsPrecision pPrecision, CFAEdge pEdge) {
    AbstractionState state =
        pPrecision.getAbstractionStateFactory()
        .getSuccessorState(abstractionState);
    state = state.addEnteringEdge(pEdge);
    if (state.equals(abstractionState)) {
      return this;
    }
    return new InvariantsState(environment, variableSelection, machineModel, variableTypes, state);
  }

  public InvariantsState setType(String pVarName, CType pType) {
    if (pType.equals(variableTypes.get(pVarName))) {
      return this;
    }
    return new InvariantsState(variableSelection, machineModel, abstractionState, environment, variableTypes.putAndCopy(pVarName, pType));
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
    PersistentSortedMap<String, CType> variableTypes = this.variableTypes;
    for (Map.Entry<String, CType> entry : pVarTypes.entrySet()) {
      String variableName = entry.getKey();
      if (!variableTypes.containsKey(variableName)) {
        variableTypes = variableTypes.putAndCopy(variableName, entry.getValue());
      }
    }
    return new InvariantsState(variableSelection, machineModel, abstractionState, environment, variableTypes);
  }

  public InvariantsState assignArray(String pArray, InvariantsFormula<CompoundInterval> pSubscript, InvariantsFormula<CompoundInterval> pValue) {
    FormulaEvaluationVisitor<CompoundInterval> fev = getFormulaResolver();
    CompoundInterval value = pSubscript.accept(fev, this.environment);
    if (value.isSingleton()) { // Exact subscript value is known
      return assignInternal(pArray + "[" + value.getValue() + "]", pValue);
    } else { // Multiple subscript values are possible: All possible subscript targets are now unknown
      InvariantsState result = this;
      for (String varName : this.environment.keySet()) {
        String prefix = pArray + "[";
        if (varName.startsWith(prefix)) {
          String subscriptValueStr = varName.replace(prefix, "").replaceAll("].*", "");
          if (subscriptValueStr.equals("*") || value.contains(new BigInteger(subscriptValueStr))) {
            result = result.assignInternal(varName, TOP);
          }
        }
      }
      return result;
    }
  }

  public InvariantsState assign(String pVarName, InvariantsFormula<CompoundInterval> pValue) {
    InvariantsState result = this;
    for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : this.environment.entrySet()) {
      String varName = entry.getKey();
      if (varName.startsWith(pVarName + "->") || varName.startsWith(pVarName + ".")) {
        result = result.assign(varName, TOP);
      }
    }
    if (pValue instanceof Variable<?>) {
      String valueVarName = ((Variable<?>) pValue).getName();
      if (valueVarName.startsWith(pVarName + "->") || valueVarName.startsWith(pVarName + ".")) {
        return assign(pVarName, TOP);
      }
      String pointerDerefPrefix = valueVarName + "->";
      String nonPointerDerefPrefix = valueVarName + ".";
      for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : this.environment.entrySet()) {
        if (entry.getKey().startsWith(pointerDerefPrefix)) {
          String suffix = entry.getKey().substring(pointerDerefPrefix.length());
          result = result.assign(pVarName + "->" + suffix, CompoundIntervalFormulaManager.INSTANCE.asVariable(entry.getKey()));
        } else if (entry.getKey().startsWith(nonPointerDerefPrefix)) {
          String suffix = entry.getKey().substring(nonPointerDerefPrefix.length());
          result = result.assign(pVarName + "." + suffix, CompoundIntervalFormulaManager.INSTANCE.asVariable(entry.getKey()));
        }
      }
      return result.assignInternal(pVarName, pValue);
    }
    return result.assignInternal(pVarName, pValue);
  }

  /**
   * Creates a new state representing the given assignment applied to the current state.
   *
   * @param pVarName the name of the variable being assigned.
   * @param pValue the new value of the variable.
   * @return a new state representing the given assignment applied to the current state.
   */
  private InvariantsState assignInternal(String pVarName, InvariantsFormula<CompoundInterval> pValue) {
    Preconditions.checkNotNull(pValue);
    // Only use information from supported variables
    if (IS_UNSUPPORTED_VARIABLE.apply(pVarName)) {
      return this;
    }
    if (FluentIterable.from(pValue.accept(new CollectVarsVisitor<CompoundInterval>())).anyMatch(IS_UNSUPPORTED_VARIABLE)) {
      return this;
    }

    // Check if the assigned variable is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection = this.variableSelection.acceptAssignment(pVarName, pValue);
    if (newVariableSelection == null) {
      // Ensure that no information about the irrelevant assigned variable is retained
      NonRecursiveEnvironment newEnvironment = this.environment;
      if (this.environment.containsKey(pVarName)) {
        newEnvironment = newEnvironment.removeAndCopy(pVarName);
      }
      if (this.environment == newEnvironment) {
        return this;
      }
      return new InvariantsState(newEnvironment,
          variableSelection,
          machineModel,
          variableTypes,
          abstractionState);
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

    /*
     * A variable is newly assigned, so the appearances of this variable
     * in any previously collected assumptions (including its new value)
     * have to be resolved with the variable's previous value.
     */
    ReplaceVisitor<CompoundInterval> replaceVisitor = new ReplaceVisitor<>(variable, previousValue);

    // Compute the assignment
    InvariantsState result = assignInternal(pVarName, pValue, newVariableSelection, EVALUATION_VISITOR, replaceVisitor);

    if (equals(result)) {
      return this;
    }
    return result;
  }

  private InvariantsState assignInternal(String pVarName, InvariantsFormula<CompoundInterval> pValue,
      VariableSelection<CompoundInterval> newVariableSelection,
      FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor, ReplaceVisitor<CompoundInterval> replaceVisitor) {
    NonRecursiveEnvironment resultEnvironment = this.environment;

    for (Map.Entry<String, InvariantsFormula<CompoundInterval>> environmentEntry : this.environment.entrySet()) {
      if (!environmentEntry.getKey().equals(pVarName)) {
        InvariantsFormula<CompoundInterval> newEnvValue =
            environmentEntry.getValue().accept(replaceVisitor).accept(partialEvaluator, EVALUATION_VISITOR);
        resultEnvironment = resultEnvironment.putAndCopy(environmentEntry.getKey(), newEnvValue);
      }
    }
    resultEnvironment = resultEnvironment.putAndCopy(pVarName, pValue.accept(replaceVisitor).accept(partialEvaluator, EVALUATION_VISITOR));
    return new InvariantsState(newVariableSelection, machineModel, abstractionState, resultEnvironment, variableTypes);
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
    return new InvariantsState(variableSelection, machineModel, abstractionState);
  }

  /**
   * Removes the value stored for the given variable.
   *
   * @param pVariableName the variable to remove.
   *
   * @return the new state.
   */
  public InvariantsState clear(String pVariableName) {
    if (environment.get(pVariableName) == null) {
      return this;
    }
    return new InvariantsState(environment.removeAndCopy(pVariableName), variableSelection, machineModel, variableTypes, abstractionState);
  }

  /**
   * Gets the environment as a set equations of the variables with their values.
   *
   * @return the environment as a set equations of the variables with their values.
   */
  public Iterable<InvariantsFormula<CompoundInterval>> getEnvironmentAsAssumptions() {
    if (this.environmentAsAssumptions == null) {
      environmentAsAssumptions = getEnvironmentAsAssumptions(this.environment);
    }
    return environmentAsAssumptions;
  }

  private static Iterable<InvariantsFormula<CompoundInterval>> getEnvironmentAsAssumptions(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    Set<InvariantsFormula<CompoundInterval>> environmentalAssumptions = new HashSet<>();
    CompoundIntervalFormulaManager ifm = CompoundIntervalFormulaManager.INSTANCE;

    List<InvariantsFormula<CompoundInterval>> atomic = new ArrayList<>(1);
    Deque<InvariantsFormula<CompoundInterval>> toCheck = new ArrayDeque<>(1);
    for (Entry<? extends String, ? extends InvariantsFormula<CompoundInterval>> entry : pEnvironment.entrySet()) {
      InvariantsFormula<CompoundInterval> variable = ifm.asVariable(entry.getKey());

      InvariantsFormula<CompoundInterval> value = entry.getValue();

      boolean isExclusion = false;
      if (value instanceof Exclusion) {
        isExclusion = true;
        value = ((Exclusion<CompoundInterval>) value).getExcluded();
      }

      atomic.clear();
      toCheck.clear();

      toCheck.add(value);
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
      InvariantsFormula<CompoundInterval> assumption = ifm.asConstant(CompoundInterval.logicalFalse());
      while (iterator.hasNext()) {
        InvariantsFormula<CompoundInterval> equation = ifm.equal(variable, iterator.next());
        if (isExclusion) {
          equation = ifm.logicalNot(equation);
        }
        assumption = ifm.logicalOr(assumption, equation);
      }

      environmentalAssumptions.add(assumption);
    }
    return environmentalAssumptions;
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
   * Gets an exact formula evaluation visitor.
   *
   * @return an exact formula evaluation visitor.
   */
  public FormulaEvaluationVisitor<CompoundInterval> getFormulaResolver() {
    return EVALUATION_VISITOR;
  }

  /**
   * Makes the given assumptions for this state and checks if this state is still valid.
   *
   * @param pAssumptions the assumptions to be made.
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions' correctness.
   * @param pNewVariableSelection
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>false</code> otherwise.
   */
  private InvariantsState assumeInternal(Collection<? extends InvariantsFormula<CompoundInterval>> pAssumptions,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor, VariableSelection<CompoundInterval> pNewVariableSelection) {
    InvariantsState result = this;
    for (InvariantsFormula<CompoundInterval> assumption : pAssumptions) {
      result = assumeInternal(assumption, pEvaluationVisitor, pNewVariableSelection);
      if (result == null) {
        return null;
      }
    }
    return result;
  }

  /**
   * Makes the given assumption for this state and checks if this state is still valid.
   *
   * @param pAssumption the assumption to be made.
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions' correctness.
   * @param pNewVariableSelection
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>false</code> otherwise.
   */
  private InvariantsState assumeInternal(InvariantsFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor, VariableSelection<CompoundInterval> pNewVariableSelection) {
    InvariantsFormula<CompoundInterval> assumption = pAssumption.accept(this.partialEvaluator, pEvaluationVisitor);
    // If there are multiple assumptions combined with &&, split them up
    List<InvariantsFormula<CompoundInterval>> assumptionParts = assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) { return assumeInternal(assumptionParts, pEvaluationVisitor, pNewVariableSelection); }
    // If the assumption is top, it adds no value
    if (assumption.equals(TOP)) { return this; }

    // Only use information from supported variables
    if (FluentIterable.from(assumption.accept(new CollectVarsVisitor<CompoundInterval>())).anyMatch(IS_UNSUPPORTED_VARIABLE)) {
      return this;
    }

    if (assumption instanceof Constant<?>) {
      return !((Constant<CompoundInterval>) assumption).getValue().isDefinitelyFalse() ? this : null;
    }

    // If the assumption is an obvious contradiction, it cannot be validly assumed
    if (assumption.equals(BOTTOM)) { return null; }

    CompoundInterval assumptionEvaluation = assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (assumptionEvaluation.isDefinitelyFalse() || assumptionEvaluation.isBottom()) { return null; }
    // If the invariant evaluates to true, it adds no value for now
    if (assumptionEvaluation.isDefinitelyTrue()) { return this; }

    NonRecursiveEnvironment.Builder environmentBuilder = new NonRecursiveEnvironment.Builder(this.environment);
    PushAssumptionToEnvironmentVisitor patev =
        new PushAssumptionToEnvironmentVisitor(pEvaluationVisitor, environmentBuilder);
    if (!assumption.accept(patev, CompoundInterval.logicalTrue())) {
      assert !assumptionEvaluation.isDefinitelyTrue();
      return null;
    }
    // Check all the assumption once more after the environment changed
    if (isDefinitelyFalse(assumption, pEvaluationVisitor)) {
      return null;
    }
    return new InvariantsState(environmentBuilder.build(), pNewVariableSelection, machineModel, variableTypes, abstractionState);
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

  public InvariantsState assume(InvariantsFormula<CompoundInterval> pAssumption) {
    // Check if at least one of the involved variables is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection = this.variableSelection.acceptAssumption(pAssumption);
    if (newVariableSelection == null) {
      return this;
    }
    FormulaEvaluationVisitor<CompoundInterval> evaluator = getFormulaResolver();
    InvariantsFormula<CompoundInterval> assumption = pAssumption.accept(this.partialEvaluator, evaluator);
    if (assumption instanceof Constant<?>) {
      CompoundInterval value = ((Constant<CompoundInterval>) assumption).getValue();
      // An assumption evaluating to false represents an unreachable state; it can never be fulfilled
      if (value.isDefinitelyFalse()) { return null; }
      // An assumption representing nothing more than "true" or "maybe true" adds no information
      return this;
    }

    InvariantsState result = assumeInternal(assumption, evaluator, newVariableSelection);
    if (equalsState(result)) {
      return this;
    }
    return result;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pManager, PathFormulaManager pfmgr) {
    FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor = getFormulaResolver();
    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeBoolean(true);
    ToFormulaVisitor<CompoundInterval, BooleanFormula> toBooleanFormulaVisitor =
        ToBooleanFormulaVisitor.getVisitor(pManager, evaluationVisitor, machineModel, variableTypes);

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
            && FluentIterable.from(CompoundIntervalFormulaManager.collectVariableNames(pInput)).allMatch(acceptVariable);
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
    return pOther != null
        && environment.equals(pOther.environment)
        && abstractionState.equals(pOther.abstractionState);
  }

  @Override
  public int hashCode() {
    int result = hash;
    if (result == 0) {
      result = 17;
      result = 31 * result + environment.hashCode();
      result = 31 * result + abstractionState.hashCode();
      hash = result;
    }
    return result;
  }

  @Override
  public String toString() {
    return FluentIterable.from(environment.entrySet()).transform(new Function<Map.Entry<String, InvariantsFormula<CompoundInterval>>, String>() {

      @Override
      public String apply(Entry<String, InvariantsFormula<CompoundInterval>> pInput) {
        String variableName = pInput.getKey();
        InvariantsFormula<?> value = pInput.getValue();
        if (value instanceof Exclusion) {
          return String.format("%s\u2260%s", variableName, ((Exclusion<?>) value).getExcluded());
        }
        return String.format("%s=%s", variableName, value);
      }

    }).join(Joiner.on(", "));
  }

  public AbstractionState getAbstractionState() {
    return abstractionState;
  }

  /**
   * Gets the environment of this state.
   *
   * @return the environment of this state.
   */
  public Map<String, InvariantsFormula<CompoundInterval>> getEnvironment() {
    return Collections.unmodifiableMap(environment);
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  @Override
  public boolean isLessOrEqual(InvariantsState pState2) {
    if (equals(pState2)) { return true; }
    if (pState2 == null) {
      return false;
    }
    if (!abstractionState.isLessThanOrEqualTo(pState2.abstractionState)) {
      return false;
    }
    // Perform the implication check (if this state definitely implies the other one, it is less than or equal to it)
    for (InvariantsFormula<CompoundInterval> rightAssumption : pState2.getEnvironmentAsAssumptions()) {
      if (!definitelyImplies(rightAssumption)) {
        return false;
      }
    }
    return true;
  }

  public boolean definitelyImplies(InvariantsFormula<CompoundInterval> pFormula) {
    return CompoundIntervalFormulaManager.definitelyImplies(this.environment, pFormula);
  }

  public InvariantsState widen(InvariantsState pOlderState,
      @Nullable InvariantsPrecision pPrecision,
      Set<String> pWideningTargets,
      Set<InvariantsFormula<CompoundInterval>> pWideningHints) {

    Set<String> wideningTargets = pWideningTargets == null ? environment.keySet() : pWideningTargets;

    if (wideningTargets.isEmpty()) {
      return this;
    }

    // Prepare result environment
    NonRecursiveEnvironment resultEnvironment = this.environment;

    // Find entries that require widening
    Map<String, InvariantsFormula<CompoundInterval>> toDo = new HashMap<>();
    for (String varName : wideningTargets) {
      InvariantsFormula<CompoundInterval> currentFormula = getEnvironmentValue(varName);
      InvariantsFormula<CompoundInterval> oldFormula = pOlderState.getEnvironmentValue(varName);
      if (oldFormula != null && (!currentFormula.equals(oldFormula)
          || currentFormula.accept(FORMULA_DEPTH_COUNT_VISITOR) > pPrecision.getMaximumFormulaDepth())) {
        InvariantsFormula<CompoundInterval> newValueFormula =
          CompoundIntervalFormulaManager.INSTANCE.union(
            currentFormula.accept(this.partialEvaluator, EVALUATION_VISITOR),
            oldFormula.accept(pOlderState.partialEvaluator, EVALUATION_VISITOR)).accept(new PartialEvaluator(), EVALUATION_VISITOR);

        // Allow only (singleton) constants for formula depth 0
        if (pPrecision.getMaximumFormulaDepth() == 0) {
          CompoundInterval value = currentFormula.accept(EVALUATION_VISITOR, environment)
              .unionWith(oldFormula.accept(EVALUATION_VISITOR, pOlderState.getEnvironment()));
          if (!value.isSingleton()) {
            value = CompoundInterval.top();
          }
          newValueFormula = CompoundIntervalFormulaManager.INSTANCE.asConstant(value);
        }

        resultEnvironment = resultEnvironment.putAndCopy(varName, newValueFormula);
        toDo.put(varName, newValueFormula);
      }
    }
    if (toDo.isEmpty()) {
      return this;
    }
    for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : toDo.entrySet()) {
      String varName = entry.getKey();
      InvariantsFormula<CompoundInterval> newValueFormula = entry.getValue();
      CompoundInterval simpleExactValue = newValueFormula.accept(EVALUATION_VISITOR, resultEnvironment);
      if (simpleExactValue.isSingleton()) {
        resultEnvironment = resultEnvironment.putAndCopy(varName, CompoundIntervalFormulaManager.INSTANCE.asConstant(simpleExactValue));
      } else {
        InvariantsFormula<CompoundInterval> oldFormula = pOlderState.getEnvironmentValue(varName);
        InvariantsFormula<CompoundInterval> currentFormula = getEnvironmentValue(varName);
        CompoundInterval oldExactValue = oldFormula.accept(EVALUATION_VISITOR, pOlderState.environment);
        CompoundInterval currentExactValue = currentFormula.accept(EVALUATION_VISITOR, environment);
        final CompoundInterval newValue;
        if (oldExactValue.contains(currentExactValue)) {
          newValue = oldExactValue;
        } else if (oldExactValue.lessEqual(currentExactValue).isDefinitelyTrue()
            || oldExactValue.hasUpperBound() && (!currentExactValue.hasUpperBound() || oldExactValue.getUpperBound().compareTo(currentExactValue.getUpperBound()) < 0)) {
          newValue = oldExactValue.unionWith(currentExactValue).extendToPositiveInfinity();
        } else if (oldExactValue.greaterEqual(currentExactValue).isDefinitelyTrue()
            || oldExactValue.hasLowerBound() && (!currentExactValue.hasLowerBound() || oldExactValue.getLowerBound().compareTo(currentExactValue.getLowerBound()) > 0)) {
          newValue = oldExactValue.unionWith(currentExactValue).extendToNegativeInfinity();
        } else {
          InvariantsFormula<CompoundInterval> newFormula = resultEnvironment.get(varName);
          if (newFormula == null) {
            newFormula = TOP;
          }
          newValue = newFormula.accept(ABSTRACTION_VISITOR, resultEnvironment);
        }
        resultEnvironment = resultEnvironment.putAndCopy(varName, CompoundIntervalFormulaManager.INSTANCE.asConstant(newValue));
      }
    }
    InvariantsState result = new InvariantsState(resultEnvironment, variableSelection, machineModel, variableTypes, abstractionState);

    for (InvariantsFormula<CompoundInterval> hint : FluentIterable.from(pWideningHints).filter(this.implies)) {
      result = result.assume(hint);
    }
    if (equals(result)) {
      return this;
    }

    return result;
  }

  @Override
  public InvariantsState join(InvariantsState state2) {
    return join(state2, InvariantsPrecision.getEmptyPrecision());
  }

  public InvariantsState join(InvariantsState pState2, InvariantsPrecision pPrecision) {

    InvariantsState result;

    InvariantsState state1 = this;
    InvariantsState state2 = pState2;

    if (state1.isLessOrEqual(state2)) {
      result = state2;
    } else if (state2.isLessOrEqual(state1)) {
      result = state1;
    } else {
      NonRecursiveEnvironment resultEnvironment = NonRecursiveEnvironment.of();

      // Get some basic information by joining the environments
      {
        Set<String> todo = new HashSet<>();

        // Join the easy ones first (both values equal or one value top)
        for (Map.Entry<String, InvariantsFormula<CompoundInterval>> entry : state1.environment.entrySet()) {
          String varName = entry.getKey();
          InvariantsFormula<CompoundInterval> rightFormula = state2.environment.get(varName);
          if (rightFormula != null) {
            InvariantsFormula<CompoundInterval> leftFormula = getEnvironmentValue(varName);
            if (leftFormula.equals(rightFormula)) {
              resultEnvironment = resultEnvironment.putAndCopy(varName, leftFormula);
            } else {
              todo.add(varName);
            }
          }
        }

        // Join the harder ones
        {
          // Join all those where one implies the other one
          boolean cont = !todo.isEmpty();
          Set<String> done = new HashSet<>();
          while (cont) {
            cont = false;
            Iterable<InvariantsFormula<CompoundInterval>> assumptions = getEnvironmentAsAssumptions(resultEnvironment);
            for (String varName : todo) {
              InvariantsFormula<CompoundInterval> leftFormula = getEnvironmentValue(varName);
              InvariantsFormula<CompoundInterval> rightFormula = state2.getEnvironmentValue(varName);
              assert leftFormula != null && rightFormula != null;
              InvariantsFormula<CompoundInterval> union = CompoundIntervalFormulaManager.INSTANCE.union(
                  leftFormula.accept(state1.partialEvaluator, EVALUATION_VISITOR),
                  rightFormula.accept(state2.partialEvaluator, EVALUATION_VISITOR)).accept(new PartialEvaluator(),
                  EVALUATION_VISITOR);
              CompoundIntervalFormulaManager cifm = CompoundIntervalFormulaManager.INSTANCE;
              InvariantsFormula<CompoundInterval> variable = cifm.asVariable(varName);
              InvariantsFormula<CompoundInterval> leftEquation = cifm.equal(variable, leftFormula);
              InvariantsFormula<CompoundInterval> rightEquation = cifm.equal(variable, rightFormula);
              InvariantsFormula<CompoundInterval> unionEquation = cifm.equal(variable, union);
              Iterable<InvariantsFormula<CompoundInterval>> candidateAssumptions = Iterables.concat(Collections.singleton(unionEquation), assumptions);
              if (CompoundIntervalFormulaManager.definitelyImplies(candidateAssumptions, leftEquation)) {
                resultEnvironment = resultEnvironment.putAndCopy(varName, leftFormula);
                done.add(varName);
              } else if (CompoundIntervalFormulaManager.definitelyImplies(candidateAssumptions, rightEquation)) {
                resultEnvironment = resultEnvironment.putAndCopy(varName, rightFormula);
                done.add(varName);
              }
            }
            if (!done.isEmpty()) {
              cont = !todo.isEmpty();
              todo.removeAll(done);
              done.clear();
            }
          }

          // Join the rest
          for (String varName : todo) {
            InvariantsFormula<CompoundInterval> leftFormula = getEnvironmentValue(varName);
            InvariantsFormula<CompoundInterval> rightFormula = state2.getEnvironmentValue(varName);
            assert leftFormula != null && rightFormula != null;
            InvariantsFormula<CompoundInterval> union = CompoundIntervalFormulaManager.INSTANCE.union(
                leftFormula.accept(state1.partialEvaluator, EVALUATION_VISITOR),
                rightFormula.accept(state2.partialEvaluator, EVALUATION_VISITOR)).accept(new PartialEvaluator(),
                EVALUATION_VISITOR);
            InvariantsFormula<CompoundInterval> evaluated = CompoundIntervalFormulaManager.INSTANCE.asConstant(union.accept(EVALUATION_VISITOR, resultEnvironment));
            resultEnvironment = resultEnvironment.putAndCopy(varName, evaluated);
          }
        }

      }

      VariableSelection<CompoundInterval> resultVariableSelection = state1.variableSelection.join(state2.variableSelection);

      PersistentSortedMap<String, CType> variableTypes = state1.variableTypes;
      for (Map.Entry<String, CType> entry : state2.variableTypes.entrySet()) {
        if (!variableTypes.containsKey(entry.getKey())) {
          variableTypes = variableTypes.putAndCopy(entry.getKey(), entry.getValue());
        }
      }

      AbstractionState abstractionState1 = determineAbstractionState(pPrecision);
      AbstractionState abstractionState2 = pState2.determineAbstractionState(pPrecision);
      AbstractionState abstractionState = abstractionState1.join(abstractionState2);

      result = new InvariantsState(resultVariableSelection, machineModel, abstractionState, resultEnvironment, variableTypes);

      if (result.equalsState(state1)) {
        result = state1;
      }
    }
    return result;
  }

  public InvariantsFormula<CompoundInterval> asFormula() {
    InvariantsFormula<CompoundInterval> result = CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.logicalTrue());
    for (InvariantsFormula<CompoundInterval> assumption : getEnvironmentAsAssumptions()) {
      result = CompoundIntervalFormulaManager.INSTANCE.logicalAnd(result, assumption);
    }
    return result;
  }

}