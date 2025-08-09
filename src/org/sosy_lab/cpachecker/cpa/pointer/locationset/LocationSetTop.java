// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.locationset;

import java.util.Set;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;

public enum LocationSetTop implements LocationSet {
  INSTANCE;

  @Override
  public boolean contains(PointerLocation pTarget) {
    return true;
  }

  @Override
  public LocationSet withPointerTargets(LocationSet pElements) {
    return this;
  }

  @Override
  public LocationSet withPointerTargets(Set<PointerLocation> pLocations) {
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
  public boolean containsAllNulls() {
    return false;
  }

  @Override
  public boolean containsAnyNull() {
    return true;
  }

  @Override
  public boolean containsAll(LocationSet locationSetToCheck) {
    // We are on the top, so we contain all other sets.
    return true;
  }

  @Override
  public String toString() {
    return Character.toString('\u22A4');
  }
}
