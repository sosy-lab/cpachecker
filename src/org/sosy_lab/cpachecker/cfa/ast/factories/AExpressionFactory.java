// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression.AUnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class makes it easier to create new epressions based on existing expressions and assign
 * their result to some left hand side.
 */
public class AExpressionFactory implements ExpressionFactory {

  private CExpressionFactory cExpressionFactory = new CExpressionFactory();
  private JExpressionFactory jExpressionFactory = new JExpressionFactory();

  public ExpressionFactory chosenFactory = null;

  public AExpressionFactory() {}

  public AExpressionFactory(AExpression pAExpression) {
    if (pAExpression instanceof CExpression) {
      this.cExpressionFactory = new CExpressionFactory((CExpression) pAExpression);
      this.chosenFactory = this.cExpressionFactory;
    } else if (pAExpression instanceof JExpression) {
      this.jExpressionFactory = new JExpressionFactory((JExpression) pAExpression);
      this.chosenFactory = this.jExpressionFactory;
    }
  }

  @Override
  public AExpression build() {
    return this.chosenFactory.build();
  }

  public void reset() {
    this.cExpressionFactory.reset();
    this.jExpressionFactory.reset();
    this.chosenFactory = null;
  }

  /**
   * @param value the Numerical value to be constructed
   * @param type A SimpleType of a Number i.e. floats, integers, doubles, ...
   * @return the expression factory with the updated current expression
   */
  public AExpressionFactory from(Number value, Type type) {
    if (type instanceof CType) {
      this.chosenFactory = this.cExpressionFactory;
      ((CExpressionFactory) this.chosenFactory).from(value, (CType) type);
    } else if (type instanceof JType) {
      this.chosenFactory = this.jExpressionFactory;
      ((JExpressionFactory) this.chosenFactory).from(value, (JType) type);
    } else {
      return null;
    }
    return this;
  }

  public AExpressionFactory from(AExpression pAExpression) {
    return new AExpressionFactory(pAExpression);
  }

  public AExpressionFactory from(AVariableDeclaration variableDeclaration) {
    if (variableDeclaration instanceof CVariableDeclaration) {
      this.chosenFactory = this.cExpressionFactory;
      ((CExpressionFactory) this.chosenFactory).from((CVariableDeclaration) variableDeclaration);
    } else if (variableDeclaration instanceof JVariableDeclaration) {
      this.chosenFactory = this.jExpressionFactory;
      ((JExpressionFactory) this.chosenFactory).from((JVariableDeclaration) variableDeclaration);
    } else {
      return null;
    }
    return this;
  }

  public AExpressionFactory binaryOperation(
      AExpression pExpr, ABinaryExpression.ABinaryOperator pOperator) {
    if (pExpr instanceof CExpression
        && pOperator instanceof CBinaryExpression.BinaryOperator
        && this.chosenFactory instanceof CExpressionFactory) {
      ((CExpressionFactory) this.chosenFactory)
          .binaryOperation((CExpression) pExpr, (CBinaryExpression.BinaryOperator) pOperator);
    } else if (pExpr instanceof JExpression
        && pOperator instanceof JBinaryExpression.BinaryOperator
        && this.chosenFactory instanceof JExpressionFactory) {
      ((JExpressionFactory) this.chosenFactory)
          .binaryOperation((JExpression) pExpr, (JBinaryExpression.BinaryOperator) pOperator);
    } else {
      return null;
    }
    return this;
  }

  public AExpressionFactory binaryOperation(
      Number value, Type type, ABinaryExpression.ABinaryOperator pOperator) {
    AExpressionFactory tmpFactory = new AExpressionFactory();
    this.binaryOperation(tmpFactory.from(value, type).build(), pOperator);
    return this;
  }

  public AExpressionFactory binaryOperation(
      Number value,
      Type valueType,
      Type operatorType,
      ABinaryExpression.ABinaryOperator pOperator) {
    AExpressionFactory tmpFactory = new AExpressionFactory();
    this.binaryOperation(tmpFactory.from(value, valueType).build(), operatorType, pOperator);
    return this;
  }

  public AExpressionFactory binaryOperation(
      AExpression pExpr, Type pOperatorType, ABinaryOperator pOperator) {
    if (pExpr instanceof CExpression
        && pOperator instanceof CBinaryExpression.BinaryOperator
        && pOperatorType instanceof CType
        && this.chosenFactory instanceof CExpressionFactory) {
      ((CExpressionFactory) this.chosenFactory)
          .binaryOperation(
              (CExpression) pExpr,
              (CType) pOperatorType,
              (CBinaryExpression.BinaryOperator) pOperator);
    } else if (pExpr instanceof JExpression
        && pOperator instanceof JBinaryExpression.BinaryOperator
        && pOperatorType instanceof JType
        && this.chosenFactory instanceof JExpressionFactory) {
      ((JExpressionFactory) this.chosenFactory)
          .binaryOperation(
              (JExpression) pExpr,
              (JType) pOperatorType,
              (JBinaryExpression.BinaryOperator) pOperator);
    } else {
      return null;
    }
    return this;
  }

  public AExpressionFactory binaryOperation(
      AVariableDeclaration var, ABinaryExpression.ABinaryOperator pOperator) {
    AExpressionFactory tmpFactory = new AExpressionFactory();
    this.binaryOperation(tmpFactory.from(var).build(), pOperator);
    return this;
  }

  public ExpressionFactory negate() {
    if (this.chosenFactory instanceof CExpressionFactory) {
      ((CExpressionFactory) this.chosenFactory).negate();
    } else if (this.chosenFactory instanceof JExpressionFactory) {
      ((JExpressionFactory) this.chosenFactory).negate();
    }
    return this;
  }

  public AExpressionFactory unaryOperation(AUnaryOperator pOperator) {
    if (pOperator instanceof CUnaryExpression.UnaryOperator) {
      ((CExpressionFactory) this.chosenFactory)
          .unaryOperation((CUnaryExpression.UnaryOperator) pOperator);
    } else if (pOperator instanceof JUnaryExpression.UnaryOperator) {
      ((JExpressionFactory) this.chosenFactory)
          .unaryOperation((JUnaryExpression.UnaryOperator) pOperator);
    } else {
      return null;
    }

    return this;
  }

  public AExpressionAssignmentStatement assignTo(AVariableDeclaration pVar) {
    if (pVar instanceof CVariableDeclaration && this.chosenFactory instanceof CExpressionFactory) {
      return ((CExpressionFactory) this.chosenFactory).assignTo((CVariableDeclaration) pVar);
    } else if (pVar instanceof JVariableDeclaration
        && this.chosenFactory instanceof JExpressionFactory) {
      return ((JExpressionFactory) this.chosenFactory).assignTo((JVariableDeclaration) pVar);
    } else {
      return null;
    }
  }
}
