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
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.NullLocation;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.PointerTarget;

public record ExplicitLocationSet(ImmutableSortedSet<PointerTarget> sortedPointerTargets)
    implements LocationSet {

  public ExplicitLocationSet {
    Preconditions.checkNotNull(sortedPointerTargets);
    Preconditions.checkArgument(!sortedPointerTargets.isEmpty());
  }

  @Override
  public boolean contains(PointerTarget pLocation) {
    return sortedPointerTargets.contains(pLocation);
  }

  @Override
  public LocationSet withPointerTargets(LocationSet pElements) {
    if (pElements == this) {
      return this;
    }
    if (pElements instanceof ExplicitLocationSet explicitLocationSet) {
      return withPointerTargets(explicitLocationSet.sortedPointerTargets);
    }
    return pElements.withPointerTargets(this.sortedPointerTargets());
  }

  @Override
  public LocationSet withPointerTargets(Set<PointerTarget> pLocations) {
    if (sortedPointerTargets.containsAll(pLocations)) {
      return this;
    }
    ImmutableSortedSet<PointerTarget> pointerTargets =
        ImmutableSortedSet.<PointerTarget>naturalOrder()
            .addAll(sortedPointerTargets)
            .addAll(pLocations)
            .build();

    return new ExplicitLocationSet(pointerTargets);
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
    return sortedPointerTargets.stream().allMatch(target -> target instanceof NullLocation);
  }

  @Override
  public boolean containsAnyNull() {
    return sortedPointerTargets.stream().anyMatch(target -> target instanceof NullLocation);
  }

  @Override
  public boolean containsAll(LocationSet locationSetToCheck) {
    if (locationSetToCheck == this) {
      return true;
    }
    if (locationSetToCheck instanceof ExplicitLocationSet explicitLocationSetToCheck) {
      return sortedPointerTargets.containsAll(explicitLocationSetToCheck.sortedPointerTargets);
    }
    return locationSetToCheck.containsAll(this);
  }

  @Override
  public String toString() {
    return sortedPointerTargets.stream()
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
      return sortedPointerTargets.equals(otherExplicitLocationSet.sortedPointerTargets);
    }

    return false;
  }

  public int getSize() {
    return sortedPointerTargets.size();
  }
}
