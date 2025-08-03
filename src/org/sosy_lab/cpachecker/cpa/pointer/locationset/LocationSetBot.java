// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.locationset;

import java.util.Set;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.PointerTarget;

public class LocationSetBot implements LocationSet {

  public static final LocationSetBot INSTANCE = new LocationSetBot();

  @Override
  public boolean mayPointTo(PointerTarget pTarget) {
    return false;
  }

  @Override
  public LocationSet addElements(Set<PointerTarget> pTargets) {
    return ExplicitLocationSet.from(pTargets);
  }

  @Override
  public LocationSet addElements(Set<PointerTarget> pLocations, boolean pContainsNull) {
    return ExplicitLocationSet.from(pLocations, pContainsNull);
  }

  @Override
  public LocationSet addElements(LocationSet pElements) {
    return pElements;
  }

  @Override
  public boolean isBot() {
    return true;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    return pElements.isBot();
  }

  @Override
  public String toString() {
    return Character.toString('\u22A5');
  }

  @Override
  public boolean containsNull() {
    return false;
  }

  @Override
  public int compareTo(LocationSet pOther) {
    // Special-case compareTo implementation for pointer analysis lattice.
    // BOT is defined as less than all other LocationSets.
    return this.equals(pOther) ? 0 : -1;
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof LocationSetBot;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
