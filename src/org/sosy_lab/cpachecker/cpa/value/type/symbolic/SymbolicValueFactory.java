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
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import com.google.common.base.Optional;

/**
 * Factory for creating {@link SymbolicValue} objects containing different symbolic formulas
 * or identifiers.
 */
@Options(prefix = "cpa.value")
public class SymbolicValueFactory {

  private static final SymbolicValueFactory INSTANCE = new SymbolicValueFactory();

  private static final String NO_SYMBOLIC_VALUE_ERROR =
      "Don't create a symbolic formula if you can just compute the expression's value!";

  @Option(name = "maxSymbolicValues",
      description = "The maximum amount of symbolic values to create per ast node.")
  private int maxValuesPerNode = 250;

  private final Map<AAstNode, Integer> valuesPerNodeMap = new HashMap<>();

  private final InvariantsFormulaManager factory = InvariantsFormulaManager.INSTANCE;

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
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator additionCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.add(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, additionCreator);
  }

  private SymbolicValue createFormula(
      Value pLeftOperand, Value pRightOperand, AAstNode pLocation, BinaryFormulaCreator pCreator)
      throws SymbolicBoundReachedException {

    checkSymbolic(pLeftOperand, pRightOperand);

    checkInBound(pLocation);

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = pCreator.createValue(leftFormulaOperand, rightFormulaOperand);
    increaseSymbolicAmount(pLocation);

    return new SymbolicFormula(formula);
  }

  private SymbolicValue createFormula(Value pOperand, AAstNode pLocation, UnaryFormulaCreator pCreator)
      throws SymbolicBoundReachedException {

    checkSymbolic(pOperand);

    checkInBound(pLocation);

    final InvariantsFormula<Value> formulaOperand = getFormulaOperand(pOperand);

    final InvariantsFormula<Value> formula = pCreator.createValue(formulaOperand);
    increaseSymbolicAmount(pLocation);

    return new SymbolicFormula(formula);
  }

  private void checkInBound(AAstNode pLocation) throws SymbolicBoundReachedException {
    Optional<Integer> currentAmount = Optional.fromNullable(valuesPerNodeMap.get(pLocation));

    if (currentAmount.isPresent() && currentAmount.get() >= maxValuesPerNode) {
      throw new SymbolicBoundReachedException(
          "Maximum of " + maxValuesPerNode + " symbolic values reached.", pLocation);
    }
  }

  private InvariantsFormula<Value> getFormulaOperand(Value pValue) {
    if (pValue instanceof SymbolicFormula) {
      return ((SymbolicFormula) pValue).getFormula();
    } else {
      return getConstant(pValue);
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
      Value pLeftValue, Type pLeftType, Value pRightValue, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator subtractionCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        final InvariantsFormula<Value> minusOperand = getNegativeOperand();
        final InvariantsFormula<Value> rightOperandNegation = factory.multiply(pOperand2, minusOperand);

        return factory.add(pOperand1, rightOperandNegation);
      }
    };

    return createFormula(pLeftValue, pRightValue, pLocation, subtractionCreator);
  }

  private InvariantsFormula<Value> getNegativeOperand() {
    return getFormulaOperand(new NumericValue(-1));
  }

  public SymbolicValue createMultiplication(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType, AAstNode pLocation) throws SymbolicBoundReachedException {

    BinaryFormulaCreator multiplicationCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.multiply(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, multiplicationCreator);
  }

  public SymbolicValue createDivision(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType, AAstNode pLocation) throws SymbolicBoundReachedException {

    BinaryFormulaCreator divisionCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.divide(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, divisionCreator);
  }

  public SymbolicValue createModulo(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType, AAstNode pLocation) throws SymbolicBoundReachedException {

    BinaryFormulaCreator moduloCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.modulo(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, moduloCreator);
  }

  public SymbolicValue createShiftLeft(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType, AAstNode pLocation) throws SymbolicBoundReachedException {

    BinaryFormulaCreator shiftCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.shiftLeft(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, shiftCreator);
  }

  public SymbolicValue createShiftRight(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType, AAstNode pLocation) throws SymbolicBoundReachedException {

    BinaryFormulaCreator shiftCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.shiftRight(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, shiftCreator);
  }

  public SymbolicValue createBinaryOr(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType, AAstNode pLocation) throws SymbolicBoundReachedException {
    checkSymbolic(pLeftOperand, pRightOperand);

    BinaryFormulaCreator orCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.binaryOr(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, orCreator);
  }

  public SymbolicValue createBinaryAnd(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType, AAstNode pLocation) throws SymbolicBoundReachedException {

    BinaryFormulaCreator andCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.binaryAnd(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, andCreator);
  }

  public SymbolicValue createBinaryXor(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator xorCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.binaryXor(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, xorCreator);
  }

  public SymbolicValue createBinaryNot(Value pOperand, Type pOperandType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    UnaryFormulaCreator notCreator = new UnaryFormulaCreator() {
      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand) {
        return factory.binaryNot(pOperand);
      }
    };

    return createFormula(pOperand, pLocation, notCreator);
  }

  public SymbolicValue createEquals(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator equalsCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.equal(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, equalsCreator);
  }

  public SymbolicValue createGreaterThan(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator greaterCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.greaterThan(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, greaterCreator);
  }

  public SymbolicValue createGreaterThanOrEqual(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator greaterEqualCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.greaterThanOrEqual(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, greaterEqualCreator);
  }

  public SymbolicValue createLessThan(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator lessCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.lessThan(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, lessCreator);
  }

  public SymbolicValue createLessThanOrEqual(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator lessEqualCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.lessThanOrEqual(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, lessEqualCreator);
  }

  public SymbolicValue createConditionalAnd(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator andCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.logicalAnd(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, andCreator);
  }

  public SymbolicValue createConditionalOr(
      Value pLeftOperand, Type pLeftType, Value pRightOperand, Type pRightType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    BinaryFormulaCreator orCreator = new BinaryFormulaCreator() {

      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2) {
        return factory.logicalOr(pOperand1, pOperand2);
      }
    };

    return createFormula(pLeftOperand, pRightOperand, pLocation, orCreator);
  }

  public SymbolicValue createLogicalNot(Value pOperand, Type pOperandType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    UnaryFormulaCreator notCreator = new UnaryFormulaCreator() {
      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand) {
        return factory.logicalNot(pOperand);
      }
    };

    return createFormula(pOperand, pLocation, notCreator);
  }

  private InvariantsFormula<Value> getConstant(Value pValue) {
    checkNotNull(pValue);
    return InvariantsFormulaManager.INSTANCE.asConstant(pValue);
  }

  private void checkSymbolic(Value pVal1, Value pVal2) {
    assert pVal1 instanceof SymbolicValue || pVal2 instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;
  }

  private void checkSymbolic(Value pVal) {
    assert pVal instanceof SymbolicValue : NO_SYMBOLIC_VALUE_ERROR;
  }

  public Value createNegation(Value pOperand, Type pExpressionType, AAstNode pLocation)
      throws SymbolicBoundReachedException {

    UnaryFormulaCreator xorCreator = new UnaryFormulaCreator() {
      @Override
      public InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand) {
        return factory.multiply(getNegativeOperand(), pOperand);
      }
    };

    return createFormula(pOperand, pLocation, xorCreator);
  }

  private static interface BinaryFormulaCreator {

    InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand1, InvariantsFormula<Value> pOperand2);
  }

  private static interface UnaryFormulaCreator {
    InvariantsFormula<Value> createValue(InvariantsFormula<Value> pOperand);
  }
}
