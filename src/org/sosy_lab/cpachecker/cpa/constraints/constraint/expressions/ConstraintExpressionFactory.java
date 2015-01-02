/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicExpression;

/**
 * Factory for creating {@link ConstraintExpression}s
 */
public class ConstraintExpressionFactory {

  private static final ConstraintExpressionFactory SINGLETON = new ConstraintExpressionFactory();

  private ConstraintExpressionFactory() {
    // DO NOTHING
  }

  public static ConstraintExpressionFactory getInstance() {
    return SINGLETON;
  }

  public ConstraintExpression asConstant(Value pValue, Type pType) {
    if (pValue instanceof SymbolicExpression) {
      return ((SymbolicExpression) pValue).getExpression();

    } else {
      return new ConstantConstraintExpression(pValue, pType);
    }
  }

  public ConstraintExpression multiply(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new MultiplicationExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression add(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new AdditionExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression minus(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
      Type pType, Type pCalculationType) {
    return new AdditionExpression(pOperand1, negate(pOperand2, pOperand1.getExpressionType()), pType, pCalculationType);
  }


  public ConstraintExpression negate(ConstraintExpression pFormula, Type pType) {
    checkNotNull(pFormula);
    final ConstraintExpressionFactory factory = ConstraintExpressionFactory.getInstance();
    final Type formulaType = pFormula.getExpressionType();

    return factory.multiply(getMinusOne(formulaType), pFormula, formulaType, formulaType);
  }

  private ConstraintExpression getMinusOne(Type pType) {
    return ConstraintExpressionFactory.getInstance().asConstant(new NumericValue(-1L), pType);
  }

  public ConstraintExpression divide(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new DivisionExpression(pOperand1, pOperand2, pType, pCalculationType);
    
  }

  public ConstraintExpression modulo(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ModuloExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression shiftLeft(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ShiftLeftExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression shiftRight(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ShiftRightExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression binaryAnd(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {
    return new BinaryAndExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression binaryOr(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {
    return new BinaryOrExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression binaryXor(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {
    return new BinaryXorExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression equal(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {
    return new EqualsExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression lessThan(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {
    return new LessThanExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression lessThanOrEqual(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
      Type pType, Type pCalculationType) {
    return new LessThanOrEqualExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression notEqual(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {
    return logicalNot(equal(pOperand1, pOperand2, pType, pCalculationType), pType);
  }

  public ConstraintExpression logicalAnd(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {
    return new LogicalAndExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression logicalOr(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {
    return new LogicalOrExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public ConstraintExpression logicalNot(ConstraintExpression pOperand, Type pType) {
    return new LogicalNotExpression(pOperand, pType);
  }

  public ConstraintExpression binaryNot(ConstraintExpression pOperand, Type pType) {
    return new BinaryNotExpression(pOperand, pType);
  }

  public ConstraintExpression greaterThan(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType, Type pCalculationType) {

    // represent 'a > b' as 'b < a' so we do need less classes
    return new LessThanExpression(pOperand2, pOperand1, pType, pCalculationType);
  }

  public ConstraintExpression greaterThanOrEqual(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
      Type pType, Type pCalculationType) {

    // represent 'a >= b' as 'b <= a' so we do need less classes
    return new LessThanOrEqualExpression(pOperand2, pOperand1, pType, pCalculationType);
  }
}
