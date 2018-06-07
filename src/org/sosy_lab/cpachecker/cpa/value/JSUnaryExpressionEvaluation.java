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
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor.IllegalOperationException;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.cpa.value.type.js.JSUndefinedValue;
import org.sosy_lab.cpachecker.cpa.value.type.js.Type;

final class JSUnaryExpressionEvaluation {
  private JSUnaryExpressionEvaluation() {}

  private static final Map<UnaryOperator, Function<Value, Value>> operatorEvaluation;

  static {
    operatorEvaluation = new HashMap<>();
    operatorEvaluation.put(UnaryOperator.PLUS, new JSUnaryPlusOperatorEvaluation());
    operatorEvaluation.put(UnaryOperator.MINUS, new JSUnaryMinusOperatorEvaluation());
    operatorEvaluation.put(UnaryOperator.NOT, new JSNotOperatorEvaluation());
    operatorEvaluation.put(UnaryOperator.VOID, (pOperand) -> JSUndefinedValue.getInstance());
  }

  public static Value evaluate(final UnaryOperator pOperator, final Value pOperand)
      throws IllegalOperationException {
    if (operatorEvaluation.containsKey(pOperator)) {
      return operatorEvaluation.get(pOperator).apply(pOperand);
    }
    throw new IllegalOperationException("Unhandled unary operator " + pOperator);
  }
}

abstract class JSNumericUnaryOperatorEvaluation implements Function<Value, Value> {

  public abstract Value apply(final NumericValue pOperand);

  @Override
  public Value apply(final Value pOperand) {
    return pOperand.isUnknown() ? UnknownValue.getInstance() : apply(Type.toNumber(pOperand));
  }
}

final class JSUnaryPlusOperatorEvaluation extends JSNumericUnaryOperatorEvaluation {
  @Override
  public Value apply(final NumericValue pOperand) {
    return pOperand;
  }
}

final class JSUnaryMinusOperatorEvaluation extends JSNumericUnaryOperatorEvaluation {

  @Override
  public Value apply(final NumericValue pOperand) {
    // There is no negative integer zero in Java, but in JavaScript.
    // Java has negative double zero.
    // Thus, numbers are always represented as double.
    return new NumericValue(-pOperand.doubleValue());
  }
}

final class JSNotOperatorEvaluation implements Function<Value, Value> {

  @Override
  public BooleanValue apply(final Value pOperand) {
    return Type.toBoolean(pOperand).negate();
  }
}