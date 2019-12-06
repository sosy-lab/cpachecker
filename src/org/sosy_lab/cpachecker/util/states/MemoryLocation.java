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

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

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

  protected MemoryLocation(String pIdentifier, @Nullable Long pOffset) {
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
    return Objects.hash(functionName, identifier, offset);
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
    return new MemoryLocation(pIdentifier, pOffset.isPresent() ? pOffset.orElseThrow() : null);
  }

  public static MemoryLocation valueOf(String pVariableName) {

    List<String> nameParts = Splitter.on("::").splitToList(pVariableName);
    List<String> offsetParts = Splitter.on('/').splitToList(pVariableName);

    boolean isScoped = nameParts.size() == 2;
    boolean hasOffset = offsetParts.size() == 2;

    @Nullable Long offset = hasOffset ? Long.parseLong(offsetParts.get(1)) : null;

    if (isScoped) {
      String functionName = nameParts.get(0);
      String varName = nameParts.get(1);
      if (hasOffset) {
        varName = varName.replace("/" + offset, "");
      }
      return new MemoryLocation(functionName, varName, offset);

    } else {
      String varName = nameParts.get(0);
      if (hasOffset) {
        varName = varName.replace("/" + offset, "");
      }
      return new MemoryLocation(varName.replace("/" + offset, ""), offset);
    }
  }

  public String getAsSimpleString() {
    String variableName = isOnFunctionStack() ? (functionName + "::" + identifier) : identifier;
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
    checkState(offset != null, "memory location '" + this + "' has no offset");
    return offset;
  }

  public MemoryLocation getReferenceStart() {
    checkState(isReference(), "Memory location is no reference: %s", this);
    if (functionName != null) {
      return new MemoryLocation(functionName, identifier, null);
    } else {
      return new MemoryLocation(identifier, null);
    }
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
    return ComparisonChain.start()
        .compare(functionName, other.functionName, Ordering.natural().nullsFirst())
        .compare(identifier, other.identifier)
        .compare(offset, other.offset, Ordering.natural().nullsFirst())
        .result();
  }
}
