/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.pointer2.util;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Iterator;
import java.util.Set;

public class ExplicitLocationSet implements LocationSet, Iterable<MemoryLocation> {

  private final Set<MemoryLocation> explicitSet;

  private ExplicitLocationSet(ImmutableSet<MemoryLocation> pLocations) {
    assert pLocations.size() >= 1;
    this.explicitSet = pLocations;
  }

  @Override
  public boolean mayPointTo(MemoryLocation pLocation) {
    return this.explicitSet.contains(pLocation);
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
    for (MemoryLocation location : this.explicitSet) {
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
