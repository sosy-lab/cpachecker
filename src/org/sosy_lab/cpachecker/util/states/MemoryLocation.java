// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.states;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.errorprone.annotations.Immutable;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;

/** This class describes a location in the memory. */
@Immutable
public final class MemoryLocation implements Comparable<MemoryLocation>, Serializable {

  @Serial private static final long serialVersionUID = -8910967707373729034L;
  private final Optional<String> functionName;
  private final String identifier;
  private final Optional<Long> offset;

  private MemoryLocation(
      Optional<String> pFunctionName, String pIdentifier, Optional<Long> pOffset) {
    functionName = checkNotNull(pFunctionName);
    identifier = checkNotNull(pIdentifier);
    offset = checkNotNull(pOffset);
  }

  @Override
  public boolean equals(Object other) {

    if (this == other) {
      return true;
    }

    return other instanceof MemoryLocation otherLocation
        && Objects.equals(functionName, otherLocation.functionName)
        && Objects.equals(identifier, otherLocation.identifier)
        && Objects.equals(offset, otherLocation.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(functionName, identifier, offset);
  }

  /** Create an instance for the given declaration, which usually should be a variable. */
  public static MemoryLocation forDeclaration(ASimpleDeclaration pDeclaration) {
    // TODO Could avoid parsing qualified name if we can get the function here.
    return MemoryLocation.fromQualifiedName(pDeclaration.getQualifiedName());
  }

  /**
   * Create an instance for the given identifier without function name and offset. Typically, this
   * should be used for global variables.
   */
  public static MemoryLocation forIdentifier(String pIdentifier) {
    return new MemoryLocation(Optional.empty(), pIdentifier, Optional.empty());
  }

  /**
   * Create an instance for the given identifier without function name but with an offset.
   * Typically, this should be used for global variables.
   */
  public static MemoryLocation forIdentifier(String pIdentifier, long pOffset) {
    return new MemoryLocation(Optional.empty(), pIdentifier, Optional.of(pOffset));
  }

  public static MemoryLocation forLocalVariable(String pFunctionName, String pIdentifier) {
    return new MemoryLocation(Optional.of(pFunctionName), pIdentifier, Optional.empty());
  }

  public static MemoryLocation forLocalVariable(
      String pFunctionName, String pIdentifier, long pOffset) {
    return new MemoryLocation(Optional.of(pFunctionName), pIdentifier, Optional.of(pOffset));
  }

  /**
   * Create an instance using a qualified name of a declaration as returned by {@link
   * ASimpleDeclaration#getQualifiedName()}.
   */
  public static MemoryLocation fromQualifiedName(String pIdentifier, long pOffset) {
    return fromQualifiedName(pIdentifier).withAddedOffset(pOffset);
  }

  /**
   * Create an instance using a qualified name of a declaration as returned by {@link
   * ASimpleDeclaration#getQualifiedName()}.
   */
  public static MemoryLocation fromQualifiedName(String pIdentifier) {
    String functionName;
    String identifier;
    int separatorIndex = pIdentifier.indexOf("::");

    if (separatorIndex >= 0) {
      functionName = pIdentifier.substring(0, separatorIndex);
      identifier = pIdentifier.substring(separatorIndex + 2);
    } else {
      functionName = null;
      identifier = pIdentifier;
    }
    return new MemoryLocation(Optional.ofNullable(functionName), identifier, Optional.empty());
  }

  /** Create an instance from a string that was produced by {@link #getExtendedQualifiedName()}. */
  public static MemoryLocation parseExtendedQualifiedName(String pVariableName) {

    List<String> nameParts = Splitter.on("::").splitToList(pVariableName);
    List<String> offsetParts = Splitter.on('/').splitToList(pVariableName);

    boolean isScoped = nameParts.size() == 2;
    boolean hasOffset = offsetParts.size() == 2;

    @Nullable Long offset = hasOffset ? Long.parseLong(offsetParts.get(1)) : null;

    if (isScoped) {
      String functionName = nameParts.getFirst();
      String varName = nameParts.get(1);
      if (hasOffset) {
        varName = varName.replace("/" + offset, "");
      }
      return new MemoryLocation(Optional.of(functionName), varName, Optional.ofNullable(offset));

    } else {
      String varName = nameParts.getFirst();
      if (hasOffset) {
        varName = varName.replace("/" + offset, "");
      }
      return new MemoryLocation(
          Optional.empty(), varName.replace("/" + offset, ""), Optional.ofNullable(offset));
    }
  }

  /**
   * Return a string that represents the full information of this class. This string should be used
   * as an opaque identifier and only be passed to {@link #parseExtendedQualifiedName(String)}.
   */
  public String getExtendedQualifiedName() {
    String variableName = getQualifiedName();
    if (offset.isEmpty()) {
      return variableName;
    }
    return variableName + "/" + offset;
  }

  /**
   * Returns the qualified name consisting of the function name if present and the identifier. Note:
   * MemoryLocation consists of more than just those Strings!
   *
   * @return a String representing the qualified name consisting of function name and identifier.
   */
  public String getQualifiedName() {
    return isOnFunctionStack() ? (functionName.orElseThrow() + "::" + identifier) : identifier;
  }

  public boolean isOnFunctionStack() {
    return functionName.isPresent();
  }

  public boolean isOnFunctionStack(String pFunctionName) {
    return isOnFunctionStack() && pFunctionName.equals(functionName.orElseThrow());
  }

  /** Returns the name of the function . Throws for global variables. */
  public String getFunctionName() {
    return functionName.orElseThrow();
  }

  public String getIdentifier() {
    return identifier;
  }

  public boolean isReference() {
    return offset.isPresent();
  }

  /**
   * Gets the offset of a reference. Only valid for references. See {@link
   * MemoryLocation#isReference()}.
   *
   * @return the offset of a reference.
   */
  public long getOffset() {
    checkState(offset.isPresent(), "memory location '%s' has no offset", this);
    return offset.orElseThrow();
  }

  /** Return new instance without offset. */
  public MemoryLocation getReferenceStart() {
    checkState(isReference(), "Memory location is no reference: %s", this);
    return new MemoryLocation(functionName, identifier, Optional.empty());
  }

  /** Return a new instance with replaced offset. */
  public MemoryLocation withOffset(long pNewOffset) {
    return new MemoryLocation(functionName, identifier, Optional.of(pNewOffset));
  }

  /**
   * Return a new instance with the given offset added to the existing offset. If the existing
   * offset is not set, 0 is used as its value.
   */
  public MemoryLocation withAddedOffset(long pAddToOffset) {
    long oldOffset = offset.isEmpty() ? 0 : offset.orElseThrow();
    return new MemoryLocation(functionName, identifier, Optional.of(oldOffset + pAddToOffset));
  }

  @Override
  public String toString() {
    return getExtendedQualifiedName();
  }

  @Override
  public int compareTo(MemoryLocation other) {
    return ComparisonChain.start()
        .compare(
            functionName.orElse(null),
            other.functionName.orElse(null),
            Ordering.natural().nullsFirst())
        .compare(identifier, other.identifier)
        .compare(offset.orElse(null), other.offset.orElse(null), Ordering.natural().nullsFirst())
        .result();
  }
}
