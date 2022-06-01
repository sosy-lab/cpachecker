// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.ibm.icu.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Add;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanConstant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectFormulasVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVarVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ContainsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Equal;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Exclusion;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaAbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaDepthCountVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.IsLinearVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalAnd;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalNot;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Mod2AbstractionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Multiply;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PartialEvaluator;
import org.sosy_lab.cpachecker.cpa.invariants.formula.PushAssumptionToEnvironmentVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ReplaceVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.StateEqualsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToBitvectorFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ToCodeFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Union;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Instances of this class represent states in the light-weight invariants analysis. */
public class InvariantsState
    implements AbstractState,
        ExpressionTreeReportingState,
        FormulaReportingState,
        LatticeAbstractState<InvariantsState>,
        AbstractQueryableState {

  private static final String PROPERTY_OVERFLOW = "overflow";

  private static final FormulaDepthCountVisitor<CompoundInterval> FORMULA_DEPTH_COUNT_VISITOR =
      new FormulaDepthCountVisitor<>();

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR =
      new CollectVarsVisitor<>();

  /** A visitor used to split boolean conjunction formulas up into the conjuncted clauses */
  private static final SplitConjunctionsVisitor<CompoundInterval> SPLIT_CONJUNCTIONS_VISITOR =
      new SplitConjunctionsVisitor<>();

  private static boolean isUnsupportedVariableName(MemoryLocation pMemoryLocation) {
    return pMemoryLocation == null || pMemoryLocation.getIdentifier().contains("[");
  }

  /** The tools used to manage states. */
  private final Tools tools;

  /** The environment currently known to the state. */
  private final NonRecursiveEnvironment environment;

  /** The variables selected for this analysis. */
  private final VariableSelection<CompoundInterval> variableSelection;

  private final PersistentSortedMap<MemoryLocation, CType> variableTypes;

  private final PartialEvaluator partialEvaluator;

  private final MachineModel machineModel;

  private final AbstractionState abstractionState;

  private final boolean overflowDetected;

  private final boolean includeTypeInformation;

  private final boolean overapproximatesUnsupportedFeature;

  private final ImmutableSet<BooleanFormula<CompoundInterval>> assumptions;

  private ImmutableSet<BooleanFormula<CompoundInterval>> environmentAsAssumptions;

  private volatile int hash = 0;

  /**
   * Creates a new invariants state with a selection of variables, and the machine model used.
   *
   * @param pVariableSelection the selected variables.
   * @param pMachineModel the machine model used.
   * @param pAbstractionState the abstraction information.
   * @param pIncludeTypeInformation whether or not to include type information for exports.
   */
  public InvariantsState(
      VariableSelection<CompoundInterval> pVariableSelection,
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      AbstractionState pAbstractionState,
      boolean pIncludeTypeInformation) {
    environment = NonRecursiveEnvironment.of(pCompoundIntervalManagerFactory);
    partialEvaluator = new PartialEvaluator(pCompoundIntervalManagerFactory, environment);
    variableSelection = pVariableSelection;
    variableTypes = PathCopyingPersistentTreeMap.of();
    tools = new Tools(pCompoundIntervalManagerFactory);
    machineModel = pMachineModel;
    abstractionState = pAbstractionState;
    overflowDetected = false;
    includeTypeInformation = pIncludeTypeInformation;
    overapproximatesUnsupportedFeature = false;
    assumptions = ImmutableSet.of();
  }

  /**
   * Creates a new invariants state with the given data, reusing the given instance of the
   * environment without copying.
   *
   * @param pVariableSelection the selected variables.
   * @param pTools the tools used to manage the state.
   * @param pMachineModel the machine model used.
   * @param pVariableTypes the variable types.
   * @param pAbstractionState the abstraction information.
   * @param pEnvironment the environment. This instance is reused and not copied.
   * @param pAssumptions additional assumptions about this state.
   * @param pOverflowDetected if an overflow has been detected.
   * @param pIncludeTypeInformation whether or not to include type information for exports.
   * @param pOverapproximatesUnsupportedFeature whether or not an unsupported feature is
   *     over-approximated by this state.
   */
  private InvariantsState(
      VariableSelection<CompoundInterval> pVariableSelection,
      Tools pTools,
      MachineModel pMachineModel,
      PersistentSortedMap<MemoryLocation, CType> pVariableTypes,
      AbstractionState pAbstractionState,
      NonRecursiveEnvironment pEnvironment,
      Set<BooleanFormula<CompoundInterval>> pAssumptions,
      boolean pOverflowDetected,
      boolean pIncludeTypeInformation,
      boolean pOverapproximatesUnsupportedFeature) {
    environment = pEnvironment;
    tools = pTools;
    partialEvaluator = new PartialEvaluator(pTools.compoundIntervalManagerFactory, environment);
    variableSelection = pVariableSelection;
    variableTypes = pVariableTypes;
    machineModel = pMachineModel;
    abstractionState = pAbstractionState;
    overflowDetected = pOverflowDetected;
    includeTypeInformation = pIncludeTypeInformation;
    overapproximatesUnsupportedFeature = pOverapproximatesUnsupportedFeature;
    assumptions = ImmutableSet.copyOf(pAssumptions);
  }

  private AbstractionState determineAbstractionState(AbstractionState pMasterState) {
    AbstractionState state = pMasterState;
    if (state.getClass() == abstractionState.getClass()) {
      state = abstractionState.join(state);
    }
    return state;
  }

  public AbstractionState determineAbstractionState(InvariantsPrecision pPrecision) {
    return determineAbstractionState(pPrecision.getAbstractionStrategy().from(abstractionState));
  }

  public InvariantsState updateAbstractionState(InvariantsPrecision pPrecision, CFAEdge pEdge) {
    AbstractionState state =
        pPrecision.getAbstractionStrategy().getSuccessorState(abstractionState);
    state = state.addEnteringEdge(pEdge);
    if (state.equals(abstractionState)) {
      return this;
    }
    return new InvariantsState(
        variableSelection,
        tools,
        machineModel,
        variableTypes,
        state,
        environment,
        assumptions,
        overflowDetected,
        includeTypeInformation,
        overapproximatesUnsupportedFeature);
  }

  public Type getType(MemoryLocation pMemoryLocation) {
    return variableTypes.get(pMemoryLocation);
  }

  public InvariantsState setType(MemoryLocation pMemoryLocation, CType pType) {
    if (pType.equals(variableTypes.get(pMemoryLocation))) {
      return this;
    }
    return new InvariantsState(
        variableSelection,
        tools,
        machineModel,
        variableTypes.putAndCopy(pMemoryLocation, pType),
        abstractionState,
        environment,
        assumptions,
        overflowDetected,
        includeTypeInformation,
        overapproximatesUnsupportedFeature);
  }

  public InvariantsState setTypes(Map<MemoryLocation, CType> pVarTypes) {
    boolean allContained = true;
    for (Map.Entry<MemoryLocation, CType> entry : pVarTypes.entrySet()) {
      if (!entry.getValue().equals(variableTypes.get(entry.getKey()))) {
        allContained = false;
        break;
      }
    }
    if (allContained) {
      return this;
    }
    PersistentSortedMap<MemoryLocation, CType> newVariableTypes = variableTypes;
    for (Map.Entry<MemoryLocation, CType> entry : pVarTypes.entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      if (!entry.getValue().equals(newVariableTypes.get(memoryLocation))) {
        newVariableTypes = newVariableTypes.putAndCopy(memoryLocation, entry.getValue());
      }
    }
    return new InvariantsState(
        variableSelection,
        tools,
        machineModel,
        newVariableTypes,
        abstractionState,
        environment,
        assumptions,
        overflowDetected,
        includeTypeInformation,
        overapproximatesUnsupportedFeature);
  }

  public InvariantsState assignArray(
      MemoryLocation pArray,
      NumeralFormula<CompoundInterval> pSubscript,
      NumeralFormula<CompoundInterval> pValue) {
    FormulaEvaluationVisitor<CompoundInterval> fev = getFormulaResolver();
    CompoundInterval value = pSubscript.accept(fev, environment);
    if (value.isSingleton()) { // Exact subscript value is known
      return assignInternal(
          MemoryLocation.parseExtendedQualifiedName(
              pArray.getExtendedQualifiedName() + "[" + value.getValue() + "]"),
          pValue);
    } else {
      // Multiple subscript values are possible: All possible subscript targets are now unknown
      InvariantsState result = overapproximateUnsupportedFeature();
      for (MemoryLocation memoryLocation : environment.keySet()) {
        String prefix = pArray.getExtendedQualifiedName() + "[";
        if (memoryLocation.getExtendedQualifiedName().startsWith(prefix)) {
          String subscriptValueStr =
              memoryLocation.getExtendedQualifiedName().replace(prefix, "").replaceAll("].*", "");
          if (subscriptValueStr.equals("*") || value.contains(new BigInteger(subscriptValueStr))) {
            result =
                result.assignInternal(
                    memoryLocation, allPossibleValuesFormula(pValue.getTypeInfo()));
          }
        }
      }
      return result;
    }
  }

  private CompoundIntervalManager getCompoundIntervalManager(TypeInfo pTypeInfo) {
    return tools.compoundIntervalManagerFactory.createCompoundIntervalManager(pTypeInfo);
  }

  private CompoundInterval allPossibleValues(TypeInfo pTypeInfo) {
    return getCompoundIntervalManager(pTypeInfo).allPossibleValues();
  }

  private NumeralFormula<CompoundInterval> allPossibleValuesFormula(TypeInfo pInfo) {
    return InvariantsFormulaManager.INSTANCE.asConstant(pInfo, allPossibleValues(pInfo));
  }

  public InvariantsState assign(
      MemoryLocation pMemoryLocation, NumeralFormula<CompoundInterval> pValue) {
    InvariantsState result = this;
    Type variableType = variableTypes.get(pMemoryLocation);
    if (variableType == null) {
      return this;
    }
    TypeInfo typeInfo = BitVectorInfo.from(machineModel, variableType);
    NumeralFormula<CompoundInterval> value =
        tools.compoundIntervalFormulaManager.cast(typeInfo, pValue);
    for (MemoryLocation memoryLocation : environment.keySet()) {
      TypeInfo varTypeInfo = BitVectorInfo.from(machineModel, getType(memoryLocation));
      if (memoryLocation
              .getExtendedQualifiedName()
              .startsWith(pMemoryLocation.getExtendedQualifiedName() + "->")
          || memoryLocation
              .getExtendedQualifiedName()
              .startsWith(pMemoryLocation.getExtendedQualifiedName() + ".")) {
        result = result.assign(memoryLocation, allPossibleValuesFormula(varTypeInfo));
      }
    }
    if (value instanceof Variable<?>) {
      MemoryLocation valueMemoryLocation = ((Variable<?>) value).getMemoryLocation();
      if (valueMemoryLocation
              .getExtendedQualifiedName()
              .startsWith(pMemoryLocation.getExtendedQualifiedName() + "->")
          || valueMemoryLocation
              .getExtendedQualifiedName()
              .startsWith(pMemoryLocation.getExtendedQualifiedName() + ".")) {
        return assign(pMemoryLocation, allPossibleValuesFormula(typeInfo));
      }
      String pointerDerefPrefix = valueMemoryLocation.getExtendedQualifiedName() + "->";
      String nonPointerDerefPrefix = valueMemoryLocation.getExtendedQualifiedName() + ".";
      for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry :
          environment.entrySet()) {
        final String suffix;
        if (entry.getKey().getExtendedQualifiedName().startsWith(pointerDerefPrefix)) {
          suffix = entry.getKey().getExtendedQualifiedName().substring(pointerDerefPrefix.length());
        } else if (entry.getKey().getExtendedQualifiedName().startsWith(nonPointerDerefPrefix)) {
          suffix =
              entry.getKey().getExtendedQualifiedName().substring(nonPointerDerefPrefix.length());
        } else {
          suffix = null;
        }
        if (suffix != null) {
          MemoryLocation memoryLocation =
              MemoryLocation.parseExtendedQualifiedName(
                  pMemoryLocation.getExtendedQualifiedName() + "->" + suffix);
          NumeralFormula<CompoundInterval> previous = environment.get(memoryLocation);
          if (previous != null) {
            result =
                result.assign(
                    memoryLocation,
                    InvariantsFormulaManager.INSTANCE.<CompoundInterval>asVariable(
                        previous.getTypeInfo(), entry.getKey()));
          }
        }
      }
      return result.assignInternal(pMemoryLocation, value);
    }
    return result.assignInternal(pMemoryLocation, value);
  }

  /**
   * Creates a new state representing the given assignment applied to the current state.
   *
   * @param pMemoryLocation the memory location of the variable being assigned.
   * @param pValue the new value of the variable.
   * @return a new state representing the given assignment applied to the current state.
   */
  private InvariantsState assignInternal(
      MemoryLocation pMemoryLocation, NumeralFormula<CompoundInterval> pValue) {
    Preconditions.checkNotNull(pValue);
    // Only use information from supported variables
    if (isUnsupportedVariableName(pMemoryLocation)) {
      return overapproximateUnsupportedFeature();
    }
    if (FluentIterable.from(pValue.accept(COLLECT_VARS_VISITOR))
        .anyMatch(InvariantsState::isUnsupportedVariableName)) {
      NumeralFormula<CompoundInterval> newEnvValue =
          InvariantsFormulaManager.INSTANCE.asConstant(
              pValue.getTypeInfo(), pValue.accept(getFormulaResolver(), environment));
      InvariantsState result = assignInternal(pMemoryLocation, newEnvValue);
      if (result != null) {
        return result.overapproximateUnsupportedFeature();
      }
      return result;
    }

    // Check if the assigned variable is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection =
        variableSelection.acceptAssignment(pMemoryLocation, pValue);
    if (newVariableSelection == null) {
      // Ensure that no information about the irrelevant assigned variable is retained
      NonRecursiveEnvironment newEnvironment = environment;
      if (environment.containsKey(pMemoryLocation)) {
        newEnvironment = newEnvironment.removeAndCopy(pMemoryLocation);
      }
      if (environment == newEnvironment) {
        return this;
      }
      return new InvariantsState(
          variableSelection,
          tools,
          machineModel,
          variableTypes,
          abstractionState,
          newEnvironment,
          ImmutableSet.of(),
          overflowDetected,
          includeTypeInformation,
          overapproximatesUnsupportedFeature);
    }

    TypeInfo typeInfo = pValue.getTypeInfo();
    Variable<CompoundInterval> variable =
        InvariantsFormulaManager.INSTANCE.asVariable(typeInfo, pMemoryLocation);

    // Optimization: If the value being assigned is equivalent to the value already stored, do
    // nothing
    if ((getEnvironmentValue(typeInfo, pMemoryLocation).equals(pValue)
            && (pValue instanceof Variable<?>
                || (pValue instanceof Constant<?>
                    && ((Constant<CompoundInterval>) pValue).getValue().isSingleton())))
        || variable.accept(
            new StateEqualsVisitor(
                getFormulaResolver(), environment, tools.compoundIntervalManagerFactory),
            pValue)) {
      return this;
    }

    // Compute the assignment
    InvariantsState result =
        assignInternal(pMemoryLocation, pValue, newVariableSelection, tools.evaluationVisitor);

    if (equals(result)) {
      return this;
    }
    return result;
  }

  private InvariantsState overapproximateUnsupportedFeature() {
    if (overapproximatesUnsupportedFeature) {
      return this;
    }
    return new InvariantsState(
        variableSelection,
        tools,
        machineModel,
        variableTypes,
        abstractionState,
        environment,
        assumptions,
        overflowDetected,
        includeTypeInformation,
        true);
  }

  private InvariantsState assignInternal(
      final MemoryLocation pMemoryLocation,
      NumeralFormula<CompoundInterval> pValue,
      VariableSelection<CompoundInterval> newVariableSelection,
      FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor) {
    NonRecursiveEnvironment resultEnvironment = environment;

    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>();

    /*
     * A variable is newly assigned, so the appearances of this variable
     * in any previously collected assumptions (including its new value)
     * have to be resolved with the variable's previous value.
     */
    TypeInfo typeInfo = pValue.getTypeInfo();
    Variable<CompoundInterval> variable =
        InvariantsFormulaManager.INSTANCE.asVariable(typeInfo, pMemoryLocation);
    NumeralFormula<CompoundInterval> previousValue = getEnvironmentValue(typeInfo, pMemoryLocation);
    ReplaceVisitor<CompoundInterval> replaceVisitor = new ReplaceVisitor<>(variable, previousValue);
    resultEnvironment =
        resultEnvironment.putAndCopy(
            pMemoryLocation,
            pValue.accept(replaceVisitor).accept(partialEvaluator, evaluationVisitor));
    if (pValue.accept(new IsLinearVisitor<>(), variable)
        && pValue.accept(containsVarVisitor, pMemoryLocation)) {
      CompoundInterval zero =
          tools.compoundIntervalManagerFactory.createCompoundIntervalManager(typeInfo).singleton(0);
      previousValue =
          pValue.accept(
              new ReplaceVisitor<>(
                  variable, InvariantsFormulaManager.INSTANCE.asConstant(typeInfo, zero)));
      previousValue = tools.compoundIntervalFormulaManager.subtract(variable, previousValue);
    }
    replaceVisitor = new ReplaceVisitor<>(variable, previousValue);

    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> environmentEntry :
        environment.entrySet()) {
      if (!environmentEntry.getKey().equals(pMemoryLocation)) {
        NumeralFormula<CompoundInterval> prevEnvValue = environmentEntry.getValue();
        if (prevEnvValue.accept(containsVarVisitor, pMemoryLocation)) {
          NumeralFormula<CompoundInterval> newEnvValue =
              prevEnvValue.accept(replaceVisitor).accept(partialEvaluator, evaluationVisitor);
          resultEnvironment = resultEnvironment.putAndCopy(environmentEntry.getKey(), newEnvValue);
        }
      }
    }
    InvariantsState result =
        new InvariantsState(
            newVariableSelection,
            tools,
            machineModel,
            variableTypes,
            abstractionState,
            resultEnvironment,
            ImmutableSet.of(),
            overflowDetected,
            includeTypeInformation,
            overapproximatesUnsupportedFeature);

    if (!assumptions.isEmpty()) {
      Set<BooleanFormula<CompoundInterval>> additionalAssumptions = new HashSet<>();
      for (BooleanFormula<CompoundInterval> assumption : assumptions) {
        BooleanFormula<CompoundInterval> evenTemplate = instantiateModTemplate(variable, 2, 0);
        BooleanFormula<CompoundInterval> oddTemplate = instantiateModTemplate(variable, 2, 1);
        BooleanFormula<CompoundInterval> complement = null;
        if (assumption.equals(evenTemplate)) {
          complement = oddTemplate;
        } else if (assumption.equals(oddTemplate)) {
          complement = evenTemplate;
        } else {
          additionalAssumptions.add(assumption.accept(replaceVisitor));
          if (pValue instanceof Variable) {
            additionalAssumptions.add(assumption.accept(new ReplaceVisitor<>(pValue, variable)));
            Mod2AbstractionVisitor.Type t =
                pValue.accept(
                    new Mod2AbstractionVisitor(
                        tools.compoundIntervalManagerFactory,
                        evaluationVisitor,
                        environment,
                        assumptions));
            if (t == Mod2AbstractionVisitor.Type.EVEN) {
              additionalAssumptions.add(instantiateModTemplate(variable, 2, 0));
            } else if (t == Mod2AbstractionVisitor.Type.ODD) {
              additionalAssumptions.add(instantiateModTemplate(variable, 2, 1));
            }
          }
        }
        if (complement != null) {
          if (preservesOrSwitchesMod2(variable, pValue, true)) {
            additionalAssumptions.add(assumption);
            result = result.assume(assumption);
          } else if (preservesOrSwitchesMod2(variable, pValue, false)) {
            additionalAssumptions.add(complement);
            result = result.assume(complement);
          } else if (pValue instanceof Variable) {
            Variable<CompoundInterval> assignedVariable = (Variable<CompoundInterval>) pValue;
            if (definitelyImplies(
                assumption.accept(new ReplaceVisitor<>(variable, assignedVariable)))) {
              additionalAssumptions.add(assumption);
            } else {
              additionalAssumptions.add(assumption.accept(replaceVisitor));
            }
          } else {
            additionalAssumptions.add(assumption.accept(replaceVisitor));
          }
        }
      }
      result = result.addAssumptions(additionalAssumptions);
    }

    return result;
  }

  private boolean preservesOrSwitchesMod2(
      Variable<CompoundInterval> pVariable,
      NumeralFormula<CompoundInterval> pValue,
      boolean pPreserves) {
    TypeInfo typeInfo = pValue.getTypeInfo();
    CompoundIntervalManager cim = getCompoundIntervalManager(typeInfo);
    final Constant<CompoundInterval> constant;
    if (pValue instanceof Add) {
      Add<CompoundInterval> addition = (Add<CompoundInterval>) pValue;
      if (addition.getOperand1().equals(pVariable) && addition.getOperand2() instanceof Constant) {
        constant = (Constant<CompoundInterval>) addition.getOperand2();
      } else if (addition.getOperand2().equals(pVariable)
          && addition.getOperand1() instanceof Constant) {
        constant = (Constant<CompoundInterval>) addition.getOperand1();
      } else {
        return false;
      }
    } else if (pValue instanceof Multiply) {
      Multiply<CompoundInterval> multiplication = (Multiply<CompoundInterval>) pValue;
      if (multiplication.getOperand1().equals(pVariable)
          && multiplication.getOperand2() instanceof Constant) {
        constant = (Constant<CompoundInterval>) multiplication.getOperand2();
      } else if (multiplication.getOperand2().equals(pVariable)
          && multiplication.getOperand1() instanceof Constant) {
        constant = (Constant<CompoundInterval>) multiplication.getOperand1();
      } else {
        return false;
      }
    } else {
      return false;
    }
    int remainder = pPreserves ? 0 : 1;
    return cim.modulo(constant.getValue(), cim.singleton(2)).equals(cim.singleton(remainder));
  }

  private InvariantsState addAssumptions(
      Set<BooleanFormula<CompoundInterval>> pAdditionalAssumptions) {
    if (assumptions.containsAll(pAdditionalAssumptions)) {
      return this;
    }
    return new InvariantsState(
        variableSelection,
        tools,
        machineModel,
        variableTypes,
        abstractionState,
        environment,
        Sets.union(assumptions, pAdditionalAssumptions),
        overflowDetected,
        includeTypeInformation,
        overapproximatesUnsupportedFeature);
  }

  /**
   * Gets a state that has no information about the program and the same information about the
   * analysis as this state.
   *
   * @return a state that has no information about the program and the same information about the
   *     analysis as this state.
   */
  public InvariantsState clear() {
    if (environment.isEmpty()) {
      return this;
    }
    return new InvariantsState(
        variableSelection,
        tools,
        machineModel,
        variableTypes,
        abstractionState,
        NonRecursiveEnvironment.of(tools.compoundIntervalManagerFactory),
        ImmutableSet.of(),
        overflowDetected,
        includeTypeInformation,
        overapproximatesUnsupportedFeature);
  }

  /**
   * Removes the value stored for the given variable.
   *
   * @param pMemoryLocation the variable to remove.
   * @return the new state.
   */
  public InvariantsState clear(MemoryLocation pMemoryLocation) {
    NumeralFormula<CompoundInterval> previous = environment.get(pMemoryLocation);
    final TypeInfo typeInfo;

    if (previous == null) {
      Type type = variableTypes.get(pMemoryLocation);
      if (type == null) {
        return this;
      }
      typeInfo = BitVectorInfo.from(machineModel, type);
    } else {
      typeInfo = previous.getTypeInfo();
    }

    NumeralFormula<CompoundInterval> allPossibleValues = allPossibleValuesFormula(typeInfo);
    InvariantsState result =
        assignInternal(
            pMemoryLocation, allPossibleValues, variableSelection, tools.evaluationVisitor);
    NonRecursiveEnvironment resultEnvironment = result.environment.removeAndCopy(pMemoryLocation);
    result =
        new InvariantsState(
            result.variableSelection,
            result.tools,
            result.machineModel,
            result.variableTypes,
            result.abstractionState,
            resultEnvironment,
            result.assumptions,
            overflowDetected,
            includeTypeInformation,
            result.overapproximatesUnsupportedFeature);
    if (equals(result)) {
      return this;
    }
    return result;
  }

  public InvariantsState clearAll(Predicate<MemoryLocation> pMemoryLocationPredicate) {
    final Set<Variable<CompoundInterval>> toClear = getVariables(pMemoryLocationPredicate);
    ContainsVisitor<CompoundInterval> containsVisitor = new ContainsVisitor<>();
    ContainsVarVisitor<CompoundInterval> containsVarVisitor = new ContainsVarVisitor<>();
    Predicate<NumeralFormula<CompoundInterval>> toClearPredicate = toClear::contains;
    Queue<MemoryLocation> potentialReferrers = new ArrayDeque<>();
    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry :
        environment.entrySet()) {
      if (entry.getValue().accept(containsVisitor, toClearPredicate)) {
        potentialReferrers.add(entry.getKey());
      }
    }

    NonRecursiveEnvironment resultEnvironment = environment;
    Set<BooleanFormula<CompoundInterval>> resultAssumptions = new HashSet<>();
    final CollectFormulasVisitor<CompoundInterval> variableCollectionVisitor =
        new CollectFormulasVisitor<>(Predicates.instanceOf(Variable.class));
    for (BooleanFormula<CompoundInterval> assumption : assumptions) {
      if (Collections.disjoint(assumption.accept(variableCollectionVisitor), toClear)) {
        resultAssumptions.add(assumption);
      }
    }

    Iterator<Variable<CompoundInterval>> toClearIterator = toClear.iterator();
    while (toClearIterator.hasNext()) {
      Variable<CompoundInterval> variable = toClearIterator.next();
      MemoryLocation memoryLocation = variable.getMemoryLocation();
      NumeralFormula<CompoundInterval> previous = resultEnvironment.get(memoryLocation);
      final TypeInfo typeInfo;

      if (previous == null) {
        Type type = variableTypes.get(memoryLocation);
        if (type == null) {
          continue;
        }
        typeInfo = BitVectorInfo.from(machineModel, type);
      } else {
        typeInfo = previous.getTypeInfo();
      }

      NumeralFormula<CompoundInterval> allPossibleValues = allPossibleValuesFormula(typeInfo);
      ReplaceVisitor<CompoundInterval> replaceVisitor =
          new ReplaceVisitor<>(variable, previous == null ? allPossibleValues : previous);

      Iterator<MemoryLocation> potentialReferrerIterator = potentialReferrers.iterator();
      while (potentialReferrerIterator.hasNext()) {
        MemoryLocation key = potentialReferrerIterator.next();
        if (key.equals(memoryLocation)) {
          potentialReferrerIterator.remove();
        } else {
          NumeralFormula<CompoundInterval> previousValue = resultEnvironment.get(key);
          if (previousValue.accept(containsVarVisitor, memoryLocation)) {
            NumeralFormula<CompoundInterval> newEnvValue =
                previousValue
                    .accept(replaceVisitor)
                    .accept(partialEvaluator, tools.evaluationVisitor);
            resultEnvironment = resultEnvironment.putAndCopy(key, newEnvValue);
            if (!newEnvValue.accept(containsVisitor, toClearPredicate)) {
              potentialReferrerIterator.remove();
            }
          }
        }
      }
      resultEnvironment = resultEnvironment.removeAndCopy(memoryLocation);
      toClearIterator.remove();
    }

    InvariantsState result =
        new InvariantsState(
            variableSelection,
            tools,
            machineModel,
            variableTypes,
            abstractionState,
            resultEnvironment,
            resultAssumptions,
            overflowDetected,
            includeTypeInformation,
            overapproximatesUnsupportedFeature);
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
  public ImmutableSet<BooleanFormula<CompoundInterval>> getEnvironmentAsAssumptions() {
    if (environmentAsAssumptions == null) {
      environmentAsAssumptions = getEnvironmentAsAssumptions0();
    }
    return environmentAsAssumptions;
  }

  /**
   * We build an interval formula like <code>{@code A <= X <= B}</code> for each known memory
   * location.
   *
   * <p>Please note that we already try to simplify the interval, i.e., if X has type 'signed int',
   * we return TRUE instead of <code>{@code MIN_INT <= X <= MAX_INT}</code> , because this is
   * trivially satisfied.
   */
  private Iterable<BooleanFormula<CompoundInterval>> getTypeInformationAsAssumptions() {
    List<BooleanFormula<CompoundInterval>> assumptionsIntervals = new ArrayList<>();
    for (Map.Entry<? extends MemoryLocation, ? extends Type> typeEntry : variableTypes.entrySet()) {
      MemoryLocation memoryLocation = typeEntry.getKey();
      Type type = typeEntry.getValue();
      if (BitVectorInfo.isSupported(type)) {
        TypeInfo typeInfo = BitVectorInfo.from(machineModel, typeEntry.getValue());
        CompoundIntervalManager cim =
            tools.compoundIntervalManagerFactory.createCompoundIntervalManager(typeInfo);
        CompoundInterval range = cim.allPossibleValues();
        Variable<CompoundInterval> variable =
            InvariantsFormulaManager.INSTANCE.asVariable(typeInfo, memoryLocation);
        NumeralFormula<CompoundInterval> value = environment.get(memoryLocation);
        if (value == null
            || value.accept(tools.evaluationVisitor, environment).containsAllPossibleValues()) {
          if (range.hasLowerBound()) {
            BooleanFormula<CompoundInterval> lowerBound =
                tools.compoundIntervalFormulaManager.greaterThanOrEqual(
                    variable,
                    InvariantsFormulaManager.INSTANCE.asConstant(
                        typeInfo, cim.singleton(range.getLowerBound())));
            if (!BooleanConstant.getTrue().equals(lowerBound)) {
              assumptionsIntervals.add(lowerBound);
            }
          }
          if (range.hasUpperBound()) {
            BooleanFormula<CompoundInterval> upperBound =
                tools.compoundIntervalFormulaManager.lessThanOrEqual(
                    variable,
                    InvariantsFormulaManager.INSTANCE.asConstant(
                        typeInfo, cim.singleton(range.getUpperBound())));
            if (!BooleanConstant.getTrue().equals(upperBound)) {
              assumptionsIntervals.add(upperBound);
            }
          }
        }
      }
    }
    return assumptionsIntervals;
  }

  private ImmutableSet<BooleanFormula<CompoundInterval>> getEnvironmentAsAssumptions0() {
    CompoundIntervalFormulaManager compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(tools.compoundIntervalManagerFactory);

    ImmutableSet.Builder<BooleanFormula<CompoundInterval>> environmentalAssumptions =
        ImmutableSet.builderWithExpectedSize(assumptions.size());
    environmentalAssumptions.addAll(assumptions);

    List<NumeralFormula<CompoundInterval>> atomic = new ArrayList<>(1);
    Deque<NumeralFormula<CompoundInterval>> toCheck = new ArrayDeque<>(1);
    for (Entry<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> entry :
        environment.entrySet()) {
      NumeralFormula<CompoundInterval> variable =
          InvariantsFormulaManager.INSTANCE.asVariable(
              entry.getValue().getTypeInfo(), entry.getKey());

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
        assumption =
            assumption == null
                ? equation
                : compoundIntervalFormulaManager.logicalOr(assumption, equation);
      }
      if (assumption != null) {
        environmentalAssumptions.add(assumption);
      }
    }
    return environmentalAssumptions.build();
  }

  /**
   * Gets the value of the variable with the given memory location from the environment.
   *
   * @param pTypeInfo the type information of the variable.
   * @param pMemoryLocation the memory location of the variable.
   * @return the value of the variable with the given memory location from the environment.
   */
  private NumeralFormula<CompoundInterval> getEnvironmentValue(
      TypeInfo pTypeInfo, MemoryLocation pMemoryLocation) {
    NumeralFormula<CompoundInterval> environmentValue = environment.get(pMemoryLocation);
    if (environmentValue == null) {
      return allPossibleValuesFormula(pTypeInfo);
    }
    return environmentValue;
  }

  /**
   * Gets an exact formula evaluation visitor.
   *
   * @return an exact formula evaluation visitor.
   */
  public FormulaEvaluationVisitor<CompoundInterval> getFormulaResolver() {
    return tools.evaluationVisitor;
  }

  /**
   * Makes the given assumptions for this state and checks if this state is still valid.
   *
   * @param pAssumptions the assumptions to be made.
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions'
   *     correctness.
   * @param pNewVariableSelection the new variable selection
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>
   *     false</code> otherwise.
   */
  private InvariantsState assumeInternal(
      Collection<? extends BooleanFormula<CompoundInterval>> pAssumptions,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      VariableSelection<CompoundInterval> pNewVariableSelection) {
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
   * @param pEvaluationVisitor the evaluation visitor to use for evaluating the assumptions'
   *     correctness.
   * @param pNewVariableSelection the new variable selection
   * @return <code>true</code> if the state is still valid after the assumptions are made, <code>
   *     false</code> otherwise.
   */
  private InvariantsState assumeInternal(
      BooleanFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      VariableSelection<CompoundInterval> pNewVariableSelection) {
    BooleanFormula<CompoundInterval> assumption =
        pAssumption.accept(partialEvaluator, pEvaluationVisitor);
    // If there are multiple assumptions combined with &&, split them up
    List<BooleanFormula<CompoundInterval>> assumptionParts =
        assumption.accept(SPLIT_CONJUNCTIONS_VISITOR);
    if (assumptionParts.size() > 1) {
      return assumeInternal(assumptionParts, pEvaluationVisitor, pNewVariableSelection);
    }

    if (assumption instanceof BooleanConstant) {
      return BooleanConstant.isTrue(assumption) ? this : null;
    }

    BooleanConstant<CompoundInterval> assumptionEvaluation =
        assumption.accept(pEvaluationVisitor, getEnvironment());
    // If the invariant evaluates to false or is bottom, it represents an invalid state
    if (BooleanConstant.isFalse(assumptionEvaluation)) {
      return null;
    }
    // If the invariant evaluates to true, it adds no value for now
    if (BooleanConstant.isTrue(assumptionEvaluation)) {
      return this;
    }

    // Only use information from supported variables
    if (FluentIterable.from(assumption.accept(COLLECT_VARS_VISITOR))
        .anyMatch(InvariantsState::isUnsupportedVariableName)) {
      return overapproximateUnsupportedFeature();
    }

    NonRecursiveEnvironment.Builder environmentBuilder =
        new NonRecursiveEnvironment.Builder(environment);
    PushAssumptionToEnvironmentVisitor patev =
        new PushAssumptionToEnvironmentVisitor(
            tools.compoundIntervalManagerFactory, pEvaluationVisitor, environmentBuilder);
    if (!assumption.accept(patev, BooleanConstant.<CompoundInterval>getTrue())) {
      assert !BooleanConstant.isTrue(assumptionEvaluation);
      return null;
    }
    // Check all the assumption once more after the environment changed
    if (isDefinitelyFalse(assumption, pEvaluationVisitor)) {
      return null;
    }
    return new InvariantsState(
        pNewVariableSelection,
        tools,
        machineModel,
        variableTypes,
        abstractionState,
        environmentBuilder.build(),
        assumptions,
        overflowDetected,
        includeTypeInformation,
        overapproximatesUnsupportedFeature);
  }

  /**
   * Checks if the given assumption is definitely false for this state.
   *
   * @param pAssumption the assumption to evaluate.
   * @param pEvaluationVisitor the evaluation visitor used to evaluate the assumption within this
   *     state's environment.
   * @return <code>true</code> if the given assumption does definitely not hold for this state's
   *     environment, <code>false</code> if it might.
   */
  private boolean isDefinitelyFalse(
      BooleanFormula<CompoundInterval> pAssumption,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    return BooleanConstant.isFalse(pAssumption.accept(pEvaluationVisitor, getEnvironment()));
  }

  public InvariantsState assume(BooleanFormula<CompoundInterval> pAssumption) {
    // Check if at least one of the involved variables is selected (newVariableSelection != null)
    VariableSelection<CompoundInterval> newVariableSelection =
        variableSelection.acceptAssumption(pAssumption);
    if (newVariableSelection == null) {
      return this;
    }
    FormulaEvaluationVisitor<CompoundInterval> evaluator = getFormulaResolver();
    BooleanFormula<CompoundInterval> assumption = pAssumption.accept(partialEvaluator, evaluator);
    if (assumption instanceof BooleanConstant) {
      // An assumption evaluating to false represents an unreachable state; it can never be
      // fulfilled
      if (BooleanConstant.isFalse(assumption)) {
        return null;
      }
      // An assumption representing nothing more than "true" or "maybe true" adds no information
      return this;
    }

    InvariantsState result = assumeInternal(assumption, evaluator, newVariableSelection);
    if (equalsState(result)) {
      return this;
    }
    for (BooleanFormula<CompoundInterval> additionalAssumption : assumptions) {
      if (result == null) {
        return result;
      }
      result = result.assumeInternal(additionalAssumption, evaluator, newVariableSelection);
    }

    return result;
  }

  @Override
  public org.sosy_lab.java_smt.api.BooleanFormula getFormulaApproximation(
      FormulaManagerView pManager) {
    final ToBitvectorFormulaVisitor toBooleanFormulaVisitor =
        new ToBitvectorFormulaVisitor(pManager, getFormulaResolver());
    return getApproximationFormulas().stream()
        .map(approximation -> approximation.accept(toBooleanFormulaVisitor, getEnvironment()))
        .filter(f -> f != null)
        .collect(pManager.getBooleanFormulaManager().toConjunction());
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximation(
      final FunctionEntryNode pFunctionEntryNode, final CFANode pReferenceNode) {
    Predicate<NumeralFormula<CompoundInterval>> isInvalidVar =
        pFormula ->
            pFormula instanceof Variable
                && !isExportable(((Variable<?>) pFormula).getMemoryLocation(), pFunctionEntryNode);
    Set<ExpressionTree<Object>> approximationsAsCode = new LinkedHashSet<>();
    for (BooleanFormula<CompoundInterval> approximation : getApproximationFormulas()) {
      approximation = replaceInvalid(approximation, isInvalidVar);
      Set<MemoryLocation> memLocs = approximation.accept(new CollectVarsVisitor<>());
      if (!memLocs.isEmpty()
          && Iterables.all(memLocs, memloc -> isExportable(memloc, pFunctionEntryNode))) {
        ExpressionTree<Object> code = formulaToCode(approximation);
        if (code != null) {
          approximationsAsCode.add(code);
        }
      }
    }

    final Set<MemoryLocation> safePointers = new HashSet<>();
    isInvalidVar =
        Predicates.or(
            isInvalidVar,
            pFormula -> {
              if (pFormula instanceof Variable) {
                return !safePointers.contains(((Variable<?>) pFormula).getMemoryLocation());
              }
              return !Iterables.any(pFormula.accept(COLLECT_VARS_VISITOR), this::isPointerOrArray);
            });
    Predicate<NumeralFormula<CompoundInterval>> isNonSingletonConstant =
        pFormula ->
            pFormula instanceof Constant
                && !((Constant<CompoundInterval>) pFormula).getValue().isSingleton();
    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry :
        environment.entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      CType type = variableTypes.get(memoryLocation);
      if (!(type instanceof CPointerType)) {
        continue;
      }
      if (!isExportable(memoryLocation, pFunctionEntryNode)) {
        continue;
      }
      NumeralFormula<CompoundInterval> value = entry.getValue();
      ContainsVisitor<CompoundInterval> containsVisitor = new ContainsVisitor<>();
      if (value.accept(containsVisitor, isNonSingletonConstant)) {
        continue;
      }
      NumeralFormula<CompoundInterval> var =
          InvariantsFormulaManager.INSTANCE.asVariable(value.getTypeInfo(), memoryLocation);
      safePointers.add(memoryLocation);
      for (MemoryLocation otherSafePointer : safePointers) {
        if (otherSafePointer == memoryLocation) {
          continue;
        }
        CType otherType = variableTypes.get(otherSafePointer);
        if (!type.equals(otherType)) {
          continue;
        }
        NumeralFormula<CompoundInterval> otherValue = environment.get(otherSafePointer);
        NumeralFormula<CompoundInterval> otherVar =
            InvariantsFormulaManager.INSTANCE.asVariable(
                otherValue.getTypeInfo(), otherSafePointer);
        BooleanFormula<CompoundInterval> equality =
            InvariantsFormulaManager.INSTANCE.equal(otherVar, var);
        if (definitelyImplies(equality)) {
          ExpressionTree<Object> code = formulaToCode(replaceInvalid(equality, isInvalidVar));
          if (code != null) {
            approximationsAsCode.add(code);
          }
        }
      }
    }
    return And.of(approximationsAsCode);
  }

  private ExpressionTree<Object> formulaToCode(BooleanFormula<CompoundInterval> pFormula) {
    return ExpressionTrees.cast(
        pFormula.accept(
            new ToCodeFormulaVisitor(tools.evaluationVisitor, machineModel), getEnvironment()));
  }

  private ReplaceVisitor<CompoundInterval> getInvalidReplacementVisitor(
      final Predicate<NumeralFormula<CompoundInterval>> isInvalidVar) {
    return new ReplaceVisitor<>(
        isInvalidVar, pFormula -> replaceOrEvaluateInvalid(pFormula, isInvalidVar));
  }

  private BooleanFormula<CompoundInterval> replaceInvalid(
      BooleanFormula<CompoundInterval> pFormula,
      final Predicate<NumeralFormula<CompoundInterval>> pIsAlwaysInvalid) {
    final Predicate<NumeralFormula<CompoundInterval>> pIsPointerOrArray =
        pFormula1 ->
            pFormula1 instanceof Variable
                && isPointerOrArray(((Variable<?>) pFormula1).getMemoryLocation());

    if (pFormula instanceof LogicalAnd) {
      LogicalAnd<CompoundInterval> and = (LogicalAnd<CompoundInterval>) pFormula;
      return InvariantsFormulaManager.INSTANCE.logicalAnd(
          replaceInvalid(and.getOperand1(), pIsAlwaysInvalid),
          replaceInvalid(and.getOperand2(), pIsAlwaysInvalid));
    }

    if (pFormula instanceof LogicalNot) {
      return InvariantsFormulaManager.INSTANCE.logicalNot(
          checkNotNull(
              replaceInvalid(
                  ((LogicalNot<CompoundInterval>) pFormula).getNegated(), pIsAlwaysInvalid)));
    }

    if (pFormula instanceof Equal) {
      Equal<CompoundInterval> eq = (Equal<CompoundInterval>) pFormula;

      if (eq.getOperand1() instanceof Variable
          && eq.getOperand2() instanceof Variable
          && !pIsAlwaysInvalid.apply(eq.getOperand1())
          && !pIsAlwaysInvalid.apply(eq.getOperand2())) {
        return pFormula;
      }

      Predicate<NumeralFormula<CompoundInterval>> isAlwaysInvalid =
          Predicates.or(pIsAlwaysInvalid, pIsPointerOrArray);
      NumeralFormula<CompoundInterval> op1 =
          eq.getOperand1().accept(getInvalidReplacementVisitor(isAlwaysInvalid));
      final Set<MemoryLocation> op1Vars = op1.accept(COLLECT_VARS_VISITOR);
      isAlwaysInvalid =
          Predicates.or(
              isAlwaysInvalid, f -> !Collections.disjoint(op1Vars, f.accept(COLLECT_VARS_VISITOR)));
      NumeralFormula<CompoundInterval> op2 =
          eq.getOperand2().accept(getInvalidReplacementVisitor(isAlwaysInvalid));
      return InvariantsFormulaManager.INSTANCE.equal(op1, op2);
    }

    return pFormula.accept(
        getInvalidReplacementVisitor(Predicates.or(pIsAlwaysInvalid, pIsPointerOrArray)));
  }

  private NumeralFormula<CompoundInterval> replaceOrEvaluateInvalid(
      NumeralFormula<CompoundInterval> pFormula,
      final Predicate<NumeralFormula<CompoundInterval>> pIsInvalid) {
    if (!pIsInvalid.apply(pFormula)) {
      return pFormula;
    }
    CompoundInterval evaluated = pFormula.accept(tools.evaluationVisitor, environment);
    if (!evaluated.isSingleton() && pFormula instanceof Variable) {
      // Try and replace the variable by a fitting value
      ReplaceVisitor<CompoundInterval> evaluateInvalidVars =
          new ReplaceVisitor<>(pIsInvalid, f -> replaceOrEvaluateInvalid(f, pIsInvalid));

      MemoryLocation memoryLocation = ((Variable<?>) pFormula).getMemoryLocation();
      NumeralFormula<CompoundInterval> value =
          getEnvironmentValue(pFormula.getTypeInfo(), memoryLocation);
      value = value.accept(evaluateInvalidVars);
      if (value instanceof Variable) {
        return value;
      }
      CompoundIntervalManager cim =
          tools.compoundIntervalManagerFactory.createCompoundIntervalManager(
              pFormula.getTypeInfo());
      if (value instanceof Constant
          && cim.contains(evaluated, ((Constant<CompoundInterval>) value).getValue())) {
        evaluated = ((Constant<CompoundInterval>) value).getValue();
      }
      if (!evaluated.isSingleton()) {
        // Try and find a variable referring to this variable
        Set<Variable<CompoundInterval>> visited = new HashSet<>();
        Queue<Variable<CompoundInterval>> waitlist = new ArrayDeque<>();
        visited.add((Variable<CompoundInterval>) pFormula);
        waitlist.addAll(visited);
        while (!waitlist.isEmpty()) {
          Variable<CompoundInterval> currentVar = waitlist.poll();
          if (!pIsInvalid.apply(currentVar)) {
            return currentVar;
          }
          for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry :
              environment.entrySet()) {
            if (entry.getValue().equals(currentVar)) {
              Variable<CompoundInterval> entryVar =
                  InvariantsFormulaManager.INSTANCE.asVariable(
                      entry.getValue().getTypeInfo(), entry.getKey());
              if (visited.add(entryVar)) {
                waitlist.offer(entryVar);
              }
            }
          }
        }
      }
    }
    return InvariantsFormulaManager.INSTANCE.asConstant(pFormula.getTypeInfo(), evaluated);
  }

  private Set<BooleanFormula<CompoundInterval>> getApproximationFormulas() {
    Iterable<BooleanFormula<CompoundInterval>> formulas = getEnvironmentAsAssumptions();
    if (includeTypeInformation) {
      formulas = Iterables.concat(formulas, getTypeInformationAsAssumptions());
    }
    Set<BooleanFormula<CompoundInterval>> result = new LinkedHashSet<>();
    for (BooleanFormula<CompoundInterval> formula : formulas) {
      if (formula != null
          && Iterables.all(
              CompoundIntervalFormulaManager.collectVariableNames(formula),
              InvariantsState::isExportable)) {
        result.add(formula);
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
    return equalsState((InvariantsState) pObj);
  }

  private boolean equalsState(InvariantsState pOther) {
    return pOther != null
        && overflowDetected == pOther.overflowDetected
        && includeTypeInformation == pOther.includeTypeInformation
        && machineModel.equals(pOther.machineModel)
        && tools.equals(pOther.tools)
        && variableTypes.equals(pOther.variableTypes)
        && variableSelection.equals(pOther.variableSelection)
        && environment.equals(pOther.environment)
        && assumptions.equals(pOther.assumptions)
        && abstractionState.equals(pOther.abstractionState);
  }

  @Override
  public int hashCode() {
    int result = hash;
    if (result == 0) {
      result =
          Objects.hash(
              overflowDetected,
              includeTypeInformation,
              machineModel,
              tools,
              variableTypes,
              variableSelection,
              environment,
              assumptions,
              abstractionState);
      hash = result;
    }
    return result;
  }

  @Override
  public String toString() {
    return Joiner.on(", ")
        .join(
            Iterables.concat(
                Collections2.transform(
                    environment.entrySet(),
                    pInput -> {
                      MemoryLocation memoryLocation = pInput.getKey();
                      NumeralFormula<?> value = pInput.getValue();
                      if (value instanceof Exclusion) {
                        return String.format(
                            "%s\u2260%s", memoryLocation, ((Exclusion<?>) value).getExcluded());
                      }
                      return String.format("%s=%s", memoryLocation, value);
                    }),
                assumptions));
  }

  public AbstractionState getAbstractionState() {
    return abstractionState;
  }

  /**
   * Gets the environment of this state.
   *
   * @return the environment of this state.
   */
  public Map<MemoryLocation, NumeralFormula<CompoundInterval>> getEnvironment() {
    return Collections.unmodifiableMap(environment);
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  @Override
  public boolean isLessOrEqual(InvariantsState pState2) {
    if (equals(pState2)) {
      return true;
    }
    if (pState2 == null) {
      return false;
    }
    if (!abstractionState.isLessThanOrEqualTo(pState2.abstractionState)) {
      return false;
    }
    // Perform the implication check (if this state definitely implies the other one, it is less
    // than or equal to it)
    for (BooleanFormula<CompoundInterval> rightAssumption : pState2.getEnvironmentAsAssumptions()) {
      if (!definitelyImplies(rightAssumption)) {
        return false;
      }
    }
    return true;
  }

  public boolean definitelyImplies(BooleanFormula<CompoundInterval> pFormula) {
    return tools.compoundIntervalFormulaManager.definitelyImplies(
        assumptions, environment, pFormula, false);
  }

  public InvariantsState widen(
      InvariantsState pOlderState,
      InvariantsPrecision pPrecision,
      @Nullable Set<MemoryLocation> pWideningTargets,
      Set<BooleanFormula<CompoundInterval>> pWideningHints) {

    final Set<MemoryLocation> wideningTargets =
        pWideningTargets == null
            ? environment.keySet()
            : Sets.intersection(pWideningTargets, environment.keySet());

    if (wideningTargets.isEmpty()) {
      return this;
    }

    // Prepare result environment
    NonRecursiveEnvironment resultEnvironment = environment;

    // Find entries that require widening
    Map<MemoryLocation, NumeralFormula<CompoundInterval>> toDo = new HashMap<>();
    for (MemoryLocation memoryLocation : wideningTargets) {
      NumeralFormula<CompoundInterval> oldFormula = pOlderState.environment.get(memoryLocation);
      if (oldFormula == null) {
        continue;
      }
      NumeralFormula<CompoundInterval> currentFormula = environment.get(memoryLocation);
      TypeInfo typeInfo = oldFormula.getTypeInfo();
      CompoundIntervalManager compoundIntervalManager =
          tools.compoundIntervalManagerFactory.createCompoundIntervalManager(typeInfo);
      currentFormula = currentFormula == null ? allPossibleValuesFormula(typeInfo) : currentFormula;
      assert currentFormula.getTypeInfo().equals(typeInfo);
      if (!currentFormula.equals(oldFormula)) {
        NumeralFormula<CompoundInterval> newValueFormula =
            tools
                .compoundIntervalFormulaManager
                .union(
                    currentFormula.accept(partialEvaluator, tools.evaluationVisitor),
                    oldFormula.accept(pOlderState.partialEvaluator, tools.evaluationVisitor))
                .accept(
                    new PartialEvaluator(tools.compoundIntervalManagerFactory),
                    tools.evaluationVisitor);

        // Trim formulas that exceed the maximum depth
        if (currentFormula.accept(FORMULA_DEPTH_COUNT_VISITOR)
            > pPrecision.getMaximumFormulaDepth()) {
          CompoundInterval value =
              compoundIntervalManager.union(
                  currentFormula.accept(tools.evaluationVisitor, environment),
                  oldFormula.accept(tools.evaluationVisitor, pOlderState.getEnvironment()));
          if (!value.isSingleton()) {
            value = compoundIntervalManager.allPossibleValues();
          }
          newValueFormula =
              InvariantsFormulaManager.INSTANCE.asConstant(currentFormula.getTypeInfo(), value);
        }

        resultEnvironment = resultEnvironment.putAndCopy(memoryLocation, newValueFormula);
        toDo.put(memoryLocation, newValueFormula);
      }
    }
    if (toDo.isEmpty()) {
      return this;
    }
    Set<BooleanFormula<CompoundInterval>> additionalHints = new HashSet<>();
    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : toDo.entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      NumeralFormula<CompoundInterval> newValueFormula = entry.getValue();
      TypeInfo typeInfo = entry.getValue().getTypeInfo();

      if (pPrecision.shouldUseMod2Template()) {
        Variable<CompoundInterval> variable =
            InvariantsFormulaManager.INSTANCE.asVariable(typeInfo, memoryLocation);
        additionalHints.add(instantiateModTemplate(variable, 2, 0));
        additionalHints.add(instantiateModTemplate(variable, 2, 1));
      }

      CompoundInterval simpleExactValue =
          newValueFormula.accept(tools.evaluationVisitor, resultEnvironment);
      if (simpleExactValue.isSingleton()) {
        resultEnvironment =
            resultEnvironment.putAndCopy(
                memoryLocation,
                InvariantsFormulaManager.INSTANCE.asConstant(typeInfo, simpleExactValue));
      } else {
        CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(typeInfo);
        NumeralFormula<CompoundInterval> oldFormula =
            pOlderState.getEnvironmentValue(typeInfo, memoryLocation);
        NumeralFormula<CompoundInterval> currentFormula =
            getEnvironmentValue(typeInfo, memoryLocation);
        CompoundInterval oldExactValue =
            oldFormula.accept(tools.evaluationVisitor, pOlderState.environment);
        CompoundInterval currentExactValue =
            currentFormula.accept(tools.evaluationVisitor, environment);
        final CompoundInterval newValue;
        if (compoundIntervalManager.contains(oldExactValue, currentExactValue)) {
          newValue = oldExactValue;
        } else if (compoundIntervalManager
                .lessEqual(oldExactValue, currentExactValue)
                .isDefinitelyTrue()
            || (oldExactValue.hasUpperBound()
                && (!currentExactValue.hasUpperBound()
                    || compare(oldExactValue.getUpperBound(), currentExactValue.getUpperBound())
                        < 0))) {
          newValue =
              compoundIntervalManager.union(oldExactValue, currentExactValue).extendToMaxValue();
        } else if (compoundIntervalManager
                .greaterEqual(oldExactValue, currentExactValue)
                .isDefinitelyTrue()
            || (oldExactValue.hasLowerBound()
                && (!currentExactValue.hasLowerBound()
                    || compare(oldExactValue.getLowerBound(), currentExactValue.getLowerBound())
                        > 0))) {
          newValue =
              compoundIntervalManager.union(oldExactValue, currentExactValue).extendToMinValue();
        } else {
          NumeralFormula<CompoundInterval> newFormula = resultEnvironment.get(memoryLocation);
          if (newFormula == null) {
            newFormula = allPossibleValuesFormula(typeInfo);
          }
          newValue = newFormula.accept(tools.abstractionVisitor, resultEnvironment);
        }
        resultEnvironment =
            resultEnvironment.putAndCopy(
                memoryLocation, InvariantsFormulaManager.INSTANCE.asConstant(typeInfo, newValue));
      }
    }
    final NonRecursiveEnvironment resEnv = resultEnvironment;
    InvariantsState result =
        new InvariantsState(
            variableSelection,
            tools,
            machineModel,
            variableTypes,
            abstractionState,
            resEnv,
            ImmutableSet.of(),
            overflowDetected,
            includeTypeInformation,
            overapproximatesUnsupportedFeature);

    if (pPrecision.shouldUseMod2Template()) {
      for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry : toDo.entrySet()) {
        MemoryLocation memoryLocation = entry.getKey();
        NumeralFormula<CompoundInterval> newValueFormula = entry.getValue();
        TypeInfo typeInfo = entry.getValue().getTypeInfo();
        Mod2AbstractionVisitor.Type t =
            newValueFormula.accept(
                new Mod2AbstractionVisitor(
                    tools.compoundIntervalManagerFactory,
                    tools.evaluationVisitor,
                    environment,
                    assumptions));
        Variable<CompoundInterval> variable =
            InvariantsFormulaManager.INSTANCE.asVariable(typeInfo, memoryLocation);
        if (t == Mod2AbstractionVisitor.Type.EVEN) {
          result = result.assume(instantiateModTemplate(variable, 2, 0));
        } else if (t == Mod2AbstractionVisitor.Type.ODD) {
          result = result.assume(instantiateModTemplate(variable, 2, 1));
        }
      }
    }

    Set<BooleanFormula<CompoundInterval>> additionalAssumptions =
        additionalHints.isEmpty() ? ImmutableSet.of() : new HashSet<>();

    for (BooleanFormula<CompoundInterval> hint :
        FluentIterable.from(Sets.union(pWideningHints, additionalHints))
            .filter(pHint -> wideningTargets.containsAll(pHint.accept(COLLECT_VARS_VISITOR)))
            .filter(this::definitelyImplies)) {
      result = result.assume(hint);
      verifyNotNull(
          result,
          "Widening with hint '%s' led abstract state '%s' to become bottom",
          hint,
          pOlderState);
      if (additionalHints.contains(hint)) {
        additionalAssumptions.add(hint);
      }
    }
    result = result.addAssumptions(additionalAssumptions);

    if (equals(result)) {
      return this;
    }

    return result;
  }

  private BooleanFormula<CompoundInterval> instantiateModTemplate(
      Variable<CompoundInterval> pDividend, int pDivisor, int pRemainder) {
    checkArgument(pDivisor >= 2, "Divisor must be greater than 1.");
    if (pRemainder < 0 || pRemainder >= pDivisor) {
      throw new IllegalArgumentException(
          String.format("The remainder must be between 0 and %d.", pDivisor - 1));
    }
    CompoundIntervalManager compoundIntervalManager =
        getCompoundIntervalManager(pDividend.getTypeInfo());
    BooleanFormula<CompoundInterval> hint =
        InvariantsFormulaManager.INSTANCE.equal(
            InvariantsFormulaManager.INSTANCE.modulo(
                pDividend,
                InvariantsFormulaManager.INSTANCE.asConstant(
                    pDividend.getTypeInfo(), compoundIntervalManager.singleton(pDivisor))),
            InvariantsFormulaManager.INSTANCE.asConstant(
                pDividend.getTypeInfo(), compoundIntervalManager.singleton(pRemainder)));
    return hint;
  }

  @Override
  public InvariantsState join(InvariantsState state2) {
    return join(
        state2,
        InvariantsPrecision.getEmptyPrecision(
            AbstractionStrategyFactories.ALWAYS.createStrategy(
                tools.compoundIntervalManagerFactory, machineModel)));
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
      NonRecursiveEnvironment resultEnvironment =
          NonRecursiveEnvironment.of(tools.compoundIntervalManagerFactory);

      // Get some basic information by joining the environments
      {
        Set<MemoryLocation> todo = new HashSet<>();

        // Join the easy ones first (both values equal or one value top)
        for (MemoryLocation memoryLocation : state1.environment.keySet()) {
          NumeralFormula<CompoundInterval> rightFormula = state2.environment.get(memoryLocation);
          if (rightFormula != null) {
            NumeralFormula<CompoundInterval> leftFormula =
                getEnvironmentValue(rightFormula.getTypeInfo(), memoryLocation);
            if (leftFormula.equals(rightFormula)) {
              resultEnvironment = resultEnvironment.putAndCopy(memoryLocation, leftFormula);
            } else {
              todo.add(memoryLocation);
            }
          }
        }

        // Compute the union of the types
        PersistentSortedMap<MemoryLocation, CType> mergedVariableTypes = state1.variableTypes;
        for (Map.Entry<MemoryLocation, CType> entry : state2.variableTypes.entrySet()) {
          if (!mergedVariableTypes.containsKey(entry.getKey())) {
            mergedVariableTypes = mergedVariableTypes.putAndCopy(entry.getKey(), entry.getValue());
          }
        }

        // Join the harder ones by constructing the union of left and right value for each variable
        for (MemoryLocation memoryLocation : todo) {
          NumeralFormula<CompoundInterval> leftFormula = environment.get(memoryLocation);
          NumeralFormula<CompoundInterval> rightFormula = state2.environment.get(memoryLocation);
          assert leftFormula != null && rightFormula != null;
          CompoundIntervalManager cim =
              tools.compoundIntervalManagerFactory.createCompoundIntervalManager(
                  leftFormula.getTypeInfo());
          NumeralFormula<CompoundInterval> evaluated =
              InvariantsFormulaManager.INSTANCE.asConstant(
                  leftFormula.getTypeInfo(),
                  cim.union(
                      leftFormula.accept(tools.evaluationVisitor, environment),
                      rightFormula.accept(state2.tools.evaluationVisitor, state2.environment)));
          resultEnvironment = resultEnvironment.putAndCopy(memoryLocation, evaluated);
        }
      }

      Set<BooleanFormula<CompoundInterval>> commonAssumptions;
      if (assumptions.isEmpty() && pState2.assumptions.isEmpty()) {
        commonAssumptions = ImmutableSet.of();
      } else {
        commonAssumptions = new HashSet<>(Sets.intersection(assumptions, pState2.assumptions));
        for (BooleanFormula<CompoundInterval> assumption :
            Sets.difference(assumptions, pState2.assumptions)) {
          if (pState2.definitelyImplies(assumption)) {
            commonAssumptions.add(assumption);
          }
        }
        for (BooleanFormula<CompoundInterval> assumption :
            Sets.difference(pState2.assumptions, assumptions)) {
          if (definitelyImplies(assumption)) {
            commonAssumptions.add(assumption);
          }
        }
      }

      VariableSelection<CompoundInterval> resultVariableSelection =
          state1.variableSelection.join(state2.variableSelection);

      AbstractionState abstractionState1 = determineAbstractionState(pPrecision);
      AbstractionState abstractionState2 = pState2.determineAbstractionState(pPrecision);
      AbstractionState joinedAbstractionState = abstractionState1.join(abstractionState2);

      result =
          new InvariantsState(
              resultVariableSelection,
              tools,
              machineModel,
              variableTypes,
              joinedAbstractionState,
              resultEnvironment,
              commonAssumptions,
              overflowDetected,
              includeTypeInformation,
              overapproximatesUnsupportedFeature || pState2.overapproximatesUnsupportedFeature);

      if (result.equalsState(state1)) {
        result = state1;
      }
    }
    return result;
  }

  public BooleanFormula<CompoundInterval> asFormula() {
    BooleanFormula<CompoundInterval> result = BooleanConstant.<CompoundInterval>getTrue();
    for (BooleanFormula<CompoundInterval> assumption : getEnvironmentAsAssumptions()) {
      result = tools.compoundIntervalFormulaManager.logicalAnd(result, assumption);
    }
    return result;
  }

  public Set<MemoryLocation> getVariables() {
    return ImmutableSet.<MemoryLocation>builder()
        .addAll(environment.keySet())
        .addAll(
            from(environment.values())
                .transformAndConcat(value -> value.accept(COLLECT_VARS_VISITOR)))
        .build();
  }

  private Set<Variable<CompoundInterval>> getVariables(
      final Predicate<MemoryLocation> pMemoryLocationPredicate) {
    final Set<Variable<CompoundInterval>> result = new LinkedHashSet<>();
    Predicate<NumeralFormula<CompoundInterval>> pCondition =
        new Predicate<>() {

          @Override
          public boolean apply(NumeralFormula<CompoundInterval> pFormula) {
            if (pFormula instanceof Variable) {
              Variable<?> variable = (Variable<?>) pFormula;
              MemoryLocation memoryLocation = variable.getMemoryLocation();
              return pMemoryLocationPredicate.apply(memoryLocation) && !result.contains(variable);
            }
            return false;
          }
        };
    CollectFormulasVisitor<CompoundInterval> collectVisitor =
        new CollectFormulasVisitor<>(pCondition);
    for (Map.Entry<MemoryLocation, NumeralFormula<CompoundInterval>> entry :
        environment.entrySet()) {
      MemoryLocation memoryLocation = entry.getKey();
      if (pMemoryLocationPredicate.apply(memoryLocation)) {
        result.add(
            InvariantsFormulaManager.INSTANCE.<CompoundInterval>asVariable(
                entry.getValue().getTypeInfo(), memoryLocation));
      }
      for (NumeralFormula<CompoundInterval> formula : entry.getValue().accept(collectVisitor)) {
        Variable<CompoundInterval> variable = (Variable<CompoundInterval>) formula;
        result.add(variable);
      }
    }
    return result;
  }

  public InvariantsState overflowDetected() {
    if (overflowDetected) {
      return this;
    }
    return new InvariantsState(
        variableSelection,
        tools,
        machineModel,
        variableTypes,
        abstractionState,
        environment,
        assumptions,
        true,
        includeTypeInformation,
        overapproximatesUnsupportedFeature);
  }

  @Override
  public String getCPAName() {
    return InvariantsCPA.class.getSimpleName();
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(PROPERTY_OVERFLOW)) {
      return overflowDetected;
    }
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }

  public boolean overapproximatesUnsupportedFeature() {
    return overapproximatesUnsupportedFeature;
  }

  private boolean isPointerOrArray(MemoryLocation pMemoryLocation) {
    Type type = getType(pMemoryLocation);
    return type instanceof CPointerType || type instanceof CArrayType;
  }

  private static boolean isExportable(
      MemoryLocation pMemoryLocation, final FunctionEntryNode pFunctionEntryNode) {
    if (pMemoryLocation.getIdentifier().startsWith("__CPAchecker_TMP_")) {
      return false;
    }
    if (pFunctionEntryNode.getReturnVariable().isPresent()
        && pMemoryLocation.isOnFunctionStack()
        && pMemoryLocation
            .getIdentifier()
            .equals(pFunctionEntryNode.getReturnVariable().get().getName())) {
      return false;
    }
    if (!isExportable(pMemoryLocation)) {
      return false;
    }
    String functionName = pFunctionEntryNode.getFunctionName();
    return !pMemoryLocation.isOnFunctionStack()
        || pMemoryLocation.getFunctionName().equals(functionName);
  }

  private static boolean isExportable(@Nullable MemoryLocation pMemoryLocation) {
    return pMemoryLocation != null
        && !pMemoryLocation.getIdentifier().contains("*")
        && !pMemoryLocation.getIdentifier().contains("->")
        && !pMemoryLocation.getIdentifier().contains(".")
        && !pMemoryLocation.getIdentifier().contains("[");
  }

  private static int compare(Number pOp1, Number pOp2) {
    if (pOp1 instanceof BigInteger && pOp2 instanceof BigInteger) {
      return ((BigInteger) pOp1).compareTo((BigInteger) pOp2);
    }
    if (pOp1 instanceof BigDecimal && pOp2 instanceof BigDecimal) {
      return ((BigDecimal) pOp1).compareTo((BigDecimal) pOp2);
    }
    if (isAssignableToLong(pOp1) && isAssignableToLong(pOp2)) {
      return Long.compare(pOp1.longValue(), pOp2.longValue());
    }
    if (isAssignableToDouble(pOp1) && isAssignableToDouble(pOp2)) {
      return Double.compare(pOp1.doubleValue(), pOp2.doubleValue());
    }
    throw new IllegalArgumentException("Unsupported comparsion: " + pOp1 + " to " + pOp2);
  }

  private static boolean isAssignableToLong(Number pNumber) {
    return pNumber instanceof Long
        || pNumber instanceof Integer
        || pNumber instanceof Short
        || pNumber instanceof Byte;
  }

  private static boolean isAssignableToDouble(Number pNumber) {
    return pNumber instanceof Double || pNumber instanceof Float;
  }

  private static class Tools {

    /** A factory for compound-interval managers. */
    private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

    /** A compound-interval manager. */
    private final CompoundIntervalFormulaManager compoundIntervalFormulaManager;

    /** A visitor used to evaluate formulas as exactly as possible. */
    private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

    /**
     * A visitor that, like the formula evaluation visitor, is used to evaluate formulas, but far
     * less exact to allow for convergence.
     */
    private final FormulaEvaluationVisitor<CompoundInterval> abstractionVisitor;

    private Tools(CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
      compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
      compoundIntervalFormulaManager =
          new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
      evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
      abstractionVisitor = new FormulaAbstractionVisitor(compoundIntervalManagerFactory);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof Tools) {
        // All tools are derived from the factory
        return compoundIntervalManagerFactory.equals(((Tools) pObj).compoundIntervalManagerFactory);
      }
      return false;
    }

    @Override
    public int hashCode() {
      // All tools are derived from the factory
      return compoundIntervalManagerFactory.hashCode();
    }
  }
}
