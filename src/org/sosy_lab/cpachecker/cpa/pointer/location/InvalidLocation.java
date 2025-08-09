// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.location;

import static org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocationComparator.compareByType;

import com.google.common.collect.ComparisonChain;

public record InvalidLocation(InvalidationReason reason) implements PointerLocation {

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
  public int compareTo(PointerLocation pOther) {
    // Compare using ComparisonChain if same type; fallback to type-based comparison otherwise.
    return (pOther instanceof InvalidLocation other)
        ? ComparisonChain.start().compare(this.reason.name(), other.reason.name()).result()
        : compareByType(this, pOther);
  }
}
