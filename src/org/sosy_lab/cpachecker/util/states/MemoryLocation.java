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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import com.google.errorprone.annotations.Immutable;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** This class describes a location in the memory. */
@Immutable
public final class MemoryLocation implements Comparable<MemoryLocation>, Serializable {

  @Serial private static final long serialVersionUID = -8910967707373729034L;
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
    return new MemoryLocation(null, pIdentifier, null);
  }

  /**
   * Create an instance for the given identifier without function name but with an offset.
   * Typically, this should be used for global variables.
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
      String functionName = nameParts.getFirst();
      String varName = nameParts.get(1);
      if (hasOffset) {
        varName = varName.replace("/" + offset, "");
      }
      return new MemoryLocation(functionName, varName, offset);

    } else {
      String varName = nameParts.getFirst();
      if (hasOffset) {
        varName = varName.replace("/" + offset, "");
      }
      return new MemoryLocation(null, varName, offset);
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
   * Returns this memory location as the C expression denoting its address, for example {@code &x},
   * {@code &a[2]} or {@code &p.y}.
   *
   * <p>The offset of a memory location counts bytes, while C pointer arithmetic counts elements, so
   * the offset is resolved to the element and the member it points to. Returns nothing if it points
   * into a scalar, which no C expression denotes.
   *
   * @param pType the declared type of the variable of this memory location
   * @param pMachineModel the machine model of the analyzed program
   */
  public Optional<String> asCExpression(CType pType, MachineModel pMachineModel) {
    StringBuilder expression = new StringBuilder("&").append(identifier);
    if (offset == null) {
      return Optional.of(expression.toString());
    }

    CType current = pType.getCanonicalType();
    if (!isAggregate(current)) {
      // Only an element or a member can be designated, and without a designator the expression
      // would be the one of the same variable without an offset, which is a different location
      return Optional.empty();
    }
    long rest = offset;

    try {
      // Descend into the arrays and the composites until the offset is consumed
      while (isAggregate(current)) {
        if (current instanceof CArrayType array) {
          long elementSize = pMachineModel.getSizeof(array.getType()).longValueExact();
          if (elementSize <= 0) {
            return Optional.empty();
          }
          long index = rest / elementSize;
          OptionalInt length = array.getLengthAsInt();
          if (length.isPresent() && index >= length.orElseThrow()) {
            return Optional.empty();
          }
          expression.append('[').append(index).append(']');
          rest -= index * elementSize;
          current = array.getType().getCanonicalType();
        } else if (current instanceof CCompositeType composite) {
          Optional<CCompositeTypeMemberDeclaration> member =
              memberAt(composite, rest, pMachineModel);
          if (member.isEmpty()) {
            return Optional.empty();
          }
          expression.append('.').append(member.orElseThrow().getName());
          rest -=
              pMachineModel
                  .getFieldOffsetInBytes(composite, member.orElseThrow().getName())
                  .orElseThrow()
                  .longValueExact();
          current = member.orElseThrow().getType().getCanonicalType();
        }
      }
    } catch (IllegalArgumentException | ArithmeticException | NoSuchElementException e) {
      // The size of the type is unknown, for example for an incomplete type
      return Optional.empty();
    }

    // A rest is left over when the offset points into a scalar, which no C expression denotes
    return rest == 0 ? Optional.of(expression.toString()) : Optional.empty();
  }

  /** Whether the type is one whose parts have an address of their own. */
  private static boolean isAggregate(CType pType) {
    return pType instanceof CArrayType
        || (pType instanceof CCompositeType composite
            && composite.getKind() != ComplexTypeKind.ENUM);
  }

  /** Returns the member of the composite type which contains the given offset. */
  private static Optional<CCompositeTypeMemberDeclaration> memberAt(
      CCompositeType pComposite, long pOffset, MachineModel pMachineModel) {
    for (CCompositeTypeMemberDeclaration member : pComposite.getMembers()) {
      Optional<BigInteger> start =
          pMachineModel.getFieldOffsetInBytes(pComposite, member.getName());
      if (start.isEmpty()) {
        // A bit field which is not aligned to a byte has no address in C
        continue;
      }
      long from = start.orElseThrow().longValueExact();
      long to = from + pMachineModel.getSizeof(member.getType()).longValueExact();
      if (from <= pOffset && pOffset < to) {
        return Optional.of(member);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the identifier of the variable a C expression produced by {@link #asCExpression(CType,
   * MachineModel)} refers to, so that its declaration can be looked up.
   */
  public static String baseIdentifierOfCExpression(String pExpression) {
    String rest = pExpression.trim();
    if (rest.startsWith("&")) {
      rest = rest.substring(1).trim();
    }
    int end = 0;
    while (end < rest.length() && isIdentifierChar(rest.charAt(end))) {
      end++;
    }
    return rest.substring(0, end);
  }

  /**
   * Returns the memory location denoted by a C expression produced by {@link #asCExpression(CType,
   * MachineModel)}, or nothing if it does not describe a location of the given type.
   *
   * @param pExpression the address expression to parse
   * @param pFunctionName the function the variable belongs to, if it is not a global one
   * @param pType the declared type of the variable of the expression
   * @param pMachineModel the machine model of the analyzed program
   */
  public static Optional<MemoryLocation> parseCExpression(
      String pExpression, Optional<String> pFunctionName, CType pType, MachineModel pMachineModel) {
    String rest = pExpression.trim();
    if (rest.startsWith("&")) {
      rest = rest.substring(1).trim();
    }
    String identifier = baseIdentifierOfCExpression(rest);
    if (identifier.isEmpty()) {
      return Optional.empty();
    }

    CType current = pType.getCanonicalType();
    long offset = 0;
    int index = identifier.length();
    boolean hasDesignator = false;

    try {
      while (index < rest.length()) {
        char next = rest.charAt(index);
        if (Character.isWhitespace(next)) {
          index++;
        } else if (next == '[') {
          int close = rest.indexOf(']', index);
          if (close < 0 || !(current instanceof CArrayType array)) {
            return Optional.empty();
          }
          offset +=
              Long.parseLong(rest.substring(index + 1, close).trim())
                  * pMachineModel.getSizeof(array.getType()).longValueExact();
          current = array.getType().getCanonicalType();
          index = close + 1;
          hasDesignator = true;
        } else if (next == '.') {
          int end = index + 1;
          while (end < rest.length() && isIdentifierChar(rest.charAt(end))) {
            end++;
          }
          if (!(current instanceof CCompositeType composite)) {
            return Optional.empty();
          }
          String member = rest.substring(index + 1, end);
          Optional<CCompositeTypeMemberDeclaration> declaration =
              FluentIterable.from(composite.getMembers())
                  .firstMatch(m -> m.getName().equals(member))
                  .toJavaUtil();
          if (declaration.isEmpty()) {
            return Optional.empty();
          }
          offset +=
              pMachineModel.getFieldOffsetInBytes(composite, member).orElseThrow().longValueExact();
          current = declaration.orElseThrow().getType().getCanonicalType();
          index = end;
          hasDesignator = true;
        } else {
          return Optional.empty();
        }
      }
    } catch (IllegalArgumentException | ArithmeticException | NoSuchElementException e) {
      return Optional.empty();
    }

    return Optional.of(
        new MemoryLocation(
            pFunctionName.orElse(null), identifier, hasDesignator ? Long.valueOf(offset) : null));
  }

  private static boolean isIdentifierChar(char pChar) {
    return Character.isLetterOrDigit(pChar) || pChar == '_';
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

  /**
   * Checks whether the {@link MemoryLocation} is on the function stack with the given function
   * name. Includes the check of {@link #isOnFunctionStack()}.
   */
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
