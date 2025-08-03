// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.locationset;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.PointerTarget;

public class ExplicitLocationSet implements LocationSet {

  private final SortedSet<PointerTarget> explicitSet;
  private final boolean containsNull;

  private ExplicitLocationSet(Set<PointerTarget> pLocations) {
    assert !pLocations.isEmpty() : "set should not be empty";
    explicitSet = ImmutableSortedSet.copyOf(pLocations);
    containsNull = false;
  }

  private ExplicitLocationSet(Set<PointerTarget> pLocations, boolean pContainsNull) {
    assert (!pLocations.isEmpty() || pContainsNull) : "set should not be empty";
    explicitSet = ImmutableSortedSet.copyOf(pLocations);
    containsNull = pContainsNull;
  }

  private ExplicitLocationSet(boolean pContainsNull) {
    explicitSet = ImmutableSortedSet.of();
    containsNull = pContainsNull;
  }

  @Override
  public boolean mayPointTo(PointerTarget pLocation) {
    return explicitSet.contains(pLocation);
  }

  @Override
  public LocationSet addElements(LocationSet pElements) {
    if (pElements == this) {
      return this;
    }
    if (pElements instanceof ExplicitLocationSet explicitLocationSet) {
      return addElements(explicitLocationSet.explicitSet, explicitLocationSet.containsNull);
    }
    return pElements.addElements(this.getExplicitLocations(), this.containsNull);
  }

  @Override
  public LocationSet addElements(Set<PointerTarget> pLocations) {
    if (explicitSet.containsAll(pLocations)) {
      return this;
    }
    ImmutableSet.Builder<PointerTarget> builder = ImmutableSet.builder();
    builder.addAll(explicitSet);
    builder.addAll(pLocations);
    return new ExplicitLocationSet(builder.build(), containsNull);
  }

  @Override
  public LocationSet addElements(Set<PointerTarget> pLocations, boolean pContainsNull) {
    if (explicitSet.containsAll(pLocations) && containsNull == pContainsNull) {
      return this;
    }
    ImmutableSet.Builder<PointerTarget> builder = ImmutableSet.builder();
    builder.addAll(explicitSet);
    builder.addAll(pLocations);
    boolean newContainsNull = containsNull || pContainsNull;
    return new ExplicitLocationSet(builder.build(), newContainsNull);
  }

  public static LocationSet from(PointerTarget pLocation) {
    return new ExplicitLocationSet(ImmutableSet.of(pLocation));
  }

  public static LocationSet from(Set<PointerTarget> pLocations) {
    if (pLocations.isEmpty()) {
      return LocationSetBot.INSTANCE;
    }
    return new ExplicitLocationSet(pLocations);
  }

  public static LocationSet from(Set<PointerTarget> pLocations, boolean pContainsNull) {
    if (pLocations.isEmpty() && !pContainsNull) {
      return LocationSetBot.INSTANCE;
    }
    return new ExplicitLocationSet(pLocations, pContainsNull);
  }

  public static LocationSet fromNull() {
    return new ExplicitLocationSet(true);
  }

  @Override
  public boolean isBot() {
    return explicitSet.isEmpty() && !containsNull;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  @Override
  public boolean isNull() {
    return containsNull && explicitSet.isEmpty();
  }

  @Override
  public boolean containsNull() {
    return containsNull;
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    if (pElements == this) {
      return true;
    }
    if (pElements instanceof ExplicitLocationSet explicitLocationSet) {
      boolean containsAllElements = explicitSet.containsAll(explicitLocationSet.explicitSet);
      boolean containsRequiredNull = !explicitLocationSet.containsNull || containsNull;
      return containsAllElements && containsRequiredNull;
    }
    return pElements.containsAll(this);
  }

  @Override
  public String toString() {
    List<String> elements = new ArrayList<>();
    if (containsNull) {
      elements.add("NULL");
    }
    for (Object o : explicitSet) {
      elements.add(o.toString());
    }
    return "[" + String.join(", ", elements) + "]";
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof LocationSet otherLocationSet) {
      if (otherLocationSet.isTop()) {
        return false;
      }
      if (otherLocationSet.isBot()) {
        return isBot();
      }
      if (otherLocationSet instanceof ExplicitLocationSet otherExplicitLocationSet) {
        return (explicitSet.equals(otherExplicitLocationSet.explicitSet)
            && containsNull == otherExplicitLocationSet.containsNull);
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * explicitSet.hashCode() + Boolean.hashCode(containsNull);
  }

  public int getSize() {
    return explicitSet.size() + (containsNull ? 1 : 0);
  }

  public int getSizeWithoutNull() {
    return explicitSet.size();
  }

  public Set<PointerTarget> getExplicitLocations() {
    return explicitSet;
  }

  @Override
  public int compareTo(LocationSet pSetToCompare) {
    // This compareTo implementation combines special-case ordering of BOT and TOP
    // with ComparisonChain for internal field comparison of ExplicitLocationSets.
    // This structure is necessary due to lattice semantics of LocationSet.
    if (this.equals(pSetToCompare)) {
      return 0;
    }
    if (pSetToCompare instanceof LocationSetBot) {
      return 1;
    } else if (pSetToCompare instanceof LocationSetTop) {
      return -1;
    } else if (pSetToCompare instanceof ExplicitLocationSet explicitSetToCompare) {
      return ComparisonChain.start()
          .compare(Boolean.compare(containsNull, explicitSetToCompare.containsNull), 0)
          .compare(
              explicitSet, explicitSetToCompare.explicitSet, Ordering.natural().lexicographical())
          .result();
    } else {
      throw new AssertionError(
          "Unexpected implementation of LocationSet: " + pSetToCompare.getClass());
    }
  }
}
