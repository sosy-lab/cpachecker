// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;

public class ConstraintAndSMGState {

  private final Constraint constraint;

  private final SMGState state;

  private ConstraintAndSMGState(Constraint pConstraint, SMGState pState) {
    Preconditions.checkNotNull(pConstraint);
    Preconditions.checkNotNull(pState);
    constraint = pConstraint;
    state = pState;
  }

  public static ConstraintAndSMGState of(Constraint pConstraint, SMGState pState) {
    return new ConstraintAndSMGState(pConstraint, pState);
  }

  public Constraint getConstraint() {
    return constraint;
  }

  public SMGState getState() {
    return state;
  }
}
