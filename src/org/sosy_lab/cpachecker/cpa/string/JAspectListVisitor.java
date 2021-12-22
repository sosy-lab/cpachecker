// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;
import org.sosy_lab.cpachecker.cpa.string.utils.AspectSet;
import org.sosy_lab.cpachecker.cpa.string.utils.JStringVariableIdentifier;
import org.sosy_lab.cpachecker.exceptions.NoException;

/*
 * Visitor that creates a list of aspects, depending on the expression.
 * We can expect that only string-variables are returned from the visitor (because of check in transfer relation)
 */
public class JAspectListVisitor
    implements JRightHandSideVisitor<AspectSet, NoException> {

  private final ImmutableList<AbstractStringDomain<?>> domains;
  private final String functionName;
  private StringState state;

  public JAspectListVisitor(StringOptions pOptions, StringState pState, String pFunctionName) {
    domains = ImmutableList.copyOf(pOptions.getDomains());
    this.functionName = pFunctionName;
    state = pState;
  }

  @Override
  public AspectSet visit(JIdExpression pE) throws NoException {
    JSimpleDeclaration jDecl = pE.getDeclaration();
    if (jDecl instanceof JVariableDeclaration) {
      JInitializer init =
          (JInitializer) ((JVariableDeclaration) pE.getDeclaration()).getInitializer();
      if (init instanceof JInitializerExpression) {
        return ((JInitializerExpression) init).getExpression().accept(this);
      }
    }
    if (jDecl instanceof JParameterDeclaration) {
      JStringVariableVisitor jvv = new JStringVariableVisitor(functionName);
      JStringVariableIdentifier jid = jvv.visit(jDecl);
      AspectSet list = state.getAspectList(jid);
      if (list == null) {
        return new AspectSet(ImmutableSet.of());
      }
      return list;
    }
    return new AspectSet(ImmutableSortedSet.of());
  }

  @Override
  public AspectSet visit(JStringLiteralExpression pE)
      throws NoException {
    ImmutableSet.Builder<Aspect<?>> builder = new ImmutableSet.Builder<>();
    String val = pE.getValue();
    if (val == null) {
      return new AspectSet(ImmutableSortedSet.of());
    }
    for (AbstractStringDomain<?> dom : domains) {
      builder.add(dom.addNewAspect(val));
    }
    return new AspectSet(builder.build());
  }

  @Override
  public AspectSet visit(JBinaryExpression pE) throws NoException {

    if (pE.getOperator().equals(BinaryOperator.STRING_CONCATENATION)) {
      return calcAspectsForStringConcat(pE.getOperand1(), pE.getOperand2());
    }
    return new AspectSet(ImmutableSortedSet.of());
  }

  private AspectSet calcAspectsForStringConcat(JExpression op1, JExpression op2) {
    AspectSet aspects1 = op1.accept(this);
    AspectSet aspects2 = op2.accept(this);
    if (aspects1 != null && aspects2 != null) {
      if (!(aspects1.getAspects().isEmpty() && aspects2.getAspects().isEmpty())) {
        ImmutableSet.Builder<Aspect<?>> builder = new ImmutableSet.Builder<>();
        for (AbstractStringDomain<?> dom : domains) {
          builder
              .add(
                  dom.combineAspectsForStringConcat(
                      aspects1.getAspect(dom),
                      aspects2.getAspect(dom)));
        }
        return new AspectSet(builder.build());
      } else {
        return new AspectSet(ImmutableSet.of());
      }
    }
    return null;
  }

  @Override
  public AspectSet visit(JUnaryExpression pE) throws NoException {

    JExpression e = pE.getOperand();
    return e.accept(this);
  }

  @Override
  public AspectSet visit(JCastExpression pE) throws NoException {

    JExpression e = pE.getOperand();
    return e.accept(this);
  }

  @Override
  public AspectSet visit(JArraySubscriptExpression pE) throws NoException {
    return pE.getSubscriptExpression().accept(this);
  }

  @Override
  public AspectSet visit(JVariableRunTimeType pE) throws NoException {
    return pE.getReferencedVariable().accept(this);
  }

  @Override
  public AspectSet visit(JRunTimeTypeEqualsType pE) throws NoException {
    return pE.getRunTimeTypeExpression().accept(this);
  }

  @Override
  public AspectSet visit(JMethodInvocationExpression pE)
      throws NoException {
    // TODO substring
    return new AspectSet(ImmutableSet.of());
  }

  @Override
  public AspectSet visit(JClassInstanceCreation pE)
      throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JCharLiteralExpression pE) throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JIntegerLiteralExpression pJIntegerLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JFloatLiteralExpression pJFloatLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JArrayCreationExpression pJArrayCreationExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JArrayInitializer pJArrayInitializer) throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JArrayLengthExpression pJArrayLengthExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JNullLiteralExpression pJNullLiteralExpression)
      throws NoException {
    return new AspectSet(ImmutableSortedSet.of());
  }

  @Override
  public AspectSet visit(JEnumConstantExpression pJEnumConstantExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JThisExpression pThisExpression) throws NoException {
    return null;
  }

  @Override
  public AspectSet visit(JClassLiteralExpression pJClassLiteralExpression)
      throws NoException {
    return null;
  }

}
