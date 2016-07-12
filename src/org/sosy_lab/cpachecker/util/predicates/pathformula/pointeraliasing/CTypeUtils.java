/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.OptionalInt;

/**
 * Utility class with helper methods for CTypes.
 */
class CTypeUtils {

  private CTypeUtils() { }

  private static final CachingCanonizingCTypeVisitor typeVisitor = new CachingCanonizingCTypeVisitor(true, true);

  /**
   * Return the length of an array if statically given.
   */
  static OptionalInt getArrayLength(CArrayType t) {
    final CExpression arrayLength = t.getLength();
    if (arrayLength instanceof CIntegerLiteralExpression) {
      return OptionalInt.of(((CIntegerLiteralExpression) arrayLength).getValue().intValue());
    }

    return OptionalInt.empty();
  }

  /**
   * Return the length of an array, honoring the options for maximum and default array length.
   */
  static int getArrayLength(CArrayType t, FormulaEncodingWithPointerAliasingOptions options) {
    OptionalInt length = getArrayLength(t);
    return length.isPresent()
        ? Integer.min(options.maxArrayLength(), length.getAsInt())
        : options.defaultArrayLength();
  }

  /**
   * The method is used to check if a composite type contains array as this means it can't be encoded as a bunch of
   * variables.
   * @param type any type to check, but normally a composite type
   * @return whether the {@code type} contains array
   */
  static boolean containsArray(CType type) {
    checkIsSimplified(type);
    if (type instanceof CArrayType) {
      return true;
    } else if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite!";
      for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (containsArray(memberDeclaration.getType())) {
          return true;
        }
      }
      return false;
    } else {
      return false;
    }
  }

  /**
   * <p>
   * The method returns the type of a base variable by the type of the given memory location.
   * </p>
   * <p>
   * Here we need special handling for arrays as their base variables are handled as pointers to their first
   * (zeroth) elements.
   * </p>
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

  /**
   * Only for use from {@link #checkIsSimplified(CType)}.
   */
  private static synchronized CType simplifyType(final CType type) {
    return type.accept(typeVisitor);
  }

  /**
   * The code in this package works only with "simplified" types,
   * which have typedefs resolved and const and volatile removed
   * (as produced by {@link TypeHandlerWithPointerAliasing#simplifyType(CType)}.
   * This method can be used as an assertion check that a given type has been simplified.
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
   * The method is used in two cases:
   * <ul>
   * <li>
   * by {@link CToFormulaConverterWithPointerAliasing#getPointerAccessName(CType)} to get the UF name corresponding to the given type.
   * </li>
   * <li>
   * to convert {@link CType}s to strings in order to use them as keys in a {@link PathCopyingPersistentTreeMap}.
   * </li>
   * </ul>
   * @param type The type
   * @return The string representation of the type
   */
  static String typeToString(final CType type) {
    checkIsSimplified(type);
    return type.toString();
  }
}
