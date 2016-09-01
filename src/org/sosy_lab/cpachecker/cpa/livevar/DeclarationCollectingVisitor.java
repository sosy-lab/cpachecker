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

import com.google.common.base.Equivalence.Wrapper;

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
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.util.LiveVariables;

import java.util.Map;
import java.util.BitSet;

/**
 * This visitor collects all ASimpleDeclarations from a given expression. This
 * is independent of the programming language of the evaluated expression.
 */
class DeclarationCollectingVisitor extends AExpressionVisitor<Void, RuntimeException> {

  private final Map<Wrapper<? extends ASimpleDeclaration>, Integer> listPos;
  private final BitSet out;

  public DeclarationCollectingVisitor(
      Map<Wrapper<? extends ASimpleDeclaration>, Integer> pListPos,
      BitSet writeInto) {
    listPos = pListPos;
    out = writeInto;
  }

  @Override
  public Void visit(CTypeIdExpression exp) {
    return null;
  }

  @Override
  public Void visit(CImaginaryLiteralExpression exp) {
    return null;
  }

  @Override
  public Void visit(CFieldReference exp) {
    return exp.getFieldOwner().accept(this);
  }

  @Override
  public Void visit(CPointerExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Void visit(CComplexCastExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Void visit(CAddressOfLabelExpression exp) {
    return null;
  }

  @Override
  public Void visit(JBooleanLiteralExpression exp) {
    return null;
  }

  @Override
  public Void visit(JArrayCreationExpression exp) {
    if (exp.getInitializer() != null) {
      return exp.getInitializer().accept(this);
    } else {
      return null;
    }
  }

  @Override
  public Void visit(JArrayInitializer exp) {
    for (JExpression innerExp : exp.getInitializerExpressions()) {
      innerExp.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(JArrayLengthExpression exp) {
    return exp.getQualifier().accept(this);
  }

  @Override
  public Void visit(JVariableRunTimeType exp) {
    return exp.getReferencedVariable().accept(this);
  }

  @Override
  public Void visit(JRunTimeTypeEqualsType exp) {
    return exp.getRunTimeTypeExpression().accept(this);
  }

  @Override
  public Void visit(JNullLiteralExpression exp) {
    return null;
  }

  @Override
  public Void visit(JEnumConstantExpression exp) {
    return null;
  }

  @Override
  public Void visit(JThisExpression exp) {
    return null;
  }

  @Override
  public Void visit(AArraySubscriptExpression exp) {
    accept0(exp.getArrayExpression());
    accept0(exp.getSubscriptExpression());
    return null;
  }

  @Override
  public Void visit(AIdExpression exp) {
    int pos = listPos.get(
        LiveVariables.LIVE_DECL_EQUIVALENCE.wrap(exp.getDeclaration()));
    out.set(pos);
    return null;
  }

  @Override
  public Void visit(ABinaryExpression exp) {
    accept0(exp.getOperand1());
    accept0(exp.getOperand2());
    return null;
  }

  @Override
  public Void visit(ACastExpression exp) {
    return accept0(exp.getOperand());
  }

  @Override
  public Void visit(ACharLiteralExpression exp) {
    return null;
  }

  @Override
  public Void visit(AFloatLiteralExpression exp) {
    return null;
  }

  @Override
  public Void visit(AIntegerLiteralExpression exp) {
    return null;
  }

  @Override
  public Void visit(AStringLiteralExpression exp) {
    return null;
  }

  @Override
  public Void visit(AUnaryExpression exp) {
    return accept0(exp.getOperand());
  }

  private Void accept0 (AExpression exp) {
    return exp.accept_(this);
  }
}
