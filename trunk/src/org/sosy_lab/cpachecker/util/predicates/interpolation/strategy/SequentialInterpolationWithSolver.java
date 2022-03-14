// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class SequentialInterpolationWithSolver extends ITPStrategy {

  /**
   * This strategy uses a SMT solver that directly computes a sequence of inductive interpolants.
   * Currently all SMT solvers except MathSat support this.
   */
  public SequentialInterpolationWithSolver(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, FormulaManagerView pFmgr) {
    super(pLogger, pShutdownNotifier, pFmgr);
  }

  @Override
  public <T> List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
      throws InterruptedException, SolverException {
    return interpolator.itpProver.getSeqInterpolants0(
        projectToThird(formulasWithStatesAndGroupdIds));
  }
}
