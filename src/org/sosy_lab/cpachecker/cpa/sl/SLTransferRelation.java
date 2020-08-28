// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sl;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class SLTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;
  private final Solver solver;
  private final PathFormulaManager pfm;
  private final SLStatistics stats;

  private SLState state;

  public SLTransferRelation(
      LogManager pLogger,
      Solver pSolver,
      PathFormulaManager pPfm,
      SLStatistics pStats) {
    logger = pLogger;
    solver = pSolver;
    pfm = pPfm;
    stats = pStats;
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {
    state = ((SLState) pState).copyWithoutErrors();
    pfm.setContext(state);
    pfm.makeAnd(state.getPathFormula(), pCfaEdge);

    // String info = "";
    // info += pCfaEdge.getCode() + "\n";
    // info += state + "\n";
    // info += "---------------------------";
    // logger.log(Level.INFO, info);
    if (pCfaEdge instanceof AssumeEdge) {
      try {
        return handleAssumption();
      } catch (SolverException e) {
        throw new CPATransferException("Termination check failed.", e);
      }
    }
    return ImmutableList.of(state);
  }

  private List<SLState> handleAssumption() throws SolverException, InterruptedException {
    BooleanFormula constraints =
        solver.getFormulaManager().getBooleanFormulaManager().and(state.getConstraints());
    boolean unsat = false;
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.addConstraint(constraints);
      stats.startSolverTime();
      unsat = prover.isUnsat();
    } catch (SolverException | InterruptedException e) {
      logger.log(Level.SEVERE, e.getMessage());
      throw e;
    } finally {
      stats.stopSolverTime();
    }
    if (unsat) {
      return Collections.emptyList();
    }
    return ImmutableList.of(state);
  }
}
