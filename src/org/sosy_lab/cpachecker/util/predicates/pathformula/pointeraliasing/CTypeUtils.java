// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.OptionalInt;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** Utility class with helper methods for CTypes. */
class CTypeUtils {

  private CTypeUtils() {}

  private static final CachingCanonizingCTypeVisitor typeVisitor =
      new CachingCanonizingCTypeVisitor(
          /*ignoreConst=*/ true, /*ignoreVolatile=*/ true, /*ignoreSignedness=*/ false);

  /** Return the length of an array, honoring the options for maximum and default array length. */
  static int getArrayLength(CArrayType t, FormulaEncodingWithPointerAliasingOptions options) {
    OptionalInt length = t.getLengthAsInt();
    return length.isPresent()
        ? Integer.min(options.maxArrayLength(), length.orElseThrow())
        : options.defaultArrayLength();
  }

  private static boolean containsArrayInComposite(final CType pType) {
    checkIsSimplified(pType);
    if (pType instanceof CArrayType) {
      return true;
    } else if (pType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) pType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite!";
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (containsArrayInComposite(memberDeclaration.getType())) {
          return true;
        }
      }
      return false;
    } else {
      return false;
    }
  }

  /**
   * The method is used to check if a composite type contains array as this means it can't be
   * encoded as a bunch of variables. It also returns {@code true} on stand-alone arrays assuming
   * they are not function parameters. Normally stand-alone arrays are different from pointers as
   * their address cannot change, unlike the value of a pointer. Thus arrays are usually encoded as
   * uninterpreted constants corresponding to their starting addresses, just as variables of
   * structure types. They can also be "assigned" somewhat similar to structures by initialization
   * or zeroing allocation function. So usually stand-alone arrays are treated together with
   * structure array members. However, this is not the case for function parameters (see § 6.7.5.3
   * (7) of the C99 standard), those are actually pointers rather than arrays as their starting
   * address can change (upon a function call since they are inlined) and the corresponding
   * assignment has the same semantics as pointer assignment. So the static method {@link
   * #containsArrayInFunctionParameter(CType)} should be used for function parameters.
   *
   * @param pType any type to check, but normally a composite type
   * @return whether the {@code type} contains array
   */
  static boolean containsArrayOutsideFunctionParameter(final CType pType) {
    checkIsSimplified(pType);
    if (pType instanceof CArrayType) {
      return true;
    } else if (pType instanceof CCompositeType) {
      return containsArrayInComposite(pType);
    } else {
      return false;
    }
  }

  /**
   * Same as {{@link #containsArrayOutsideFunctionParameter(CType)}, but returns {@code false} on
   * stand-alone arrays. This corresponds to the fact that arrays in function parameters are to be
   * treated as pointers.
   *
   * @param pType type any type to check, normally a composite type
   * @return whether the {@code type} contains array
   */
  static boolean containsArrayInFunctionParameter(final CType pType) {
    checkIsSimplified(pType);
    if (pType instanceof CCompositeType) {
      return containsArrayInComposite(pType);
    } else {
      return false;
    }
  }

  /**
   * A generalization of {@link #containsArrayOutsideFunctionParameter(CType)} and {@link
   * #containsArrayInFunctionParameter(CType)} for a known declaration.
   *
   * @param pType pType type any type to check
   * @param pDeclaration declaration of a variable for which the type is checked
   * @return whether the {@code type} contains array
   */
  static boolean containsArray(final CType pType, final CSimpleDeclaration pDeclaration) {
    checkIsSimplified(pType);
    if (pDeclaration instanceof CParameterDeclaration) {
      return containsArrayInFunctionParameter(pType);
    } else {
      return containsArrayOutsideFunctionParameter(pType);
    }
  }

  /**
   * The method returns the type of a base variable by the type of the given memory location.
   *
   * <p>Here we need special handling for arrays as their base variables are handled as pointers to
   * their first (zeroth) elements.
   *
   * @param type The type of the memory location
   * @return The type of the base variable
   */
  static CType getBaseType(CType type) {
    checkIsSimplified(type);
    if (!(type instanceof CArrayType)) {
      return new CPointerType(false, false, type);
    } else {
      return new CPointerType(false, false, ((CArrayType) type).getType());
    }
  }

  static CType implicitCastToPointer(CType type) {
    checkIsSimplified(type);
    if (type instanceof CArrayType) {
      return new CPointerType(false, false, checkIsSimplified(((CArrayType) type).getType()));
    } else if (type instanceof CFunctionType) {
      return new CPointerType(false, false, type);
    } else {
      return type;
    }
  }

  static boolean isSimpleType(final CType type) {
    return !(type instanceof CArrayType) && !(type instanceof CCompositeType);
  }

  /** Only for use from {@link #checkIsSimplified(CType)}. */
  private static synchronized CType simplifyType(final CType type) {
    return type.accept(typeVisitor);
  }

  /**
   * The code in this package works only with "simplified" types, which have typedefs resolved and
   * const and volatile removed (as produced by {@link
   * TypeHandlerWithPointerAliasing#simplifyType(CType)}. This method can be used as an assertion
   * check that a given type has been simplified.
   *
   * @param type A C-type.
   * @return The same type object, if it is simplified, otherwise an exception is thrown.
   */
  static <T extends CType> T checkIsSimplified(final T type) {
    checkArgument(!type.isConst(), "Type %s is const but should have been simplified.", type);
    checkArgument(!type.isVolatile(), "Type %s is volatile but should have been simplified.", type);
    // More expensive checks as assertions
    assert type.equals(type.getCanonicalType())
        : "Type " + type + " is not equal to its canonical type but should have been simplified.";
    assert type.equals(simplifyType(type))
        : "Type " + type + " is not equal to its simplified type but should have been simplified.";
    return type;
  }

  /**
   * The method is used to convert {@link CType}s to strings in order to use them as keys in a
   * {@link PathCopyingPersistentTreeMap}.
   *
   * <p>Note that {@link TypeHandlerWithPointerAliasing#getPointerAccessNameForType(CType)} also
   * converts types to string but in a different context.
   *
   * @param type The type
   * @return The string representation of the type
   */
  static String typeToString(final CType type) {
    checkIsSimplified(type);
    return type.toString();
  }
}
