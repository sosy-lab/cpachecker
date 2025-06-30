// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import com.google.common.collect.ComparisonChain;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import static com.google.common.base.Preconditions.checkNotNull;

public class MemoryLocationPointer implements PointerTarget {
  private final MemoryLocation memoryLocation;

  public MemoryLocationPointer(MemoryLocation pMemoryLocation) {
    checkNotNull(pMemoryLocation);
    memoryLocation = pMemoryLocation;
  }

  public MemoryLocation getMemoryLocation() {
    return memoryLocation;
  }

  public boolean isNotLocalVariable() {
    return !memoryLocation.getQualifiedName().contains("::");
  }

  @Override
  public int compareTo(PointerTarget pOther) {
    return (pOther instanceof MemoryLocationPointer other)
        ? ComparisonChain.start().compare(this.memoryLocation, other.memoryLocation).result()
        : this.getClass().getName().compareTo(pOther.getClass().getName());
  }

  @Override
  public boolean equals(Object pOther) {
    return this == pOther
        || (pOther instanceof MemoryLocationPointer other
            && memoryLocation.equals(other.memoryLocation));
  }

  @Override
  public int hashCode() {
    return memoryLocation.hashCode();
  }

  @Override
  public String toString() {
    return memoryLocation.toString();
  }
}
