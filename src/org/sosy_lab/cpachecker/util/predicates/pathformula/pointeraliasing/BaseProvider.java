// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.util.NavigableSet;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

interface BaseProvider {

  boolean isActualBase(String name);

  boolean isPreparedBase(String name);

  boolean isBase(String name, CType type);

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
    return isBase(idExpression.getDeclaration().getQualifiedName(), idExpressionType)
        || CTypeUtils.containsArray(idExpressionType, idExpression.getDeclaration());
  }
}
