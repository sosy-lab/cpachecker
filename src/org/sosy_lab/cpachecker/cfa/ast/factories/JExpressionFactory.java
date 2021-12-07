// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public class JExpressionFactory implements IExpressionFactory {

  private JExpression currentExpression = null;

  public JExpressionFactory(JExpression pAExpression) {
    this.currentExpression = pAExpression;
  }

  public JExpressionFactory() {

  }

  @Override
  public JExpression build() {
    return this.currentExpression;
  }

  @Override
  public JExpressionFactory from(AExpression pAExpression) {
    if (pAExpression instanceof JExpression) {
      this.currentExpression = (JExpression) pAExpression;
    } else {
      return null;
    }
    return this;
  }

  public JExpressionFactory from(Number pValue, JType pType) {
    if (pType == JSimpleType.getInt() || pType == JSimpleType.getLong() || pType == JSimpleType.getShort()) {
      this.currentExpression = JIntegerLiteralExpression.createDummyLiteral((long) pValue);
      return this;
    } else if (pType == JSimpleType.getDouble() || pType == JSimpleType.getFloat()) {
      this.currentExpression = JFloatLiteralExpression.createDummyLiteral((double) pValue);
      return this;
    } else {
      return null;
    }
  }

  public JExpressionFactory binaryOperation(JExpression pExpr, BinaryOperator pOperator) {
    this.currentExpression =
        new JBinaryExpression(
            FileLocation.DUMMY,
            TypeFactory.getMostGeneralType(
                this.currentExpression.getExpressionType(), pExpr.getExpressionType()),
            this.currentExpression,
            pExpr,
            pOperator);
    return this;
  }

  @Override
  public void reset() {
    this.currentExpression = null;
  }

  @Override
  public JExpressionFactory negate() {
    this.currentExpression =
        new JUnaryExpression(
            FileLocation.DUMMY,
            this.currentExpression.getExpressionType(),
            currentExpression,
            JUnaryExpression.UnaryOperator.MINUS);
    return this;
  }

  public JExpressionAssignmentStatement assignTo(JVariableDeclaration pVar) {
    JExpressionFactory tmpFactory = new JExpressionFactory();
    return new JExpressionAssignmentStatement(
        FileLocation.DUMMY, (JLeftHandSide) tmpFactory.from(pVar).build(), currentExpression);
  }

  public JExpressionFactory from(JVariableDeclaration pVariableDeclaration) {
    this.currentExpression =
        new JIdExpression(
            FileLocation.DUMMY,
            pVariableDeclaration.getType(),
            pVariableDeclaration.getQualifiedName(),
            pVariableDeclaration);
    return this;
  }

  public JExpressionFactory unaryOperation(UnaryOperator pOperator) {
    this.currentExpression =
        new JUnaryExpression(
            FileLocation.DUMMY,
            this.currentExpression.getExpressionType(),
            currentExpression,
            pOperator);
    return this;
  }
}
