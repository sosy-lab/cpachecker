// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
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
import org.sosy_lab.cpachecker.cfa.ast.visitors.AggregateConstantsVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/*
 * This class returns the amount of iterations for a given Bound and LoopStructure
 * The current Iteration is still very basic and allows only a small subset of the possible
 * The assumption is that the expression being analyzed is in the Loop at:
 * while(EXPR) {}
 */
public class LoopVariableDeltaVisitor extends AExpressionVisitor<Optional<Integer>, NoException> {

  private boolean linearTermsOnly;
  private Loop loopStructure;
  AggregateConstantsVisitor noVariablesVisitor =
      new AggregateConstantsVisitor(Optional.empty(), linearTermsOnly);

  public LoopVariableDeltaVisitor(Loop pLoopStructure, boolean pLinearTermsOnly) {
    this.loopStructure = pLoopStructure;
    linearTermsOnly = pLinearTermsOnly;
  }

  @Override
  public Optional<Integer> visit(CTypeIdExpression pIastTypeIdExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CImaginaryLiteralExpression PIastLiteralExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CFieldReference pIastFieldReference) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CPointerExpression pPointerExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CComplexCastExpression pComplexCastExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JBooleanLiteralExpression pJBooleanLiteralExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JArrayCreationExpression pJArrayCreationExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JArrayInitializer pJArrayInitializer) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JArrayLengthExpression pJArrayLengthExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JVariableRunTimeType pJThisRunTimeType) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JNullLiteralExpression pJNullLiteralExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JEnumConstantExpression pJEnumConstantExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JThisExpression pThisExpression) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AArraySubscriptExpression pExp) {
    // TODO may be used to allow for more complex bounds
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AIdExpression pExp) {
    return loopStructure.getDelta(pExp);
  }

  @Override
  public Optional<Integer> visit(ABinaryExpression pExp) {
    if (pExp instanceof CBinaryExpression) {
      switch (((CBinaryExpression) pExp).getOperator()) {
        case DIVIDE:
          Optional<Integer> operand2Evaluated;
          if (this.linearTermsOnly) {
            operand2Evaluated = pExp.getOperand2().accept_(this.noVariablesVisitor);
          } else {
            operand2Evaluated = pExp.getOperand2().accept_(this);
          }
          Optional<Integer> operand1Evaluated = pExp.getOperand1().accept_(this);
          if (operand2Evaluated.isPresent() && operand1Evaluated.isPresent()) {
            // TODO here a float division may be more appropriate than integer division.
            // But it is the question how do you handle this case. Use the same types as in c?
            return Optional.of(operand1Evaluated.orElseThrow() / operand2Evaluated.orElseThrow());
          } else {
            return Optional.empty();
          }
        case PLUS:
          Optional<Integer> operand2EvaluatedPlus = pExp.getOperand2().accept_(this);
          Optional<Integer> operand1EvaluatedPlus = pExp.getOperand1().accept_(this);
          if (operand2EvaluatedPlus.isPresent() && operand1EvaluatedPlus.isPresent()) {
            return Optional.of(
                operand2EvaluatedPlus.orElseThrow() + operand1EvaluatedPlus.orElseThrow());
          } else {
            return Optional.empty();
          }
        case MINUS:
          Optional<Integer> operand2EvaluatedMinus = pExp.getOperand2().accept_(this);
          Optional<Integer> operand1EvaluatedMinus = pExp.getOperand1().accept_(this);
          if (operand2EvaluatedMinus.isPresent() && operand1EvaluatedMinus.isPresent()) {
            return Optional.of(
                operand1EvaluatedMinus.orElseThrow() - operand2EvaluatedMinus.orElseThrow());
          } else {
            return Optional.empty();
          }
        case MULTIPLY:
          Optional<Integer> operand1EvaluatedMultiply1 = pExp.getOperand1().accept_(this);
          Optional<Integer> operand2EvaluatedMultiply2 = pExp.getOperand2().accept_(this);
          if (this.linearTermsOnly) {
            Optional<Integer> operand1EvaluatedMultiply2 =
                pExp.getOperand1().accept_(this.noVariablesVisitor);
            Optional<Integer> operand2EvaluatedMultiply1 =
                pExp.getOperand2().accept_(this.noVariablesVisitor);
            if (operand2EvaluatedMultiply1.isPresent() && operand1EvaluatedMultiply1.isPresent()) {
              return Optional.of(
                  operand2EvaluatedMultiply1.orElseThrow()
                      * operand1EvaluatedMultiply1.orElseThrow());
            } else if (operand2EvaluatedMultiply2.isPresent()
                && operand1EvaluatedMultiply2.isPresent()) {
              return Optional.of(
                  operand2EvaluatedMultiply2.orElseThrow()
                      * operand1EvaluatedMultiply2.orElseThrow());
            } else {
              return Optional.empty();
            }
          } else {
            if (operand2EvaluatedMultiply2.isPresent() && operand1EvaluatedMultiply1.isPresent()) {
              return Optional.of(
                  operand2EvaluatedMultiply2.orElseThrow()
                      * operand1EvaluatedMultiply1.orElseThrow());
            } else {
              return Optional.empty();
            }
          }
        default:
          return Optional.empty();
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(ACastExpression pExp) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(ACharLiteralExpression pExp) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AFloatLiteralExpression pExp) {
    return Optional.of(0);
  }

  @Override
  public Optional<Integer> visit(AIntegerLiteralExpression pExp) {
    return Optional.of(0);
  }

  @Override
  public Optional<Integer> visit(AStringLiteralExpression pExp) {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AUnaryExpression pExp) {
    if (pExp instanceof CUnaryExpression) {
      if (pExp.getOperator() == UnaryOperator.MINUS) {
        Optional<Integer> result = pExp.getOperand().accept_(this);
        if (result.isPresent()) {
          return Optional.of(-result.orElseThrow());
        } else {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JClassLiteralExpression pJClassLiteralExpression) {
    return Optional.empty();
  }
}
