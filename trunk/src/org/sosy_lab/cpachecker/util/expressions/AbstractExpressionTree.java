// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;

abstract class AbstractExpressionTree<LeafType> implements ExpressionTree<LeafType> {

  @Override
  public String toString() {
    return accept(new ToCodeVisitor<>(this::formatLeafExpression));
  }

  private String formatLeafExpression(LeafType pLeafExpression) {
    if (pLeafExpression instanceof CExpression) {
      return ((CExpression) pLeafExpression)
          .accept(CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER);
    }
    if (pLeafExpression == null) {
      return "null";
    }
    return pLeafExpression.toString();
  }
}
