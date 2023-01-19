// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * This interface represents types in C. Note that different object instances might represent the
 * conceptually same type (e.g., due to typedefs). {@link #getCanonicalType()} can be used to get a
 * canonical representation of each type.
 *
 * <p>{@link CTypes} contains further helper methods for type instances.
 *
 * <p>The files "CTypes.dia"/"CTypes.pdf" in this package document the C type system and the
 * CPAchecker-specific classes.
 */
@SuppressWarnings("serial")
public sealed interface CType extends Type
    permits CArrayType,
        CBitFieldType,
        CComplexType,
        CFunctionType,
        CFunctionTypeWithNames,
        CPointerType,
        CProblemType,
        CSimpleType,
        CTypedefType,
        CVoidType {

  boolean isConst();

  @Override
  String toString();

  boolean isVolatile();

  /**
   * Check whether the current type is *incomplete* as defined by the C standard in § 6.2.5 (1).
   * Incomplete types miss some information (e.g., <code>struct s;</code>), and for example their
   * size cannot be computed.
   */
  boolean isIncomplete();

  /**
   * Check whether the current type has *known constant size* as defined by the C standard in §
   * 6.2.5 (23). These are types for which the size can be computed statically. Only incomplete
   * types and variable-length arrays do not have known constant size, but GCC has a an extension
   * that also allows such arrays in structs:
   * https://gcc.gnu.org/onlinedocs/gcc/Variable-Length.html
   */
  boolean hasKnownConstantSize();

  /** Will throw a UnsupportedOperationException */
  @Override
  int hashCode();

  /**
   * Be careful, this method compares the CType as it is to the given object, typedefs won't be
   * resolved. If you want to compare the type without having typedefs in it use
   * #getCanonicalType().equals()
   */
  @Override
  boolean equals(@Nullable Object obj);

  <R, X extends Exception> R accept(CTypeVisitor<R, X> visitor) throws X;

  CType getCanonicalType();

  CType getCanonicalType(boolean forceConst, boolean forceVolatile);

  /**
   * Implements assignment compatibility for simple assignments (=) as described in the constraints
   * of C-Standard §6.5.16.1 (1).
   *
   * <p>Currently the fifth of those constraints is not considered, since a {@link CType} does not
   * expose if it is a null pointer constant.
   *
   * <p>Do not override this method. If you find some condition that is not met by this
   * implementation yet but required for compliance with the standard, just add the necessary
   * condition to this code.
   *
   * @param pType the {@link CType} to check, if it can be assigned to <b><code>this</code></b>
   * @return if pType can be assigned to <b><code>this</code></b>
   */
  default boolean canBeAssignedFrom(CType pType) {
    CType leftHandSide = this.getCanonicalType();
    CType rightHandSide = pType.getCanonicalType();

    // Cf. C-Standard §6.5.16.1 (1), first and last constraint of the list.
    if (CTypes.isArithmeticType(leftHandSide)) {
      if (CTypes.isArithmeticType(rightHandSide)) {
        return true;
      }
      if (leftHandSide instanceof CSimpleType
          && ((CSimpleType) leftHandSide).getType().equals(CBasicType.BOOL)
          && rightHandSide instanceof CPointerType) {
        return true;
      }
    }

    // Cf. C-Standard §6.5.16.1 (1), second constraint.
    if (leftHandSide instanceof CCompositeType && rightHandSide instanceof CCompositeType) {
      CType plainCompositeLeft = CTypes.copyDequalified(leftHandSide);
      CType plainCompositeRight = CTypes.copyDequalified(rightHandSide);

      return CTypes.areTypesCompatible(plainCompositeLeft, plainCompositeRight);
    }

    // Cf. C-Standard §6.3.2.3 (1):
    if (leftHandSide instanceof CPointerType) {
      if (((CPointerType) leftHandSide).getType() instanceof CVoidType) {
        if (rightHandSide.isIncomplete() || CTypes.isObjectType(rightHandSide)) {
          return true;
        }
      }
    }

    if (rightHandSide instanceof CPointerType pointerRight
        && leftHandSide instanceof CPointerType pointerLeft) {
      CType leftPointedToType = pointerLeft.getType();
      CType rightPointedToType = pointerRight.getType();

      if (leftPointedToType instanceof CProblemType || rightPointedToType instanceof CProblemType) {
        return true;
      }

      // Cf. C-Standard §6.5.16.1 (1), third and fourth constraint.
      return (((leftPointedToType.isConst() || !rightPointedToType.isConst())
              && (leftPointedToType.isVolatile() || !rightPointedToType.isVolatile()))
          && ((leftPointedToType instanceof CVoidType || rightPointedToType instanceof CVoidType)
              || CTypes.areTypesCompatible(
                  CTypes.copyDequalified(leftPointedToType),
                  CTypes.copyDequalified(rightPointedToType))));
    }

    // Cf. C-Standard §6.3.2.1 (3)
    if (leftHandSide instanceof CPointerType pointerLeft
        && rightHandSide instanceof CArrayType arrayRight) {
      return CTypes.areTypesCompatible(pointerLeft.getType(), arrayRight.getType());
    }

    // default case
    return false;
  }
}
