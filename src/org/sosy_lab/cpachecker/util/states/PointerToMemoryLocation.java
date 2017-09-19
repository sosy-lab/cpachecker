/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

public class PointerToMemoryLocation extends MemoryLocation implements Comparable<MemoryLocation>, Serializable {

  private static final long serialVersionUID = -8910967707373729035L;

  private PointerToMemoryLocation(String pFunctionName, String pIdentifier, @Nullable Long pOffset) {
    super(pFunctionName, pIdentifier, pOffset);
  }

  private PointerToMemoryLocation(String pIdentifier, @Nullable Long pOffset) {
    super(pIdentifier, pOffset);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof PointerToMemoryLocation)) {
      return false;
    }

    PointerToMemoryLocation otherLocation = (PointerToMemoryLocation) other;

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

  public static PointerToMemoryLocation valueOf(String pFunctionName, String pIdentifier) {
    if (pFunctionName == null) {
      return new PointerToMemoryLocation(pIdentifier, null);
    } else {
      return new PointerToMemoryLocation(pFunctionName, pIdentifier, null);
    }
  }

  public static PointerToMemoryLocation valueOf(String pFunctionName, String pIdentifier, long pOffset) {
    return new PointerToMemoryLocation(pFunctionName, pIdentifier, pOffset);
  }

  public static PointerToMemoryLocation valueOf(String pIdentifier, long pOffset) {
    return new PointerToMemoryLocation(pIdentifier, pOffset);
  }

  public static PointerToMemoryLocation valueOf(String pIdentifier, OptionalLong pOffset) {
    return new PointerToMemoryLocation(pIdentifier, pOffset.isPresent() ? pOffset.getAsLong() : null);
  }

  public static PointerToMemoryLocation valueOf(String pVariableName) {

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
      nameParts[1] = "(*" + nameParts[1] + ")";
      return new PointerToMemoryLocation(nameParts[0], nameParts[1], offset);

    } else {
      if (hasOffset) {
        nameParts[0] = nameParts[0].replace("/" + offset, "");
      }
      return new PointerToMemoryLocation(nameParts[0].replace("/" + offset, ""), offset);
    }
  }

  @Override
  public String getAsSimpleString() {
    String variableName = isOnFunctionStack() ? (functionName + "::" + identifier) : (identifier);
    if (offset == null) {
      return variableName;
    }
    return variableName + "/" + offset;
  }

  @Override
  public String serialize() {
    return getAsSimpleString();
  }

  @Override
  public boolean isOnFunctionStack() {
    return functionName != null;
  }

  @Override
  public boolean isOnFunctionStack(String pFunctionName) {
    return functionName != null && pFunctionName.equals(functionName);
  }

  @Override
  public String getFunctionName() {
    return checkNotNull(functionName);
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public boolean isReference() {
    return offset != null;
  }

  /**
   * Gets the offset of a reference. Only valid for references.
   * See {@link MemoryLocation#isReference()}.
   *
   * @return the offset of a reference.
   */
  @Override
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

  public int compareTo(PointerToMemoryLocation other) {

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