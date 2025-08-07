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

public enum LocationSetBot implements LocationSet {
  INSTANCE;

  @Override
  public boolean contains(PointerTarget pTarget) {
    return false;
  }

  @Override
  public LocationSet withPointerTargets(Set<PointerTarget> pLocations) {
    return LocationSetBuilder.withPointerTargets(pLocations);
  }

  @Override
  public LocationSet withPointerTargets(LocationSet pElements) {
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
  public boolean containsAllNulls() {
    return false;
  }

  @Override
  public boolean containsAll(LocationSet locationSetToCheck) {
    return locationSetToCheck.isBot();
  }

  @Override
  public String toString() {
    return "⊥";
  }

  @Override
  public boolean containsAnyNull() {
    return false;
  }
}
