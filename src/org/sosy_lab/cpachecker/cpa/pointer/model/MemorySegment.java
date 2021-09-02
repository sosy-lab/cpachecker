// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class MemorySegment implements Comparable<MemorySegment> {

  private final Identifier identifier;
  private final long offset;
  private final long length;

  private MemorySegment(Identifier pIdentifier, long pOffset, long pLength) {

    checkNotNull(pIdentifier);
    checkArgument(pOffset >= 0, "segment offset must be >= 0, but is %d", pOffset);
    checkArgument(pLength >= 0, "segment length must be > 0, but is %d", pLength);
    checkArgument(pOffset + pLength >= 0, "segment offset + length must be <= Long.MAX_VALUE");

    identifier = pIdentifier;
    offset = pOffset;
    length = pLength;
  }

  public static MemorySegment forMemoryLocation(MemoryLocation pMemoryLocation, long pLength) {

    Identifier identifier;
    if (pMemoryLocation.isOnFunctionStack()) {
      identifier =
          new Identifier(pMemoryLocation.getFunctionName(), pMemoryLocation.getIdentifier());
    } else {
      identifier = new Identifier(null, pMemoryLocation.getIdentifier());
    }

    long offset = pMemoryLocation.isReference() ? pMemoryLocation.getOffset() : 0;

    return new MemorySegment(identifier, offset, pLength);
  }

  Identifier getIdentifier() {
    return identifier;
  }

  public Optional<String> getFunctionName() {
    return identifier.getFunctionName();
  }

  public String getVariableName() {
    return identifier.getVariableName();
  }

  public long getOffset() {
    return offset;
  }

  public long getLength() {
    return length;
  }

  public boolean contains(MemorySegment pOther) {
    return identifier.equals(pOther.identifier)
        && offset <= pOther.offset
        && offset + length >= pOther.offset + pOther.length;
  }

  @Override
  public int compareTo(MemorySegment pOther) {

    if (this == pOther) {
      return 0;
    }

    int order = identifier.compareTo(pOther.identifier);

    if (order != 0) {
      return order;
    }

    return ComparisonChain.start()
        .compare(offset, pOther.offset)
        .compare(length, pOther.length)
        .result();
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, length, offset);
  }

  @Override
  public boolean equals(Object pObject) {

    if (this == pObject) {
      return true;
    }

    if (!(pObject instanceof MemorySegment)) {
      return false;
    }

    MemorySegment other = (MemorySegment) pObject;
    return identifier.equals(other.identifier) && length == other.length && offset == other.offset;
  }

  @Override
  public String toString() {
    return identifier.toString() + '/' + offset + '-' + (offset + length - 1);
  }

  static final class Identifier implements Comparable<Identifier> {

    private final @Nullable String functionName;
    private final String variableName;
    private final int hash;

    private Identifier(@Nullable String pFunctionName, String pVariableName) {

      checkNotNull(pVariableName);

      functionName = pFunctionName;
      variableName = pVariableName;
      hash = Objects.hash(functionName, variableName);
    }

    Optional<String> getFunctionName() {
      return Optional.ofNullable(functionName);
    }

    String getVariableName() {
      return variableName;
    }

    @Override
    public int compareTo(Identifier pOther) {

      if (this == pOther) {
        return 0;
      }

      int order = Integer.compare(hash, pOther.hash);

      if (order != 0) {
        return order;
      }

      return ComparisonChain.start()
          .compare(functionName, pOther.functionName, Ordering.natural().nullsFirst())
          .compare(variableName, pOther.variableName)
          .result();
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object pOther) {

      if (this == pOther) {
        return true;
      }

      if (!(pOther instanceof Identifier)) {
        return false;
      }

      Identifier other = (Identifier) pOther;
      return Objects.equals(functionName, other.functionName)
          && Objects.equals(variableName, other.variableName);
    }

    @Override
    public String toString() {
      return functionName != null ? functionName + "::" + variableName : variableName;
    }
  }
}
