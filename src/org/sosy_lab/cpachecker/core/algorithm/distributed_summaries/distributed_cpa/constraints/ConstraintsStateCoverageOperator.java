// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ConstraintsStateCoverageOperator implements CoverageOperator {

  private final ConstraintsCPA constraintsCPA;

  public ConstraintsStateCoverageOperator(ConstraintsCPA pCPA) {
    constraintsCPA = pCPA;
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    ConstraintsState constraintsState1 = (ConstraintsState) state1;
    ConstraintsState constraintsState2 = (ConstraintsState) state2;
    return constraintsCPA.getAbstractDomain().isLessOrEqual(constraintsState1, constraintsState2);
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
