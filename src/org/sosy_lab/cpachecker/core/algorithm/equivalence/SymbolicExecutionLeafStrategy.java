// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.equivalence;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.SafeAndUnsafeConstraints;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ValueTransferBasedStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class SymbolicExecutionLeafStrategy implements LeafStrategy {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Solver solver;

  public SymbolicExecutionLeafStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Solver pSolver) {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    solver = pSolver;
  }

  private ValueTransferBasedStrongestPostOperator createValueTransferBasedStrongestPostOperator(
      CFA pCfa) throws InvalidConfigurationException {
    CtoFormulaConverter converter =
        initializeCToFormulaConverter(
            solver.getFormulaManager(), logger, config, shutdownNotifier, pCfa.getMachineModel());
    ConstraintsSolver constraintsSolver =
        new ConstraintsSolver(
            config, solver, solver.getFormulaManager(), converter, new ConstraintsStatistics());
    return new ValueTransferBasedStrongestPostOperator(constraintsSolver, logger, config, pCfa);
  }

  @Override
  public SafeAndUnsafeConstraints export(ReachedSet pReachedSet, CFA pCfa, AlgorithmStatus pStatus)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    PathFormulaManagerImpl pathFormulaManager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            config,
            logger,
            shutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);
    FluentIterable<ARGState> statesWithoutChildren =
        LeafStrategy.filterStatesWithNoChildren(pReachedSet);
    ImmutableList.Builder<BooleanFormula> safe = ImmutableList.builder();
    ImmutableList.Builder<BooleanFormula> unsafe = ImmutableList.builder();
    for (ARGState state : statesWithoutChildren) {
      for (ARGPath path : ARGUtils.getAllPaths(pReachedSet, state)) {
        BooleanFormula formula =
            runSymbolicExecutionOnCex(path.getFullPath(), pCfa, pathFormulaManager);
        if (state.isTarget()) {
          unsafe.add(formula);
        } else {
          safe.add(formula);
        }
      }
    }
    return new SafeAndUnsafeConstraints(
        pStatus, safe.build(), unsafe.build(), LeafStrategy.findTouchedLines(pReachedSet));
  }

  // Can only be called after machineModel and formulaManager are set
  private CtoFormulaConverter initializeCToFormulaConverter(
      FormulaManagerView pFormulaManager,
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel)
      throws InvalidConfigurationException {

    FormulaEncodingWithPointerAliasingOptions options =
        new FormulaEncodingWithPointerAliasingOptions(pConfig);
    TypeHandlerWithPointerAliasing typeHandler =
        new TypeHandlerWithPointerAliasing(pLogger, pMachineModel, options);

    return new CToFormulaConverterWithPointerAliasing(
        options,
        pFormulaManager,
        pMachineModel,
        Optional.empty(),
        pLogger,
        pShutdownNotifier,
        typeHandler,
        AnalysisDirection.FORWARD);
  }

  BooleanFormula runSymbolicExecutionOnCex(
      List<CFAEdge> cex, CFA cfa, PathFormulaManagerImpl pathFormulaManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    ForgettingCompositeState currentState =
        new ForgettingCompositeState(
            new ValueAnalysisState(cfa.getMachineModel()), new ConstraintsState());
    ValueTransferBasedStrongestPostOperator strongestPostOperator =
        createValueTransferBasedStrongestPostOperator(cfa);
    for (CFAEdge edge : cex) {
      Optional<ForgettingCompositeState> optional =
          strongestPostOperator.getStrongestPost(
              currentState, SingletonPrecision.getInstance(), edge);
      if (optional.isPresent()) {
        currentState = optional.orElseThrow();
      } else {
        return solver.getFormulaManager().getBooleanFormulaManager().makeFalse();
      }
    }
    BooleanFormula result =
        solver
            .getFormulaManager()
            .getBooleanFormulaManager()
            .and(
                currentState
                    .getConstraintsState()
                    .toFormula(solver.getFormulaManager(), pathFormulaManager),
                currentState
                    .getValueState()
                    .toFormula(solver.getFormulaManager(), pathFormulaManager));
    String formulaRepr = solver.getFormulaManager().dumpArbitraryFormula(result);
    return solver.getFormulaManager().parse(formulaRepr.replaceAll("#\\d+", ""));
  }
}
