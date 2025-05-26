// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import java.util.Set;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class LocationSetTop implements LocationSet {

  public static final LocationSetTop INSTANCE = new LocationSetTop();

  @Override
  public boolean mayPointTo(MemoryLocation pTarget) {
    return true;
  }

  @Override
  public LocationSet addElements(Set<MemoryLocation> pTargets) {
    return this;
  }

  @Override
  public LocationSet addElements(LocationSet pElements) {
    return this;
  }

  @Override
  public LocationSet addElements(Set<MemoryLocation> pLocations, boolean pContainsNull) {
    return this;
  }

  @Override
  public boolean isBot() {
    return false;
  }

  @Override
  public boolean isTop() {
    return true;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean containsNull() {
    return true;
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    return true;
  }

  @Override
  public String toString() {
    return Character.toString('\u22A4');
  }

  @Override
  public int compareTo(LocationSet pOther) {
    // Special-case compareTo implementation for pointer analysis lattice.
    // TOP is defined as greater than all other LocationSets.
    return this.equals(pOther) ? 0 : 1;
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof LocationSetTop;
  }

  @Override
  public int hashCode() {
    return Integer.MAX_VALUE;
  }
}
