// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

public class BlockStrengtheningOperator {

  private final Solver solver;
  private final PathFormulaManagerImpl pfmgr;

  public BlockStrengtheningOperator(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    Configuration strengtheningConfig =
        Configuration.builder()
            .copyFrom(pConfig)
            .setOption("cpa.predicate.handlePointerAliasing", "false")
            .build();
    pfmgr =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            strengtheningConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.BACKWARD);
  }

  public Solver getSolver() {
    return solver;
  }

  public PathFormulaManagerImpl getPfmgr() {
    return pfmgr;
  }

  public Collection<ValueAnalysisState> strengthen(
      ValueAnalysisState pValueAnalysisState, BlockState pBlockState)
      throws SolverException, InterruptedException {
    if (solver.isUnsat(
        solver
            .getFormulaManager()
            .getBooleanFormulaManager()
            .and(
                pValueAnalysisState.getFormulaApproximation(solver.getFormulaManager()),
                pBlockState.getFormulaApproximation(solver.getFormulaManager())))) {
      return ImmutableSet.of();
    }
    return ImmutableSet.of(pValueAnalysisState);
  }
}
