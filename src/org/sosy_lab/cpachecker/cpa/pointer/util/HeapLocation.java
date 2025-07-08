// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import static org.sosy_lab.cpachecker.cpa.pointer.util.PointerUtils.compareByType;

import com.google.common.collect.ComparisonChain;
import java.util.Objects;

public class HeapLocation implements PointerTarget {
  private final String functionName;
  private final String identifier;

  private HeapLocation(String pFunctionName, String pIdentifier) {
    functionName = pFunctionName;
    identifier = pIdentifier;
  }

  public static HeapLocation forAllocation(String pFunctionName, int pIndex) {
    String finalIdentifier = (pIndex == -1) ? "heap_obj" : "heap_obj" + pIndex;
    return new HeapLocation(pFunctionName, finalIdentifier);
  }

  public static HeapLocation forLineBasedAllocation(String pFunctionName, int lineNumber) {
    return new HeapLocation(pFunctionName, "heap_obj" + lineNumber);
  }

  @Override
  public int compareTo(PointerTarget pOther) {
    if (!(pOther instanceof HeapLocation other)) {
      return compareByType(this, pOther);
    }

    return ComparisonChain.start()
        .compare(this.identifier, other.identifier)
        .compare(this.functionName, other.functionName)
        .result();
  }

  @Override
  public boolean equals(Object pOther) {
    return this == pOther
        || (pOther instanceof HeapLocation other
            && identifier.equals(other.identifier)
            && functionName.equals(other.functionName));
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, functionName);
  }

  @Override
  public String toString() {
    return functionName + "::" + identifier;
  }
}
