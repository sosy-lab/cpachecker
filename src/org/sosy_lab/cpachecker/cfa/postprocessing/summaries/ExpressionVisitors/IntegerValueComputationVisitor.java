// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.ExpressionVisitors;

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

public class IntegerValueComputationVisitor<X extends Exception>
    extends AExpressionVisitor<Optional<Integer>, X> {

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
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(AIdExpression pExp) throws X {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> visit(ABinaryExpression pExp) throws X {
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
        Optional<Integer> result = pExp.getOperand().accept_(this);
        if (result.isEmpty()) {
          return Optional.empty();
        } else {
          return Optional.of(-result.get());
        }
      }
    }

    return Optional.empty();
  }
}
