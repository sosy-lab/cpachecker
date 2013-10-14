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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;


@SuppressWarnings("unused")
class EvaluatingCExpressionVisitor extends DefaultCExpressionVisitor<Integer, IllegalArgumentException> {

  @Override
  protected Integer visitDefault(final CExpression e) {
    return null;
  }

  @Override
  public Integer visit(final CIntegerLiteralExpression e) throws IllegalArgumentException {
    return (int) e.asLong();
  }

  @Override
  public Integer visit(final CBinaryExpression e) throws IllegalArgumentException {
    final BinaryOperator binaryOperator = e.getOperator();
    final CExpression lVarInBinaryExp = e.getOperand1();
    final CExpression rVarInBinaryExp = e.getOperand2();

    switch (binaryOperator) {
    case PLUS:
    case MINUS:
    case DIVIDE:
    case MULTIPLY:
    case MODULO:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR: {
      final Integer lVal = lVarInBinaryExp.accept(this);
      if (lVal == null) {
        return null;
      }

      final Integer rVal = rVarInBinaryExp.accept(this);
      if (rVal == null) {
        return null;
      }

      switch (binaryOperator) {
      case PLUS:
        return lVal + rVal;

      case MINUS:
        return lVal - rVal;

      case DIVIDE:
        // TODO maybe we should signal a division by zero error?
        if (rVal == 0) {
            return null;
        }

        return lVal / rVal;

      case MULTIPLY:
        return lVal * rVal;

      case MODULO:
        return lVal % rVal;

      case SHIFT_LEFT:
        return lVal << rVal;

      case SHIFT_RIGHT:
        return lVal >> rVal;

      case BINARY_AND:
        return lVal & rVal;

      case BINARY_OR:
        return lVal | rVal;

      case BINARY_XOR:
        return lVal ^ rVal;

      default:
        return null;
      }
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {

      final Integer lVal = lVarInBinaryExp.accept(this);
      if (lVal == null) {
        return null;
      }

      final Integer rVal = rVarInBinaryExp.accept(this);
      if (rVal == null) {
        return null;
      }

      final boolean result;
      switch (binaryOperator) {
      case EQUALS:
        result = (lVal.equals(rVal));
        break;
      case NOT_EQUALS:
        result = !(lVal.equals(rVal));
        break;
      case GREATER_THAN:
        result = (lVal > rVal);
        break;
      case GREATER_EQUAL:
        result = (lVal >= rVal);
        break;
      case LESS_THAN:
        result = (lVal < rVal);
        break;
      case LESS_EQUAL:
        result = (lVal <= rVal);
        break;

      default:
        return null;
      }

      // return 1 if expression holds, 0 otherwise
      return result ? 1 : 0;
    }

    default:
      // TODO check which cases can be handled
      return null;
    }
  }

  @Override
  public Integer visit(CUnaryExpression e) throws IllegalArgumentException {
    final UnaryOperator unaryOperator = e.getOperator();
    final CExpression unaryOperand = e.getOperand();

    Integer value = null;

    switch (unaryOperator) {
    case MINUS:
      value = unaryOperand.accept(this);
      return (value != null) ? -value : null;

    case NOT:
      value = unaryOperand.accept(this);

      if (value == null) {
        return null;
      } else {
        return value == 0 ? 1 : 0;
      }

    case SIZEOF:
      return machineModel.getSizeof(unaryOperand.getExpressionType());

    case TILDE:
    default:
      return null;
    }
  }

  @Override
  public Integer visit(final CTypeIdExpression e) throws IllegalArgumentException {

    final TypeIdOperator typeOperator = e.getOperator();
    final CType type = e.getType();

    switch (typeOperator) {
    case SIZEOF:
      return machineModel.getSizeof(type);
    default:
      return null;
      //TODO Investigate the other Operators.
    }
  }

  @Override
  public Integer visit(CCastExpression e) throws IllegalArgumentException {
    return e.getOperand().accept(this);
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  public EvaluatingCExpressionVisitor(MachineModel model) {
    this.machineModel = model;
  }

  private final MachineModel machineModel;
}

