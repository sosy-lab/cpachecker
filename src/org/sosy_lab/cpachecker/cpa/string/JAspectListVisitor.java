// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.domains.StringSetDomain;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;
import org.sosy_lab.cpachecker.cpa.string.utils.AspectList;
import org.sosy_lab.cpachecker.cpa.string.utils.AspectList.UnknownValueAndAspects;
import org.sosy_lab.cpachecker.exceptions.NoException;

/*
 * Visitor that creates a list of aspects, depending on the expression.
 * We can expect that only string-variables are used in the visitor (because of check in transfer relation)
 */
public class JAspectListVisitor
    implements JRightHandSideVisitor<AspectList, NoException> {

  private final ImmutableList<AbstractStringDomain<?>> domains;
  private StringOptions options;

  public JAspectListVisitor(StringOptions pOptions) {
    domains = ImmutableList.copyOf(pOptions.getDomains());
    options = pOptions;
  }

  @Override
  public AspectList visit(JIdExpression pE) throws NoException {

    if (pE.getDeclaration() instanceof JVariableDeclaration ) {

      JInitializer init =
          (JInitializer) ((JVariableDeclaration) pE.getDeclaration()).getInitializer();

      if (init instanceof JInitializerExpression) {
        return ((JInitializerExpression) init).getExpression().accept(this);
      }
    }

    return new AspectList(ImmutableList.of(UnknownAspect.getInstance()));
  }

  @Override
  public AspectList visit(JStringLiteralExpression pE)
      throws NoException {

    ImmutableList.Builder<Aspect<?>> builder = new ImmutableList.Builder<>();
    String val = pE.getValue();

    for (AbstractStringDomain<?> dom : domains) {

      // Add string literal to string
      if (dom instanceof StringSetDomain && !options.getStringSet().contains(val)) {
        options.addStringToGivenSet(val);
      }

      builder.add(dom.addNewAspect(val));

    }

    return new AspectList(builder.build());
  }

  @Override
  public AspectList visit(JBinaryExpression pE) throws NoException {

    if (pE.getOperator().equals(BinaryOperator.STRING_CONCATENATION)) {
      return calcAspectsForStringConcat(pE.getOperand1(), pE.getOperand2());
    }

    return new AspectList(ImmutableList.of(UnknownAspect.getInstance()));
  }

  private AspectList calcAspectsForStringConcat(JExpression op1, JExpression op2) {

    AspectList vaa1 = op1.accept(this);
    AspectList vaa2 = op2.accept(this);

    if (vaa1 != null && vaa2 != null) {
      if (!(vaa1 instanceof UnknownValueAndAspects) && !(vaa2 instanceof UnknownValueAndAspects)) {

        ImmutableList.Builder<Aspect<?>> builder = new ImmutableList.Builder<>();

        for (AbstractStringDomain<?> dom : domains) {
          builder
              .add(
                  dom.combineAspectsForStringConcat(
                      vaa1.getAspectOfDomain(dom),
                      vaa2.getAspectOfDomain(dom)));
        }

        return new AspectList(builder.build());

      } else {
        return new AspectList(ImmutableList.of(UnknownAspect.getInstance()));

      }

    }

    return null;
  }

  @Override
  public AspectList visit(JUnaryExpression pE) throws NoException {

    JExpression e = pE.getOperand();
    return e.accept(this);
  }

  @Override
  public AspectList visit(JCastExpression pE) throws NoException {

    JExpression e = pE.getOperand();
    return e.accept(this);
  }

  @Override
  public AspectList visit(JArraySubscriptExpression pE) throws NoException {
    return pE.getSubscriptExpression().accept(this);
  }

  @Override
  public AspectList visit(JVariableRunTimeType pE) throws NoException {
    return pE.getReferencedVariable().accept(this);
  }

  @Override
  public AspectList visit(JRunTimeTypeEqualsType pE) throws NoException {
    return pE.getRunTimeTypeExpression().accept(this);
  }

  @Override
  public AspectList visit(JMethodInvocationExpression pE)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JClassInstanceCreation pE)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JCharLiteralExpression pE) throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JIntegerLiteralExpression pJIntegerLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JFloatLiteralExpression pJFloatLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JArrayCreationExpression pJArrayCreationExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JArrayInitializer pJArrayInitializer) throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JArrayLengthExpression pJArrayLengthExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JNullLiteralExpression pJNullLiteralExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JEnumConstantExpression pJEnumConstantExpression)
      throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JThisExpression pThisExpression) throws NoException {
    return null;
  }

  @Override
  public AspectList visit(JClassLiteralExpression pJClassLiteralExpression)
      throws NoException {
    return null;
  }

}
