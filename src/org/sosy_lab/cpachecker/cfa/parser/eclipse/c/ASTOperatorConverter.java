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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;

import com.google.common.collect.ImmutableSet;

/** This Class contains functions,
 * that convert operators from C-source into CPAchecker-format. */
class ASTOperatorConverter {

  /** converts and returns the operator of an unaryExpression
   * (PLUS, MINUS, NOT, STAR,...) */
  static UnaryOperator convertUnaryOperator(final IASTUnaryExpression e) {
    switch (e.getOperator()) {
    case IASTUnaryExpression.op_amper:
      return UnaryOperator.AMPER;
    case IASTUnaryExpression.op_minus:
      return UnaryOperator.MINUS;
    case IASTUnaryExpression.op_sizeof:
      return UnaryOperator.SIZEOF;
    case IASTUnaryExpression.op_star:
      throw new IllegalArgumentException("For the star operator, CPointerExpression should be used instead of CUnaryExpression with a star operator.");
    case IASTUnaryExpression.op_tilde:
      return UnaryOperator.TILDE;
    case IASTUnaryExpression.op_alignOf:
      return UnaryOperator.ALIGNOF;
    default:
      throw new CFAGenerationRuntimeException("Unknown unary operator", e);
    }
  }

  /** converts and returns the operator of an binaryExpression
   * (PLUS, MINUS, MULTIPLY,...) with an flag, if the operator causes an assignment. */
  static Pair<BinaryOperator, Boolean> convertBinaryOperator(final IASTBinaryExpression e) {
    boolean isAssign = false;
    final BinaryOperator operator;

    switch (e.getOperator()) {
    case IASTBinaryExpression.op_multiply:
      operator = BinaryOperator.MULTIPLY;
      break;
    case IASTBinaryExpression.op_divide:
      operator = BinaryOperator.DIVIDE;
      break;
    case IASTBinaryExpression.op_modulo:
      operator = BinaryOperator.MODULO;
      break;
    case IASTBinaryExpression.op_plus:
      operator = BinaryOperator.PLUS;
      break;
    case IASTBinaryExpression.op_minus:
      operator = BinaryOperator.MINUS;
      break;
    case IASTBinaryExpression.op_shiftLeft:
      operator = BinaryOperator.SHIFT_LEFT;
      break;
    case IASTBinaryExpression.op_shiftRight:
      operator = BinaryOperator.SHIFT_RIGHT;
      break;
    case IASTBinaryExpression.op_lessThan:
      operator = BinaryOperator.LESS_THAN;
      break;
    case IASTBinaryExpression.op_greaterThan:
      operator = BinaryOperator.GREATER_THAN;
      break;
    case IASTBinaryExpression.op_lessEqual:
      operator = BinaryOperator.LESS_EQUAL;
      break;
    case IASTBinaryExpression.op_greaterEqual:
      operator = BinaryOperator.GREATER_EQUAL;
      break;
    case IASTBinaryExpression.op_binaryAnd:
      operator = BinaryOperator.BINARY_AND;
      break;
    case IASTBinaryExpression.op_binaryXor:
      operator = BinaryOperator.BINARY_XOR;
      break;
    case IASTBinaryExpression.op_binaryOr:
      operator = BinaryOperator.BINARY_OR;
      break;
    case IASTBinaryExpression.op_assign:
      operator = null;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_multiplyAssign:
      operator = BinaryOperator.MULTIPLY;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_divideAssign:
      operator = BinaryOperator.DIVIDE;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_moduloAssign:
      operator = BinaryOperator.MODULO;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_plusAssign:
      operator = BinaryOperator.PLUS;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_minusAssign:
      operator = BinaryOperator.MINUS;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_shiftLeftAssign:
      operator = BinaryOperator.SHIFT_LEFT;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_shiftRightAssign:
      operator = BinaryOperator.SHIFT_RIGHT;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_binaryAndAssign:
      operator = BinaryOperator.BINARY_AND;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_binaryXorAssign:
      operator = BinaryOperator.BINARY_XOR;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_binaryOrAssign:
      operator = BinaryOperator.BINARY_OR;
      isAssign = true;
      break;
    case IASTBinaryExpression.op_equals:
      operator = BinaryOperator.EQUALS;
      break;
    case IASTBinaryExpression.op_notequals:
      operator = BinaryOperator.NOT_EQUALS;
      break;
    default:
      throw new CFAGenerationRuntimeException("Unknown binary operator", e);
    }

    return Pair.of(operator, isAssign);
  }

  /** converts and returns the operator of an idExpression
   * (alignOf, sizeOf,...) */
  static TypeIdOperator convertTypeIdOperator(IASTTypeIdExpression e) {
    switch (e.getOperator()) {
    case IASTTypeIdExpression.op_alignof:
      return TypeIdOperator.ALIGNOF;
    case IASTTypeIdExpression.op_sizeof:
      return TypeIdOperator.SIZEOF;
    case IASTTypeIdExpression.op_typeid:
      return TypeIdOperator.TYPEID;
    case IASTTypeIdExpression.op_typeof:
      return TypeIdOperator.TYPEOF;
    default:
      throw new CFAGenerationRuntimeException("Unknown type id operator", e);
    }
  }

  private static final Set<BinaryOperator> BOOLEAN_BINARY_OPERATORS = ImmutableSet.of(
      BinaryOperator.EQUALS,
      BinaryOperator.NOT_EQUALS,
      BinaryOperator.GREATER_EQUAL,
      BinaryOperator.GREATER_THAN,
      BinaryOperator.LESS_EQUAL,
      BinaryOperator.LESS_THAN);

  static boolean isBooleanExpression(CExpression e) {
    if (e instanceof CBinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(((CBinaryExpression)e).getOperator());

    } else {
      return false;
    }
  }
}
