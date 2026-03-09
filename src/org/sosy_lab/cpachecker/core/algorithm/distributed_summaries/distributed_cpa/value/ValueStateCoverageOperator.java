// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier.Converter;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierRenamer;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CFormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class ValueStateCoverageOperator implements CoverageOperator {
  ConstraintsSolver constraintsSolver;
  MachineModel machineModel;
  CtoFormulaConverter c2Formula;
  String functionName;

  public ValueStateCoverageOperator(
      MachineModel pMachineModel,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      String pFunctionName)
      throws InvalidConfigurationException {
    Solver solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    FormulaManagerView formulaManager = solver.getFormulaManager();

    c2Formula =
        new CtoFormulaConverter(
            new CFormulaEncodingOptions(pConfig),
            solver.getFormulaManager(),
            pMachineModel,
            Optional.empty(),
            pLogger,
            pShutdownNotifier,
            new CtoFormulaTypeHandler(pLogger, pMachineModel),
            AnalysisDirection.FORWARD);

    constraintsSolver =
        new ConstraintsSolver(
            pConfig, pMachineModel, solver, formulaManager, c2Formula, new ConstraintsStatistics());
    machineModel = pMachineModel;
    functionName = pFunctionName;
  }

  public boolean isSubsumed0(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {

    ValueAnalysisState valState1 = (ValueAnalysisState) state1;
    ValueAnalysisState valState2 = (ValueAnalysisState) state2;

    Set<SymbolicIdentifier> identifiersState1 = new HashSet<>();
    Set<SymbolicIdentifier> identifiersState2 = new HashSet<>();

    for (Entry<MemoryLocation, ValueAndType> constant : valState1.getConstants()) {
      if (constant.getValue().getValue() instanceof SymbolicValue symVal) {
        identifiersState1.addAll(SymbolicValues.getContainedSymbolicIdentifiers(symVal));
      }
      if (constant.getValue().getValue() instanceof SymbolicIdentifier symID) {
        identifiersState1.add(symID);
      }
    }

    ValueAnalysisState valState2Renamed = new ValueAnalysisState(valState2.getMachineModel());

    SymbolicIdentifierRenamer visitor =
        new SymbolicIdentifierRenamer(new HashMap<>(), identifiersState1);
    for (Entry<MemoryLocation, ValueAndType> entry : valState2.getConstants()) {
      MemoryLocation key = entry.getKey();
      if (!valState1.contains(key)
          && !(entry.getValue().getValue() instanceof SymbolicIdentifier)) {
        return false;
      }
      if (entry.getValue().getValue() instanceof SymbolicValue symValue) {
        valState2Renamed.assignConstant(
            entry.getKey(), symValue.accept(visitor), entry.getValue().getType());
        identifiersState2.addAll(SymbolicValues.getContainedSymbolicIdentifiers(symValue));
      } else {
        valState2Renamed.assignConstant(
            entry.getKey(), entry.getValue().getValue(), entry.getValue().getType());
      }
    }

    ConstraintsState constraints =
        new ConstraintsState(
            new HashSet<>(ValueAnalysisState.compareInConstraint(valState1, valState2)));
    BooleanFormulaManagerView bfm =
        constraintsSolver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula comparisonFormula =
        bfm.and(constraintsSolver.getFullFormula(constraints, functionName));

    Map<String, Formula> variables =
        constraintsSolver.getFormulaManager().extractVariables(comparisonFormula);

    List<Formula> existVariables = new ArrayList<>();
    List<Formula> forallVariables = new ArrayList<>();

    for (SymbolicIdentifier id1 : identifiersState1) {
      Formula var = variables.get(Converter.getInstance().convertToStringEncoding(id1) + "@1");
      if (var != null) {
        forallVariables.add(var);
      }
    }
    for (SymbolicIdentifier id2 : identifiersState2) {
      Formula var = variables.get(Converter.getInstance().convertToStringEncoding(id2) + "@1");
      if (var != null) {
        existVariables.add(var);
      }
    }

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
    } catch (SolverException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return isSubsumed0(state1, state2) && isSubsumed0(state2, state1);
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
