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

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.value.type.*;

/**
 * Factory for creating {@link SymbolicValue} objects containing different symbolic formulas
 * or identifiers.
 */
public class SymbolicValueFactory {

  private static final SymbolicValueFactory INSTANCE = new SymbolicValueFactory();

  private static final String NO_SYMBOLIC_VALUE_ERROR =
      "Don't create a symbolic formula if you can just compute the expression's value!";


  private SymbolicValueFactory() {
    // do nothing special
  }

  public static SymbolicValueFactory getInstance() {
    return INSTANCE;
  }

  public SymbolicValue createIdentifier(Type pType) {
    return SymbolicIdentifier.getInstance(pType);
  }

  public SymbolicValue createAddition(Value pLeftOperand,
      Type pLeftType, Value pRightOperand, Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.add(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  private InvariantsFormula<Value> getFormulaOperand(Value pValue) {
    if (pValue instanceof SymbolicFormula) {
      return ((SymbolicFormula) pValue).getFormula();
    } else {
      return getConstant(pValue);
    }
  }

  public SymbolicValue createMultiplication(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.multiply(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createDivision(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.divide(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createModulo(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.modulo(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createShiftLeft(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.shiftLeft(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createShiftRight(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.shiftRight(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createBinaryOr(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.binaryOr(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createBinaryAnd(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.binaryAnd(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createBinaryXor(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.binaryXor(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createBinaryNot(Value pOperand, Type pOperandType) {
    assert pOperand instanceof SymbolicValue : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.binaryNot(
        getFormulaOperand(pOperand));

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createEquals(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.equal(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createGreaterThan(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.greaterThan(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createGreaterThanOrEqual(Value pLeftOperand, Type pLeftType,
      Value pRightOperand, Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.greaterThanOrEqual(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createLessThan(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.lessThan(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createLessThanOrEqual(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.lessThanOrEqual(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createConditionalAnd(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.logicalAnd(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createConditionalOr(Value pLeftOperand, Type pLeftType, Value pRightOperand,
      Type pRightType) {
    assert pLeftOperand instanceof SymbolicValue || pRightOperand instanceof SymbolicValue
        : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> leftFormulaOperand = getFormulaOperand(pLeftOperand);
    final InvariantsFormula<Value> rightFormulaOperand = getFormulaOperand(pRightOperand);

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.logicalOr(
        leftFormulaOperand, rightFormulaOperand);

    return new SymbolicFormula(formula);
  }

  public SymbolicValue createLogicalNot(Value pOperand, Type pOperandType) {
    assert pOperand instanceof SymbolicValue : NO_SYMBOLIC_VALUE_ERROR;

    final InvariantsFormula<Value> formula = InvariantsFormulaManager.INSTANCE.logicalNot(
        getFormulaOperand(pOperand));

    return new SymbolicFormula(formula);
  }

  private InvariantsFormula<Value> getConstant(Value pValue) {
    checkNotNull(pValue);
    return InvariantsFormulaManager.INSTANCE.asConstant(pValue);
  }
}
