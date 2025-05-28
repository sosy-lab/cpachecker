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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.SafeAndUnsafeConstraints;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class MixEquivalenceStrategy implements LeafStrategy {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Solver solver;
  private final SymbolicExecutionLeafStrategy symbolicExecutionLeafStrategy;

  public MixEquivalenceStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Solver pSolver) {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    solver = pSolver;
    symbolicExecutionLeafStrategy =
        new SymbolicExecutionLeafStrategy(pConfig, pLogger, pShutdownNotifier, pSolver);
  }

  @Override
  public SafeAndUnsafeConstraints export(
      ReachedSet pReachedSet, AnalysisComponents pComponents, AlgorithmStatus pStatus)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    PathFormulaManagerImpl pathFormulaManager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            config,
            logger,
            shutdownNotifier,
            pComponents.cfa(),
            AnalysisDirection.FORWARD);
    BooleanFormulaManagerView bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    FluentIterable<ARGState> statesWithoutChildren =
        LeafStrategy.filterStatesWithNoChildren(pReachedSet);
    ImmutableList.Builder<BooleanFormula> safe = ImmutableList.builder();
    ImmutableList.Builder<BooleanFormula> unsafe = ImmutableList.builder();
    for (ARGState state : statesWithoutChildren) {
      BooleanFormula formula = state.toFormula(solver.getFormulaManager(), pathFormulaManager);
      if (bmgr.isTrue(formula)) {
        formula = bmgr.makeFalse();
        for (ARGPath path : ARGUtils.getAllPaths(pReachedSet, state)) {
          formula =
              bmgr.or(
                  formula,
                  symbolicExecutionLeafStrategy.runSymbolicExecutionOnCex(
                      path.getFullPath(), pComponents.cfa(), pathFormulaManager));
        }
      }
      if (state.isTarget()) {
        unsafe.add(formula);
      } else {
        safe.add(formula);
      }
    }
    return new SafeAndUnsafeConstraints(
        pStatus, safe.build(), unsafe.build(), LeafStrategy.findTouchedLines(pReachedSet));
  }
}
