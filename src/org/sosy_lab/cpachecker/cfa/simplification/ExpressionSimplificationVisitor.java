/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.collect.Sets;

/** This visitor visits an expression and evaluates it.
 * The returnvalue of the visit consists of the simplified expression and
 * - if possible - a numeral value for the expression. */
public class ExpressionSimplificationVisitor extends DefaultCExpressionVisitor
    <Pair<CExpression, Number>, RuntimeException> {

  private final MachineModel machineModel;

  public ExpressionSimplificationVisitor(MachineModel mm) {
    this.machineModel = mm;
  }

  @Override
  protected Pair<CExpression, Number> visitDefault(final CExpression expr) {
    return Pair.of(expr, null);
  }

  @Override
  public Pair<CExpression, Number> visit(final CBinaryExpression expr) {
    final BinaryOperator binaryOperator = expr.getOperator();

    final CExpression op1 = expr.getOperand1();
    final Pair<CExpression, Number> pair1 = op1.accept(this);

    final CExpression op2 = expr.getOperand2();
    final Pair<CExpression, Number> pair2 = op2.accept(this);

    // if one side can not be evaluated, build new expression
    if (pair1.getSecond() == null || pair2.getSecond() == null) {
      final CBinaryExpression newExpr;
      if (pair1.getFirst() == op1 && pair2.getFirst() == op2) {
        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        newExpr = new CBinaryExpression(
            expr.getFileLocation(), expr.getExpressionType(),
            pair1.getFirst(), pair2.getFirst(), binaryOperator);
      }
      return Pair.of((CExpression) newExpr, null);
    }

    long value1 = pair1.getSecond().longValue();
    long value2 = pair2.getSecond().longValue();
    long result;

    switch (binaryOperator) {
    case PLUS:
    case MINUS:
    case DIVIDE:
    case MODULO:
    case MULTIPLY:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR: {

      result = arithmeticOperation(value1, value2, binaryOperator);

      break;
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {

      final boolean tmp = booleanOperation(value1, value2, binaryOperator);
      // return 1 if expression holds, 0 otherwise
      result = tmp ? 1L : 0L;

      break;
    }

    default:
      throw new AssertionError("unknown binary operation: " + binaryOperator);
    }

    return Pair.<CExpression, Number> of(
        new CIntegerLiteralExpression(expr.getFileLocation(),
            expr.getExpressionType(), BigInteger.valueOf(result)),
        result);
  }

  private long arithmeticOperation(long l, long r, BinaryOperator op) {
    // TODO machinemodel
    switch (op) {
    case PLUS:
      return l + r;
    case MINUS:
      return l - r;
    case DIVIDE:
      // TODO signal a division by zero error?
      if (r == 0) { return 0; }
      return l / r;
    case MODULO:
      return l % r;
    case MULTIPLY:
      return l * r;
    case SHIFT_LEFT:
      return l << r;
    case SHIFT_RIGHT: // TODO Java vs C?
      return l >> r;
    case BINARY_AND:
      return l & r;
    case BINARY_OR:
      return l | r;
    case BINARY_XOR:
      return l ^ r;

    default:
      throw new AssertionError("unknown binary operation: " + op);
    }
  }

  private boolean booleanOperation(long l, long r, BinaryOperator op) {
    // TODO machinemodel
    switch (op) {
    case EQUALS:
      return (l == r);
    case NOT_EQUALS:
      return (l != r);
    case GREATER_THAN:
      return (l > r);
    case GREATER_EQUAL:
      return (l >= r);
    case LESS_THAN:
      return (l < r);
    case LESS_EQUAL:
      return (l <= r);

    default:
      throw new AssertionError("unknown binary operation: " + op);
    }
  }

  @Override
  public Pair<CExpression, Number> visit(CCastExpression expr) {
    final CExpression op = expr.getOperand();
    final Pair<CExpression, Number> pair = op.accept(this);

    // if expr can not be evaluated, build new expression
    if (pair.getSecond() == null) {
      final CCastExpression newExpr;
      if (pair.getFirst() == op) {
        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        newExpr = new CCastExpression(
            expr.getFileLocation(), expr.getExpressionType(), pair.getFirst());
      }
      return Pair.of((CExpression) newExpr, null);
    }

    // TODO cast the number
    return pair;
  }

  @Override
  public Pair<CExpression, Number> visit(CComplexCastExpression expr) {
    // evaluation of complex numbers is not supported by now
    return visitDefault(expr);
  }

  @Override
  public Pair<CExpression, Number> visit(CCharLiteralExpression expr) {
    // TODO machinemodel
    return Pair.<CExpression, Number> of(expr, (int) expr.getCharacter());
  }

  @Override
  public Pair<CExpression, Number> visit(CFloatLiteralExpression expr) {
    return visitDefault(expr);
  }

  @Override
  public Pair<CExpression, Number> visit(CIntegerLiteralExpression expr) {
    // TODO machinemodel
    return Pair.<CExpression, Number> of(expr, expr.asLong());
  }

  @Override
  public Pair<CExpression, Number> visit(CImaginaryLiteralExpression expr) {
    return visitDefault(expr);
  }

  @Override
  public Pair<CExpression, Number> visit(CStringLiteralExpression expr) {
    return visitDefault(expr);
  }

  @Override
  public Pair<CExpression, Number> visit(final CIdExpression expr) {
    return visitDefault(expr);
  }

  @Override
  public Pair<CExpression, Number> visit(final CTypeIdExpression expr) {
    final TypeIdOperator idOperator = expr.getOperator();
    final CType innerType = expr.getType();

    switch (idOperator) {
    case SIZEOF:
      int size = machineModel.getSizeof(innerType);
      return Pair.<CExpression, Number> of(
          new CIntegerLiteralExpression(expr.getFileLocation(),
              expr.getExpressionType(), BigInteger.valueOf(size)),
          size);

    default: // TODO support more operators
      return visitDefault(expr);
    }
  }

  @Override
  public Pair<CExpression, Number> visit(final CTypeIdInitializerExpression expr) {
    return visitDefault(expr);
  }

  @Override
  public Pair<CExpression, Number> visit(final CUnaryExpression expr) {
    final UnaryOperator unaryOperator = expr.getOperator();
    final CExpression op = expr.getOperand();

    // in case of a SIZEOF we do not need to know the explicit value of the variable,
    // it is enough to know its type
    if (unaryOperator == UnaryOperator.SIZEOF) {
      final int result = machineModel.getSizeof(op.getExpressionType());
      return Pair.<CExpression, Number> of(
          new CIntegerLiteralExpression(expr.getFileLocation(),
              expr.getExpressionType(), BigInteger.valueOf(result)),
              result);
    }

    final Pair<CExpression, Number> pair = op.accept(this);

    final Set<UnaryOperator> evaluableUnaryOperators = Sets.newHashSet(
        UnaryOperator.PLUS, UnaryOperator.MINUS, UnaryOperator.NOT);

    // if expr can not be evaluated, build new expression
    if (pair.getSecond() == null ||
        !evaluableUnaryOperators.contains(unaryOperator)) {
      final CUnaryExpression newExpr;
      if (pair.getFirst() == op) {
        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        newExpr = new CUnaryExpression(
            expr.getFileLocation(), expr.getExpressionType(),
            pair.getFirst(), unaryOperator);
      }
      return Pair.of((CExpression) newExpr, null);
    }

    long value = pair.getSecond().longValue();
    long result;

    // TODO machinemodel
    switch (unaryOperator) {
    case PLUS:
      result = value;
      break;

    case MINUS:
      result = -value;
      break;

    case NOT:
      result = (value == 0L) ? 1L : 0L;
      break;

    default:
      throw new AssertionError("unknown unary operation: " + unaryOperator);
    }

    return Pair.<CExpression, Number> of(
        new CIntegerLiteralExpression(expr.getFileLocation(),
            expr.getExpressionType(), BigInteger.valueOf(result)),
        result);
  }

  @Override
  public Pair<CExpression, Number> visit(final CPointerExpression expr) {
    return visitDefault(expr);
  }

  @Override
  public Pair<CExpression, Number> visit(final CFieldReference expr) {
    return visitDefault(expr);
  }
}
