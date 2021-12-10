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

public class ReplaceVariablesVisitor<X extends Exception>
    extends AExpressionVisitor<AExpression, X> {

  private Map<AVariableDeclaration, AVariableDeclaration> originalToNewVariables;

  public ReplaceVariablesVisitor(
      Map<AVariableDeclaration, AVariableDeclaration> pOriginalToNewVariables) {
    this.originalToNewVariables = pOriginalToNewVariables;
  }

  @Override
  public AExpression visit(CTypeIdExpression pIastTypeIdExpression) throws X {
    return pIastTypeIdExpression;
  }

  @Override
  public AExpression visit(CImaginaryLiteralExpression PIastLiteralExpression) throws X {
    return PIastLiteralExpression;
  }

  @Override
  public AExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws X {
    return pAddressOfLabelExpression;
  }

  @Override
  public AExpression visit(CFieldReference pIastFieldReference) throws X {
    // TODO: This may need to be updated if some errors occur
    return pIastFieldReference;
  }

  @Override
  public AExpression visit(CPointerExpression pPointerExpression) throws X {
    return pPointerExpression.copyWithExpression(pPointerExpression.accept(this));
  }

  @Override
  public AExpression visit(CComplexCastExpression pComplexCastExpression) throws X {
    return pComplexCastExpression.copyWithExpression(pComplexCastExpression.accept(this));
  }

  @Override
  public AExpression visit(JBooleanLiteralExpression pJBooleanLiteralExpression) throws X {
    return pJBooleanLiteralExpression;
  }

  @Override
  public AExpression visit(JArrayCreationExpression pJArrayCreationExpression) throws X {
    // TODO: This may need to be updated if some errors occur
    return pJArrayCreationExpression;
  }

  @Override
  public AExpression visit(JArrayInitializer pJArrayInitializer) throws X {
    // TODO: This may need to be updated if some errors occur
    return pJArrayInitializer;
  }

  @Override
  public AExpression visit(JArrayLengthExpression pJArrayLengthExpression) throws X {

    return pJArrayLengthExpression;
  }

  @Override
  public AExpression visit(JVariableRunTimeType pJThisRunTimeType) throws X {
    return pJThisRunTimeType;
  }

  @Override
  public AExpression visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) throws X {
    return pJRunTimeTypeEqualsType;
  }

  @Override
  public AExpression visit(JNullLiteralExpression pJNullLiteralExpression) throws X {
    return pJNullLiteralExpression;
  }

  @Override
  public AExpression visit(JEnumConstantExpression pJEnumConstantExpression) throws X {
    return pJEnumConstantExpression;
  }

  @Override
  public AExpression visit(JThisExpression pThisExpression) throws X {
    return pThisExpression;
  }

  @Override
  public AExpression visit(AArraySubscriptExpression pExp) throws X {
    return pExp.copyWithExpressions(
        pExp.getArrayExpression().accept_(this), pExp.getSubscriptExpression().accept_(this));
  }

  @Override
  public AExpression visit(AIdExpression pExp) throws X {
    return pExp.copyWithDeclaration(this.originalToNewVariables.get(pExp.getDeclaration()));
  }

  @Override
  public AExpression visit(ABinaryExpression pExp) throws X {
    return new AExpressionFactory(pExp.getOperand1().accept_(this))
        .binaryOperation(pExp.getOperand2().accept_(this), pExp.getOperator())
        .build();
  }

  @Override
  public AExpression visit(ACastExpression pExp) throws X {
    return pExp.copyWithExpression(pExp.getOperand().accept_(this));
  }

  @Override
  public AExpression visit(ACharLiteralExpression pExp) throws X {
    return pExp;
  }

  @Override
  public AExpression visit(AFloatLiteralExpression pExp) throws X {
    return pExp;
  }

  @Override
  public AExpression visit(AIntegerLiteralExpression pExp) throws X {
    return pExp;
  }

  @Override
  public AExpression visit(AStringLiteralExpression pExp) throws X {
    return pExp;
  }

  @Override
  public AExpression visit(AUnaryExpression pExp) throws X {
    return new AExpressionFactory()
        .from(pExp.getOperand().accept_(this))
        .unaryOperation(pExp.getOperator())
        .build();
  }
  
  @Override
  public AExpression visit(JClassLiteralExpression pJClassLiteralExpression) throws X {
    return pJClassLiteralExpression;
  }
}
