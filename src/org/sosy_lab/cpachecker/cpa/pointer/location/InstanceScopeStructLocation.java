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
import com.google.common.collect.Ordering;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public record InstanceScopeStructLocation(
    @Nullable String functionName, @NonNull CType structType, String instanceName)
    implements StructLocation {
  @Override
  public int compareTo(PointerLocation pOther) {
    if (!(pOther instanceof InstanceScopeStructLocation other)) {
      return compareByType(this, pOther);
    }
    return ComparisonChain.start()
        .compare(functionName, other.functionName, Ordering.natural().nullsFirst())
        .compare(structType.toString(), other.structType.toString())
        .compare(instanceName, other.instanceName, Ordering.natural().nullsFirst())
        .result();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof InstanceScopeStructLocation other
        && Objects.equals(functionName, other.functionName)
        && structType.equals(other.structType)
        && Objects.equals(instanceName, other.instanceName);
  }

  @Override
  public String toString() {
    return getQualifiedName();
  }

  @Override
  public boolean isOnFunctionStack() {
    return functionName != null;
  }

  @Override
  public String getQualifiedName() {
    return instanceName != null ? (getStructScope() + " " + instanceName) : getStructScope();
  }

  @Override
  public String getFunctionName() {
    return functionName;
  }

  private String getStructScope() {
    return isOnFunctionStack() ? (functionName + "::" + structType) : structType.toString();
  }
}
