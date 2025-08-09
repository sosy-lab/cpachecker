// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.location;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.pointer.util.PointerUtils.compareByType;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record HeapLocation(
    @NonNull String functionName, @NonNull String identifier, @Nullable Long offset)
    implements PointerLocation {
  private static final String HEAP_PREFIX = "heap_obj";

  public HeapLocation(String functionName, String identifier, Long offset) {
    this.functionName = checkNotNull(functionName);
    this.identifier = checkNotNull(identifier);
    this.offset = offset;
  }

  /** Single logical heap object (no per-call index). */
  public static HeapLocation forSingleAllocation(String functionName, @Nullable Long offset) {
    return new HeapLocation(functionName, HEAP_PREFIX, offset);
  }

  /** Per-call (or generally indexed) heap object. */
  public static HeapLocation forIndexedAllocation(
      String functionName, int index, @Nullable Long offset) {
    checkArgument(index >= 0, "index must be >= 0 for indexed allocations (was %s)", index);
    return new HeapLocation(functionName, HEAP_PREFIX + index, offset);
  }

  public static HeapLocation forLineBasedAllocation(
      String pFunctionName, int lineNumber, @Nullable Long pOffset) {
    return new HeapLocation(pFunctionName, "heap_obj" + lineNumber, pOffset);
  }

  @Override
  public int compareTo(PointerLocation pOther) {
    // Custom type check required for comparing different PointerTarget subclasses.
    // Fallback to type-based comparison for heterogeneous comparisons.
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
  public String toString() {
    String heapName = functionName + "::" + identifier;
    if (offset == null) {
      return heapName;
    }
    return heapName + "/" + offset;
  }

  /**
   * Offset have only heap locations where we track each allocation separately.
   *
   * @return true if this heap location has an offset, false otherwise.
   */
  public boolean hasOffset() {
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
