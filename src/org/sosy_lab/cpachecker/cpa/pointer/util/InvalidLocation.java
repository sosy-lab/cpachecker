// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import com.google.common.collect.ComparisonChain;

public final class InvalidLocation implements PointerTarget {

  private final InvalidationReason reason;

  private InvalidLocation(InvalidationReason pReason) {
    reason = pReason;
  }

  public static InvalidLocation forInvalidation(InvalidationReason pReason) {
    return new InvalidLocation(pReason);
  }

  @Override
  public String toString() {
    return "invalid<" + reason + ">";
  }

  @Override
  public boolean equals(Object pOther) {
    return this == pOther || (pOther instanceof InvalidLocation other && reason == other.reason);
  }

  @Override
  public int hashCode() {
    return reason.hashCode();
  }

  @Override
  public int compareTo(PointerTarget o) {
    return ComparisonChain.start()
        .compare(this.getClass().getName(), o.getClass().getName())
        .compare(
            this.reason.name(), (o instanceof InvalidLocation other) ? other.reason.name() : "")
        .result();
  }

  //  @Override
  //  public int compareTo(PointerTarget pOther) {
  //    if (pOther instanceof InvalidLocation other) {
  //      return reason.compareTo(other.reason);
  //    } else if (pOther instanceof HeapLocation || pOther instanceof MemoryLocationPointer) {
  //      return 1;
  //    } else {
  //      return 0;
  //    }
  //  }
}
