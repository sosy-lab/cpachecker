// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.pointertarget;

import static org.sosy_lab.cpachecker.cpa.pointer.util.PointerUtils.compareByType;

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
  public int compareTo(PointerTarget pOther) {
    return (pOther instanceof InvalidLocation other)
        ? ComparisonChain.start().compare(this.reason.name(), other.reason.name()).result()
        : compareByType(this, pOther);
  }
}
