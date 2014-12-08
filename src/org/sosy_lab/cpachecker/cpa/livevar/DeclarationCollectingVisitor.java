/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.livevar;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;

import com.google.common.collect.Sets;

/**
 * This visitor collects all ASimpleDeclarations from a given expression. This
 * is independent of the programming language of the evaluated expression.
 */
public class DeclarationCollectingVisitor extends AExpressionVisitor<Set<ASimpleDeclaration>, RuntimeException> {

  @Override
  public Set<ASimpleDeclaration> visit(CTypeIdExpression pIastTypeIdExpression) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(CImaginaryLiteralExpression PIastLiteralExpression) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(CFieldReference pIastFieldReference) throws RuntimeException {
    return pIastFieldReference.getFieldOwner().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(CPointerExpression pPointerExpression) throws RuntimeException {
    return pPointerExpression.getOperand().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(CComplexCastExpression pComplexCastExpression) throws RuntimeException {
    return pComplexCastExpression.getOperand().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(JBooleanLiteralExpression pJBooleanLiteralExpression) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(JArrayCreationExpression pJBooleanLiteralExpression) throws RuntimeException {
    return pJBooleanLiteralExpression.getInitializer().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(JArrayInitializer pJArrayInitializer) throws RuntimeException {
    Set<ASimpleDeclaration> result = Collections.emptySet();
    for (JExpression exp : pJArrayInitializer.getInitializerExpressions()) {
      result = Sets.union(result, exp.accept(this));
    }
    return result;
  }

  @Override
  public Set<ASimpleDeclaration> visit(JVariableRunTimeType pJThisRunTimeType) throws RuntimeException {
    return pJThisRunTimeType.getReferencedVariable().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) throws RuntimeException {
    return pJRunTimeTypeEqualsType.getRunTimeTypeExpression().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(JNullLiteralExpression pJNullLiteralExpression) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(JEnumConstantExpression pJEnumConstantExpression) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(JThisExpression pThisExpression) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AArraySubscriptExpression pExp) throws RuntimeException {
    return Sets.union(accept0(pExp.getArrayExpression()),
                      accept0(pExp.getSubscriptExpression()));
  }

  @Override
  public Set<ASimpleDeclaration> visit(AIdExpression pExp) throws RuntimeException {
    return Collections.singleton(pExp.getDeclaration());
  }

  @Override
  public Set<ASimpleDeclaration> visit(ABinaryExpression pExp) throws RuntimeException {
    return Sets.union(accept0(pExp.getOperand1()),
                      accept0(pExp.getOperand2()));
  }

  @Override
  public Set<ASimpleDeclaration> visit(ACastExpression pExp) throws RuntimeException {
    return accept0(pExp.getOperand());
  }

  @Override
  public Set<ASimpleDeclaration> visit(ACharLiteralExpression pExp) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AFloatLiteralExpression pExp) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AIntegerLiteralExpression pExp) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AStringLiteralExpression pExp) throws RuntimeException {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AUnaryExpression pExp) throws RuntimeException {
    return accept0(pExp.getOperand());
  }

  private Set<ASimpleDeclaration> accept0 (AExpression exp) {
    return exp.<Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                RuntimeException,
                RuntimeException,
                DeclarationCollectingVisitor>accept_(this);
  }
}
