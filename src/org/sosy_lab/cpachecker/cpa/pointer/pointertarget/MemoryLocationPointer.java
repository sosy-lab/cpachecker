// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.pointertarget;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.pointer.util.PointerUtils.compareByType;

import com.google.common.collect.ComparisonChain;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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
    // Compare using ComparisonChain if same type; fallback to type-based comparison otherwise.
    return (pOther instanceof MemoryLocationPointer other)
        ? ComparisonChain.start().compare(this.memoryLocation, other.memoryLocation).result()
        : compareByType(this, pOther);
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

  public static MemoryLocation getMemoryLocation(AbstractSimpleDeclaration pDeclaration) {
    return MemoryLocation.parseExtendedQualifiedName(pDeclaration.getQualifiedName());
  }
}
