// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/** Full precision. Tracks all constraints at every location. */
public class FullConstraintsPrecision implements ConstraintsPrecision {

  public static FullConstraintsPrecision getInstance() {
    return new FullConstraintsPrecision();
  }

  private FullConstraintsPrecision() {
    // DO NOTHING
  }

  @Override
  public boolean isTracked(final Constraint pConstraint, final CFANode pNode) {
    return true;
  }

  @Override
  public ConstraintsPrecision join(ConstraintsPrecision pOther) {
    throw new UnsupportedOperationException(
        FullConstraintsPrecision.class.getSimpleName() + " can't be joined");
  }

  @Override
  public ConstraintsPrecision withIncrement(Increment pIncrement) {
    throw new UnsupportedOperationException(
        FullConstraintsPrecision.class.getSimpleName() + " can't be incremented");
  }
}
