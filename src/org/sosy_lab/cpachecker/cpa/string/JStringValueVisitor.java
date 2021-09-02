// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cpa.string.utils.ValueAndAspects;
import org.sosy_lab.cpachecker.exceptions.NoException;

//All methods which just return null are methods that shouldnt be reached -> will be changed
public class JStringValueVisitor
    implements JRightHandSideVisitor<ValueAndAspects, NoException> {

  @Override
  public ValueAndAspects visit(JMethodInvocationExpression pE)
      throws NoException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValueAndAspects visit(JClassInstanceCreation pE)
      throws NoException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValueAndAspects visit(JArraySubscriptExpression pAArraySubscriptExpression)
      throws NoException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValueAndAspects visit(JIdExpression pE) throws NoException {
    if (pE.getDeclaration() instanceof JVariableDeclaration ) {
      JInitializer init =
          (JInitializer) ((JVariableDeclaration) pE.getDeclaration()).getInitializer();
      if (init instanceof JInitializerExpression) {
        return ((JInitializerExpression) init).getExpression().accept(this);
      }
    }
    return null;
  }

  // Convert to String?
  @Override
  public ValueAndAspects visit(JCharLiteralExpression pE)
      throws NoException {
    return null;
  }

  @Override
  public ValueAndAspects visit(JStringLiteralExpression pE)
      throws NoException {
    return new ValueAndAspects(pE.getValue());
  }

  @Override
  public ValueAndAspects visit(JBinaryExpression pE) throws NoException {
    if (pE.getOperator().equals(BinaryOperator.STRING_CONCATENATION)) {
      StringBuilder build = new StringBuilder();
      ValueAndAspects vaa1 = pE.getOperand1().accept(this);
      ValueAndAspects vaa2 = pE.getOperand2().accept(this);
      if (vaa1 != null && vaa2 != null) {
        build.append(vaa1.getValue());
        build.append(vaa2.getValue());
        return new ValueAndAspects(build.toString());
      }
    }
    return null;
  }

  @Override
  public ValueAndAspects visit(JUnaryExpression pE) throws NoException {
    JExpression e = pE.getOperand();
    return e.accept(this);
  }

  @Override
  public ValueAndAspects visit(JIntegerLiteralExpression pJIntegerLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public ValueAndAspects visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public ValueAndAspects visit(JFloatLiteralExpression pJFloatLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public ValueAndAspects visit(JArrayCreationExpression pJArrayCreationExpression)
      throws NoException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValueAndAspects visit(JArrayInitializer pJArrayInitializer) throws NoException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValueAndAspects visit(JArrayLengthExpression pJArrayLengthExpression)
      throws NoException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValueAndAspects visit(JVariableRunTimeType pJThisRunTimeType) throws NoException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValueAndAspects visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType)
      throws NoException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ValueAndAspects visit(JNullLiteralExpression pJNullLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public ValueAndAspects visit(JEnumConstantExpression pJEnumConstantExpression)
      throws NoException {
    return null;
  }

  @Override
  public ValueAndAspects visit(JCastExpression pE) throws NoException {
    JExpression e = pE.getOperand();
    return e.accept(this);
  }

  @Override
  public ValueAndAspects visit(JThisExpression pThisExpression) throws NoException {
    return null;
  }

  @Override
  public ValueAndAspects visit(JClassLiteralExpression pJClassLiteralExpression)
      throws NoException {
    return null;
  }
}
