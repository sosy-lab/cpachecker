// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.equivalence;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.SafeAndUnsafeConstraints;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ValueTransferBasedStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
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

  record CFAPaths(List<CFAEdge> edges, boolean unsafe) {}

  private List<CFAPaths> pathsInARG(ARGState root) {
    logger.log(Level.INFO, "Extracting paths from ARG");
    record ARGStateWithEdges(ARGState state, Set<ARGState> seen, List<CFAEdge> edges) {}
    List<ARGStateWithEdges> allPaths = new ArrayList<>();
    for (ARGState child : root.getChildren()) {
      for (CFAEdge cfaEdge : root.getEdgesToChild(child)) {
        List<CFAEdge> edges = new ArrayList<>();
        edges.add(cfaEdge);
        Set<ARGState> seen = new HashSet<>();
        seen.add(root);
        allPaths.add(new ARGStateWithEdges(child, seen, edges));
      }
    }
    List<CFAPaths> done = new ArrayList<>();
    while (!allPaths.isEmpty()) {
      ARGStateWithEdges current = allPaths.remove(0);
      ARGState currentState = current.state();
      if (currentState.getChildren().isEmpty()) {
        // Leaf node, we can stop here
        done.add(new CFAPaths(current.edges(), currentState.isTarget()));
        continue;
      }
      for (ARGState child : currentState.getChildren()) {
        List<CFAEdge> upcoming = currentState.getEdgesToChild(child);
        if (!Sets.intersection(ImmutableSet.copyOf(current.edges()), ImmutableSet.copyOf(upcoming))
                .isEmpty()
            && current.seen().contains(child)) {
          continue;
        }
        List<CFAEdge> copy = new ArrayList<>(current.edges());
        copy.addAll(upcoming);
        Set<ARGState> seen = new HashSet<>(current.seen());
        seen.add(child);
        allPaths.add(new ARGStateWithEdges(child, seen, copy));
      }
    }
    logger.log(Level.INFO, "Extracted", done.size(), "paths from ARG");
    return done;
  }

  @Override
  public SafeAndUnsafeConstraints export(
      ReachedSet pReachedSet, AnalysisComponents pComponents, AlgorithmStatus pStatus)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    FormulaManagerView fmgr = solver.getFormulaManager();
    PathFormulaManagerImpl pathFormulaManager =
        new PathFormulaManagerImpl(
            fmgr, config, logger, shutdownNotifier, pComponents.cfa(), AnalysisDirection.FORWARD);
    ImmutableList.Builder<BooleanFormula> safe = ImmutableList.builder();
    ImmutableList.Builder<BooleanFormula> unsafe = ImmutableList.builder();
    for (CFAPaths path : pathsInARG((ARGState) pReachedSet.getFirstState())) {
      BooleanFormula formula =
          runSymbolicExecutionOnCex(path.edges(), pComponents.cfa(), pathFormulaManager);
      if (path.unsafe()) {
        unsafe.add(formula);
      } else {
        safe.add(formula);
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

  private BooleanFormula stateToFormula(
      ForgettingCompositeState currentState, PathFormulaManagerImpl pathFormulaManager)
      throws CPATransferException, InterruptedException {
    return solver
        .getFormulaManager()
        .getBooleanFormulaManager()
        .and(
            currentState
                .getConstraintsState()
                .toFormula(solver.getFormulaManager(), pathFormulaManager),
            currentState.getValueState().toFormula(solver.getFormulaManager(), pathFormulaManager));
  }

  BooleanFormula runSymbolicExecutionOnCex(
      List<CFAEdge> cex, CFA cfa, PathFormulaManagerImpl pathFormulaManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    ForgettingCompositeState currentState =
        new ForgettingCompositeState(
            new ValueAnalysisState(cfa.getMachineModel()), new ConstraintsState());
    ValueTransferBasedStrongestPostOperator strongestPostOperator =
        createValueTransferBasedStrongestPostOperator(cfa);
    BooleanFormulaManagerView bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula formula = bmgr.makeTrue();
    for (CFAEdge edge : cex) {
      if (edge instanceof CReturnStatementEdge
          && edge.getPredecessor().getFunctionName().equals("main")) {
        formula = bmgr.and(formula, stateToFormula(currentState, pathFormulaManager));
      }
      Optional<ForgettingCompositeState> optional =
          strongestPostOperator.getStrongestPost(
              currentState, SingletonPrecision.getInstance(), edge);
      if (optional.isPresent()) {
        currentState = optional.orElseThrow();
      } else {
        return bmgr.makeFalse();
      }
    }
    formula = bmgr.and(formula, stateToFormula(currentState, pathFormulaManager));
    String formulaRepr = solver.getFormulaManager().dumpArbitraryFormula(formula);
    return solver.getFormulaManager().parse(formulaRepr.replaceAll("#\\d+", ""));
  }
}
