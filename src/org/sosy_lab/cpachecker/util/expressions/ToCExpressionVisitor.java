// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ToCExpressionVisitor
    extends CachingVisitor<AExpression, CExpression, UnrecognizedCodeException> {

  private final CBinaryExpressionBuilder builder;

  public ToCExpressionVisitor(final MachineModel model, final LogManager logger) {
    builder = new CBinaryExpressionBuilder(model, logger);
  }

  @Override
  protected CExpression cacheMissAnd(And<AExpression> pAnd) throws UnrecognizedCodeException {

    List<CExpression> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pAnd) {
      elements.add(element.accept(this));
    }

    CExpression result =
        builder.buildBinaryExpression(
            elements.getFirst(), CIntegerLiteralExpression.ZERO, BinaryOperator.NOT_EQUALS);

    for (CExpression expr : Iterables.skip(elements, 1)) {
      // This is needed, since the result of a binary AND operation must not
      //   be the same as a boolean AND, e.g. 01 & 10 = 00, but 01 && 10 = true.
      //   To map any non-zero value to 1 we use expr != 0 here

      CExpression boolExpr =
          builder.buildBinaryExpression(
              expr, CIntegerLiteralExpression.ZERO, BinaryOperator.NOT_EQUALS);

      result = builder.buildBinaryExpression(result, boolExpr, BinaryOperator.BINARY_AND);
    }

    return result;
  }

  @Override
  protected CExpression cacheMissOr(Or<AExpression> pOr) throws UnrecognizedCodeException {
    List<CExpression> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pOr) {
      elements.add(element.accept(this));
    }

    CExpression result =
        builder.buildBinaryExpression(
            elements.getFirst(), CIntegerLiteralExpression.ZERO, BinaryOperator.NOT_EQUALS);

    for (CExpression expr : Iterables.skip(elements, 1)) {
      CExpression boolExpr =
          builder.buildBinaryExpression(
              expr, CIntegerLiteralExpression.ZERO, BinaryOperator.NOT_EQUALS);

      result = builder.buildBinaryExpression(result, boolExpr, BinaryOperator.BINARY_OR);
    }

    return result;
  }

  @Override
  protected CExpression cacheMissLeaf(LeafExpression<AExpression> pLeafExpression)
      throws UnrecognizedCodeException {
    if (pLeafExpression.getExpression() instanceof CExpression cExpression) {
      if (pLeafExpression.assumeTruth()) {
        return cExpression;
      } else {
        // We need to negate the expression
        return builder.buildBinaryExpression(
            cExpression, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);
      }
    }
    throw new AssertionError("Unsupported expression type.");
  }

  @Override
  protected CExpression cacheMissTrue() throws UnrecognizedCodeException {
    return CIntegerLiteralExpression.ONE;
  }

  @Override
  protected CExpression cacheMissFalse() throws UnrecognizedCodeException {
    return CIntegerLiteralExpression.ZERO;
  }
}
