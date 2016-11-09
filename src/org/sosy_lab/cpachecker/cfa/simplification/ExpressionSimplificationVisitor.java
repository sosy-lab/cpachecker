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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

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

  /** return a simplified version of the expression. */
  protected CExpression recursive(CExpression expr) {
    return expr.accept(this);
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
    if (numericResult != null && expr.getExpressionType() instanceof CSimpleType) {
      CSimpleType type = (CSimpleType) expr.getExpressionType();
      if (type.getType().isIntegerType()) {
        return new CIntegerLiteralExpression(expr.getFileLocation(),
                expr.getExpressionType(), BigInteger.valueOf(numericResult.longValue()));
      } else if (type.getType().isFloatingPointType()) {
        try {
          return new CFloatLiteralExpression(expr.getFileLocation(),
              expr.getExpressionType(), numericResult.bigDecimalValue());
        } catch (NumberFormatException nfe) {
          // catch NumberFormatException here, which is caused by, e.g., value being <infinity>
          logger.logf(Level.FINE, "Cannot simplify expression to numeric value %s, keeping original expression %s instead", numericResult, expr.toASTString());
          return expr;
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

    final CExpression op1 = recursive(expr.getOperand1());
    final NumericValue value1 = getValue(op1);

    final CExpression op2 = recursive(expr.getOperand2());
    final NumericValue value2 = getValue(op2);

    // if one side can not be evaluated, build new expression
    if (value1 == null || value2 == null) {
      final CBinaryExpression newExpr;
      if (op1 == expr.getOperand1() && op2 == expr.getOperand2()) {
        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        final CBinaryExpressionBuilder binExprBuilder = new CBinaryExpressionBuilder(machineModel, logger);
        newExpr = binExprBuilder.buildBinaryExpressionUnchecked(
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
    final CExpression op = recursive(expr.getOperand());
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
    final Value castedValue = AbstractExpressionValueVisitor.castCValue(
        value, expr.getExpressionType(), machineModel, logger, expr.getFileLocation());


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

      case ALIGNOF:
        int alignment = machineModel.getAlignof(innerType);
        return new CIntegerLiteralExpression(
            expr.getFileLocation(), expr.getExpressionType(), BigInteger.valueOf(alignment));

    default: // TODO support more operators
      return visitDefault(expr);
    }
  }

  @Override
  public CExpression visit(final CUnaryExpression expr) {
    final UnaryOperator unaryOperator = expr.getOperator();
    final FileLocation loc = expr.getFileLocation();
    final CType exprType = expr.getExpressionType();
    final CExpression operand = expr.getOperand();
    final CType operandType = operand.getExpressionType();

    // in case of a SIZEOF we do not need to know the explicit value of the variable,
    // it is enough to know its type
    if (unaryOperator == UnaryOperator.SIZEOF) {
      return new CIntegerLiteralExpression(loc, exprType, BigInteger.valueOf(machineModel.getSizeof(operandType)));
    } else if (unaryOperator == UnaryOperator.ALIGNOF) {
      return new CIntegerLiteralExpression(loc, exprType, BigInteger.valueOf(machineModel.getAlignof(operandType)));
    }

    final CExpression op = recursive(operand);
    assert op.getExpressionType().equals(operandType) : "simplification should not change type";
    final NumericValue value = getValue(op);

    if (value != null && operandType instanceof CSimpleType) {
      if (unaryOperator == UnaryOperator.MINUS) {
        // we have to cast the value, because it can overflow, for example for the unary-expression "-2147483648" (=MIN_INT),
        // where the operand's value "2147483648" itself creates an overflow, and the negation reverses it.
        final NumericValue negatedValue = (NumericValue) AbstractExpressionValueVisitor.castCValue(
            value.negate(), exprType, machineModel, logger, loc);
        switch (((CSimpleType)operandType).getType()) {
        case BOOL: // negation of zero is zero, other values should be irrelevant
        case CHAR:
        case INT:
          return new CIntegerLiteralExpression(loc, exprType, BigInteger.valueOf(negatedValue.longValue()));
        case FLOAT:
        case DOUBLE:
          double v = negatedValue.doubleValue();
          // Check if v is -0.0; if so, we cannot simplify it,
          // because we cannot represent it with BigDecimal
          if (v == 0 && 1 / v < 0) {
            return new CUnaryExpression(loc, exprType, op, unaryOperator);
          }
          return new CFloatLiteralExpression(loc, exprType, BigDecimal.valueOf(v));
        default:
          // fall-through and return the original expression
        }

      } else if (unaryOperator == UnaryOperator.TILDE && ((CSimpleType)operandType).getType().isIntegerType()) {
        // cast the value, because the evaluation of "~" is done for long and maybe the target-type is integer.
        final NumericValue complementValue = (NumericValue) AbstractExpressionValueVisitor.castCValue(
            new NumericValue(~value.longValue()), exprType, machineModel, logger, loc);
        return new CIntegerLiteralExpression(loc, exprType, BigInteger.valueOf(complementValue.longValue()));
      }
    }

    final CUnaryExpression newExpr;
    if (op == operand) {
      // shortcut: if nothing has changed, use the original expression
      newExpr = expr;
    } else {
      newExpr = new CUnaryExpression(loc, exprType, op, unaryOperator);
    }
    return newExpr;
  }

  @Override
  public CExpression visit(CIdExpression expr) {
    final CSimpleDeclaration decl = expr.getDeclaration();
    final CType type = expr.getExpressionType();

    // enum constant
    if (decl instanceof CEnumType.CEnumerator &&
            ((CEnumType.CEnumerator)decl).hasValue()) {
      final long v = ((CEnumType.CEnumerator)decl).getValue();
      return new CIntegerLiteralExpression(expr.getFileLocation(),
              type, BigInteger.valueOf(v));
    }

    // const variable, inline initializer
    if (!(type instanceof CProblemType)
        && type.isConst()
        && decl instanceof CVariableDeclaration) {

      final CInitializer init = ((CVariableDeclaration)decl).getInitializer();
      if (init instanceof CExpression) {
        NumericValue v = getValue((CExpression)init);

        if (v != null && decl.getType() instanceof CSimpleType) {
          switch (((CSimpleType) type).getType()) {
            case BOOL:
            case CHAR:
            case INT:
              return new CIntegerLiteralExpression(expr.getFileLocation(),
                      type, BigInteger.valueOf(v.longValue()));
            case FLOAT:
            case DOUBLE:
              return new CFloatLiteralExpression(expr.getFileLocation(),
                      type, BigDecimal.valueOf(v.doubleValue()));
            default:
              // fall-through and return the original expression
          }
        }
      }
    }

    return visitDefault(expr);
  }
}
