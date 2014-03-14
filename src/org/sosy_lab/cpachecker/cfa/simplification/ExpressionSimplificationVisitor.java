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
package org.sosy_lab.cpachecker.cfa.simplification;

import java.math.BigInteger;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.Value;

/** This visitor visits an expression and evaluates it.
 * The returnvalue of the visit consists of the simplified expression and
 * - if possible - a numeral value for the expression. */
public class ExpressionSimplificationVisitor extends DefaultCExpressionVisitor
    <CExpression, RuntimeException> {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  public ExpressionSimplificationVisitor(MachineModel mm, LogManagerWithoutDuplicates pLogger) {
    this.machineModel = mm;
    this.logger = pLogger;
  }

  private NumericValue getValue(CExpression expr) {
    if (expr instanceof CIntegerLiteralExpression) {
      return new NumericValue(((CIntegerLiteralExpression)expr).getValue());
    } else if (expr instanceof CCharLiteralExpression) {
      return new NumericValue((int)((CCharLiteralExpression)expr).getCharacter());
    } else if (expr instanceof CFloatLiteralExpression) {
      return new NumericValue(((CFloatLiteralExpression)expr).getValue());
    }
    return null;
  }

  /**
   * Takes an explicit value as returned by various ExplicitCPA functions and
   * converts it to a <code>Pair<CExpression, Number></code> as required by
   * this class.
   */
  private CExpression convertExplicitValueToExpression(final CExpression expr, Value value) {
    // TODO: handle cases other than numeric values
    NumericValue numericResult = value.asNumericValue();
    if(numericResult != null && expr.getExpressionType() instanceof CSimpleType) {
      CSimpleType type = (CSimpleType) expr.getExpressionType();
      switch(type.getType()) {
        case INT:
        case CHAR: {
          return new CIntegerLiteralExpression(expr.getFileLocation(),
                  expr.getExpressionType(), BigInteger.valueOf(numericResult.longValue()));
        }
        case FLOAT:
        case DOUBLE: {
          return new CFloatLiteralExpression(expr.getFileLocation(),
                  expr.getExpressionType(), numericResult.bigDecimalValue());
        }
      }
    }
    if (numericResult != null) {
      logger.logf(Level.FINE, "Can not handle result of expression %s", numericResult.toString());
    } else {
      logger.logf(Level.FINE, "Can not handle result of expression, numericResult is null.");
    }
    return expr;
  }

  @Override
  protected CExpression visitDefault(final CExpression expr) {
    return expr;
  }

  @Override
  public CExpression visit(final CBinaryExpression expr) {
    final BinaryOperator binaryOperator = expr.getOperator();

    final CExpression op1 = expr.getOperand1().accept(this);
    final NumericValue value1 = getValue(op1);

    final CExpression op2 = expr.getOperand2().accept(this);
    final NumericValue value2 = getValue(op2);

    // if one side can not be evaluated, build new expression
    if (value1 == null || value2 == null) {
      final CBinaryExpression newExpr;
      if (op1 == expr.getOperand1() && op2 == expr.getOperand2()) {
        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        final CBinaryExpressionBuilder binExprBuilder = new CBinaryExpressionBuilder(machineModel, logger);
        newExpr = binExprBuilder.buildBinaryExpression(
            op1, op2, binaryOperator);
      }
      return newExpr;
    }

    // TODO: handle the case that it's not a CSimpleType or that it's not a number
    Value result = AbstractExpressionValueVisitor.calculateBinaryOperation(
        value1, value2,
        expr, machineModel, logger);

    return convertExplicitValueToExpression(expr, result);
  }

  @Override
  public CExpression visit(CCastExpression expr) {
    final CExpression op = expr.getOperand().accept(this);
    final NumericValue value = getValue(op);

    // if expr can not be evaluated, build new expression
    if (value == null) {
      final CCastExpression newExpr;
      if (op == expr.getOperand()) {
        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        newExpr = new CCastExpression(
            expr.getFileLocation(), expr.getExpressionType(), op);
      }
      return newExpr;
    }

    // TODO: handle the case that the result is not a numeric value
    CSimpleType type = (CSimpleType) op.getExpressionType().getCanonicalType();
    final Value castedValue = AbstractExpressionValueVisitor.castCValue(
        value, expr.getOperand().getExpressionType(), expr.getExpressionType(), machineModel, logger, expr.getFileLocation());


    return convertExplicitValueToExpression(expr, castedValue);
  }

  @Override
  public CExpression visit(final CTypeIdExpression expr) {
    final TypeIdOperator idOperator = expr.getOperator();
    final CType innerType = expr.getType();

    switch (idOperator) {
    case SIZEOF:
      int size = machineModel.getSizeof(innerType);
      return new CIntegerLiteralExpression(expr.getFileLocation(),
              expr.getExpressionType(), BigInteger.valueOf(size));

    default: // TODO support more operators
      return visitDefault(expr);
    }
  }

  @Override
  public CExpression visit(final CUnaryExpression expr) {
    final UnaryOperator unaryOperator = expr.getOperator();
    // in case of a SIZEOF we do not need to know the explicit value of the variable,
    // it is enough to know its type
    if (unaryOperator == UnaryOperator.SIZEOF) {
      final int result = machineModel.getSizeof(expr.getOperand().getExpressionType());
      return new CIntegerLiteralExpression(expr.getFileLocation(),
              expr.getExpressionType(), BigInteger.valueOf(result));
    }

    final CExpression op = expr.getOperand().accept(this);
    final NumericValue value = getValue(op);

    if (unaryOperator == UnaryOperator.MINUS && value != null) {
      final long negatedValue = -value.longValue();
      return new CIntegerLiteralExpression(expr.getFileLocation(),
                      expr.getExpressionType(), BigInteger.valueOf(negatedValue));

    }

    final CUnaryExpression newExpr;
    if (op == expr.getOperand()) {
      // shortcut: if nothing has changed, use the original expression
      newExpr = expr;
    } else {
      newExpr = new CUnaryExpression(
          expr.getFileLocation(), expr.getExpressionType(),
          op, unaryOperator);
    }
    return newExpr;
  }
}
