// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.util.Objects;
import java.util.OptionalInt;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;

public final class CArrayType extends AArrayType implements CType {

  @Serial private static final long serialVersionUID = -6314468260643330323L;

  /**
   * A {@link CArrayType} for unsigned integers ({@code unsigned int[]}) without any type
   * qualifiers.
   */
  public static final CArrayType UNSIGNED_INT_ARRAY =
      new CArrayType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_INT);

  private final @Nullable CExpression length;
  private final CTypeQualifiers qualifiers;

  public CArrayType(CTypeQualifiers pQualifiers, CType pType) {
    this(pQualifiers, pType, null);
  }

  /**
   * Create an array type. Most callers should ensure that the length is either null, a {@link
   * CIntegerLiteralExpression}, or a {@link CIdExpression} referring to a const variable.
   */
  public CArrayType(CTypeQualifiers pQualifiers, CType pType, @Nullable CExpression pLength) {
    super(pType);

    if (pLength instanceof CIntegerLiteralExpression lengthExp) {
      checkArgument(lengthExp.getValue().signum() >= 0, "Illegal negative array size %s", pLength);
    }

    qualifiers = checkNotNull(pQualifiers);
    length = pLength;
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  /**
   * Get the length expression of the array. This can be one of the following cases:
   *
   * <ul>
   *   <li>null: array has no specified length (should not happen for regular array declarations,
   *       cf. #265)
   *   <li>{@link CIntegerLiteralExpression}: array has constant length
   *   <li>{@link CIdExpression} with const type: variable-length array (frontend ensures that a
   *       const variable is added to store the length)
   *   <li>anything else: We would like to simplify this but in function parameters this is too
   *       tricky right now (cf. #1146). Note that for variable-length arrays any reference to a
   *       variable captures the value of the variable at declaration time of the array and does not
   *       refer to the current value of this variable!
   * </ul>
   *
   * Note that if you would like to get all length expressions even for cases like multi-dimensional
   * arrays of structs that again contain arrays, use {@link
   * CTypes#getArrayLengthExpressions(CType)}.
   */
  public @Nullable CExpression getLength() {
    return length;
  }

  /** Return the length of this array if statically known and small enough for an int. */
  public OptionalInt getLengthAsInt() {
    return length instanceof CIntegerLiteralExpression cIntegerLiteralExpression
        ? OptionalInt.of(cIntegerLiteralExpression.getValue().intValueExact())
        : OptionalInt.empty();
  }

  /**
   * Convert this array type to a pointer type with the same target type. Note that in most cases
   * the method {@link CTypes#adjustFunctionOrArrayType(CType)} should be used instead, which
   * implements this conversion properly and also the similar conversion for function types.
   */
  public CPointerType asPointerType() {
    return new CPointerType(this.getQualifiers(), getType());
  }

  @Override
  public String toASTString(String pDeclarator) {
    return toASTString(pDeclarator, AAstNodeRepresentation.DEFAULT);
  }

  private String toASTString(String pDeclarator, AAstNodeRepresentation pAAstNodeRepresentation) {
    checkNotNull(pDeclarator);
    // TODO: wrong, cf. #1375
    return qualifiers.toASTStringPrefix()
        + getType()
            .toASTString(
                pDeclarator
                    + ("["
                        + (length != null ? length.toASTString(pAAstNodeRepresentation) : "")
                        + "]"));
  }

  public String toQualifiedASTString(String pDeclarator) {
    return toASTString(pDeclarator, AAstNodeRepresentation.QUALIFIED);
  }

  @Override
  public CTypeQualifiers getQualifiers() {
    return qualifiers;
  }

  @Override
  public boolean isIncomplete() {
    return length == null; // C standard § 6.2.5 (22)
  }

  @Override
  public boolean hasKnownConstantSize() {
    // C standard § 6.7.6.2 (4)
    return length instanceof CIntegerLiteralExpression && getType().hasKnownConstantSize();
  }

  @Override
  public String toString() {
    return qualifiers.toASTStringPrefix()
        + "("
        + getType()
        + (")[" + (length != null ? length.toASTString() : "") + "]");
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(length, qualifiers) * 31 + super.hashCode();
  }

  /**
   * Be careful, this method compares the CType as it is to the given object, typedefs won't be
   * resolved. If you want to compare the type without having typedefs in it use
   * #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof CArrayType other
        && qualifiers.equals(other.qualifiers)
        && super.equals(obj)) {

      // If lengths are constants, compare their values directly (ignores type of expression).
      if (length instanceof CIntegerLiteralExpression lengthValue
          && other.length instanceof CIntegerLiteralExpression otherLengthValue) {
        return lengthValue.getValue().equals(otherLengthValue.getValue());
      } else {
        return Objects.equals(length, other.length);
      }
    }

    return false;
  }

  @Override
  public CArrayType getCanonicalType() {
    return getCanonicalType(CTypeQualifiers.NONE);
  }

  @Override
  public CArrayType getCanonicalType(CTypeQualifiers pQualifiersToAdd) {
    // C11 standard 6.7.3 (9) specifies that qualifiers like const and volatile
    // on an array type always refer to the element type, not the array type.
    // So we push these modifiers down to the element type here.
    // TODO: wrong, cf. #1375 Only pQualifiersToAdd needs to be pushed down.
    // TODO 6.7.3 (3) specifies that _Atomic must not be applied to array types.
    return new CArrayType(
        CTypeQualifiers.NONE,
        getType().getCanonicalType(CTypeQualifiers.union(qualifiers, pQualifiersToAdd)),
        length);
  }

  @Override
  public CArrayType withQualifiersSetTo(CTypeQualifiers pNewQualifiers) {
    if (pNewQualifiers.equals(qualifiers)) {
      return this;
    }
    return new CArrayType(pNewQualifiers, getType(), getLength());
  }
}
