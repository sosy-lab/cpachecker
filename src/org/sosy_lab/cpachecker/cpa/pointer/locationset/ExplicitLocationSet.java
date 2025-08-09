// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.locationset;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.pointer.location.NullLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;

public record ExplicitLocationSet(ImmutableSortedSet<PointerLocation> sortedPointerLocations)
    implements LocationSet {

  public ExplicitLocationSet {
    Preconditions.checkNotNull(sortedPointerLocations);
    Preconditions.checkArgument(!sortedPointerLocations.isEmpty());
  }

  @Override
  public boolean contains(PointerLocation pLocation) {
    return sortedPointerLocations.contains(pLocation);
  }

  @Override
  public LocationSet withPointerTargets(LocationSet pElements) {
    if (pElements == this) {
      return this;
    }
    if (pElements instanceof ExplicitLocationSet explicitLocationSet) {
      return withPointerTargets(explicitLocationSet.sortedPointerLocations);
    }
    return pElements.withPointerTargets(this.sortedPointerLocations());
  }

  @Override
  public LocationSet withPointerTargets(Set<PointerLocation> pLocations) {
    if (sortedPointerLocations.containsAll(pLocations)) {
      return this;
    }
    ImmutableSortedSet<PointerLocation> pointerLocations =
        ImmutableSortedSet.<PointerLocation>naturalOrder()
            .addAll(sortedPointerLocations)
            .addAll(pLocations)
            .build();

    return new ExplicitLocationSet(pointerLocations);
  }

  @Override
  public boolean isBot() {
    return false;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  @Override
  public boolean containsAllNulls() {
    return sortedPointerLocations.stream().allMatch(target -> target instanceof NullLocation);
  }

  @Override
  public boolean containsAnyNull() {
    return sortedPointerLocations.stream().anyMatch(target -> target instanceof NullLocation);
  }

  @Override
  public boolean containsAll(LocationSet locationSetToCheck) {
    if (locationSetToCheck == this) {
      return true;
    }
    if (locationSetToCheck instanceof ExplicitLocationSet explicitLocationSetToCheck) {
      return sortedPointerLocations.containsAll(explicitLocationSetToCheck.sortedPointerLocations);
    }
    return locationSetToCheck.containsAll(this);
  }

  @Override
  public String toString() {
    return sortedPointerLocations.stream()
        .map(Object::toString)
        .collect(Collectors.joining(", ", "[", "]"));
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    // Compare with other location set.
    if (pOther instanceof LocationSet otherLocationSet) {
      return equalsToOtherLocationSet(otherLocationSet);
    }
    return false;
  }

  private boolean equalsToOtherLocationSet(LocationSet otherLocationSet) {
    if (otherLocationSet.isBot()) {
      return isBot();
    }
    if (otherLocationSet instanceof ExplicitLocationSet otherExplicitLocationSet) {
      return sortedPointerLocations.equals(otherExplicitLocationSet.sortedPointerLocations);
    }

    return false;
  }

  public int getSize() {
    return sortedPointerLocations.size();
  }
}
