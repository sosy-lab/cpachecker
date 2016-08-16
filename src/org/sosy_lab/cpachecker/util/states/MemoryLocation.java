/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.states;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

import javax.annotation.Nullable;

/**
* This class describes a location in the memory.
*/
public class MemoryLocation implements Comparable<MemoryLocation>, Serializable {

  private static final long serialVersionUID = -8910967707373729034L;
  private final String functionName;
  private final String identifier;
  private final @Nullable Long offset;

  private MemoryLocation(String pFunctionName, String pIdentifier, @Nullable Long pOffset) {
    checkNotNull(pFunctionName);
    checkNotNull(pIdentifier);

    functionName = pFunctionName;
    identifier = pIdentifier;
    offset = pOffset;
  }

  private MemoryLocation(String pIdentifier, @Nullable Long pOffset) {
    checkNotNull(pIdentifier);

    int separatorIndex = pIdentifier.indexOf("::");
    if (separatorIndex >= 0) {
      functionName = pIdentifier.substring(0, separatorIndex);
      identifier = pIdentifier.substring(separatorIndex + 2);
    } else {
      functionName = null;
      identifier = pIdentifier;
    }
    offset = pOffset;
  }

  @Override
  public boolean equals(Object other) {

    if (this == other) {
      return true;
    }

    if (!(other instanceof MemoryLocation)) {
      return false;
    }

    MemoryLocation otherLocation = (MemoryLocation) other;

    return Objects.equals(functionName, otherLocation.functionName)
        && Objects.equals(identifier, otherLocation.identifier)
        && Objects.equals(offset, otherLocation.offset);
  }

  @Override
  public int hashCode() {

    int hc = 17;
    int hashMultiplier = 59;

    hc = hc * hashMultiplier + Objects.hashCode(functionName);
    hc = hc * hashMultiplier + identifier.hashCode();
    hc = hc * hashMultiplier + Objects.hashCode(offset);

    return hc;
  }

  public static MemoryLocation valueOf(String pFunctionName, String pIdentifier) {
    return new MemoryLocation(pFunctionName, pIdentifier, null);
  }

  public static MemoryLocation valueOf(String pFunctionName, String pIdentifier, long pOffset) {
    return new MemoryLocation(pFunctionName, pIdentifier, pOffset);
  }

  public static MemoryLocation valueOf(String pIdentifier, long pOffset) {
    return new MemoryLocation(pIdentifier, pOffset);
  }

  public static MemoryLocation valueOf(String pIdentifier, OptionalLong pOffset) {
    return new MemoryLocation(pIdentifier, pOffset.isPresent() ? pOffset.getAsLong() : null);
  }

  public static MemoryLocation valueOf(String pVariableName) {

    String[] nameParts    = pVariableName.split("::");
    String[] offsetParts  = pVariableName.split("/");

    boolean isScoped  = nameParts.length == 2;
    boolean hasOffset = offsetParts.length == 2;

    @Nullable Long offset =
        hasOffset ? Long.parseLong(offsetParts[1]) : null;

    if (isScoped) {
      if (hasOffset) {
        nameParts[1] = nameParts[1].replace("/" + offset, "");
      }
      return new MemoryLocation(nameParts[0], nameParts[1], offset);

    } else {
      if (hasOffset) {
        nameParts[0] = nameParts[0].replace("/" + offset, "");
      }
      return new MemoryLocation(nameParts[0].replace("/" + offset, ""), offset);
    }
  }

  public String getAsSimpleString() {
    String variableName = isOnFunctionStack() ? (functionName + "::" + identifier) : (identifier);
    if (offset == null) {
      return variableName;
    }
    return variableName + "/" + offset;
  }

  public String serialize() {
    return getAsSimpleString();
  }

  public boolean isOnFunctionStack() {
    return functionName != null;
  }

  public boolean isOnFunctionStack(String pFunctionName) {
    return functionName != null && pFunctionName.equals(functionName);
  }

  public String getFunctionName() {
    return checkNotNull(functionName);
  }

  public String getIdentifier() {
    return identifier;
  }

  public boolean isReference() {
    return offset != null;
  }

  /**
   * Gets the offset of a reference. Only valid for references.
   * See {@link MemoryLocation#isReference()}.
   *
   * @return the offset of a reference.
   */
  public long getOffset() {
    checkState(offset != null);
    return offset;
  }

  @Override
  public String toString() {
    return getAsSimpleString();
  }

  public static PersistentMap<MemoryLocation, Long> transform(
      PersistentMap<String, Long> pConstantMap) {

    PersistentMap<MemoryLocation, Long> result = PathCopyingPersistentTreeMap.of();

    for (Map.Entry<String, Long> entry : pConstantMap.entrySet()) {
      result = result.putAndCopy(valueOf(entry.getKey()), checkNotNull(entry.getValue()));
    }

    return result;
  }

  @Override
  public int compareTo(MemoryLocation other) {

    int result = 0;

    if (isOnFunctionStack()) {
      if (other.isOnFunctionStack()) {
        result = functionName.compareTo(other.functionName);
      } else {
        result = 1;
      }
    } else {
      if (other.isOnFunctionStack()) {
        result = -1;
      } else {
        result = 0;
      }
    }

    if (result != 0) {
      return result;
    }

    return ComparisonChain.start()
        .compare(identifier, other.identifier)
        .compare(offset, other.offset, Ordering.<Long>natural().nullsFirst())
        .result();
  }
}
