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
package org.sosy_lab.cpachecker.cpa.value.type.symbolic;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpressionFactory;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import com.google.common.base.Optional;

/**
 * Factory for creating {@link SymbolicValue} objects containing different symbolic expressions
 * or identifiers.
 */
@Options(prefix = "cpa.value")
public class SymbolicValueFactory {

  private static final SymbolicValueFactory INSTANCE = new SymbolicValueFactory();

  private static final String NO_SYMBOLIC_VALUE_ERROR =
      "Don't create a symbolic expression if you can just compute the expression's value!";

  @Option(name = "maxSymbolicValues",
      description = "The maximum amount of symbolic values to create per ast node.")
  private int maxValuesPerNode = 250;

  private final Map<AAstNode, Integer> valuesPerNodeMap = new HashMap<>();

  private final ConstraintExpressionFactory factory = ConstraintExpressionFactory.getInstance();

  private SymbolicValueFactory() {
    // do nothing special
  }

  public static SymbolicValueFactory getInstance() {
    return INSTANCE;
  }

  public SymbolicValue createIdentifier(Type pType, AAstNode pLocation) throws SymbolicBoundReachedException {

    return SymbolicIdentifier.getInstance(pType);
  }

  public SymbolicValue createAddition(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator additionCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {

        return factory.add(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, additionCreator);
  }

  private SymbolicValue createFormula(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation, BinaryExpressionCreator pCreator)
      throws SymbolicBoundReachedException {

    checkEitherSymbolic(pLeftOperand, pRightOperand);

    checkInBound(pLocation);

    final ConstraintExpression leftExpOperand = getConstantExpression(pLeftOperand, pLeftType);
    final ConstraintExpression rightExpOperand = getConstantExpression(pRightOperand, pRightType);

    final ConstraintExpression exp = pCreator.createValue(leftExpOperand, rightExpOperand, pExpressionType);
    increaseSymbolicAmount(pLocation);

    return new SymbolicExpression(exp);
  }

  private SymbolicValue createFormula(Value pOperand, Type pOperandType, Type pExpressionType, AAstNode pLocation,
      UnaryExpressionCreator pCreator)
      throws SymbolicBoundReachedException {

    checkSymbolic(pOperand);

    checkInBound(pLocation);

    final ConstraintExpression expOperand = getConstantExpression(pOperand, pOperandType);

    final ConstraintExpression exp = pCreator.createValue(expOperand, pExpressionType);
    increaseSymbolicAmount(pLocation);

    return new SymbolicExpression(exp);
  }

  private void checkInBound(AAstNode pLocation) throws SymbolicBoundReachedException {
    Optional<Integer> currentAmount = Optional.fromNullable(valuesPerNodeMap.get(pLocation));

    if (currentAmount.isPresent() && currentAmount.get() >= maxValuesPerNode) {
      throw new SymbolicBoundReachedException(
          "Maximum of " + maxValuesPerNode + " symbolic values reached.", pLocation);
    }
  }

  private ConstraintExpression getConstantExpression(Value pValue, Type pType) {
    if (pValue instanceof SymbolicExpression) {
      return ((SymbolicExpression) pValue).getExpression();
    } else {
      return getConstant(pValue, pType);
    }
  }

  private void increaseSymbolicAmount(AAstNode pLocation) {
    Optional<Integer> currentAmount = Optional.fromNullable(valuesPerNodeMap.get(pLocation));
    int newAmount;

    if (currentAmount.isPresent()) {
      newAmount = currentAmount.get() + 1;
    } else {
      newAmount = 1;
    }

    valuesPerNodeMap.put(pLocation, newAmount);
  }

  public SymbolicValue createSubtraction(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator subtractionCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {

        return factory.minus(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation,
        subtractionCreator);
  }

  public SymbolicValue createMultiplication(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator multiplicationCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.multiply(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation,
        multiplicationCreator);
  }

  public SymbolicValue createDivision(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator divisionCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.divide(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation,
        divisionCreator);
  }

  public SymbolicValue createModulo(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, final Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator moduloCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.modulo(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, moduloCreator);
  }

  public SymbolicValue createShiftLeft(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator shiftCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.shiftLeft(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, shiftCreator);
  }

  public SymbolicValue createShiftRight(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator shiftCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.shiftRight(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, shiftCreator);
  }

  public SymbolicValue createBinaryOr(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {
    checkEitherSymbolic(pLeftOperand, pRightOperand);

    BinaryExpressionCreator orCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.binaryOr(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, orCreator);
  }

  public SymbolicValue createBinaryAnd(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator andCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.binaryAnd(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, andCreator);
  }

  public SymbolicValue createBinaryXor(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator xorCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.binaryXor(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, xorCreator);
  }

  public SymbolicValue createBinaryNot(Value pOperand, Type pOperandType, Type pExpressionType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    UnaryExpressionCreator notCreator = new UnaryExpressionCreator() {
      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand, Type pType) {
        return factory.binaryNot(pOperand, pType);
      }
    };

    return createFormula(pOperand, pOperandType, pExpressionType, pLocation, notCreator);
  }

  public SymbolicValue createEquals(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator equalsCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.equal(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, equalsCreator);
  }

  public SymbolicValue createNotEquals(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator notEqualsCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.notEqual(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation,
        notEqualsCreator);
  }

  public SymbolicValue createGreaterThan(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator greaterCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.greaterThan(pOperand2, pOperand1, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, greaterCreator);
  }

  public SymbolicValue createGreaterThanOrEqual(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator greaterEqualCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.greaterThanOrEqual(pOperand2, pOperand1, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation,
        greaterEqualCreator);
  }

  public SymbolicValue createLessThan(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator lessCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.lessThan(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, lessCreator);
  }

  public SymbolicValue createLessThanOrEqual(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator lessEqualCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.lessThanOrEqual(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation,
        lessEqualCreator);
  }

  public SymbolicValue createConditionalAnd(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator andCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.logicalAnd(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, andCreator);
  }

  public SymbolicValue createConditionalOr(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, Type pExpressionType,
      AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryExpressionCreator orCreator = new BinaryExpressionCreator() {

      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
          Type pType) {
        return factory.logicalOr(pOperand1, pOperand2, pType);
      }
    };

    return createFormula(pLeftOperand, pLeftType, pRightOperand, pRightType, pExpressionType, pLocation, orCreator);
  }

  public SymbolicValue createLogicalNot(Value pOperand, Type pOperandType, Type pExpressionType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    UnaryExpressionCreator notCreator = new UnaryExpressionCreator() {
      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand, Type pType) {
        return factory.logicalNot(pOperand, pType);
      }
    };

    return createFormula(pOperand, pOperandType, pExpressionType, pLocation, notCreator);
  }

  private ConstraintExpression getConstant(Value pValue, Type pType) {
    checkNotNull(pValue);
    return ConstraintExpressionFactory.getInstance().asConstant(pValue, pType);
  }

  private void checkEitherSymbolic(Value pVal1, Value pVal2) {
    assert pVal1 instanceof SymbolicValue || pVal2 instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;
  }

  private void checkSymbolic(Value pVal) {
    assert pVal instanceof SymbolicValue : NO_SYMBOLIC_VALUE_ERROR;
  }

  public Value createNegation(Value pOperand, Type pOperandType, Type pExpressionType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    UnaryExpressionCreator xorCreator = new UnaryExpressionCreator() {
      @Override
      public ConstraintExpression createValue(ConstraintExpression pOperand, Type pType) {
        return factory.negate(pOperand, pType);
      }
    };

    return createFormula(pOperand, pOperandType, pExpressionType, pLocation, xorCreator);
  }

  private static interface BinaryExpressionCreator {
    ConstraintExpression createValue(ConstraintExpression pOperand1, ConstraintExpression pOperand2, Type pType);
  }

  private static interface UnaryExpressionCreator {
    ConstraintExpression createValue(ConstraintExpression pOperand, Type pType);
  }
}
