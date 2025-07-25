// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.util.Pair;

/** This Class contains functions, that convert operators from C-source into CPAchecker-format. */
class ASTOperatorConverter {

  private final ParseContext parseContext;

  ASTOperatorConverter(ParseContext pParseContext) {
    parseContext = pParseContext;
  }

  /** converts and returns the operator of a unaryExpression (PLUS, MINUS, NOT, STAR,...) */
  UnaryOperator convertUnaryOperator(final IASTUnaryExpression e) {
    return switch (e.getOperator()) {
      case IASTUnaryExpression.op_amper -> UnaryOperator.AMPER;
      case IASTUnaryExpression.op_minus -> UnaryOperator.MINUS;
      case IASTUnaryExpression.op_sizeof -> UnaryOperator.SIZEOF;
      case IASTUnaryExpression.op_star ->
          throw new IllegalArgumentException(
              "For the star operator, CPointerExpression should be used instead of CUnaryExpression"
                  + " with a star operator.");
      case IASTUnaryExpression.op_tilde -> UnaryOperator.TILDE;
      case IASTUnaryExpression.op_alignOf -> UnaryOperator.ALIGNOF;
      default -> throw parseContext.parseError("Unknown unary operator", e);
    };
  }

  /**
   * converts and returns the operator of a binaryExpression (PLUS, MINUS, MULTIPLY,...) with a
   * flag, if the operator causes an assignment.
   */
  Pair<BinaryOperator, Boolean> convertBinaryOperator(final IASTBinaryExpression e) {
    boolean isAssign = false;
    final BinaryOperator operator;

    switch (e.getOperator()) {
      case IASTBinaryExpression.op_multiply -> operator = BinaryOperator.MULTIPLY;
      case IASTBinaryExpression.op_divide -> operator = BinaryOperator.DIVIDE;
      case IASTBinaryExpression.op_modulo -> operator = BinaryOperator.MODULO;
      case IASTBinaryExpression.op_plus -> operator = BinaryOperator.PLUS;
      case IASTBinaryExpression.op_minus -> operator = BinaryOperator.MINUS;
      case IASTBinaryExpression.op_shiftLeft -> operator = BinaryOperator.SHIFT_LEFT;
      case IASTBinaryExpression.op_shiftRight -> operator = BinaryOperator.SHIFT_RIGHT;
      case IASTBinaryExpression.op_lessThan -> operator = BinaryOperator.LESS_THAN;
      case IASTBinaryExpression.op_greaterThan -> operator = BinaryOperator.GREATER_THAN;
      case IASTBinaryExpression.op_lessEqual -> operator = BinaryOperator.LESS_EQUAL;
      case IASTBinaryExpression.op_greaterEqual -> operator = BinaryOperator.GREATER_EQUAL;
      case IASTBinaryExpression.op_binaryAnd -> operator = BinaryOperator.BINARY_AND;
      case IASTBinaryExpression.op_binaryXor -> operator = BinaryOperator.BINARY_XOR;
      case IASTBinaryExpression.op_binaryOr -> operator = BinaryOperator.BINARY_OR;
      case IASTBinaryExpression.op_assign -> {
        operator = null;
        isAssign = true;
      }
      case IASTBinaryExpression.op_multiplyAssign -> {
        operator = BinaryOperator.MULTIPLY;
        isAssign = true;
      }
      case IASTBinaryExpression.op_divideAssign -> {
        operator = BinaryOperator.DIVIDE;
        isAssign = true;
      }
      case IASTBinaryExpression.op_moduloAssign -> {
        operator = BinaryOperator.MODULO;
        isAssign = true;
      }
      case IASTBinaryExpression.op_plusAssign -> {
        operator = BinaryOperator.PLUS;
        isAssign = true;
      }
      case IASTBinaryExpression.op_minusAssign -> {
        operator = BinaryOperator.MINUS;
        isAssign = true;
      }
      case IASTBinaryExpression.op_shiftLeftAssign -> {
        operator = BinaryOperator.SHIFT_LEFT;
        isAssign = true;
      }
      case IASTBinaryExpression.op_shiftRightAssign -> {
        operator = BinaryOperator.SHIFT_RIGHT;
        isAssign = true;
      }
      case IASTBinaryExpression.op_binaryAndAssign -> {
        operator = BinaryOperator.BINARY_AND;
        isAssign = true;
      }
      case IASTBinaryExpression.op_binaryXorAssign -> {
        operator = BinaryOperator.BINARY_XOR;
        isAssign = true;
      }
      case IASTBinaryExpression.op_binaryOrAssign -> {
        operator = BinaryOperator.BINARY_OR;
        isAssign = true;
      }
      case IASTBinaryExpression.op_equals -> operator = BinaryOperator.EQUALS;
      case IASTBinaryExpression.op_notequals -> operator = BinaryOperator.NOT_EQUALS;
      default -> throw parseContext.parseError("Unknown binary operator", e);
    }

    return Pair.of(operator, isAssign);
  }

  /** converts and returns the operator of an idExpression (alignOf, sizeOf,...) */
  TypeIdOperator convertTypeIdOperator(IASTTypeIdExpression e) {
    return switch (e.getOperator()) {
      case IASTTypeIdExpression.op_alignof -> TypeIdOperator.ALIGNOF;
      case IASTTypeIdExpression.op_sizeof -> TypeIdOperator.SIZEOF;
      case IASTTypeIdExpression.op_typeof -> TypeIdOperator.TYPEOF;
      default -> throw parseContext.parseError("Unknown type id operator", e);
    };
  }

  private static final ImmutableSet<BinaryOperator> BOOLEAN_BINARY_OPERATORS =
      Sets.immutableEnumSet(
          BinaryOperator.EQUALS,
          BinaryOperator.NOT_EQUALS,
          BinaryOperator.GREATER_EQUAL,
          BinaryOperator.GREATER_THAN,
          BinaryOperator.LESS_EQUAL,
          BinaryOperator.LESS_THAN);

  static boolean isBooleanExpression(CExpression e) {
    if (e instanceof CBinaryExpression cBinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(cBinaryExpression.getOperator());

    } else {
      return false;
    }
  }
}
