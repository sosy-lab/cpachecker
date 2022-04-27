// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/**
 * {@link ConstraintsPrecision} that determines whether a {@link Constraint} is tracked based on the
 * memory locations occurring in it.
 */
final class LocationBasedConstraintsPrecision implements ConstraintsPrecision {

  private static final LocationBasedConstraintsPrecision EMPTY =
      new LocationBasedConstraintsPrecision();

  private Set<CFANode> trackedLocations;

  private LocationBasedConstraintsPrecision() {
    trackedLocations = new HashSet<>();
  }

  private LocationBasedConstraintsPrecision(final LocationBasedConstraintsPrecision pOther) {
    trackedLocations = pOther.trackedLocations;
  }

  public static ConstraintsPrecision getEmptyPrecision() {
    return EMPTY;
  }

  @Override
  public boolean isTracked(final Constraint pConstraint, final CFANode pLocation) {
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

    LocationBasedConstraintsPrecision that = (LocationBasedConstraintsPrecision) o;

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
    return "LocationBasedConstraintsPrecision{" + trackedLocations + "}";
  }
}
