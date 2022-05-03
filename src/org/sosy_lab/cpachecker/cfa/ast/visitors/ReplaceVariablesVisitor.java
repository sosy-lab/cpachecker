// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.visitors;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
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

public class ReplaceVariablesVisitor extends AExpressionVisitor<AExpression, NoException> {

  private Map<AVariableDeclaration, AVariableDeclaration> originalToNewVariables;

  public ReplaceVariablesVisitor(
      Map<AVariableDeclaration, AVariableDeclaration> pOriginalToNewVariables) {
    this.originalToNewVariables = pOriginalToNewVariables;
  }

  @Override
  public AExpression visit(CTypeIdExpression pIastTypeIdExpression) {
    return pIastTypeIdExpression;
  }

  @Override
  public AExpression visit(CImaginaryLiteralExpression PIastLiteralExpression) {
    return PIastLiteralExpression;
  }

  @Override
  public AExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
    return pAddressOfLabelExpression;
  }

  @Override
  public AExpression visit(CFieldReference pIastFieldReference) {
    // TODO: This may need to be updated if some errors occur
    return pIastFieldReference;
  }

  @Override
  public AExpression visit(CPointerExpression pPointerExpression) {
    return pPointerExpression.copyWithExpression(pPointerExpression.accept(this));
  }

  @Override
  public AExpression visit(CComplexCastExpression pComplexCastExpression) {
    return pComplexCastExpression.copyWithExpression(pComplexCastExpression.accept(this));
  }

  @Override
  public AExpression visit(JBooleanLiteralExpression pJBooleanLiteralExpression) {
    return pJBooleanLiteralExpression;
  }

  @Override
  public AExpression visit(JArrayCreationExpression pJArrayCreationExpression) {
    // TODO: This may need to be updated if some errors occur
    return pJArrayCreationExpression;
  }

  @Override
  public AExpression visit(JArrayInitializer pJArrayInitializer) {
    // TODO: This may need to be updated if some errors occur
    return pJArrayInitializer;
  }

  @Override
  public AExpression visit(JArrayLengthExpression pJArrayLengthExpression) {

    return pJArrayLengthExpression;
  }

  @Override
  public AExpression visit(JVariableRunTimeType pJThisRunTimeType) {
    return pJThisRunTimeType;
  }

  @Override
  public AExpression visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) {
    return pJRunTimeTypeEqualsType;
  }

  @Override
  public AExpression visit(JNullLiteralExpression pJNullLiteralExpression) {
    return pJNullLiteralExpression;
  }

  @Override
  public AExpression visit(JEnumConstantExpression pJEnumConstantExpression) {
    return pJEnumConstantExpression;
  }

  @Override
  public AExpression visit(JThisExpression pThisExpression) {
    return pThisExpression;
  }

  @Override
  public AExpression visit(AArraySubscriptExpression pExp) {
    return pExp.copyWithExpressions(
        pExp.getArrayExpression().accept_(this), pExp.getSubscriptExpression().accept_(this));
  }

  @Override
  public AExpression visit(AIdExpression pExp) {
    return pExp.copyWithDeclaration(this.originalToNewVariables.get(pExp.getDeclaration()));
  }

  @Override
  public AExpression visit(ABinaryExpression pExp) {
    return new AExpressionFactory(pExp.getOperand1().accept_(this))
        .binaryOperation(pExp.getOperand2().accept_(this), pExp.getOperator())
        .build();
  }

  @Override
  public AExpression visit(ACastExpression pExp) {
    return pExp.copyWithExpression(pExp.getOperand().accept_(this));
  }

  @Override
  public AExpression visit(ACharLiteralExpression pExp) {
    return pExp;
  }

  @Override
  public AExpression visit(AFloatLiteralExpression pExp) {
    return pExp;
  }

  @Override
  public AExpression visit(AIntegerLiteralExpression pExp) {
    return pExp;
  }

  @Override
  public AExpression visit(AStringLiteralExpression pExp) {
    return pExp;
  }

  @Override
  public AExpression visit(AUnaryExpression pExp) {
    return new AExpressionFactory()
        .from(pExp.getOperand().accept_(this))
        .unaryOperation(pExp.getOperator())
        .build();
  }

  @Override
  public AExpression visit(JClassLiteralExpression pJClassLiteralExpression) {
    return pJClassLiteralExpression;
  }
}
