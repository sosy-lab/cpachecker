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

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.List;

public class SMGMemoryPath implements Comparable<SMGMemoryPath> {

  private final String variableName;
  private final String functionName;
  private final Integer locationOnStack;
  private final boolean globalStart;
  private final List<Integer> pathOffsets;

  private SMGMemoryPath(String pVariableName, String pFunctionName, Integer pPathOffset,
      Integer pLocationOnStack) {
    globalStart = false;
    variableName = pVariableName;
    functionName = pFunctionName;
    pathOffsets = ImmutableList.of(pPathOffset);
    locationOnStack = pLocationOnStack;
  }

  private SMGMemoryPath(String pVariableName, Integer pPathOffset) {
    globalStart = true;
    variableName = pVariableName;
    functionName = null;
    locationOnStack = null;
    pathOffsets = ImmutableList.of(pPathOffset);
  }

  public SMGMemoryPath(SMGMemoryPath pParent, Integer pOffset) {
    globalStart = pParent.globalStart;
    variableName = pParent.variableName;
    functionName = pParent.functionName;
    locationOnStack = pParent.locationOnStack;

    List<Integer> offsets = new ArrayList<>(pParent.getPathOffset());
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

  public List<Integer> getPathOffset() {
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

    for (Integer offset : pathOffsets) {
      result.append("->");
      result.append(offset);
    }

    return result.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((functionName == null) ? 0 : functionName.hashCode());
    result = prime * result + (globalStart ? 1231 : 1237);
    result = prime * result + ((pathOffsets == null) ? 0 : pathOffsets.hashCode());
    result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
    result = prime * result + ((locationOnStack == null) ? 0 : locationOnStack.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGMemoryPath other = (SMGMemoryPath) obj;
    if (locationOnStack == null) {
      if (other.locationOnStack != null) {
        return false;
      }
    } else if (!locationOnStack.equals(other.locationOnStack)) {
      return false;
    }
    if (functionName == null) {
      if (other.functionName != null) {
        return false;
      }
    } else if (!functionName.equals(other.functionName)) {
      return false;
    }
    if (globalStart != other.globalStart) {
      return false;
    }
    if (pathOffsets == null) {
      if (other.pathOffsets != null) {
        return false;
      }
    } else if (!pathOffsets.equals(other.pathOffsets)) {
      return false;
    }
    if (variableName == null) {
      if (other.variableName != null) {
        return false;
      }
    } else if (!variableName.equals(other.variableName)) {
      return false;
    }
    return true;
  }

  public static SMGMemoryPath valueOf(String pVariableName, String pFunctionName,
      Integer pPathOffset, Integer pLocationOnStack) {
    return new SMGMemoryPath(pVariableName, pFunctionName, pPathOffset, pLocationOnStack);
  }

  public static SMGMemoryPath valueOf(String pVariableName,
      Integer pPathOffset) {
    return new SMGMemoryPath(pVariableName, pPathOffset);
  }

  public static SMGMemoryPath valueOf(SMGMemoryPath pParent, Integer pOffset) {
    return new SMGMemoryPath(pParent, pOffset);
  }

  @Override
  public int compareTo(SMGMemoryPath other) {
    int result = 0;

    if (startsWithGlobalVariable()) {
      if (other.startsWithGlobalVariable()) {
        result = 0;
      } else {
        result = 1;
      }
    } else {
      if (other.startsWithGlobalVariable()) {
        result = -1;
      } else {
        result = ComparisonChain.start().compare(functionName, other.functionName).result();
      }
    }

    if (result != 0) {
      return result;
    }

    result = ComparisonChain.start()
        .compare(variableName, other.variableName)
        .compare(locationOnStack, other.locationOnStack, Ordering.<Integer> natural().nullsFirst())
        .result();

    if (result != 0) {
      return result;
    }

    for (int i = 0; i < pathOffsets.size() && i < other.pathOffsets.size(); i++) {
      int offset = pathOffsets.get(i);
      int otherOffset = other.pathOffsets.get(i);

      result = ComparisonChain.start()
          .compare(offset, otherOffset, Ordering.<Integer> natural().nullsFirst())
          .result();

      if (result != 0) {
        return result;
      }
    }

    if (pathOffsets.size() < other.pathOffsets.size()) {
      return -1;
    }

    if (pathOffsets.size() > other.pathOffsets.size()) {
      return 1;
    }

    return result;
  }
}