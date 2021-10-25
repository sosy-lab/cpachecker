// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/*
 * Creates new Expressions based on old ones to generate the Expressions containing the extrapolations for the Summaries
 */

public class AExpressionsFactory {

  private AAstNode currentExpression;
  private ExpressionType expressionLanguage = ExpressionType.AExpression;

  public enum ExpressionType {
    C,
    JAVA,
    AExpression,
  }

  public AExpressionsFactory() {}

  public AExpressionsFactory(ExpressionType pExpressionLanguage) {
    this.expressionLanguage = pExpressionLanguage;
  }

  public AIntegerLiteralExpression from(Integer value, ExpressionType type) {
    if (type == ExpressionType.C) {
      return CIntegerLiteralExpression.createDummyLiteral(value, CNumericTypes.INT);
    } else if (type == ExpressionType.JAVA) {
      return JIntegerLiteralExpression.createDummyLiteral(value);
    }
    return null;
  }

  public AExpressionsFactory from(AExpression expr) {
    this.currentExpression = expr;
    return this;
  }

  public AExpressionsFactory arithmeticExpression(
      AAstNode pExpr, ABinaryExpression.ABinaryOperator pOperator) {
    if (this.expressionLanguage == ExpressionType.C) {
      // TODO: Better error handling when getting expression type if the current Expression is not
      // an expression.
      // This may include, but is not only, checking if the operation can be done
      this.currentExpression =
          new CBinaryExpression(
              FileLocation.DUMMY,
              (CType)
                  ((AbstractRightHandSide) this.currentExpression)
                      .getExpressionType(), // TODO: See comment in the next line
              (CType)
                  ((AbstractRightHandSide) this.currentExpression)
                      .getExpressionType(), // TODO: Implement a Factory for the types of the
              // expression
              // according to the C standard specification, the current solution my work in most
              // cases but not always
              (CExpression) this.currentExpression,
              (CExpression) pExpr,
              (CBinaryExpression.BinaryOperator) pOperator);
    } else if (this.expressionLanguage == ExpressionType.JAVA) {
      this.currentExpression =
          new JBinaryExpression(
              FileLocation.DUMMY,
              (JType)
                  ((AbstractRightHandSide) this.currentExpression)
                      .getExpressionType(), // TODO: Implement a Factory for the types of the
              // expression
              // according to the Java standard specification, the current solution my work in most
              // cases but not always
              (JExpression) this.currentExpression,
              (JExpression) pExpr,
              (JBinaryExpression.BinaryOperator) pOperator);
    } else {
      // TODO: some logging and better error handling than simply stopping the execution of the
      // Program
      return null;
    }
    return this;
  }

  public AExpressionsFactory minus(int pI) {
    if (this.expressionLanguage == ExpressionType.C) {
      return this.arithmeticExpression(
          CIntegerLiteralExpression.createDummyLiteral(pI, CNumericTypes.INT),
          CBinaryExpression.BinaryOperator.MINUS);
    } else if (this.expressionLanguage == ExpressionType.JAVA) {
      JIntegerLiteralExpression.createDummyLiteral(pI);
      return this.arithmeticExpression(
          JIntegerLiteralExpression.createDummyLiteral(pI), JBinaryExpression.BinaryOperator.MINUS);
    } else {
      return null;
    }
  }

  public AAstNode build() {
    return this.currentExpression;
  }

  public AExpressionsFactory multiply(Integer pI) {
    if (this.expressionLanguage == ExpressionType.C) {
      return this.arithmeticExpression(
          CIntegerLiteralExpression.createDummyLiteral(pI, CNumericTypes.INT),
          CBinaryExpression.BinaryOperator.MULTIPLY);
    } else if (this.expressionLanguage == ExpressionType.JAVA) {
      return this.arithmeticExpression(
          JIntegerLiteralExpression.createDummyLiteral(pI),
          JBinaryExpression.BinaryOperator.MULTIPLY);
    } else {
      return null;
    }
  }

  public AExpressionsFactory divide(Integer pI) {
    if (this.expressionLanguage == ExpressionType.C) {
      return this.arithmeticExpression(
          CIntegerLiteralExpression.createDummyLiteral(pI, CNumericTypes.INT),
          CBinaryExpression.BinaryOperator.DIVIDE);
    } else if (this.expressionLanguage == ExpressionType.JAVA) {
      return this.arithmeticExpression(
          JIntegerLiteralExpression.createDummyLiteral(pI),
          JBinaryExpression.BinaryOperator.DIVIDE);
    } else {
      return null;
    }
  }

  public AExpressionsFactory add(Integer pI) {
    if (this.expressionLanguage == ExpressionType.C) {
      return this.arithmeticExpression(
          CIntegerLiteralExpression.createDummyLiteral(pI, CNumericTypes.INT),
          CBinaryExpression.BinaryOperator.PLUS);
    } else if (this.expressionLanguage == ExpressionType.JAVA) {
      return this.arithmeticExpression(
          JIntegerLiteralExpression.createDummyLiteral(pI), JBinaryExpression.BinaryOperator.PLUS);
    } else {
      return null;
    }
  }

  public AExpressionsFactory assignTo(AVariableDeclaration pVar) {
    if (this.expressionLanguage == ExpressionType.C) {
      CIdExpression leftHandSide = new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) pVar);
      this.currentExpression =
              new CExpressionAssignmentStatement(
                  FileLocation.DUMMY, leftHandSide, (CExpression) this.currentExpression);
      return this;
    } else if (this.expressionLanguage == ExpressionType.JAVA) {
      JIdExpression leftHandSide =
          new JIdExpression(
              FileLocation.DUMMY,
              (JType) ((AbstractRightHandSide) this.currentExpression).getExpressionType(),
              pVar.getName(),
              (JSimpleDeclaration) pVar);
      this.currentExpression =
              new JExpressionAssignmentStatement(
                  FileLocation.DUMMY, leftHandSide, (JExpression) this.currentExpression);
      return this;
    } else {
      return null;
    }
  }

  public AExpressionsFactory add(AAstNode pVar) {
    if (this.expressionLanguage == ExpressionType.C) {
      return this.arithmeticExpression(pVar, CBinaryExpression.BinaryOperator.PLUS);
    } else if (this.expressionLanguage == ExpressionType.JAVA) {
      return this.arithmeticExpression(pVar, JBinaryExpression.BinaryOperator.PLUS);
    } else {
      return null;
    }
  }
}
