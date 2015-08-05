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
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanConstant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Exclusion;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaAbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaDepthCountVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PushAssumptionToEnvironmentVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ReplaceVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.StateEqualsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBitvectorFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Union;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Instances of this class represent states in the light-weight invariants analysis.
 */
public class InvariantsState implements AbstractState, FormulaReportingState,
    LatticeAbstractState<InvariantsState> {

  private static final FormulaDepthCountVisitor<CompoundInterval> FORMULA_DEPTH_COUNT_VISITOR = new FormulaDepthCountVisitor<>();

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  /**
   * A visitor used to split boolean conjunction formulas up into the conjuncted clauses
   */
  private static final SplitConjunctionsVisitor<CompoundInterval> SPLIT_CONJUNCTIONS_VISITOR =
      new SplitConjunctionsVisitor<>();

  private static final Predicate<? super String> IS_UNSUPPORTED_VARIABLE = new Predicate<String>() {

      @Override
      public boolean apply(String pArg0) {
        return pArg0 == null || pArg0.contains("[");
      }};

  private final Predicate<BooleanFormula<CompoundInterval>> implies = new Predicate<BooleanFormula<CompoundInterval>>() {

    @Override
    public boolean apply(BooleanFormula<CompoundInterval> pArg0) {
      return definitelyImplies(pArg0);
    }

  };

  /**
   * A visitor used to evaluate formulas as exactly as possible.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  /**
   * A visitor that, like the formula evaluation visitor, is used to evaluate formulas, but far less exact to allow for convergence.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> abstractionVisitor;

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

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final CompoundIntervalFormulaManager compoundIntervalFormulaManager;

  private final AbstractionState abstractionState;

  private Iterable<BooleanFormula<CompoundInterval>> environmentAsAssumptions;

  private volatile int hash = 0;

  public InvariantsState(
      VariableSelection<CompoundInterval> pVariableSelection,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      InvariantsState pInvariant,
      AbstractionState pAbstractionState) {
    this.environment = pInvariant.environment;
    this.partialEvaluator = pInvariant.partialEvaluator;
    this.variableSelection = pVariableSelection;
    this.variableTypes = pInvariant.variableTypes;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
    this.compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    this.evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
  }

  /**
   * Creates a new invariants state with a selection of
   * variables, and the machine model used.
   *
   * @param pVariableSelection the selected variables.
   * @param pMachineModel the machine model used.
   */
  public InvariantsState(VariableSelection<CompoundInterval> pVariableSelection,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      AbstractionState pAbstractionState) {
    this.environment = NonRecursiveEnvironment.of(pCompoundIntervalManagerFactory);
    this.partialEvaluator = new PartialEvaluator(pCompoundIntervalManagerFactory, this.environment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = PathCopyingPersistentTreeMap.of();
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
    this.compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    this.evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
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
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      AbstractionState pAbstractionState,
      NonRecursiveEnvironment pEnvironment,
      PersistentSortedMap<String, CType> pVariableTypes) {
    this.environment = pEnvironment;
    this.partialEvaluator = new PartialEvaluator(pCompoundIntervalManagerFactory, this.environment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = pVariableTypes;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
    this.compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    this.evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
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
  private InvariantsState(Map<String, NumeralFormula<CompoundInterval>> pEnvironment,
      VariableSelection<CompoundInterval> pVariableSelection,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      PersistentSortedMap<String, CType> pVariableTypes,
      AbstractionState pAbstractionState) {
    this.environment = NonRecursiveEnvironment.copyOf(pCompoundIntervalManagerFactory, pEnvironment);
    this.partialEvaluator = new PartialEvaluator(pCompoundIntervalManagerFactory, pEnvironment);
    this.variableSelection = pVariableSelection;
    this.variableTypes = pVariableTypes;
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.abstractionState = pAbstractionState;
    this.compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    this.evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
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
        pPrecision.getAbstractionStrategy()
        .from(abstractionState));
  }

  public InvariantsState updateAbstractionState(InvariantsPrecision pPrecision, CFAEdge pEdge) {
    AbstractionState state =
        pPrecision.getAbstractionStrategy()
        .getSuccessorState(abstractionState);
    state = state.addEnteringEdge(pEdge);
    if (state.equals(abstractionState)) {
      return this;
    }
    return new InvariantsState(environment, variableSelection, compoundIntervalManagerFactory, machineModel, variableTypes, state);
  }

  public Type getType(String pVarName) {
    return variableTypes.get(pVarName);
  }

  public InvariantsState setType(String pVarName, CType pType) {
    if (pType.equals(variableTypes.get(pVarName))) {
      return this;
    }
    return new InvariantsState(variableSelection, compoundIntervalManagerFactory, machineModel, abstractionState, environment, variableTypes.putAndCopy(pVarName, pType));
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
    return new InvariantsState(variableSelection, compoundIntervalManagerFactory, machineModel, abstractionState, environment, variableTypes);
  }

  public InvariantsState assignArray(String pArray, NumeralFormula<CompoundInterval> pSubscript, NumeralFormula<CompoundInterval> pValue) {
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
            result = result.assignInternal(varName, allPossibleValuesFormula(pValue.getBitVectorInfo()));
          }
        }
      }
      return result;
    }
  }

  private CompoundIntervalManager getCompoundIntervalManager(BitVectorInfo pBitVectorInfo) {
    return compoundIntervalManagerFactory.createCompoundIntervalManager(pBitVectorInfo);
  }

  private CompoundInterval allPossibleValues(BitVectorInfo pBitVectorInfo) {
    return getCompoundIntervalManager(pBitVectorInfo).allPossibleValues();
  }

  private NumeralFormula<CompoundInterval> allPossibleValuesFormula(BitVectorInfo pBitVectorInfo) {
    return InvariantsFormulaManager.INSTANCE.asConstant(
        pBitVectorInfo,
        allPossibleValues(pBitVectorInfo));
  }

  public InvariantsState assign(String pVarName, NumeralFormula<CompoundInterval> pValue) {
    InvariantsState result = this;
    Type variableType = variableTypes.get(pVarName);
    if (variableType == null) {
      return this;
    }
    BitVectorInfo bitVectorInfo = BitVectorInfo.from(machineModel, variableType);
    NumeralFormula<CompoundInterval> value = InvariantsFormulaManager.INSTANCE.cast(bitVectorInfo, pValue);
    for (Map.Entry<String, NumeralFormula<CompoundInterval>> entry : this.environment.entrySet()) {
      String varName = entry.getKey();
      BitVectorInfo varBitVectorInfo = BitVectorInfo.from(machineModel, getType(varName));
      if (varName.startsWith(pVarName + "->") || varName.startsWith(pVarName + ".")) {
        result = result.assign(varName, allPossibleValuesFormula(varBitVectorInfo));
      }
    }
    if (value instanceof Variable<?>) {
      String valueVarName = ((Variable<?>) value).getName();
      if (valueVarName.startsWith(pVarName + "->") || valueVarName.startsWith(pVarName + ".")) {
        return assign(pVarName, allPossibleValuesFormula(bitVectorInfo));
      }
      String pointerDerefPrefix = valueVarName + "->";
      String nonPointerDerefPrefix = valueVarName + ".";
      for (Map.Entry<String, NumeralFormula<CompoundInterval>> entry : this.environment.entrySet()) {
        final String suffix;
        if (entry.getKey().startsWith(pointerDerefPrefix)) {
          suffix = entry.getKey().substring(pointerDerefPrefix.length());
        } else if (entry.getKey().startsWith(nonPointerDerefPrefix)) {
          suffix = entry.getKey().substring(nonPointerDerefPrefix.length());
        } else {
          suffix = null;
        }
        if (suffix != null) {
          String varName = pVarName + "->" + suffix;
          NumeralFormula<CompoundInterval> previous = this.environment.get(varName);
          if (previous != null) {
            result = result.assign(
                varName,
                InvariantsFormulaManager.INSTANCE.<CompoundInterval>asVariable(
                    previous.getBitVectorInfo(),
                    entry.getKey()));
          }
        }
      }
      return result.assignInternal(pVarName, value);
    }
    return result.assignInternal(pVarName, value);
  }

  /**
   * Creates a new state representing the given assignment applied to the current state.
   *
   * @param pVarName the name of the variable being assigned.
   * @param pValue the new value of the variable.
   * @return a new state representing the given assignment applied to the current state.
   */
  private InvariantsState assignInternal(String pVarName, NumeralFormula<CompoundInterval> pValue) {
    Preconditions.checkNotNull(pValue);
    // Only use information from supported variables
    if (IS_UNSUPPORTED_VARIABLE.apply(pVarName)) {
      return this;
    }
    if (FluentIterable.from(pValue.accept(COLLECT_VARS_VISITOR)).anyMatch(IS_UNSUPPORTED_VARIABLE)) {
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
          compoundIntervalManagerFactory,
          machineModel,
          variableTypes,
          abstractionState);
    }

    BitVectorInfo bitVectorInfo = pValue.getBitVectorInfo();
    Variable<CompoundInterval> variable = InvariantsFormulaManager.INSTANCE.asVariable(
        bitVectorInfo,
        pVarName);
    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>();

    // Optimization: If the value being assigned is equivalent to the value already stored, do nothing
    if (getEnvironmentValue(bitVectorInfo, pVarName).equals(pValue)
        && (pValue instanceof Variable<?> || pValue instanceof Constant<?> && ((Constant<CompoundInterval>) pValue).getValue().isSingleton())
        || variable.accept(new StateEqualsVisitor(getFormulaResolver(), this.environment, compoundIntervalManagerFactory), pValue)) {
      return this;
    }

    // Avoid self-assignments if an equivalent alternative is available
    if (pValue.accept(containsVarVisitor, pVarName)) {
      NumeralFormula<CompoundInterval> varValue = environment.get(pVarName);
      boolean isVarValueConstant = varValue instanceof Constant && ((Constant<CompoundInterval>) varValue).getValue().isSingleton();
      NumeralFormula<CompoundInterval> alternative = varValue;
      if (!(alternative instanceof Variable)) {
        alternative = null;
        for (Map.Entry<String, NumeralFormula<CompoundInterval>> entry : environment.entrySet()) {
          NumeralFormula<CompoundInterval> value = entry.getValue();
          if (!entry.getKey().equals(pVarName)
              && (value.equals(variable) || isVarValueConstant && value.equals(varValue))) {
            alternative = InvariantsFormulaManager.INSTANCE.asVariable(bitVectorInfo, entry.getKey());
            break;
          }
        }
      }
      if (alternative != null) {
        pValue = pValue.accept(new ReplaceVisitor<>(variable, alternative));
      }
      CompoundInterval value = pValue.accept(evaluationVisitor, environment);
      if (value.isSingleton()) {
        for (Map.Entry<String, NumeralFormula<CompoundInterval>> entry : environment.entrySet()) {
          NumeralFormula<CompoundInterval> v = entry.getValue();
          if (v instanceof Constant && value.equals(((Constant<CompoundInterval>) v).getValue())) {
            pValue = InvariantsFormulaManager.INSTANCE.asVariable(bitVectorInfo, entry.getKey());
            break;
          }
        }
      }
    }


    NumeralFormula<CompoundInterval> previousValue = getEnvironmentValue(bitVectorInfo, pVarName);

    /*
     * A variable is newly assigned, so the appearances of this variable
     * in any previously collected assumptions (including its new value)
     * have to be resolved with the variable's previous value.
     */
    ReplaceVisitor<CompoundInterval> replaceVisitor = new ReplaceVisitor<>(variable, previousValue);

    // Compute the assignment
    InvariantsState result = assignInternal(pVarName, pValue, newVariableSelection, evaluationVisitor, replaceVisitor);

    if (equals(result)) {
      return this;
    }
    return result;
  }

  private InvariantsState assignInternal(String pVarName, NumeralFormula<CompoundInterval> pValue,
      VariableSelection<CompoundInterval> newVariableSelection,
      FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor, ReplaceVisitor<CompoundInterval> replaceVisitor) {
    NonRecursiveEnvironment resultEnvironment = this.environment;

    for (Map.Entry<String, NumeralFormula<CompoundInterval>> environmentEntry : this.environment.entrySet()) {
      if (!environmentEntry.getKey().equals(pVarName)) {
        NumeralFormula<CompoundInterval> newEnvValue =
            environmentEntry.getValue().accept(replaceVisitor).accept(partialEvaluator, evaluationVisitor);
        resultEnvironment = resultEnvironment.putAndCopy(environmentEntry.getKey(), newEnvValue);
      }
    }
    resultEnvironment = resultEnvironment.putAndCopy(pVarName, pValue.accept(replaceVisitor).accept(partialEvaluator, evaluationVisitor));
    return new InvariantsState(newVariableSelection, compoundIntervalManagerFactory, machineModel, abstractionState, resultEnvironment, variableTypes);
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
    return new InvariantsState(variableSelection, compoundIntervalManagerFactory, machineModel, abstractionState);
  }

  /**
   * Removes the value stored for the given variable.
   *
   * @param pVariableName the variable to remove.
   *
   * @return the new state.
   */
  public InvariantsState clear(String pVariableName) {
    NumeralFormula<CompoundInterval> previous = environment.get(pVariableName);
    final BitVectorInfo bitVectorInfo;

    if (previous == null) {
      Type type = variableTypes.get(pVariableName);
      if (type == null) {
        return this;
      }
      bitVectorInfo = BitVectorInfo.from(machineModel, type);
    } else {
      bitVectorInfo = previous.getBitVectorInfo();
    }

    Variable<CompoundInterval> variable = InvariantsFormulaManager.INSTANCE.asVariable(bitVectorInfo, pVariableName);
    NumeralFormula<CompoundInterval> allPossibleValues = allPossibleValuesFormula(bitVectorInfo);
    ReplaceVisitor<CompoundInterval> replaceVisitor = new ReplaceVisitor<>(variable, previous == null ? allPossibleValues : previous);
    InvariantsState result = assignInternal(pVariableName, allPossibleValues, variableSelection, evaluationVisitor, replaceVisitor);
    NonRecursiveEnvironment resultEnvironment = result.environment.removeAndCopy(pVariableName);
    result = new InvariantsState(
        result.variableSelection,
        result.compoundIntervalManagerFactory,
        result.machineModel,
        result.abstractionState,
        resultEnvironment,
        result.variableTypes);
    if (equals(result)) {
      return this;
    }
    return result;
  }

  /**
   * Gets the environment as a set equations of the variables with their values.
   *
   * @return the environment as a set equations of the variables with their values.
   */
  public Iterable<BooleanFormula<CompoundInterval>> getEnvironmentAsAssumptions() {
    if (this.environmentAsAssumptions == null) {
      environmentAsAssumptions = getEnvironmentAsAssumptions(compoundIntervalManagerFactory, this.environment);
    }
    return environmentAsAssumptions;
  }

  private static Iterable<BooleanFormula<CompoundInterval>> getEnvironmentAsAssumptions(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {

    CompoundIntervalFormulaManager compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(pCompoundIntervalManagerFactory);

    Set<BooleanFormula<CompoundInterval>> environmentalAssumptions = new HashSet<>();

    List<NumeralFormula<CompoundInterval>> atomic = new ArrayList<>(1);
    Deque<NumeralFormula<CompoundInterval>> toCheck = new ArrayDeque<>(1);
    for (Entry<? extends String, ? extends NumeralFormula<CompoundInterval>> entry : pEnvironment.entrySet()) {
      NumeralFormula<CompoundInterval> variable =
          InvariantsFormulaManager.INSTANCE.asVariable(
              entry.getValue().getBitVectorInfo(),
              entry.getKey());

      NumeralFormula<CompoundInterval> value = entry.getValue();

      boolean isExclusion = false;
      if (value instanceof Exclusion) {
        isExclusion = true;
        value = ((Exclusion<CompoundInterval>) value).getExcluded();
      }

      atomic.clear();
      toCheck.clear();

      toCheck.add(value);
      while (!toCheck.isEmpty()) {
        NumeralFormula<CompoundInterval> current = toCheck.poll();
        if (current instanceof Union<?>) {
          Union<CompoundInterval> union = (Union<CompoundInterval>) current;
          toCheck.add(union.getOperand1());
          toCheck.add(union.getOperand2());
        } else {
          atomic.add(current);
        }
      }
      assert !atomic.isEmpty();
      Iterator<NumeralFormula<CompoundInterval>> iterator = atomic.iterator();
      BooleanFormula<CompoundInterval> assumption = null;
      while (iterator.hasNext()) {
        BooleanFormula<CompoundInterval> equation =
            compoundIntervalFormulaManager.equal(variable, iterator.next());
        if (isExclusion) {
          equation = compoundIntervalFormulaManager.logicalNot(equation);
        }
        assumption = assumption == null
            ? equation
            : compoundIntervalFormulaManager.logicalOr(assumption, equation);
      }
      if (assumption != null) {
        environmentalAssumptions.add(assumption);
      }
    }
    return environmentalAssumptions;
  }

  /**
   * Gets the value of the variable with the given name from the environment.
   *
   * @param pBitVectorInfo the bit vector information of the variable.
   * @param pVarName the name of the variable.
   *
   * @return the value of the variable with the given name from the environment.
   */
  private NumeralFormula<CompoundInterval> getEnvironmentValue(BitVectorInfo pBitVectorInfo, String pVarName) {
    NumeralFormula<CompoundInterval> environmentValue = this.environment.get(pVarName);
    if (environmentValue == null) { return allPossibleValuesFormula(pBitVectorInfo); }
    return environmentValue;
  }

  /**
   * Gets an exact formula evaluation visitor.
   *
   * @return an exact formula evaluation visitor.
   */
  public FormulaEvaluationVisitor<CompoundInterval> getFormulaResolver() {
    return evaluationVisitor;
  }

  /**
   * Makes the given assumptions for this state and checks if this state is still valid.
   *
   * @param pAssumptions the assumptions to be made.
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions' correctness.
   * @param pNewVariableSelection
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>false</code> otherwise.
   */
  private InvariantsState assumeInternal(Collection<? extends BooleanFormula<CompoundInterval>> pAssumptions,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor, VariableSelection<CompoundInterval> pNewVariableSelection) {
    InvariantsState result = this;
    for (BooleanFormula<CompoundInterval> assumption : pAssumptions) {
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
  private InvariantsState assumeInternal(BooleanFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor, VariableSelection<CompoundInterval> pNewVariableSelection) {
    BooleanFormula<CompoundInterval> assumption = pAssumption.accept(this.partialEvaluator, pEvaluationVisitor);
    // If there are multiple assumptions combined with &&, split them up
    List<BooleanFormula<CompoundInterval>> assumptionParts = assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) { return assumeInternal(assumptionParts, pEvaluationVisitor, pNewVariableSelection); }

    if (assumption instanceof BooleanConstant) {
      return BooleanConstant.isTrue(assumption) ? this : null;
    }

    // Only use information from supported variables
    if (FluentIterable.from(assumption.accept(COLLECT_VARS_VISITOR)).anyMatch(IS_UNSUPPORTED_VARIABLE)) {
      return this;
    }

    BooleanConstant<CompoundInterval> assumptionEvaluation = assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (BooleanConstant.isFalse(assumptionEvaluation)) { return null; }
    // If the invariant evaluates to true, it adds no value for now
    if (BooleanConstant.isTrue(assumptionEvaluation)) { return this; }

    NonRecursiveEnvironment.Builder environmentBuilder = new NonRecursiveEnvironment.Builder(this.environment);
    PushAssumptionToEnvironmentVisitor patev =
        new PushAssumptionToEnvironmentVisitor(compoundIntervalManagerFactory, pEvaluationVisitor, environmentBuilder);
    if (!assumption.accept(patev, BooleanConstant.<CompoundInterval>getTrue())) {
      assert !BooleanConstant.isTrue(assumptionEvaluation);
      return null;
    }
    // Check all the assumption once more after the environment changed
    if (isDefinitelyFalse(assumption, pEvaluationVisitor)) {
      return null;
    }
    return new InvariantsState(environmentBuilder.build(), pNewVariableSelection, compoundIntervalManagerFactory, machineModel, variableTypes, abstractionState);
  }

  /**
   * Checks if the given assumption is definitely false for this state.
   * @param pAssumption the assumption to evaluate.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate the assumption within this state's environment.
   * @return <code>true</code> if the given assumption does definitely not hold for this state's environment, <code>false</code> if it might.
   */
  private boolean isDefinitelyFalse(BooleanFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return BooleanConstant.isFalse(pAssumption.accept(pEvaluationVisitor, getEnvironment()));
  }

  public InvariantsState assume(BooleanFormula<CompoundInterval> pAssumption) {
    // Check if at least one of the involved variables is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection = this.variableSelection.acceptAssumption(pAssumption);
    if (newVariableSelection == null) {
      return this;
    }
    FormulaEvaluationVisitor<CompoundInterval> evaluator = getFormulaResolver();
    BooleanFormula<CompoundInterval> assumption = pAssumption.accept(this.partialEvaluator, evaluator);
    if (assumption instanceof BooleanConstant) {
      // An assumption evaluating to false represents an unreachable state; it can never be fulfilled
      if (BooleanConstant.isFalse(assumption)) { return null; }
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
  public org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula getFormulaApproximation(FormulaManagerView pManager, PathFormulaManager pfmgr) {
    FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor = getFormulaResolver();
    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula result = bfmgr.makeBoolean(true);
    ToBitvectorFormulaVisitor toBooleanFormulaVisitor =
        new ToBitvectorFormulaVisitor(pManager, evaluationVisitor, variableTypes, machineModel);

    final Predicate<String> acceptVariable = new Predicate<String>() {

      @Override
      public boolean apply(@Nullable String pInput) {
        return pInput != null && !pInput.contains("*");
      }

    };

    final Predicate<BooleanFormula<CompoundInterval>> acceptFormula = new Predicate<BooleanFormula<CompoundInterval>>() {

      @Override
      public boolean apply(@Nullable BooleanFormula<CompoundInterval> pInput) {
        return pInput != null
            && FluentIterable.from(CompoundIntervalFormulaManager.collectVariableNames(pInput)).allMatch(acceptVariable);
      }

    };

    for (BooleanFormula<CompoundInterval> assumption : getEnvironmentAsAssumptions()) {
      if (acceptFormula.apply(assumption)) {
        org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula assumptionFormula =
            assumption.accept(toBooleanFormulaVisitor, getEnvironment());
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
    return FluentIterable.from(environment.entrySet()).transform(new Function<Map.Entry<String, NumeralFormula<CompoundInterval>>, String>() {

      @Override
      public String apply(Entry<String, NumeralFormula<CompoundInterval>> pInput) {
        String variableName = pInput.getKey();
        NumeralFormula<?> value = pInput.getValue();
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
  public Map<String, NumeralFormula<CompoundInterval>> getEnvironment() {
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
    for (BooleanFormula<CompoundInterval> rightAssumption : pState2.getEnvironmentAsAssumptions()) {
      if (!definitelyImplies(rightAssumption)) {
        return false;
      }
    }
    return true;
  }

  public boolean definitelyImplies(BooleanFormula<CompoundInterval> pFormula) {
    return compoundIntervalFormulaManager.definitelyImplies(this.environment, pFormula);
  }

  public InvariantsState widen(InvariantsState pOlderState,
      @Nullable InvariantsPrecision pPrecision,
      Set<String> pWideningTargets,
      Set<BooleanFormula<CompoundInterval>> pWideningHints) {

    Set<String> wideningTargets = pWideningTargets == null ? environment.keySet() : pWideningTargets;

    if (wideningTargets.isEmpty()) {
      return this;
    }

    // Prepare result environment
    NonRecursiveEnvironment resultEnvironment = this.environment;

    // Find entries that require widening
    Map<String, NumeralFormula<CompoundInterval>> toDo = new HashMap<>();
    for (String varName : FluentIterable.from(pOlderState.environment.keySet()).filter(Predicates.in(wideningTargets))) {
      NumeralFormula<CompoundInterval> oldFormula = pOlderState.environment.get(varName);
      if (oldFormula == null) {
        continue;
      }
      NumeralFormula<CompoundInterval> currentFormula = environment.get(varName);
      BitVectorInfo bitVectorInfo = oldFormula.getBitVectorInfo();
      CompoundIntervalManager compoundIntervalManager = compoundIntervalManagerFactory.createCompoundIntervalManager(bitVectorInfo);
      currentFormula = currentFormula == null ? allPossibleValuesFormula(bitVectorInfo) : currentFormula;
      assert currentFormula.getBitVectorInfo().equals(bitVectorInfo);
      if (!currentFormula.equals(oldFormula)
          || currentFormula.accept(FORMULA_DEPTH_COUNT_VISITOR) > pPrecision.getMaximumFormulaDepth()) {
        NumeralFormula<CompoundInterval> newValueFormula =
          compoundIntervalFormulaManager.union(
            currentFormula.accept(this.partialEvaluator, evaluationVisitor),
            oldFormula.accept(pOlderState.partialEvaluator, evaluationVisitor)).accept(new PartialEvaluator(compoundIntervalManagerFactory), evaluationVisitor);

        // Allow only (singleton) constants for formula depth 0
        if (pPrecision.getMaximumFormulaDepth() == 0) {
          CompoundInterval value = compoundIntervalManager.union(
              currentFormula.accept(evaluationVisitor, environment),
              oldFormula.accept(evaluationVisitor, pOlderState.getEnvironment()));
          if (!value.isSingleton()) {
            value = compoundIntervalManager.allPossibleValues();
          }
          newValueFormula = InvariantsFormulaManager.INSTANCE.asConstant(
              currentFormula.getBitVectorInfo(),
              value);
        }

        resultEnvironment = resultEnvironment.putAndCopy(varName, newValueFormula);
        toDo.put(varName, newValueFormula);
      }
    }
    if (toDo.isEmpty()) {
      return this;
    }
    for (Map.Entry<String, NumeralFormula<CompoundInterval>> entry : toDo.entrySet()) {
      String varName = entry.getKey();
      NumeralFormula<CompoundInterval> newValueFormula = entry.getValue();
      BitVectorInfo bitVectorInfo = entry.getValue().getBitVectorInfo();
      CompoundInterval simpleExactValue = newValueFormula.accept(evaluationVisitor, resultEnvironment);
      if (simpleExactValue.isSingleton()) {
        resultEnvironment = resultEnvironment.putAndCopy(varName, InvariantsFormulaManager.INSTANCE.asConstant(bitVectorInfo, simpleExactValue));
      } else {
        CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(bitVectorInfo);
        NumeralFormula<CompoundInterval> oldFormula = pOlderState.getEnvironmentValue(bitVectorInfo, varName);
        NumeralFormula<CompoundInterval> currentFormula = getEnvironmentValue(bitVectorInfo, varName);
        CompoundInterval oldExactValue = oldFormula.accept(evaluationVisitor, pOlderState.environment);
        CompoundInterval currentExactValue = currentFormula.accept(evaluationVisitor, environment);
        final CompoundInterval newValue;
        if (compoundIntervalManager.contains(oldExactValue, currentExactValue)) {
          newValue = oldExactValue;
        } else if (compoundIntervalManager.lessEqual(oldExactValue, currentExactValue).isDefinitelyTrue()
            || oldExactValue.hasUpperBound() && (!currentExactValue.hasUpperBound() || oldExactValue.getUpperBound().compareTo(currentExactValue.getUpperBound()) < 0)) {
          newValue = compoundIntervalManager.union(oldExactValue, currentExactValue).extendToMaxValue();
        } else if (compoundIntervalManager.greaterEqual(oldExactValue, currentExactValue).isDefinitelyTrue()
            || oldExactValue.hasLowerBound() && (!currentExactValue.hasLowerBound() || oldExactValue.getLowerBound().compareTo(currentExactValue.getLowerBound()) > 0)) {
          newValue = compoundIntervalManager.union(oldExactValue, currentExactValue).extendToMinValue();
        } else {
          NumeralFormula<CompoundInterval> newFormula = resultEnvironment.get(varName);
          if (newFormula == null) {
            newFormula = allPossibleValuesFormula(bitVectorInfo);
          }
          newValue = newFormula.accept(abstractionVisitor, resultEnvironment);
        }
        resultEnvironment = resultEnvironment.putAndCopy(
            varName,
            InvariantsFormulaManager.INSTANCE.asConstant(
                bitVectorInfo,
                newValue));
      }
    }
    final NonRecursiveEnvironment resEnv = resultEnvironment;
    InvariantsState result = new InvariantsState(resEnv, variableSelection, compoundIntervalManagerFactory, machineModel, variableTypes, abstractionState);

    for (BooleanFormula<CompoundInterval> hint : FluentIterable
        .from(pWideningHints)
        .filter(new Predicate<BooleanFormula<CompoundInterval>>() {

          @Override
          public boolean apply(BooleanFormula<CompoundInterval> pHint) {
            return resEnv.keySet().containsAll(pHint.accept(COLLECT_VARS_VISITOR));
          }})
        .filter(this.implies)) {
      result = result.assume(hint);
    }
    if (equals(result)) {
      return this;
    }

    return result;
  }

  @Override
  public InvariantsState join(InvariantsState state2) {
    return join(state2, InvariantsPrecision.getEmptyPrecision(
        AbstractionStrategyFactories.ALWAYS.createStrategy(
            compoundIntervalManagerFactory,
            machineModel)));
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
      NonRecursiveEnvironment resultEnvironment = NonRecursiveEnvironment.of(compoundIntervalManagerFactory);

      // Get some basic information by joining the environments
      {
        Set<String> todo = new HashSet<>();

        // Join the easy ones first (both values equal or one value top)
        for (Map.Entry<String, NumeralFormula<CompoundInterval>> entry : state1.environment.entrySet()) {
          String varName = entry.getKey();
          NumeralFormula<CompoundInterval> rightFormula = state2.environment.get(varName);
          if (rightFormula != null) {
            NumeralFormula<CompoundInterval> leftFormula = getEnvironmentValue(rightFormula.getBitVectorInfo(), varName);
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
            Iterable<BooleanFormula<CompoundInterval>> assumptions = getEnvironmentAsAssumptions(compoundIntervalManagerFactory, resultEnvironment);
            for (String varName : todo) {
              NumeralFormula<CompoundInterval> leftFormula = environment.get(varName);
              NumeralFormula<CompoundInterval> rightFormula = state2.environment.get(varName);
              assert leftFormula != null && rightFormula != null;
              assert leftFormula.getBitVectorInfo().equals(rightFormula.getBitVectorInfo());
              NumeralFormula<CompoundInterval> union = compoundIntervalFormulaManager.union(
                  leftFormula.accept(state1.partialEvaluator, evaluationVisitor),
                  rightFormula.accept(state2.partialEvaluator, evaluationVisitor)).accept(new PartialEvaluator(compoundIntervalManagerFactory),
                  evaluationVisitor);
              NumeralFormula<CompoundInterval> variable =
                  InvariantsFormulaManager.INSTANCE.asVariable(
                      leftFormula.getBitVectorInfo(),
                      varName);
              BooleanFormula<CompoundInterval> leftEquation = compoundIntervalFormulaManager.equal(variable, leftFormula);
              BooleanFormula<CompoundInterval> rightEquation = compoundIntervalFormulaManager.equal(variable, rightFormula);
              BooleanFormula<CompoundInterval> unionEquation = compoundIntervalFormulaManager.equal(variable, union);
              Iterable<BooleanFormula<CompoundInterval>> candidateAssumptions = Iterables.concat(Collections.singleton(unionEquation), assumptions);
              if (compoundIntervalFormulaManager.definitelyImplies(candidateAssumptions, leftEquation)) {
                resultEnvironment = resultEnvironment.putAndCopy(varName, leftFormula);
                done.add(varName);
              } else if (compoundIntervalFormulaManager.definitelyImplies(candidateAssumptions, rightEquation)) {
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
            NumeralFormula<CompoundInterval> leftFormula = environment.get(varName);
            NumeralFormula<CompoundInterval> rightFormula = state2.environment.get(varName);
            assert leftFormula != null && rightFormula != null;
            NumeralFormula<CompoundInterval> union = compoundIntervalFormulaManager.union(
                leftFormula.accept(state1.partialEvaluator, evaluationVisitor),
                rightFormula.accept(state2.partialEvaluator, evaluationVisitor)).accept(new PartialEvaluator(compoundIntervalManagerFactory),
                evaluationVisitor);
            NumeralFormula<CompoundInterval> evaluated =
                InvariantsFormulaManager.INSTANCE.asConstant(
                    union.getBitVectorInfo(),
                    union.accept(evaluationVisitor, resultEnvironment));
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

      result = new InvariantsState(resultVariableSelection, compoundIntervalManagerFactory, machineModel, abstractionState, resultEnvironment, variableTypes);

      if (result.equalsState(state1)) {
        result = state1;
      }
    }
    return result;
  }

  public BooleanFormula<CompoundInterval> asFormula() {
    BooleanFormula<CompoundInterval> result = BooleanConstant.<CompoundInterval>getTrue();
    for (BooleanFormula<CompoundInterval> assumption : getEnvironmentAsAssumptions()) {
      result = compoundIntervalFormulaManager.logicalAnd(result, assumption);
    }
    return result;
  }

  public Set<String> getVariables() {
    Set<String> result = environment.keySet();
    for (NumeralFormula<CompoundInterval> value : environment.values()) {
      Set<String> valueVars = value.accept(COLLECT_VARS_VISITOR);
      if (!valueVars.isEmpty()) {
        result = Sets.union(result, valueVars);
      }
    }
    return result;
  }

}