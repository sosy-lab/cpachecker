// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;


import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class ConflictPair {
  private final SideEffectInfo  accessA;
  private final SideEffectInfo  accessB;
  private final CFAEdge location;

  public ConflictPair(SideEffectInfo pAccessA, SideEffectInfo pAccessB, CFAEdge plocation) {
    // (a, b) == (b, a)
    if (pAccessA.toString().compareTo(pAccessB.toString()) <= 0) {
      this.accessA = pAccessA;
      this.accessB = pAccessB;
    } else {
      this.accessA = pAccessB;
      this.accessB = pAccessA;
    }
    this.location = plocation;
  }

  public SideEffectInfo getAccessA() {
    return accessA;
  }

  public SideEffectInfo getAccessB() {
    return accessB;
  }

  public CFAEdge getLocation() {
    return location;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ConflictPair other)) return false;
    return Objects.equals(location, other.location) &&
        Objects.equals(accessA, other.accessA) &&
        Objects.equals(accessB, other.accessB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, accessA, accessB);
  }

  @Override
  public String toString() {
    return String.format("Conflict at %s:\n  - %s\n  - %s",
        location.getRawStatement(), accessA.toString(), accessB.toString());
  }

}
