/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class ToCExpressionVisitor
    extends CachingVisitor<AExpression, CExpression, UnrecognizedCCodeException> {

  private final CBinaryExpressionBuilder builder;

  public ToCExpressionVisitor(final MachineModel model, final LogManager logger) {
    builder = new CBinaryExpressionBuilder(model, logger);
  }

  @Override
  protected CExpression cacheMissAnd(And<AExpression> pAnd) throws UnrecognizedCCodeException {

    List<CExpression> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pAnd) {
      elements.add(element.accept(this));
    }

    CExpression result = elements.get(0);

    for (CExpression expr : Iterables.skip(elements, 1)) {
      result = builder.buildBinaryExpression(result, expr, BinaryOperator.BINARY_AND);
    }

    return result;
  }

  @Override
  protected CExpression cacheMissOr(Or<AExpression> pOr) throws UnrecognizedCCodeException {
    List<CExpression> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pOr) {
      elements.add(element.accept(this));
    }

    CExpression result = elements.get(0);

    for (CExpression expr : Iterables.skip(elements, 1)) {
      result = builder.buildBinaryExpression(result, expr, BinaryOperator.BINARY_OR);
    }

    return result;
  }

  @Override
  protected CExpression cacheMissLeaf(LeafExpression<AExpression> pLeafExpression)
      throws UnrecognizedCCodeException {
    if (pLeafExpression.getExpression() instanceof CExpression) {
      return (CExpression) pLeafExpression.getExpression();
    }
    throw new AssertionError("Unsupported expression type.");
  }

  @Override
  protected CExpression cacheMissTrue() throws UnrecognizedCCodeException {
    return CIntegerLiteralExpression.ONE;
  }

  @Override
  protected CExpression cacheMissFalse() throws UnrecognizedCCodeException {
    return CIntegerLiteralExpression.ZERO;
  }
}
