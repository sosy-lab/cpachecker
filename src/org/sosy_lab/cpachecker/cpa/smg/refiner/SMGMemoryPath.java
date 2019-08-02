/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SMGMemoryPath {

  private final String variableName;
  private final String functionName;
  private final Integer locationOnStack;
  private final boolean globalStart;
  private final List<Long> pathOffsets;

  private SMGMemoryPath(String pVariableName, String pFunctionName, Long pPathOffset,
      Integer pLocationOnStack) {
    globalStart = false;
    variableName = pVariableName;
    functionName = pFunctionName;
    pathOffsets = ImmutableList.of(pPathOffset);
    locationOnStack = pLocationOnStack;
  }

  private SMGMemoryPath(String pVariableName, Long pPathOffset) {
    globalStart = true;
    variableName = pVariableName;
    functionName = null;
    locationOnStack = null;
    pathOffsets = ImmutableList.of(pPathOffset);
  }

  public SMGMemoryPath(SMGMemoryPath pParent, Long pOffset) {
    globalStart = pParent.globalStart;
    variableName = pParent.variableName;
    functionName = pParent.functionName;
    locationOnStack = pParent.locationOnStack;

    List<Long> offsets = new ArrayList<>(pParent.getPathOffset());
    offsets.add(pOffset);
    pathOffsets = ImmutableList.copyOf(offsets);
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

  public List<Long> getPathOffset() {
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

    for (Long offset : pathOffsets) {
      result.append("->");
      result.append(offset);
    }

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

  public static SMGMemoryPath valueOf(String pVariableName, String pFunctionName,
                                      Long pPathOffset, Integer pLocationOnStack) {
    return new SMGMemoryPath(pVariableName, pFunctionName, pPathOffset, pLocationOnStack);
  }

  public static SMGMemoryPath valueOf(String pVariableName, Long pPathOffset) {
    return new SMGMemoryPath(pVariableName, pPathOffset);
  }

  public static SMGMemoryPath valueOf(SMGMemoryPath pParent, Long pOffset) {
    return new SMGMemoryPath(pParent, pOffset);
  }
}