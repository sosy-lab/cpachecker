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
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicBoundReachedException;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.Types;

import com.google.common.base.Optional;

/**
 * Factory for creating {@link SymbolicValue}s
 */
@Options(prefix = "cpa.value")
public class SymbolicValueFactory {

  private static final SymbolicValueFactory INSTANCE = new SymbolicValueFactory();

  private static final String NO_SYMBOLIC_VALUE_ERROR =
      "Don't create a symbolic expression if you can just compute the expression's value!";

  @Option(name = "maxSymbolicValues",
      description = "The maximum amount of symbolic values to create per ast node.")
  private int maxValuesPerNode = 100000;

  private final Map<AAstNode, Integer> valuesPerNodeMap = new HashMap<>();

  private static final SymbolicValueFactory SINGLETON = new SymbolicValueFactory();

  private SymbolicValueFactory() {
    // DO NOTHING
  }

  public static SymbolicValueFactory getInstance() {
    return SINGLETON;
  }

  public SymbolicIdentifier newIdentifier(AAstNode pLocation) throws SymbolicBoundReachedException {

    return SymbolicIdentifier.getNewIdentifier();
  }

  private void checkInBound(AAstNode pLocation) throws SymbolicBoundReachedException {
    Optional<Integer> currentAmount = Optional.fromNullable(valuesPerNodeMap.get(pLocation));

    if (currentAmount.isPresent() && currentAmount.get() >= maxValuesPerNode) {
      throw new SymbolicBoundReachedException(
          "Maximum of " + maxValuesPerNode + " symbolic values reached.", pLocation);
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

  public SymbolicExpression asConstant(Value pValue, Type pType) {
    checkNotNull(pValue);
    if (pValue instanceof SymbolicExpression) {
      return ((SymbolicExpression) pValue);

    } else {
      return new ConstantSymbolicExpression(pValue, pType);
    }
  }

  public SymbolicExpression multiply(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new MultiplicationExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression add(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new AdditionExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression minus(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pType, Type pCalculationType) {
    return new AdditionExpression(pOperand1, negate(pOperand2, pOperand1.getType()), pType, pCalculationType);
  }


  public SymbolicExpression negate(SymbolicExpression pFormula, Type pType) {
    checkNotNull(pFormula);
    final Type formulaType = pFormula.getType();

    return multiply(getMinusOne(formulaType), pFormula, pType, pType);
  }

  private SymbolicExpression getMinusOne(Type pType) {
    return asConstant(new NumericValue(-1L), pType);
  }

  public SymbolicExpression divide(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new DivisionExpression(pOperand1, pOperand2, pType, pCalculationType);

  }

  public SymbolicExpression modulo(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ModuloExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression shiftLeft(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ShiftLeftExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression shiftRightSigned(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ShiftRightExpression(pOperand1, pOperand2, pType, pCalculationType,
        ShiftRightExpression.ShiftType.SIGNED);
  }

  public SymbolicExpression shiftRightUnsigned(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ShiftRightExpression(pOperand1, pOperand2, pType, pCalculationType,
        ShiftRightExpression.ShiftType.UNSIGNED);
  }

  public SymbolicExpression binaryAnd(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new BinaryAndExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression binaryOr(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new BinaryOrExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression binaryXor(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new BinaryXorExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public EqualsExpression equal(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new EqualsExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression lessThan(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new LessThanExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression lessThanOrEqual(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pType, Type pCalculationType) {
    return new LessThanOrEqualExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression notEqual(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return logicalNot(equal(pOperand1, pOperand2, pType, pCalculationType), pType);
  }

  public SymbolicExpression logicalAnd(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new LogicalAndExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression logicalOr(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new LogicalOrExpression(pOperand1, pOperand2, pType, pCalculationType);
  }

  public SymbolicExpression logicalNot(SymbolicExpression pOperand, Type pType) {
    return new LogicalNotExpression(pOperand, pType);
  }

  public SymbolicExpression binaryNot(SymbolicExpression pOperand, Type pType) {
    return new BinaryNotExpression(pOperand, pType);
  }

  public SymbolicExpression greaterThan(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {

    // represent 'a > b' as 'b < a' so we do need less classes
    return new LessThanExpression(pOperand2, pOperand1, pType, pCalculationType);
  }

  public SymbolicExpression greaterThanOrEqual(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pType, Type pCalculationType) {

    // represent 'a >= b' as 'b <= a' so we do need less classes
    return new LessThanOrEqualExpression(pOperand2, pOperand1, pType, pCalculationType);
  }

  /**
   * Creates a {@link SymbolicExpression} representing the cast of the given value to the given type.
   * If multiple casts occur sequentially, it is tried to simplify them.
   * A {@link MachineModel} might be necessary for this if the cast types are instances of {@link CType}.
   *
   * @param pValue the value to cast
   * @param pTargetType the type to cast to
   * @param pMachineModel the machine model, optionally
   * @return a <code>SymbolicExpression</code> representing the cast of the given value to the given type
   */
  public SymbolicExpression cast(SymbolicValue pValue, Type pTargetType, Optional<MachineModel> pMachineModel) {
    SymbolicExpression operand;

    if (!(pValue instanceof SymbolicExpression)) {
      return asConstant(pValue, pTargetType);
    } else {
      operand = (SymbolicExpression) pValue;
    }

    if (operand.getType().equals(pTargetType)) {
      return operand;

    } else {
      boolean isCast = operand instanceof CastExpression;

      if (isCast) {
        operand = simplifyCasts((CastExpression) operand, pMachineModel);
      }

      return operand;
    }
  }

  /**
   * Removes unnecessary sequential casts.
   *
   * <p>If a cast that does not change the value of the operand is proceeded by another cast, this
   * first cast is removed.</p>
   *
   * <p>Example:
   *  <pre>
   *    char b = nondet();
   *    b = (int) (long) b;
   *  </pre>
   *  In the above example, an expression representing <code>(int) b</code> will be returned.
   * </p>
   *
   * @param pExpression the {@link CastExpression} to simplify
   * @param pMachineModel the machine model
   * @return a simplified version of the given expression.
   */
  private SymbolicExpression simplifyCasts(CastExpression pExpression, Optional<MachineModel> pMachineModel) {
    Type typeOfBasicExpression = getTypeOfBasicExpression(pExpression);
    SymbolicExpression operand = pExpression.getOperand();

    if (operand instanceof CastExpression) {
      Type typeOfOuterCast = pExpression.getType();
      Type typeOfInnerCast = operand.getType();
      SymbolicExpression nextOperand = ((CastExpression) operand).getOperand();

      if (typeOfOuterCast instanceof CType && pMachineModel.isPresent()) {
        assert typeOfInnerCast instanceof CType && typeOfBasicExpression instanceof CType;
        if (Types.canHoldAllValues(typeOfInnerCast, typeOfBasicExpression, pMachineModel.get())) {
          return cast(nextOperand, typeOfOuterCast, pMachineModel);
        }

      } else if (typeOfOuterCast instanceof JType) {
        assert typeOfInnerCast instanceof JType && typeOfBasicExpression instanceof JType;
        if (Types.canHoldAllValues((JType) typeOfInnerCast, (JType) typeOfBasicExpression)) {
          return cast(nextOperand, typeOfOuterCast, pMachineModel);
        }
      }
    }

    return pExpression;
  }

  private Type getTypeOfBasicExpression(SymbolicExpression pExpression) {
    if (pExpression instanceof CastExpression) {
      return getTypeOfBasicExpression(((CastExpression) pExpression).getOperand());

    } else {
      return pExpression.getType();
    }
  }

  public PointerExpression pointer(SymbolicExpression pOperand, Type pType) {
    checkNotNull(pOperand);
    return new PointerExpression(pOperand, pType);
  }
}
