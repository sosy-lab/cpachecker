// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;

import java.util.NavigableSet;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** Interface for handling pointer bases in a path formula. */
interface BaseProvider {

  /**
   * Return the currently known pointer bases as a map from variable names to their types.
   *
   * @return A map from variable names to their types, where the types are simplified.
   */
  PersistentSortedMap<String, CType> getBases();

  /**
   * Returns a set of all pointer bases.
   *
   * @return A set of all pointer bases.
   */
  default NavigableSet<String> getAllBases() {
    return getBases().keySet();
  }

  /**
   * Checks if a variable is a prepared base, i.e., if it is a base that has been added to the
   * bases.
   *
   * @param name The name of the variable to check.
   * @return True, if the variable is a prepared base, false otherwise.
   * @see #getBases()
   */
  default boolean isPreparedBase(String name) {
    return getBases().containsKey(name);
  }

  /**
   * Returns, if a variable is the actual base of a pointer. This means that the variable is a base
   * that is not a fake base.
   *
   * @param name The name of the variable.
   * @return True, if the variable is an actual base, false otherwise.
   * @see PointerTargetSetManager#isFakeBaseType(CType)
   */
  default boolean isActualBase(final String name) {
    return isPreparedBase(name) && !PointerTargetSetManager.isFakeBaseType(getBases().get(name));
  }

  /**
   * Checks whether the type associated with the variable is equal to the provided simplified type.
   *
   * @param name the name of the variable
   * @param type the type to check the content of the bases against
   * @return true if the type is a base type, false otherwise
   */
  default boolean isBaseType(String name, CType type) {
    final CType baseType = getBases().get(name);
    return baseType != null && baseType.equals(checkIsSimplified(type));
  }

  /**
   * Checks whether aliasing is necessary for the given id expression.
   *
   * @param idExpression the id expression to check
   * @param idExpressionType the type of the id expression
   * @return true if aliasing is necessary, false otherwise
   */
  default boolean isAliasedWithActualBase(
      final CIdExpression idExpression, final CType idExpressionType) {
    return isActualBase(idExpression.getDeclaration().getQualifiedName())
        || CTypeUtils.containsArray(idExpressionType, idExpression.getDeclaration());
  }

  /**
   * Checks whether aliasing is necessary for the given id expression.
   *
   * @param idExpression the id expression to check
   * @param idExpressionType the type of the id expression
   * @return true if aliasing is necessary, false otherwise
   */
  default boolean isAliasedWithBase(
      final CIdExpression idExpression, final CType idExpressionType) {
    return isBaseType(idExpression.getDeclaration().getQualifiedName(), idExpressionType)
        || CTypeUtils.containsArray(idExpressionType, idExpression.getDeclaration());
  }
}
