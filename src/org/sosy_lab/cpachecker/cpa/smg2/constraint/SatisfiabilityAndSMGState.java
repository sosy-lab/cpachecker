// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult.Satisfiability;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;

public class SatisfiabilityAndSMGState {

  private final Satisfiability sat;

  private final SMGState state;

  private SatisfiabilityAndSMGState(final Satisfiability pSatisfiability, final SMGState pState) {
    Preconditions.checkNotNull(pState);
    sat = pSatisfiability;
    state = pState;
  }

  public static SatisfiabilityAndSMGState of(
      final Satisfiability pSatisfiability, final SMGState pState) {
    return new SatisfiabilityAndSMGState(pSatisfiability, pState);
  }

  public Satisfiability getSatisfiability() {
    return sat;
  }

  public boolean isSAT() {
    return sat.equals(Satisfiability.SAT);
  }

  public boolean isUNSAT() {
    return sat.equals(Satisfiability.UNSAT);
  }

  public SMGState getState() {
    return state;
  }
}
