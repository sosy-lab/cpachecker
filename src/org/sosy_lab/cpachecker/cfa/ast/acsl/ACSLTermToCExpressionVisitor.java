// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ACSLTermToCExpressionVisitor
    implements ACSLTermVisitor<CExpression, UnrecognizedCodeException> {

  private final CFA cfa;
  private final LogManager logger;

  private final Map<ACSLTerm, CExpression> cache = new HashMap<>();

  public ACSLTermToCExpressionVisitor(CFA pCfa, LogManager pLogger) {
    cfa = pCfa;
    logger = pLogger;
  }

  @Override
  public CExpression visit(ACSLBinaryTerm binaryTerm) throws UnrecognizedCodeException {
    CExpression result = cache.get(binaryTerm);
    if (result == null) {
      CBinaryExpressionBuilder builder =
          new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
      CExpression leftExpression = binaryTerm.getLeft().accept(this);
      CExpression rightExpression = binaryTerm.getRight().accept(this);
      BinaryOperator op =
          switch (binaryTerm.getOperator()) {
            case BAND -> CBinaryExpression.BinaryOperator.BINARY_AND;
            case BOR -> CBinaryExpression.BinaryOperator.BINARY_OR;
            case BXOR -> CBinaryExpression.BinaryOperator.BINARY_XOR;
            case PLUS -> CBinaryExpression.BinaryOperator.PLUS;
            case MINUS -> CBinaryExpression.BinaryOperator.MINUS;
            case TIMES -> CBinaryExpression.BinaryOperator.MULTIPLY;
            case DIVIDE -> CBinaryExpression.BinaryOperator.DIVIDE;
            case MOD -> CBinaryExpression.BinaryOperator.MODULO;
            case LSHIFT -> CBinaryExpression.BinaryOperator.SHIFT_LEFT;
            case RSHIFT -> CBinaryExpression.BinaryOperator.SHIFT_RIGHT;
            case EQ -> CBinaryExpression.BinaryOperator.EQUALS;
            case NEQ -> CBinaryExpression.BinaryOperator.NOT_EQUALS;
            case LEQ -> CBinaryExpression.BinaryOperator.LESS_EQUAL;
            case GEQ -> CBinaryExpression.BinaryOperator.GREATER_EQUAL;
            case LT -> CBinaryExpression.BinaryOperator.LESS_THAN;
            case GT -> CBinaryExpression.BinaryOperator.GREATER_THAN;
            default -> throw new AssertionError("Invalid operator: " + binaryTerm.getOperator());
          };
      result = builder.buildBinaryExpression(leftExpression, rightExpression, op);
      cache.put(binaryTerm, result);
    }
    return result;
  }

  @Override
  public CExpression visit(ACSLUnaryTerm unaryTerm) throws UnrecognizedCodeException {
    CExpression result = cache.get(unaryTerm);
    if (result == null) {
      CExpression inner = unaryTerm.getInnerTerm().accept(this);
      ACSLUnaryOperator operator = unaryTerm.getOperator();
      if (operator == ACSLUnaryOperator.POINTER_DEREF) {
        return new CPointerExpression(inner.getFileLocation(), inner.getExpressionType(), inner);
      } else if (operator == ACSLUnaryOperator.PLUS) {
        return inner;
      }
      UnaryOperator op =
          switch (operator) {
            case BNEG -> CUnaryExpression.UnaryOperator.TILDE;
            case MINUS -> CUnaryExpression.UnaryOperator.MINUS;
            case ADDRESS_OF -> CUnaryExpression.UnaryOperator.AMPER;
            case SIZEOF -> CUnaryExpression.UnaryOperator.SIZEOF;
            default -> throw new AssertionError("Unknown unary operator: " + operator);
          };
      result = new CUnaryExpression(inner.getFileLocation(), inner.getExpressionType(), inner, op);
      cache.put(unaryTerm, result);
    }
    return result;
  }

  @Override
  public CExpression visit(ACSLArrayAccess arrayAccess) throws UnrecognizedCodeException {
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

  @Override
  public CExpression visit(ACSLCast cast) throws UnrecognizedCodeException {
    CExpression result = cache.get(cast);
    if (result == null) {
      CExpression inner = cast.getTerm().accept(this);
      CType type = cast.getType().toCType();
      result = new CCastExpression(inner.getFileLocation(), type, inner);
      cache.put(cast, result);
    }
    return result;
  }

  @Override
  public CExpression visit(ACSLIdentifier identifier) {
    CExpression result = cache.get(identifier);
    if (result == null) {
      CProgramScope scope = new CProgramScope(cfa, logger);
      CSimpleDeclaration variableDeclaration = scope.lookupVariable(identifier.getName());
      if (variableDeclaration != null) {
        result = new CIdExpression(variableDeclaration.getFileLocation(), variableDeclaration);
        cache.put(identifier, result);
      } else {
        throw new AssertionError("Unknown variable identifier: " + identifier.getName());
      }
    }
    return result;
  }

  @Override
  public CExpression visit(BoundIdentifier boundIdentifier) {
    CExpression result = cache.get(boundIdentifier);
    if (result == null) {
      // TODO: Dummy implementation as placeholder, remove this later
      CVariableDeclaration declaration =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              CNumericTypes.INT,
              boundIdentifier.getName(),
              boundIdentifier.getName(),
              boundIdentifier.getFunctionName() + "::" + boundIdentifier.getName(),
              null);
      result = new CIdExpression(FileLocation.DUMMY, declaration);
    }
    return result;
  }

  @Override
  public CExpression visit(ACSLIntegerLiteral integerLiteral) {
    CExpression result = cache.get(integerLiteral);
    if (result == null) {
      result =
          new CIntegerLiteralExpression(
              FileLocation.DUMMY, CNumericTypes.INT, integerLiteral.getLiteral());
      cache.put(integerLiteral, result);
    }
    return result;
  }

  @Override
  public CExpression visit(ACSLStringLiteral stringLiteral) {
    CExpression result = cache.get(stringLiteral);
    if (result == null) {
      result = new CStringLiteralExpression(FileLocation.DUMMY, stringLiteral.getLiteral());
      cache.put(stringLiteral, result);
    }
    return result;
  }

  @Override
  public CExpression visit(ACSLResult acslResult) {
    CExpression result = cache.get(acslResult);
    if (result == null) {
      CProgramScope scope = new CProgramScope(cfa, logger);
      CSimpleDeclaration variableDeclaration =
          scope.getFunctionReturnVariable(acslResult.getFunctionName());
      result = new CIdExpression(variableDeclaration.getFileLocation(), variableDeclaration);
      cache.put(acslResult, result);
    }
    return result;
  }

  @Override
  public CExpression visit(TermAt at) {
    CExpression result = cache.get(at);
    if (result == null) {
      if (at.getLabel().equals(ACSLDefaultLabel.OLD)) {
        result = cache.get(at.getInner());
        assert result != null : "Expected to have seen the old value already";
        cache.put(at, result);
      } else {
        throw new UnsupportedOperationException("Translation of \\at currently not supported.");
      }
    }
    return result;
  }
}
