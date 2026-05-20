// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.symbolic_execution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.symbolicExecution.SymbolicExecutionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier.Converter;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class SymbolicExecutionStateCoverageOperator implements CoverageOperator {
  ConstraintsSolver constraintsSolver;
  String functionName;

  public SymbolicExecutionStateCoverageOperator(
      ConstraintsSolver pConstraintsSolver, String pFunctionName) {
    constraintsSolver = pConstraintsSolver;
    functionName = pFunctionName;
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    SymbolicExecutionState symbolicState1 = (SymbolicExecutionState) state1;
    SymbolicExecutionState symbolicState2 =
        ((SymbolicExecutionState) state2).renameIDs(symbolicState1.getSymbolicIdentifiers());

    return isSubsumedForValueStates(
            symbolicState1.valueAnalysisState(), symbolicState2.valueAnalysisState())
        && isSubsumedForConstraints(
            symbolicState1.constraintsState(),
            symbolicState2.constraintsState(),
            ValueAnalysisState.compareInConstraint(
                symbolicState1.valueAnalysisState(), symbolicState2.valueAnalysisState()));
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }

  private boolean isSubsumedForValueStates(ValueAnalysisState pState1, ValueAnalysisState pState2)
      throws UnrecognizedCodeException, InterruptedException {

    // Quick syntactic checks
    if (pState1.isLessOrEqual(pState2)) {
      return true;
    }

    for (Entry<MemoryLocation, ValueAndType> entry : pState2.getConstants()) {
      if (!pState1.contains(entry.getKey())
          && !(entry.getValue().getValue() instanceof ConstantSymbolicExpression constEx
              && constEx.getValue() instanceof SymbolicIdentifier)) {
        return false;
      }
    }

    // Quantified solver check
    ConstraintsState constraints =
        new ConstraintsState(
            new HashSet<>(ValueAnalysisState.compareInConstraint(pState1, pState2)));
    BooleanFormulaManagerView bfm =
        constraintsSolver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula comparisonFormula =
        bfm.and(constraintsSolver.getFullFormula(constraints, functionName));

    Map<String, Formula> variables =
        constraintsSolver.getFormulaManager().extractVariables(comparisonFormula);

    List<Formula> existVariables = getIdentifiersAsFormulas(pState1, variables);
    List<Formula> forallVariables = getIdentifiersAsFormulas(pState2, variables);

    QuantifiedFormulaManager qfm =
        constraintsSolver.getFormulaManager().getQuantifiedFormulaManager();
    BooleanFormula quantifiedExists = comparisonFormula;

    if (!existVariables.isEmpty()) {
      quantifiedExists = qfm.exists(existVariables, comparisonFormula);
    }
    BooleanFormula quantifiedForall = quantifiedExists;

    if (!forallVariables.isEmpty()) {
      quantifiedForall = qfm.forall(forallVariables, quantifiedExists);
    }

    try {
      return !constraintsSolver.getSolver().isUnsat(quantifiedForall);
    } catch (SolverException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Formula> getIdentifiersAsFormulas(
      ValueAnalysisState pState, Map<String, Formula> pVariableMap) {
    Set<SymbolicIdentifier> identifiersState1 = new HashSet<>();
    List<Formula> variables = new ArrayList<>();
    for (Entry<MemoryLocation, ValueAndType> constant : pState.getConstants()) {
      if (constant.getValue().getValue() instanceof SymbolicValue symVal) {
        identifiersState1.addAll(SymbolicValues.getContainedSymbolicIdentifiers(symVal));
      }
    }
    for (SymbolicIdentifier id1 : identifiersState1) {
      Formula var = pVariableMap.get(Converter.getInstance().convertToStringEncoding(id1) + "@1");
      if (var != null) {
        variables.add(var);
      }
    }
    return variables;
  }

  private boolean isSubsumedForConstraints(
      ConstraintsState pState1, ConstraintsState pState2, List<Constraint> pValueComparison)
      throws CPAException, InterruptedException {

    if (pState1.containsAll(pState2)) {
      return true;
    }

    BooleanFormulaManagerView bfm =
        constraintsSolver.getFormulaManager().getBooleanFormulaManager();

    BooleanFormula stateAsFormula1 =
        bfm.and(constraintsSolver.getFullFormula(pState1, functionName));
    BooleanFormula stateAsFormula2 =
        bfm.and(constraintsSolver.getFullFormula(pState2, functionName));

    BooleanFormula compareValues =
        bfm.and(constraintsSolver.getFullFormula(pValueComparison, functionName));

    try {
      return constraintsSolver
          .getSolver()
          .implies(bfm.and(stateAsFormula1, compareValues), stateAsFormula2);
    } catch (SolverException e) {
      throw new CPAException("Solver encountered an issue when calculating implication.", e);
    }
  }
}
