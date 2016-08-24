/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/**
 * Full precision. Tracks all constraints at every location.
 */
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
        FullConstraintsPrecision.class.getSimpleName() + " can't be incremented"
    );
  }
}
