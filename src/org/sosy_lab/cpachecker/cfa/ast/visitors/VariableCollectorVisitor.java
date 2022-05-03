// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.visitors;

import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
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

public class VariableCollectorVisitor
    extends AExpressionVisitor<Set<AVariableDeclaration>, NoException> {

  @Override
  public Set<AVariableDeclaration> visit(CTypeIdExpression pIastTypeIdExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(CImaginaryLiteralExpression PIastLiteralExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(CFieldReference pIastFieldReference) {
    return pIastFieldReference.getFieldOwner().accept(this);
  }

  @Override
  public Set<AVariableDeclaration> visit(CPointerExpression pPointerExpression) {
    return pPointerExpression.getOperand().accept(this);
  }

  @Override
  public Set<AVariableDeclaration> visit(CComplexCastExpression pComplexCastExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JBooleanLiteralExpression pJBooleanLiteralExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JArrayCreationExpression pJArrayCreationExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JArrayInitializer pJArrayInitializer) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JArrayLengthExpression pJArrayLengthExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JVariableRunTimeType pJThisRunTimeType) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JNullLiteralExpression pJNullLiteralExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JEnumConstantExpression pJEnumConstantExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JThisExpression pThisExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AArraySubscriptExpression pExp) {
    Set<AVariableDeclaration> result = pExp.getArrayExpression().accept_(this);
    result.addAll(pExp.getSubscriptExpression().accept_(this));
    return result;
  }

  @Override
  public Set<AVariableDeclaration> visit(AIdExpression pExp) {
    Set<AVariableDeclaration> modifiedVariables = new HashSet<>();
    modifiedVariables.add((AVariableDeclaration) pExp.getDeclaration());
    return modifiedVariables;
  }

  @Override
  public Set<AVariableDeclaration> visit(ABinaryExpression pExp) {
    Set<AVariableDeclaration> modifiedVariables = pExp.getOperand1().accept_(this);
    modifiedVariables.addAll(pExp.getOperand2().accept_(this));
    return modifiedVariables;
  }

  @Override
  public Set<AVariableDeclaration> visit(ACastExpression pExp) {
    return pExp.getOperand().accept_(this);
  }

  @Override
  public Set<AVariableDeclaration> visit(ACharLiteralExpression pExp) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AFloatLiteralExpression pExp) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AIntegerLiteralExpression pExp) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AStringLiteralExpression pExp) {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AUnaryExpression pExp) {
    return pExp.getOperand().accept_(this);
  }

  @Override
  public Set<AVariableDeclaration> visit(JClassLiteralExpression pJClassLiteralExpression) {
    return new HashSet<>();
  }
}
