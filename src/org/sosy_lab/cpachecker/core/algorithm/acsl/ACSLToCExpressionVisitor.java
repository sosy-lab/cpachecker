/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ACSLToCExpressionVisitor {

  private final CFA cfa;
  private final LogManager logger;

  private Map<ACSLTerm, CExpression> cache = new HashMap<>();

  public ACSLToCExpressionVisitor(CFA pCfa, LogManager pLogger) {
    cfa = pCfa;
    logger = pLogger;
  }

  public CExpression visit(ACSLBinaryTerm binaryTerm) throws UnrecognizedCodeException {
    CExpression result = cache.get(binaryTerm);
    if (result == null) {
      CBinaryExpressionBuilder builder =
          new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
      CExpression leftExpression = binaryTerm.getLeft().accept(this);
      CExpression rightExpression = binaryTerm.getRight().accept(this);
      CBinaryExpression.BinaryOperator op;
      switch (binaryTerm.getOperator()) {
        case BAND:
          op = CBinaryExpression.BinaryOperator.BINARY_AND;
          break;
        case BOR:
          op = CBinaryExpression.BinaryOperator.BINARY_OR;
          break;
        case BXOR:
          op = CBinaryExpression.BinaryOperator.BINARY_XOR;
          break;
        case PLUS:
          op = CBinaryExpression.BinaryOperator.PLUS;
          break;
        case MINUS:
          op = CBinaryExpression.BinaryOperator.MINUS;
          break;
        case TIMES:
          op = CBinaryExpression.BinaryOperator.MULTIPLY;
          break;
        case DIVIDE:
          op = CBinaryExpression.BinaryOperator.DIVIDE;
          break;
        case MOD:
          op = CBinaryExpression.BinaryOperator.MODULO;
          break;
        case LSHIFT:
          op = CBinaryExpression.BinaryOperator.SHIFT_LEFT;
          break;
        case RSHIFT:
          op = CBinaryExpression.BinaryOperator.SHIFT_RIGHT;
          break;
        default:
          throw new AssertionError("Invalid operator: " + binaryTerm.getOperator());
      }
      result = builder.buildBinaryExpression(leftExpression, rightExpression, op);
      cache.put(binaryTerm, result);
    }
    return result;
  }

  public CExpression visit(ACSLUnaryTerm unaryTerm) throws UnrecognizedCodeException {
    CExpression result = cache.get(unaryTerm);
    if (result == null) {
      CExpression inner = unaryTerm.getInnerTerm().accept(this);
      UnaryOperator operator = unaryTerm.getOperator();
      if (operator == UnaryOperator.POINTER_DEREF) {
        return new CPointerExpression(inner.getFileLocation(), inner.getExpressionType(), inner);
      } else if (operator == UnaryOperator.PLUS) {
        return inner;
      }
      CUnaryExpression.UnaryOperator op;
      switch (operator) {
        case BNEG:
          op = CUnaryExpression.UnaryOperator.TILDE;
          break;
        case MINUS:
          op = CUnaryExpression.UnaryOperator.MINUS;
          break;
        case ADDRESS_OF:
          op = CUnaryExpression.UnaryOperator.AMPER;
          break;
        case SIZEOF:
          op = CUnaryExpression.UnaryOperator.SIZEOF;
          break;
        default:
          throw new AssertionError("Unknown unary operator: " + operator);
      }
      result = new CUnaryExpression(inner.getFileLocation(), inner.getExpressionType(), inner, op);
      cache.put(unaryTerm, result);
    }
    return result;
  }

  public CExpression visit(ArrayAccess arrayAccess) throws UnrecognizedCodeException {
    CExpression result = cache.get(arrayAccess);
    if (result == null) {
      CExpression arrayExpression = arrayAccess.getArray().accept(this);
      CExpression subscriptExpression = arrayAccess.getIndex().accept(this);
      result =
          new CArraySubscriptExpression(
              arrayExpression.getFileLocation(),
              arrayExpression.getExpressionType(),
              arrayExpression,
              subscriptExpression);
      cache.put(arrayAccess, result);
    }
    return result;
  }

  public CExpression visit(Cast cast) throws UnrecognizedCodeException {
    CExpression result = cache.get(cast);
    if (result == null) {
      CExpression inner = cast.getTerm().accept(this);
      CType type = cast.getType().toCType();
      result = new CCastExpression(inner.getFileLocation(), type, inner);
      cache.put(cast, result);
    }
    return result;
  }

  public CExpression visit(Identifier identifier) {
    CExpression result = cache.get(identifier);
    if (result == null) {
      if (identifier.shouldUseOldValue()) {
        result = cache.get(new Identifier(identifier.getName()));
        assert result != null : "Expected to have seen the old value already";
        cache.put(identifier, result);
      } else {
        CProgramScope scope = new CProgramScope(cfa, logger);
        CSimpleDeclaration variableDeclaration = scope.lookupVariable(identifier.getName());
        if (variableDeclaration != null) {
          result = new CIdExpression(variableDeclaration.getFileLocation(), variableDeclaration);
          cache.put(identifier, result);
        } else {
          throw new AssertionError("Unknown variable identifier: " + identifier.getName());
        }
      }
    }
    return result;
  }

  public CExpression visit(IntegerLiteral integerLiteral) {
    CExpression result = cache.get(integerLiteral);
    if (result == null) {
      result =
          new CIntegerLiteralExpression(
              FileLocation.DUMMY,
              CNumericTypes.LONG_LONG_INT,
              BigInteger.valueOf(integerLiteral.getLiteral()));
      cache.put(integerLiteral, result);
    }
    return result;
  }

  public CExpression visit(StringLiteral stringLiteral) {
    CExpression result = cache.get(stringLiteral);
    if (result == null) {
      result =
          new CStringLiteralExpression(
              FileLocation.DUMMY,
              new CPointerType(false, false, CNumericTypes.UNSIGNED_CHAR),
              stringLiteral.getLiteral());
      cache.put(stringLiteral, result);
    }
    return result;
  }

  public CExpression visit(ACSLComparisonPredicate pred) throws UnrecognizedCodeException {
    CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    CExpression leftExpression = pred.getLeft().accept(this);
    CExpression rightExpression = pred.getRight().accept(this);
    CBinaryExpression.BinaryOperator op;
    switch (pred.getOperator()) {
      case EQ:
        op = CBinaryExpression.BinaryOperator.EQUALS;
        break;
      case NEQ:
        op = CBinaryExpression.BinaryOperator.NOT_EQUALS;
        break;
      case LEQ:
        op = CBinaryExpression.BinaryOperator.LESS_EQUAL;
        break;
      case GEQ:
        op = CBinaryExpression.BinaryOperator.GREATER_EQUAL;
        break;
      case LT:
        op = CBinaryExpression.BinaryOperator.LESS_THAN;
        break;
      case GT:
        op = CBinaryExpression.BinaryOperator.GREATER_THAN;
        break;
      default:
        throw new AssertionError("Unknown comparison operator: " + pred.getOperator());
    }
    return builder.buildBinaryExpression(leftExpression, rightExpression, op);
  }
}
