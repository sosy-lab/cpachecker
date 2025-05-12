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
import com.google.common.collect.ImmutableList.Builder;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.SafeAndUnsafeConstraints;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ARGLeafStrategy implements LeafStrategy {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Solver solver;

  public ARGLeafStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Solver pSolver) {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    solver = pSolver;
  }

  @Override
  public SafeAndUnsafeConstraints export(ReachedSet pReachedSet, CFA pCfa, AlgorithmStatus pStatus)
      throws CPATransferException, InterruptedException, InvalidConfigurationException {
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
    Builder<BooleanFormula> safe = ImmutableList.builder();
    Builder<BooleanFormula> unsafe = ImmutableList.builder();
    for (ARGState state : statesWithoutChildren) {
      if (state.isTarget()) {
        unsafe.add(state.toFormula(solver.getFormulaManager(), pathFormulaManager));
      } else {
        safe.add(state.toFormula(solver.getFormulaManager(), pathFormulaManager));
      }
    }
    return new SafeAndUnsafeConstraints(pStatus, safe.build(), unsafe.build());
  }
}
