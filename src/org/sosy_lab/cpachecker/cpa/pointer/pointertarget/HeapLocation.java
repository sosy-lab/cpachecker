// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.pointertarget;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.pointer.util.PointerUtils.compareByType;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HeapLocation implements PointerTarget {
  private final String functionName;
  private final String identifier;
  private final @Nullable Long offset;

  private HeapLocation(String pFunctionName, String pIdentifier, Long pOffset) {
    functionName = pFunctionName;
    identifier = pIdentifier;
    offset = pOffset;
  }

  public static HeapLocation forAllocation(
      String pFunctionName, int pIndex, @Nullable Long pOffset) {
    String finalIdentifier;
    if (pIndex == -1) {
      finalIdentifier = "heap_obj";
    } else {
      finalIdentifier = "heap_obj" + pIndex;
    }
    return new HeapLocation(pFunctionName, finalIdentifier, pOffset);
  }

  public static HeapLocation forLineBasedAllocation(
      String pFunctionName, int lineNumber, @Nullable Long pOffset) {
    return new HeapLocation(pFunctionName, "heap_obj" + lineNumber, pOffset);
  }

  @Override
  public int compareTo(PointerTarget pOther) {
    // Custom type check required for comparing different PointerTarget subclasses.
    if (!(pOther instanceof HeapLocation other)) {
      return compareByType(this, pOther);
    }

    return ComparisonChain.start()
        .compare(this.identifier, other.identifier)
        .compare(this.functionName, other.functionName)
        .compare(offset, other.offset, Ordering.natural().nullsFirst())
        .result();
  }

  @Override
  public boolean equals(Object pOther) {
    return this == pOther
        || (pOther instanceof HeapLocation other
            && identifier.equals(other.identifier)
            && functionName.equals(other.functionName)
            && Objects.equals(offset, other.offset));
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, functionName, offset);
  }

  @Override
  public String toString() {
    String heapName = functionName + "::" + identifier;
    if (offset == null) {
      return heapName;
    }
    return heapName + "/" + offset;
  }

  public boolean isReference() {
    return offset != null;
  }

  public long getOffset() {
    checkState(offset != null, "heap location '%s' has no offset", this);
    return offset;
  }

  public HeapLocation withAddedOffset(long pAddToOffset) {
    long oldOffset = offset == null ? 0 : offset;
    return new HeapLocation(functionName, identifier, oldOffset + pAddToOffset);
  }
}
