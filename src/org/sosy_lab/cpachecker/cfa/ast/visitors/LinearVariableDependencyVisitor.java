// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.visitors;

import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils.LinearVariableDependency;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * This visitor takes an AAssignment expression and return an Optional of the Linear Depedendencies
 * of the Variables. The Dependencies are returned if the assignment variable is only linearly
 * constructed by the other variables.
 *
 * <p>The entry method of this visitor is only the AASignment expression and all other functions
 * should be ignored
 */
public class LinearVariableDependencyVisitor
    extends AExpressionVisitor<Optional<LinearVariableDependency>, NoException> {

  public LinearVariableDependencyVisitor() {}

  @Override
  public Optional<LinearVariableDependency> visit(CTypeIdExpression pIastTypeIdExpression) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(
      CImaginaryLiteralExpression PIastLiteralExpression) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(
      CAddressOfLabelExpression pAddressOfLabelExpression) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(CFieldReference pIastFieldReference) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(CPointerExpression pPointerExpression) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(CComplexCastExpression pComplexCastExpression) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(
      JBooleanLiteralExpression pJBooleanLiteralExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(
      JArrayCreationExpression pJArrayCreationExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(JArrayInitializer pJArrayInitializer) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(JArrayLengthExpression pJArrayLengthExpression) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(JVariableRunTimeType pJThisRunTimeType) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(JNullLiteralExpression pJNullLiteralExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(
      JEnumConstantExpression pJEnumConstantExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(JThisExpression pThisExpression) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(AArraySubscriptExpression pExp) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(AIdExpression pExp) {
    LinearVariableDependency linVarDependency = new LinearVariableDependency();
    if (pExp.getDeclaration() instanceof AVariableDeclaration) {
      linVarDependency.insertOrOverwriteDependency(
          (AVariableDeclaration) pExp.getDeclaration(),
          new AExpressionFactory().from(1, pExp.getDeclaration().getType()).build());
    } else {
      // TODO: Support arrays and Parameters
      return Optional.empty();
    }

    return Optional.of(linVarDependency);
  }

  @Override
  public Optional<LinearVariableDependency> visit(ABinaryExpression pExp) {
    Optional<LinearVariableDependency> operand1Result = pExp.getOperand1().accept_(this);
    Optional<LinearVariableDependency> operand2Result = pExp.getOperand2().accept_(this);
    if (operand1Result.isPresent() && operand2Result.isPresent()) {
      if (!operand1Result
          .orElseThrow()
          .modifyDependency(operand2Result.orElseThrow(), pExp.getOperator())) {
        return Optional.empty();
      }
      return operand1Result;
    }
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(ACastExpression pExp) {
    // TODO: It may also be possible to also do this
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(ACharLiteralExpression pExp) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(AFloatLiteralExpression pExp) {
    return Optional.of(new LinearVariableDependency(pExp));
  }

  @Override
  public Optional<LinearVariableDependency> visit(AIntegerLiteralExpression pExp) {
    return Optional.of(new LinearVariableDependency(pExp));
  }

  @Override
  public Optional<LinearVariableDependency> visit(AStringLiteralExpression pExp) {
    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(AUnaryExpression pExp) {
    if (pExp.getOperator() == CUnaryExpression.UnaryOperator.MINUS
        || pExp.getOperator() == JUnaryExpression.UnaryOperator.MINUS) {
      Optional<LinearVariableDependency> innerVisitor = pExp.getOperand().accept_(this);
      if (innerVisitor.isPresent()) {
        innerVisitor.orElseThrow().negateDependencies();
      }
      return innerVisitor;
    }

    return Optional.empty();
  }

  @Override
  public Optional<LinearVariableDependency> visit(
      JClassLiteralExpression pJClassLiteralExpression) {
    return Optional.empty();
  }
}
