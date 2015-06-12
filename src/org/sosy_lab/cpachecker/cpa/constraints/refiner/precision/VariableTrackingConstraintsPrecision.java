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

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.MemoryLocationLocator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * {@link ConstraintsPrecision} that determines whether a {@link Constraint} is tracked based on the
 * memory locations occurring in it.
 */
class VariableTrackingConstraintsPrecision implements ConstraintsPrecision {

  private static final VariableTrackingConstraintsPrecision EMPTY =
      new VariableTrackingConstraintsPrecision();

  private Multimap<CFANode, MemoryLocation> trackedLocations;

  private VariableTrackingConstraintsPrecision() {
    trackedLocations = HashMultimap.create();
  }

  private VariableTrackingConstraintsPrecision(
      final VariableTrackingConstraintsPrecision pOther
  ) {
    trackedLocations = pOther.trackedLocations;
  }

  public static ConstraintsPrecision getEmptyPrecision() {
    return EMPTY;
  }

  @Override
  public boolean isTracked(
      final Constraint pConstraint,
      final CFANode pLocation
  ) {
    Collection<MemoryLocation> usedLocations =
        pConstraint.accept(MemoryLocationLocator.getInstance());

    Collection<MemoryLocation> locationsTrackedAtNode = trackedLocations.get(pLocation);

    return locationsTrackedAtNode.containsAll(usedLocations);
  }

  @Override
  public VariableTrackingConstraintsPrecision join(final ConstraintsPrecision pOther) {
    assert pOther instanceof VariableTrackingConstraintsPrecision;

    VariableTrackingConstraintsPrecision that = (VariableTrackingConstraintsPrecision) pOther;

    VariableTrackingConstraintsPrecision newPrec = new VariableTrackingConstraintsPrecision(this);
    newPrec.trackedLocations.putAll(that.trackedLocations);

    return newPrec;
  }

  @Override
  public VariableTrackingConstraintsPrecision withIncrement(final Increment<?> pIncrement) {
    // May produce a class cast exception later on if the wrong increment type is used
    Increment<MemoryLocation> memLocIncr = (Increment<MemoryLocation>) pIncrement;

    assert memLocIncr.getTrackedGlobally().isEmpty();
    assert memLocIncr.getTrackedInFunction().isEmpty();

    VariableTrackingConstraintsPrecision newPrec = new VariableTrackingConstraintsPrecision(this);

    newPrec.trackedLocations.putAll(memLocIncr.getTrackedLocally());

    return newPrec;
  }
}
