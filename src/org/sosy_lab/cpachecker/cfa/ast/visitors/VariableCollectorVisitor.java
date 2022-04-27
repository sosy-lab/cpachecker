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

public class VariableCollectorVisitor<X extends Exception>
    extends AExpressionVisitor<Set<AVariableDeclaration>, X> {

  @Override
  public Set<AVariableDeclaration> visit(CTypeIdExpression pIastTypeIdExpression) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(CFieldReference pIastFieldReference) throws X {
    return pIastFieldReference.getFieldOwner().accept(this);
  }

  @Override
  public Set<AVariableDeclaration> visit(CPointerExpression pPointerExpression) throws X {
    return pPointerExpression.getOperand().accept(this);
  }

  @Override
  public Set<AVariableDeclaration> visit(CComplexCastExpression pComplexCastExpression) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JArrayCreationExpression pJArrayCreationExpression)
      throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JArrayInitializer pJArrayInitializer) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JArrayLengthExpression pJArrayLengthExpression) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JVariableRunTimeType pJThisRunTimeType) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JNullLiteralExpression pJNullLiteralExpression) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JEnumConstantExpression pJEnumConstantExpression)
      throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(JThisExpression pThisExpression) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AArraySubscriptExpression pExp) throws X {
    Set<AVariableDeclaration> result = pExp.getArrayExpression().accept_(this);
    result.addAll(pExp.getSubscriptExpression().accept_(this));
    return result;
  }

  @Override
  public Set<AVariableDeclaration> visit(AIdExpression pExp) throws X {
    Set<AVariableDeclaration> modifiedVariables = new HashSet<>();
    modifiedVariables.add((AVariableDeclaration) pExp.getDeclaration());
    return modifiedVariables;
  }

  @Override
  public Set<AVariableDeclaration> visit(ABinaryExpression pExp) throws X {
    Set<AVariableDeclaration> modifiedVariables = pExp.getOperand1().accept_(this);
    modifiedVariables.addAll(pExp.getOperand2().accept_(this));
    return modifiedVariables;
  }

  @Override
  public Set<AVariableDeclaration> visit(ACastExpression pExp) throws X {
    return pExp.getOperand().accept_(this);
  }

  @Override
  public Set<AVariableDeclaration> visit(ACharLiteralExpression pExp) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AFloatLiteralExpression pExp) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AIntegerLiteralExpression pExp) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AStringLiteralExpression pExp) throws X {
    return new HashSet<>();
  }

  @Override
  public Set<AVariableDeclaration> visit(AUnaryExpression pExp) throws X {
    return pExp.getOperand().accept_(this);
  }

  @Override
  public Set<AVariableDeclaration> visit(JClassLiteralExpression pJClassLiteralExpression)
      throws X {
    return new HashSet<>();
  }
}
