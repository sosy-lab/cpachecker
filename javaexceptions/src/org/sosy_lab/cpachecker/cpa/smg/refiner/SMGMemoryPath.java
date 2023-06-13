// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.primitives.ImmutableLongArray;
import java.util.Objects;

public class SMGMemoryPath {

  private final String variableName;
  private final String functionName;
  private final Integer locationOnStack;
  private final boolean globalStart;
  private final ImmutableLongArray pathOffsets;

  private SMGMemoryPath(
      String pVariableName, String pFunctionName, long pPathOffset, Integer pLocationOnStack) {
    globalStart = false;
    variableName = pVariableName;
    functionName = pFunctionName;
    pathOffsets = ImmutableLongArray.of(pPathOffset);
    locationOnStack = pLocationOnStack;
  }

  private SMGMemoryPath(String pVariableName, long pPathOffset) {
    globalStart = true;
    variableName = pVariableName;
    functionName = null;
    locationOnStack = null;
    pathOffsets = ImmutableLongArray.of(pPathOffset);
  }

  public SMGMemoryPath(SMGMemoryPath pParent, long pOffset) {
    globalStart = pParent.globalStart;
    variableName = pParent.variableName;
    functionName = pParent.functionName;
    locationOnStack = pParent.locationOnStack;

    pathOffsets =
        ImmutableLongArray.builder(pParent.getPathOffset().length() + 1)
            .addAll(pParent.getPathOffset())
            .add(pOffset)
            .build();
  }

  public String getFunctionName() {
    return functionName;
  }

  public Integer getLocationOnStack() {
    return locationOnStack;
  }

  public String getVariableName() {
    return variableName;
  }

  public ImmutableLongArray getPathOffset() {
    return pathOffsets;
  }

  public boolean startsWithGlobalVariable() {
    return globalStart;
  }

  @Override
  public String toString() {

    StringBuilder result = new StringBuilder();

    if (!globalStart) {
      result.append(functionName);
      result.append(":");
    }

    result.append(variableName);

    pathOffsets.forEach(
        offset -> {
          result.append("->");
          result.append(offset);
        });

    return result.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(globalStart, locationOnStack, functionName, pathOffsets, variableName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SMGMemoryPath)) {
      return false;
    }
    SMGMemoryPath other = (SMGMemoryPath) obj;
    return globalStart == other.globalStart
        && Objects.equals(locationOnStack, other.locationOnStack)
        && Objects.equals(functionName, other.functionName)
        && Objects.equals(pathOffsets, other.pathOffsets)
        && Objects.equals(variableName, other.variableName);
  }

  public static SMGMemoryPath valueOf(
      String pVariableName, String pFunctionName, long pPathOffset, Integer pLocationOnStack) {
    return new SMGMemoryPath(pVariableName, pFunctionName, pPathOffset, pLocationOnStack);
  }

  public static SMGMemoryPath valueOf(String pVariableName, long pPathOffset) {
    return new SMGMemoryPath(pVariableName, pPathOffset);
  }

  public static SMGMemoryPath valueOf(SMGMemoryPath pParent, long pOffset) {
    return new SMGMemoryPath(pParent, pOffset);
  }
}
