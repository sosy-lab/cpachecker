// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.visitors;

import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class OnlyConstantVariableIncrementsVisitor
    extends AExpressionVisitor<Boolean, NoException> {

  Optional<Set<AVariableDeclaration>> knownVariables;
  AggregateConstantsVisitor constantExpressionsVisitor;

  public OnlyConstantVariableIncrementsVisitor(
      Optional<Set<AVariableDeclaration>> pKnownVariables) {
    knownVariables = pKnownVariables;
    constantExpressionsVisitor = new AggregateConstantsVisitor(Optional.empty(), false);
  }

  @Override
  public Boolean visit(CTypeIdExpression pIastTypeIdExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(CFieldReference pIastFieldReference) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(CPointerExpression pPointerExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(CComplexCastExpression pComplexCastExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JArrayCreationExpression pJArrayCreationExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JArrayInitializer pJArrayInitializer) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JArrayLengthExpression pJArrayLengthExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JVariableRunTimeType pJThisRunTimeType) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JNullLiteralExpression pJNullLiteralExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JEnumConstantExpression pJEnumConstantExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JThisExpression pThisExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(AArraySubscriptExpression pExp) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(AIdExpression pExp) {
    if (this.knownVariables.isEmpty()) {
      return Boolean.valueOf(false);
    } else if (this.knownVariables.orElseThrow().contains(pExp.getDeclaration())) {
      return Boolean.valueOf(true);
    } else {
      return Boolean.valueOf(false);
    }
  }

  @Override
  public Boolean visit(ABinaryExpression pExp) {
    if (pExp instanceof CBinaryExpression) {

      switch (((CBinaryExpression) pExp).getOperator()) {
        case MINUS:
        case PLUS:
          return pExp.getOperand1().accept_(this) && pExp.getOperand2().accept_(this);
        case MULTIPLY:
        case DIVIDE:
          return pExp.getOperand1().accept_(this.constantExpressionsVisitor).isPresent()
              && pExp.getOperand2().accept_(this.constantExpressionsVisitor).isPresent();
        default:
          return Boolean.valueOf(false);
      }
    }
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(ACastExpression pExp) {
    // TODO: How do we handle casts?
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(ACharLiteralExpression pExp) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(AFloatLiteralExpression pExp) {
    // TODO: How do we handle floats?
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(AIntegerLiteralExpression pExp) {
    return Boolean.valueOf(true);
  }

  @Override
  public Boolean visit(AStringLiteralExpression pExp) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(AUnaryExpression pExp) {
    if (pExp instanceof CUnaryExpression) {
      if (pExp.getOperator() == UnaryOperator.MINUS) {
        return pExp.getOperand().accept_(this);
      }
    }
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JBooleanLiteralExpression pJBooleanLiteralExpression) {
    return Boolean.valueOf(false);
  }

  @Override
  public Boolean visit(JClassLiteralExpression pJClassLiteralExpression) {
    return Boolean.valueOf(false);
  }
}
