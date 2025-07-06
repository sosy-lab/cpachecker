// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import com.google.common.collect.ComparisonChain;

import static com.google.common.base.Preconditions.checkNotNull;

public class HeapLocation implements PointerTarget {
  private final String functionName;
  private final String identifier;

  private HeapLocation(String pFunctionName, String pIdentifier) {
    checkNotNull(pIdentifier);
    functionName = pFunctionName;
    identifier = pIdentifier;
  }

  public static HeapLocation forAllocation(String pFunctionName, int pIndex) {
    String finalIdentifier = (pIndex == -1) ? "heap_obj" : "heap_obj" + pIndex;
    // String finalIdentifier = "heap_obj" + pIndex;
    return new HeapLocation(pFunctionName, finalIdentifier);
  }

  public static HeapLocation forLineBasedAllocation(String pFunctionName, int lineNumber) {
    return new HeapLocation(pFunctionName, "heap_obj" + lineNumber);
  }

  @Override
  public int compareTo(PointerTarget pOther) {
    return ComparisonChain.start()
        .compare(this.getClass().getName(), pOther.getClass().getName())
        .compare(this.identifier, (pOther instanceof HeapLocation other) ? other.identifier : "")
        .result();
  }

  @Override
  public boolean equals(Object pOther) {
    return this == pOther
        || (pOther instanceof HeapLocation other && identifier.equals(other.identifier));
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public String toString() {
    return functionName + "::" + identifier;
  }
}
