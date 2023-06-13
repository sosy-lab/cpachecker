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
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;

/** This class describes a location in the memory. */
@Immutable
public final class MemoryLocation implements Comparable<MemoryLocation>, Serializable {

  private static final long serialVersionUID = -8910967707373729034L;
  private final @Nullable String functionName;
  private final String identifier;
  private final @Nullable Long offset;

  private MemoryLocation(
      @Nullable String pFunctionName, String pIdentifier, @Nullable Long pOffset) {
    checkNotNull(pIdentifier);

    functionName = pFunctionName;
    identifier = pIdentifier;
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

  /** Create an instance for the given declaration, which usually should be a variable. */
  public static MemoryLocation forDeclaration(ASimpleDeclaration pDeclaration) {
    // TODO Could avoid parsing qualified name if we can get the function here.
    return MemoryLocation.fromQualifiedName(pDeclaration.getQualifiedName());
  }

  /**
   * Create an instance for the given identifier without function name and offset. Typically this
   * should be used for global variables.
   */
  public static MemoryLocation forIdentifier(String pIdentifier) {
    return new MemoryLocation(null, pIdentifier, null);
  }

  /**
   * Create an instance for the given identifier without function name but with an offset. Typically
   * this should be used for global variables.
   */
  public static MemoryLocation forIdentifier(String pIdentifier, long pOffset) {
    return new MemoryLocation(null, pIdentifier, pOffset);
  }

  public static MemoryLocation forLocalVariable(String pFunctionName, String pIdentifier) {
    return new MemoryLocation(checkNotNull(pFunctionName), pIdentifier, null);
  }

  public static MemoryLocation forLocalVariable(
      String pFunctionName, String pIdentifier, long pOffset) {
    return new MemoryLocation(checkNotNull(pFunctionName), pIdentifier, pOffset);
  }

  private static MemoryLocation fromQualifiedName(String pIdentifier, @Nullable Long pOffset) {
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
    return new MemoryLocation(functionName, identifier, pOffset);
  }

  /**
   * Create an instance using a qualified name of a declaration as returned by {@link
   * ASimpleDeclaration#getQualifiedName()}.
   */
  public static MemoryLocation fromQualifiedName(String pIdentifier) {
    return fromQualifiedName(pIdentifier, null);
  }

  /**
   * Create an instance using a qualified name of a declaration as returned by {@link
   * ASimpleDeclaration#getQualifiedName()}.
   */
  public static MemoryLocation fromQualifiedName(String pIdentifier, long pOffset) {
    return fromQualifiedName(pIdentifier, Long.valueOf(pOffset));
  }

  /** Create an instance from a string that was produced by {@link #getExtendedQualifiedName()}. */
  public static MemoryLocation parseExtendedQualifiedName(String pVariableName) {

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
      return new MemoryLocation(null, varName.replace("/" + offset, ""), offset);
    }
  }

  /**
   * Return a string that represents the full information of this class. This string should be used
   * as an opaque identifier and only be passed to {@link #parseExtendedQualifiedName(String)}.
   */
  public String getExtendedQualifiedName() {
    String variableName = getQualifiedName();
    if (offset == null) {
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
    return isOnFunctionStack() ? (functionName + "::" + identifier) : identifier;
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
   * Gets the offset of a reference. Only valid for references. See {@link
   * MemoryLocation#isReference()}.
   *
   * @return the offset of a reference.
   */
  public long getOffset() {
    checkState(offset != null, "memory location '%s' has no offset", this);
    return offset;
  }

  /** Return new instance without offset. */
  public MemoryLocation getReferenceStart() {
    checkState(isReference(), "Memory location is no reference: %s", this);
    return new MemoryLocation(functionName, identifier, null);
  }

  /** Return a new instance with replaced offset. */
  public MemoryLocation withOffset(long pNewOffset) {
    return new MemoryLocation(functionName, identifier, pNewOffset);
  }

  /**
   * Return a new instance with the given offset added to the existing offset. If the existing
   * offset is not set, 0 is used as its value.
   */
  public MemoryLocation withAddedOffset(long pAddToOffset) {
    long oldOffset = offset == null ? 0 : offset;
    return new MemoryLocation(functionName, identifier, oldOffset + pAddToOffset);
  }

  @Override
  public String toString() {
    return getExtendedQualifiedName();
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
