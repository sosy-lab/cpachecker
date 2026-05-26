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

  /**
   * Return the qualifiers of the type, e.g., its const/volatile flags. This method returns only the
   * outermost flags, i.e. for a regular pointer to const int it returns no qualifiers. In some
   * cases (array types, typedefs) this does not reflect the actually effective qualifiers. If you
   * need the latter, call {@link #getCanonicalType()} first.
   *
   * <p>More information can be found in C11 § 6.7.3.
   */
  CTypeQualifiers getQualifiers();

  default boolean isAtomic() {
    return getQualifiers().containsAtomic();
  }

  default boolean isConst() {
    return getQualifiers().containsConst();
  }

  @Override
  String toString();

  default boolean isVolatile() {
    return getQualifiers().containsVolatile();
  }

  /**
   * Check whether the current type is *incomplete* as defined by the C standard in § 6.2.5 (1).
   * Incomplete types miss some information (e.g., <code>struct s;</code>), and for example their
   * size cannot be computed.
   */
  boolean isIncomplete();

  /**
   * Check whether the current type has *known constant size* as defined by the C standard in §
   * 6.2.5 (23). These are types for which the size can be computed statically. Only incomplete
   * types and variable-length arrays do not have known constant size, but GCC has an extension that
   * also allows such arrays in structs: https://gcc.gnu.org/onlinedocs/gcc/Variable-Length.html
   */
  boolean hasKnownConstantSize();

  /** Will throw an UnsupportedOperationException */
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

  /**
   * Return a canonical representation of this type. For example, this resolves typedefs, makes the
   * use of "signed" consistent etc. This allows to compare type equality by calling {@link
   * #equals(Object)}.
   *
   * @implNote When implementing this method, please strengthen the return type to the type itself
   *     if possible. This method should always have exactly the same effect as the default
   *     implementation.
   */
  default CType getCanonicalType() {
    return getCanonicalType(CTypeQualifiers.NONE);
  }

  /**
   * Return a canonical representation of this type, optionally with some modifiers added. For
   * example, this resolves typedefs, makes the use of "signed" consistent etc. This allows to
   * compare type equality by calling {@link #equals(Object)}.
   *
   * <p>Note: Code outside of subclasses should always call {@link #getCanonicalType()} instead.
   *
   * @implNote When implementing this method, please strengthen the return type to the type itself
   *     if possible. If this is possible, please also implement {@link #getCanonicalType()} with
   *     the same return type. {@link CTypeQualifiers#union(CTypeQualifiers, CTypeQualifiers)} is
   *     useful for handling qualifiers.
   */
  CType getCanonicalType(CTypeQualifiers qualifiersToAdd);

  /**
   * Implements assignment compatibility for simple assignments (=) as described in the constraints
   * of C-Standard §6.5.16.1 (1). Note that this does not forbid assigning to const objects, so this
   * method does not check whether the current instance is const.
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
      if (leftHandSide instanceof CSimpleType cSimpleType
          && cSimpleType.getType().equals(CBasicType.BOOL)
          && rightHandSide instanceof CPointerType) {
        return true;
      }
    }

    // Cf. C-Standard §6.5.16.1 (1), second constraint.
    if (leftHandSide instanceof CCompositeType && rightHandSide instanceof CCompositeType) {
      CType plainCompositeLeft = leftHandSide.withoutQualifiers();
      CType plainCompositeRight = rightHandSide.withoutQualifiers();

      return CTypes.areTypesCompatible(plainCompositeLeft, plainCompositeRight);
    }

    // Cf. C-Standard §6.3.2.3 (1):
    if (leftHandSide instanceof CPointerType cPointerType) {
      if (cPointerType.getType() instanceof CVoidType) {
        // TODO This is wrong, cf. #1035 and
        // https://gitlab.com/sosy-lab/software/cpachecker/-/commit/c8bc0c7a1433b70fc28771314d61c26ea6f618b6#note_1170818138
        // When fixed, update the test in CTypeCompatibilityTest
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
      return (leftPointedToType.getQualifiers().containsAllOf(rightPointedToType.getQualifiers())
          && ((leftPointedToType instanceof CVoidType || rightPointedToType instanceof CVoidType)
              || CTypes.areTypesCompatible(
                  leftPointedToType.withoutQualifiers(), rightPointedToType.withoutQualifiers())));
    }

    // Cf. C-Standard §6.3.2.1 (3)
    if (leftHandSide instanceof CPointerType pointerLeft
        && rightHandSide instanceof CArrayType arrayRight) {
      return CTypes.areTypesCompatible(pointerLeft.getType(), arrayRight.getType());
    }

    // default case
    return false;
  }

  /**
   * Return a copy of a given type that has the "atomic" flag not set. If the given type is already
   * a non-atomic type, it is returned unchanged.
   *
   * <p>This method only eliminates the outermost atomic flag, if it is present, i.e., it does not
   * change a non-atomic pointer to an atomic int.
   *
   * <p>This method always returns an instance of the same type as it is called on, so it is safe to
   * cast the result.
   */
  default CType withoutAtomic() {
    return withQualifiersSetTo(getQualifiers().withoutAtomic());
  }

  /**
   * Return a copy of a given type that has the "atomic" flag set. If the given type is already an
   * atomic type, it is returned unchanged.
   *
   * <p>This method only adds the outermost atomic flag, if it is not present, i.e., it does not
   * change an atomic pointer to a non-atomic int.
   *
   * <p>This method always returns an instance of the same type as it is called on, so it is safe to
   * cast the result.
   */
  default CType withAtomic() {
    return withQualifiersSetTo(getQualifiers().withAtomic());
  }

  /**
   * Return a copy of a given type that has the "const" flag not set. If the given type is already a
   * non-const type, it is returned unchanged.
   *
   * <p>This method only eliminates the outermost const flag, if it is present, i.e., it does not
   * change a non-const pointer to a const int.
   *
   * <p>This method always returns an instance of the same type as it is called on, so it is safe to
   * cast the result.
   */
  default CType withoutConst() {
    return withQualifiersSetTo(getQualifiers().withoutConst());
  }

  /**
   * Return a copy of a given type that has the "const" flag set. If the given type is already a
   * const type, it is returned unchanged.
   *
   * <p>This method only adds the outermost const flag, if it is not present, i.e., it does not
   * change a const pointer to a non-const int.
   *
   * <p>This method always returns an instance of the same type as it is called on, so it is safe to
   * cast the result.
   */
  default CType withConst() {
    return withQualifiersSetTo(getQualifiers().withConst());
  }

  /**
   * Return a copy of a given type that has the "volatile" flag not set. If the given type is
   * already a non-volatile type, it is returned unchanged.
   *
   * <p>This method only eliminates the outermost volatile flag, if it is present, i.e., it does not
   * change a non-volatile pointer to a volatile int.
   *
   * <p>This method always returns an instance of the same type as it is called on, so it is safe to
   * cast the result.
   */
  default CType withoutVolatile() {
    return withQualifiersSetTo(getQualifiers().withoutVolatile());
  }

  /**
   * Return a copy of a given type that has the "volatile" flag set. If the given type is already a
   * volatile type, it is returned unchanged.
   *
   * <p>This method only adds the outermost volatile flag, if it is not present, i.e., it does not
   * change a volatile pointer to a non-volatile int.
   *
   * <p>This method always returns an instance of the same type as it is called on, so it is safe to
   * cast the result.
   */
  default CType withVolatile() {
    return withQualifiersSetTo(getQualifiers().withVolatile());
  }

  /**
   * Return a copy of this type that has the "atomic", "const", and "volatile" flags removed. If the
   * type already has no qualifiers, it is returned unchanged.
   *
   * <p>This method only eliminates the outermost qualifiers, if present, i.e., it does not change a
   * non-const non-volatile pointer to a const volatile int.
   *
   * <p>This method always returns an instance of the same type as it is called on, so it is safe to
   * cast the result.
   */
  default CType withoutQualifiers() {
    return withQualifiersSetTo(CTypeQualifiers.NONE);
  }

  /**
   * Return a copy of this type that has the quantifiers (e.g., atomic/const/volatile) set to the
   * given values.
   *
   * <p>This method only changes the outermost quantifiers.
   *
   * <p>This method always returns an instance of the same type as it is called on, so it is safe to
   * cast the result.
   *
   * @implNote When implementing this method, please strengthen the return type to the type itself.
   *     Every implementation of this method should return exactly the same kind of type as it is
   *     called on.
   */
  CType withQualifiersSetTo(CTypeQualifiers newQualifiers);
}
