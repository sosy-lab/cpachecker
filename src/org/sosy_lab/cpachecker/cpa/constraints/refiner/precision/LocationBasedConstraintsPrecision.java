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

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/**
 * {@link ConstraintsPrecision} that determines whether a {@link Constraint} is tracked based on the
 * memory locations occurring in it.
 */
class LocationBasedConstraintsPrecision implements ConstraintsPrecision {

  private static final LocationBasedConstraintsPrecision EMPTY =
      new LocationBasedConstraintsPrecision();

  private Set<CFANode> trackedLocations;

  private LocationBasedConstraintsPrecision() {
    trackedLocations = new HashSet<>();
  }

  private LocationBasedConstraintsPrecision(
      final LocationBasedConstraintsPrecision pOther
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
    return trackedLocations.contains(pLocation);
  }

  @Override
  public LocationBasedConstraintsPrecision join(final ConstraintsPrecision pOther) {
    assert pOther instanceof LocationBasedConstraintsPrecision;

    LocationBasedConstraintsPrecision that = (LocationBasedConstraintsPrecision) pOther;

    LocationBasedConstraintsPrecision newPrec = new LocationBasedConstraintsPrecision(this);
    newPrec.trackedLocations.addAll(that.trackedLocations);

    return newPrec;
  }

  @Override
  public LocationBasedConstraintsPrecision withIncrement(final Increment pIncrement) {
    assert pIncrement.getTrackedGlobally().isEmpty();
    assert pIncrement.getTrackedInFunction().isEmpty();

    LocationBasedConstraintsPrecision newPrec = new LocationBasedConstraintsPrecision(this);

    newPrec.trackedLocations.addAll(pIncrement.getTrackedLocally().keySet());

    return newPrec;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LocationBasedConstraintsPrecision that = (LocationBasedConstraintsPrecision)o;

    if (!trackedLocations.equals(that.trackedLocations)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return trackedLocations.hashCode();
  }

  @Override
  public String toString() {
    return "LocationBasedConstraintsPrecision{" + trackedLocations.toString() + "}";
  }
}
