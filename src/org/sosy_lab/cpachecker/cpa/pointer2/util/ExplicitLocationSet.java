// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer2.util;

import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ExplicitLocationSet implements LocationSet, Iterable<MemoryLocation> {

  private final Set<MemoryLocation> explicitSet;

  private ExplicitLocationSet(ImmutableSet<MemoryLocation> pLocations) {
    assert pLocations.size() >= 1;
    explicitSet = pLocations;
  }

  @Override
  public boolean mayPointTo(MemoryLocation pLocation) {
    return explicitSet.contains(pLocation);
  }

  @Override
  public LocationSet addElement(MemoryLocation pLocation) {
    if (explicitSet.contains(pLocation)) {
      return this;
    }
    ImmutableSet.Builder<MemoryLocation> builder = ImmutableSet.builder();
    builder.addAll(explicitSet).add(pLocation);
    return new ExplicitLocationSet(builder.build());
  }

  @Override
  public LocationSet addElements(Iterable<MemoryLocation> pLocations) {
    ImmutableSet.Builder<MemoryLocation> builder = null;
    for (MemoryLocation target : pLocations) {
      if (!explicitSet.contains(target)) {
        if (builder == null) {
          builder = ImmutableSet.builder();
          builder.addAll(explicitSet);
        }
        builder.add(target);
      }
    }
    if (builder == null) {
      return this;
    }
    return new ExplicitLocationSet(builder.build());
  }

  @Override
  public LocationSet removeElement(MemoryLocation pLocation) {
    if (!explicitSet.contains(pLocation)) {
      return this;
    }
    if (getSize() == 1) {
      return LocationSetBot.INSTANCE;
    }
    ImmutableSet.Builder<MemoryLocation> builder = ImmutableSet.builder();
    for (MemoryLocation location : explicitSet) {
      if (!location.equals(pLocation)) {
        builder.add(location);
      }
    }
    return new ExplicitLocationSet(builder.build());
  }

  public static LocationSet from(MemoryLocation pLocation) {
    return new ExplicitLocationSet(ImmutableSet.of(pLocation));
  }

  public static LocationSet from(Iterable<? extends MemoryLocation> pLocations) {
    Iterator<? extends MemoryLocation> elementIterator = pLocations.iterator();
    if (!elementIterator.hasNext()) {
      return LocationSetBot.INSTANCE;
    }
    ImmutableSet.Builder<MemoryLocation> builder = ImmutableSet.builder();
    while (elementIterator.hasNext()) {
      MemoryLocation location = elementIterator.next();
      builder.add(location);
    }
    return new ExplicitLocationSet(builder.build());
  }

  @Override
  public boolean isBot() {
    return explicitSet.isEmpty();
  }

  @Override
  public boolean isTop() {
    return false;
  }

  @Override
  public LocationSet addElements(LocationSet pElements) {
    if (pElements == this) {
      return this;
    }
    if (pElements instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pElements;
      return addElements(explicitLocationSet.explicitSet);
    }
    return pElements.addElements((Iterable<MemoryLocation>) this);
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    if (pElements == this) {
      return true;
    }
    if (pElements instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pElements;
      return explicitSet.containsAll(explicitLocationSet.explicitSet);
    }
    return pElements.containsAll(this);
  }

  @Override
  public String toString() {
    return explicitSet.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof LocationSet) {
      LocationSet o = (LocationSet) pO;
      if (o.isTop()) {
        return false;
      }
      if (o.isBot()) {
        return explicitSet.isEmpty();
      }
      if (o instanceof ExplicitLocationSet) {
        ExplicitLocationSet other = (ExplicitLocationSet) o;
        return explicitSet.equals(other.explicitSet);
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (isBot()) {
      return LocationSetBot.INSTANCE.hashCode();
    }
    return explicitSet.hashCode();
  }

  @Override
  public Iterator<MemoryLocation> iterator() {
    return explicitSet.iterator();
  }

  /**
   * Gets the size of the explicit location set.
   *
   * @return the size of the explicit location set.
   */
  public int getSize() {
    return explicitSet.size();
  }
}
