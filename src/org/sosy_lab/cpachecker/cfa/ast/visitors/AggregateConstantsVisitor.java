// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.visitors;

import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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

public class AggregateConstantsVisitor<X extends Exception>
    extends AExpressionVisitor<Optional<Integer>, X> {

  Optional<Set<String>> knownVariables;
  private boolean linearTermsOnly;
  AggregateConstantsVisitor<X> noVariablesVisitor;

  /*
   * The Set of Known variables must contain the qualified names of the variables
   */
  public AggregateConstantsVisitor(
      Optional<Set<String>> pKnownVariables, boolean pLinearTermsOnly) {
    knownVariables = pKnownVariables;
    linearTermsOnly = pLinearTermsOnly;
    if (pKnownVariables.isPresent()) {
      noVariablesVisitor = new AggregateConstantsVisitor<>(Optional.empty(), linearTermsOnly);
    } else {
      noVariablesVisitor = this;
    }
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
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AIdExpression pExp) throws X {
    if (this.knownVariables.isEmpty()) {
      return Optional.empty();
    } else if (this.knownVariables.orElseThrow().contains(pExp.getDeclaration().getQualifiedName())) {
      return Optional.of(0);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Integer> visit(ABinaryExpression pExp) throws X {
    if (pExp instanceof CBinaryExpression) {
      Optional<Integer> operand1Maybe =  pExp.getOperand1().accept_(this);
      if (((CBinaryExpression) pExp).getOperator() == BinaryOperator.DIVIDE) {
        Optional<Integer> operand2Maybe;
        if (this.linearTermsOnly) {
          operand2Maybe = pExp.getOperand2().accept_(this.noVariablesVisitor);
        } else {
          operand2Maybe = pExp.getOperand2().accept_(this);
        }

        if (operand2Maybe.isPresent() && operand1Maybe.isPresent()) {
          // TODO here a float division may be more appropriate than integer division.
          // But it is the question how do you handle this case. Use the same types as in c?
          return Optional.of(operand1Maybe.orElseThrow() / operand2Maybe.orElseThrow());
        } else {
          return Optional.empty();
        }
      }

      Optional<Integer> operand2Maybe =  pExp.getOperand2().accept_(this);
      if (operand1Maybe.isEmpty() || operand2Maybe.isEmpty()) {
        return Optional.empty();
      }

      Integer operand1 = operand1Maybe.orElseThrow();
      Integer operand2 = operand2Maybe.orElseThrow();

      switch (((CBinaryExpression) pExp).getOperator()) {
        case MINUS:
          return Optional.of(operand1 - operand2);
        case MULTIPLY:
          if (this.linearTermsOnly) {
            if (pExp.getOperand1() instanceof AIntegerLiteralExpression || pExp.getOperand2() instanceof AIntegerLiteralExpression) {
              return Optional.of(operand1 * operand2);
            } else {
              return Optional.empty();
            }
          } else {
            return Optional.of(operand1 * operand2);
          }
        case PLUS:
          return Optional.of(operand1 + operand2);
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
    // TODO: How do we handle floats?
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AIntegerLiteralExpression pExp) throws X {
    return Optional.of(pExp.getValue().intValue());
  }

  @Override
  public Optional<Integer> visit(AStringLiteralExpression pExp) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AUnaryExpression pExp) throws X {
    if (pExp instanceof CUnaryExpression) {
      if (pExp.getOperator() == UnaryOperator.MINUS) {
        Optional<Integer> operandEvaluated = pExp.getOperand().accept_(this);
        if (operandEvaluated.isPresent()) {
          return Optional.of(-operandEvaluated.orElseThrow());
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(JBooleanLiteralExpression pJBooleanLiteralExpression) throws X {
    return Optional.empty();
  }
}
