// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CExpressionFactory implements ExpressionFactory {

  private CExpression currentExpression = null;

  public CExpressionFactory(CExpression pAExpression) {
    this.currentExpression = pAExpression;
  }

  public CExpressionFactory() {}

  @Override
  public CExpression build() {
    return this.currentExpression;
  }

  public CExpressionFactory from(AExpression pAExpression) {
    if (pAExpression instanceof CExpression) {
      this.currentExpression = (CExpression) pAExpression;
    } else {
      return null;
    }
    return this;
  }

  public CExpressionFactory from(Number pValue, CType pType) {
    if (pType instanceof CSimpleType) {
      if (((CSimpleType) pType).getType() == CBasicType.INT
          || ((CSimpleType) pType).getType() == CBasicType.INT128
          || ((CSimpleType) pType).isLong()
          || ((CSimpleType) pType).isLongLong()
          || ((CSimpleType) pType).isUnsigned()) {
        this.currentExpression =
            CIntegerLiteralExpression.createDummyLiteral(pValue.longValue(), pType);
      } else if (((CSimpleType) pType).getType() == CBasicType.FLOAT
          || ((CSimpleType) pType).getType() == CBasicType.DOUBLE
          || ((CSimpleType) pType).getType() == CBasicType.FLOAT128) {
        this.currentExpression =
            CFloatLiteralExpression.createDummyLiteral(pValue.doubleValue(), pType);
      }
    } else {
      return null;
    }
    return this;
  }

  public CExpressionFactory binaryOperation(CExpression pExpr, BinaryOperator pOperator) {
    this.currentExpression =
        new CBinaryExpression(
            FileLocation.DUMMY,
            (CType)
                TypeFactory.getMostGeneralType(
                    this.currentExpression.getExpressionType(), pExpr.getExpressionType()),
            (CType)
                TypeFactory.getMostGeneralType(
                    this.currentExpression.getExpressionType(), pExpr.getExpressionType()),
            this.currentExpression,
            pExpr,
            pOperator);
    return this;
  }

  public void reset() {
    this.currentExpression = null;
  }

  public ExpressionFactory negate() {
    this.currentExpression =
        new CUnaryExpression(
            FileLocation.DUMMY,
            this.currentExpression.getExpressionType(),
            currentExpression,
            CUnaryExpression.UnaryOperator.MINUS);
    return this;
  }

  public CExpressionAssignmentStatement assignTo(CVariableDeclaration pVar) {
    return new CExpressionAssignmentStatement(
        FileLocation.DUMMY,
        (CIdExpression) new CExpressionFactory().from(pVar).build(),
        currentExpression);
  }

  public CExpressionFactory from(CVariableDeclaration pVariableDeclaration) {
    this.currentExpression =
        new CIdExpression(
            FileLocation.DUMMY,
            pVariableDeclaration.getType(),
            pVariableDeclaration.getName(),
            pVariableDeclaration);
    return this;
  }

  public CExpressionFactory unaryOperation(UnaryOperator pOperator) {
    this.currentExpression =
        new CUnaryExpression(
            FileLocation.DUMMY,
            this.currentExpression.getExpressionType(),
            currentExpression,
            pOperator);
    return this;
  }

  public CExpressionFactory binaryOperation(
      CExpression pExpr, CType pOperatorType, BinaryOperator pOperator) {
    this.currentExpression =
        new CBinaryExpression(
            FileLocation.DUMMY,
            pOperatorType,
            pOperatorType,
            this.currentExpression,
            pExpr,
            pOperator);
    return this;
  }

  public AExpressionAssignmentStatement assignTo(CLeftHandSide pVar) {
    return new CExpressionAssignmentStatement(FileLocation.DUMMY, pVar, currentExpression);
  }
}
