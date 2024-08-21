// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer2.util;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public enum LocationSetBot implements LocationSet {
  INSTANCE;

  @Override
  public boolean mayPointTo(MemoryLocation pTarget) {
    return false;
  }

  @Override
  public LocationSet addElement(MemoryLocation pTarget) {
    return ExplicitLocationSet.from(pTarget);
  }

  @Override
  public LocationSet removeElement(MemoryLocation pTarget) {
    return this;
  }

  @Override
  public LocationSet addElements(Iterable<MemoryLocation> pTargets) {
    return ExplicitLocationSet.from(pTargets);
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
  public LocationSet addElements(LocationSet pElements) {
    return pElements;
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    return pElements.isBot();
  }

  @Override
  public String toString() {
    return Character.toString('\u22A5');
  }
}
