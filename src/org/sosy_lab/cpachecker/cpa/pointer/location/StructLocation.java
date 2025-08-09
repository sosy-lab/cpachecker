// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.location;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocationComparator.compareByType;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record StructLocation(
    @Nullable String functionName,
    String structType,
    @Nullable String instanceName,
    @Nullable String fieldName)
    implements PointerLocation {

  public StructLocation(
      @Nullable String functionName,
      @NonNull String structType,
      @Nullable String instanceName,
      @Nullable String fieldName) {
    checkNotNull(structType);
    this.functionName = functionName;
    this.structType = structType;
    this.instanceName = instanceName;
    this.fieldName = fieldName;
  }

  public static StructLocation forStruct(
      @Nullable String functionName, @NonNull String structType) {
    return new StructLocation(functionName, structType, null, null);
  }

  public static StructLocation forStructInstance(
      @Nullable String functionName, @NonNull String structType, String instanceName) {
    return new StructLocation(functionName, structType, instanceName, null);
  }

  public static StructLocation forField(
      @Nullable String functionName,
      @NonNull String structType,
      String instanceName,
      String fieldName) {
    return new StructLocation(functionName, structType, instanceName, fieldName);
  }

  @Override
  public int compareTo(PointerLocation pOther) {
    // Custom type check required for comparing different PointerTarget subclasses.
    // Fallback to type-based comparison for heterogeneous comparisons.
    if (!(pOther instanceof StructLocation other)) {
      return compareByType(this, pOther);
    }
    return ComparisonChain.start()
        .compare(functionName, other.functionName, Ordering.natural().nullsFirst())
        .compare(structType, other.structType)
        .compare(instanceName, other.instanceName, Ordering.natural().nullsFirst())
        .compare(fieldName, other.fieldName, Ordering.natural().nullsFirst())
        .result();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof StructLocation other
        && Objects.equals(functionName, other.functionName)
        && structType.equals(other.structType)
        && Objects.equals(instanceName, other.instanceName)
        && Objects.equals(fieldName, other.fieldName);
  }

  @Override
  public String toString() {
    return getCanonicalName();
  }

  public String getStructScope() {
    return isOnFunctionStack() ? (functionName + "::" + structType) : structType;
  }

  public String getInstanceScope() {
    return instanceName != null ? (getStructScope() + " " + instanceName) : getStructScope();
  }

  public String getCanonicalName() {
    return fieldName != null ? (getInstanceScope() + "." + fieldName) : getInstanceScope();
  }

  public boolean isOnFunctionStack() {
    return functionName != null;
  }
}
