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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import javax.annotation.Nonnull;

import org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

/**
 * Utility class with helper methods for CTypes.
 */
public class CTypeUtils {

  private CTypeUtils() { }

  private static final CachingCanonizingCTypeVisitor typeVisitor = new CachingCanonizingCTypeVisitor(true, true);

  /**
   * Return the length of an array if statically given, or null.
   */
  public static Integer getArrayLength(CArrayType t) {

    final CExpression arrayLength = t.getLength();
    if (arrayLength instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression)arrayLength).getValue().intValue();
    }

    return null;
  }

  /**
   * The method is used to check if a composite type contains array as this means it can't be encoded as a bunch of
   * variables.
   * @param type any type to check, but normally a composite type
   * @return whether the {@code type} contains array
   */
  public static boolean containsArray(CType type) {
    type = simplifyType(type);
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
  public static CType getBaseType(CType type) {
    type = simplifyType(type);
    if (!(type instanceof CArrayType)) {
      return new CPointerType(false, false, type);
    } else {
      return new CPointerType(false, false, ((CArrayType) type).getType());
    }
  }

  static boolean isCompositeType(CType type) {
    type = simplifyType(type);
    assert !(type instanceof CElaboratedType) : "Unresolved elaborated type";
    assert !(type instanceof CCompositeType) || ((CCompositeType) type).getKind() == ComplexTypeKind.STRUCT ||
                                                ((CCompositeType) type).getKind() == ComplexTypeKind.UNION :
           "Enums are not composite";
    return type instanceof CArrayType || type instanceof CCompositeType;
  }

  static CType implicitCastToPointer(CType type) {
    type = CTypeUtils.simplifyType(type);
    if (type instanceof CArrayType) {
      return new CPointerType(false,
                              false,
                              CTypeUtils.simplifyType(((CArrayType) type).getType()));
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
   * <p>
   * The method should be used everywhere the type of any expression is determined. This is because the encoding uses
   * types for naming of the UFs as well as for over-approximating points-to sets (may-aliases). To make the encoding
   * precise enough the types should correspond to actually different types (requiring explicit casts to be
   * converted to one another), so {@link CCompositeType}s, corresponding  {@link CElaboratedType}s and
   * {@link CTypedefType}s shouldn't be distinguished and are converted to the same canonical type by this method.
   * </p>
   * <p>
   * This method will also perform {@code const} and {@code volatile} modifiers elimination.
   * </p>
   * @param type The type obtained form the CFA
   * @return The corresponding simplified canonical type
   */
  public static CType simplifyType(final @Nonnull CType type) {
    return type.accept(typeVisitor);
  }

  /**
   * The method is used in two cases:
   * <ul>
   * <li>
   * by {@link CToFormulaWithUFConverter#getUFName(CType)} to get the UF name corresponding to the given type.
   * </li>
   * <li>
   * to convert {@link CType}s to strings in order to use them as keys in a {@link PathCopyingPersistentTreeMap}.
   * </li>
   * </ul>
   * @param type The type
   * @return The string representation of the type
   */
  public static String typeToString(final CType type) {
    return simplifyType(type).toString();
  }
}
