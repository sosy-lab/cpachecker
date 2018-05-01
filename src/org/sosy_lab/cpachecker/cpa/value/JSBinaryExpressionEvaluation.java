/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor.IllegalOperationException;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

final class JSBinaryExpressionEvaluation {
  private JSBinaryExpressionEvaluation() {}

  private static final Map<JSBinaryExpression.BinaryOperator, BiFunction<Value, Value, Value>>
      operatorEvaluation;

  static {
    operatorEvaluation = new HashMap<>();
    operatorEvaluation.put(BinaryOperator.EQUAL_EQUAL_EQUAL, new JSStrictEqualityEvaluation());
    operatorEvaluation.put(BinaryOperator.NOT_EQUAL_EQUAL, new JSStrictUnequalityEvaluation());
    operatorEvaluation.put(BinaryOperator.PLUS, new JSAdditionOperatorEvaluation());
    operatorEvaluation.put(BinaryOperator.MINUS, new JSSubtractionOperatorEvaluation());
  }

  public static Value evaluate(
      final BinaryOperator pOperator, final Value pLeft, final Value pRight)
      throws IllegalOperationException {
    if (operatorEvaluation.containsKey(pOperator)) {
      return operatorEvaluation.get(pOperator).apply(pLeft, pRight);
    }
    throw new IllegalOperationException("Unhandled operator " + pOperator);
  }
}

final class JSStrictEqualityEvaluation implements BiFunction<Value, Value, Value> {

  @Override
  public BooleanValue apply(final Value pLeft, final Value pRight) {
    if (pLeft instanceof NumericValue && pRight instanceof NumericValue) {
      return apply((NumericValue) pLeft, (NumericValue) pRight);
    }
    return BooleanValue.valueOf(pLeft.equals(pRight));
  }

  public BooleanValue apply(final NumericValue pLeft, final NumericValue pRight) {
    return BooleanValue.valueOf(pLeft.bigDecimalValue().equals(pRight.bigDecimalValue()));
  }
}

final class JSStrictUnequalityEvaluation implements BiFunction<Value, Value, Value> {

  @Override
  public BooleanValue apply(final Value pLeft, final Value pRight) {
    return new JSStrictEqualityEvaluation().apply(pLeft, pRight).negate();
  }
}

abstract class JSNumericBinaryOperatorEvaluation implements BiFunction<Value, Value, Value> {

  public abstract Value apply(final NumericValue pLeft, final NumericValue pRight);

  @Override
  public Value apply(final Value pLeft, final Value pRight) {
    final NumericValue pLeftNumeric = pLeft.asNumericValue();
    final NumericValue pRightNumeric = pRight.asNumericValue();
    if (pLeftNumeric != null && pRightNumeric != null) {
      return apply(pLeftNumeric, pRightNumeric);
    }
    return UnknownValue.getInstance();
  }
}

final class JSAdditionOperatorEvaluation extends JSNumericBinaryOperatorEvaluation {

  @Override
  public Value apply(final NumericValue pLeft, final NumericValue pRight) {
    return new NumericValue(pLeft.bigDecimalValue().add(pRight.bigDecimalValue()));
  }
}

final class JSSubtractionOperatorEvaluation extends JSNumericBinaryOperatorEvaluation {

  @Override
  public Value apply(final NumericValue pLeft, final NumericValue pRight) {
    return new NumericValue(pLeft.bigDecimalValue().subtract(pRight.bigDecimalValue()));
  }
}
