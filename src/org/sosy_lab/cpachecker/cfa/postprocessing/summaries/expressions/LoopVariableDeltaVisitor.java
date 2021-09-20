// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions;

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
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/*
 * This class returns the amount of iterations for a given Bound and LoopStructure
 * The current Iteration is still very basic and allows only a small subset of the possible
 * The assumption is that the expression being analyzed is in the Loop at:
 * while(EXPR) {}
 */
public class LoopVariableDeltaVisitor<X extends Exception>
    extends AExpressionVisitor<Optional<Integer>, X> {

  private boolean linearTermsOnly;
  private Loop loopStructure;
  AggregateConstantsVisitor<X> noVariablesVisitor =
      new AggregateConstantsVisitor<>(Optional.empty(), linearTermsOnly);

  public LoopVariableDeltaVisitor(Loop pLoopStructure, boolean pLinearTermsOnly) {
    this.loopStructure = pLoopStructure;
    linearTermsOnly = pLinearTermsOnly;
  }

  @Override
  public Optional<Integer> visit(CTypeIdExpression pIastTypeIdExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CImaginaryLiteralExpression PIastLiteralExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CFieldReference pIastFieldReference) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CPointerExpression pPointerExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(CComplexCastExpression pComplexCastExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JBooleanLiteralExpression pJBooleanLiteralExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JArrayCreationExpression pJArrayCreationExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JArrayInitializer pJArrayInitializer) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JArrayLengthExpression pJArrayLengthExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JVariableRunTimeType pJThisRunTimeType) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JNullLiteralExpression pJNullLiteralExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JEnumConstantExpression pJEnumConstantExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JThisExpression pThisExpression) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AArraySubscriptExpression pExp) throws X {
    // TODO may be used to allow for more complex bounds
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AIdExpression pExp) throws X {
    return loopStructure.getDelta(pExp);
  }

  @Override
  public Optional<Integer> visit(ABinaryExpression pExp) throws X {
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
            return Optional.of(operand1Evaluated.get() / operand2Evaluated.get());
          } else {
            return Optional.empty();
          }
        case PLUS:
          Optional<Integer> operand2EvaluatedPlus = pExp.getOperand2().accept_(this);
          Optional<Integer> operand1EvaluatedPlus = pExp.getOperand1().accept_(this);
          if (operand2EvaluatedPlus.isPresent() && operand1EvaluatedPlus.isPresent()) {
            return Optional.of(operand2EvaluatedPlus.get() + operand1EvaluatedPlus.get());
          } else {
            return Optional.empty();
          }
        case MINUS:
          Optional<Integer> operand2EvaluatedMinus = pExp.getOperand2().accept_(this);
          Optional<Integer> operand1EvaluatedMinus = pExp.getOperand1().accept_(this);
          if (operand2EvaluatedMinus.isPresent() && operand1EvaluatedMinus.isPresent()) {
            return Optional.of(operand1EvaluatedMinus.get() - operand2EvaluatedMinus.get());
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
                  operand2EvaluatedMultiply1.get() * operand1EvaluatedMultiply1.get());
            } else if (operand2EvaluatedMultiply2.isPresent()
                && operand1EvaluatedMultiply2.isPresent()) {
              return Optional.of(
                  operand2EvaluatedMultiply2.get() * operand1EvaluatedMultiply2.get());
            } else {
              return Optional.empty();
            }
          } else {
            if (operand2EvaluatedMultiply2.isPresent() && operand1EvaluatedMultiply1.isPresent()) {
              return Optional.of(
                  operand2EvaluatedMultiply2.get() * operand1EvaluatedMultiply1.get());
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
  public Optional<Integer> visit(ACastExpression pExp) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(ACharLiteralExpression pExp) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AFloatLiteralExpression pExp) throws X {
    return Optional.of(0);
  }

  @Override
  public Optional<Integer> visit(AIntegerLiteralExpression pExp) throws X {
    return Optional.of(0);
  }

  @Override
  public Optional<Integer> visit(AStringLiteralExpression pExp) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AUnaryExpression pExp) throws X {
    if (pExp instanceof CUnaryExpression) {
      if (pExp.getOperator() == UnaryOperator.MINUS) {
        Optional<Integer> result = pExp.getOperand().accept_(this);
        if (result.isPresent()) {
          return Optional.of(-result.get());
        } else {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }
}
