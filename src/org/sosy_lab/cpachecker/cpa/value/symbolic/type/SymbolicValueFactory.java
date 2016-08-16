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

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.Types;

import java.util.Optional;

/**
 * Factory for creating {@link SymbolicValue}s.
 * All {@link SymbolicExpression}s created with this factory use canonical C types, as provided by
 * {@link CType#getCanonicalType()}.
 */
public class SymbolicValueFactory {

  private static final SymbolicValueFactory SINGLETON = new SymbolicValueFactory();

  private SymbolicValueFactory() {
    // DO NOTHING
  }

  public static SymbolicValueFactory getInstance() {
    return SINGLETON;
  }

  public SymbolicIdentifier newIdentifier() {
    return SymbolicIdentifier.getNewIdentifier();
  }

  public SymbolicExpression asConstant(Value pValue, Type pType) {
    checkNotNull(pValue);
    assert !pValue.isUnknown();
    if (pValue instanceof SymbolicExpression) {
      return ((SymbolicExpression) pValue);

    } else {
      return new ConstantSymbolicExpression(pValue, getCanonicalType(pType));
    }
  }

  public SymbolicExpression multiply(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new MultiplicationExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression add(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new AdditionExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression minus(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pType, Type pCalculationType) {

    Type canonicalCalcType = getCanonicalType(pCalculationType);

    return new SubtractionExpression(pOperand1, pOperand2, getCanonicalType(pType), canonicalCalcType);
  }

  public SymbolicExpression negate(SymbolicExpression pFormula, Type pType) {
    checkNotNull(pFormula);
    if (pFormula instanceof NegationExpression) {
      return ((NegationExpression) pFormula).getOperand();

    } else {
      return new NegationExpression(pFormula, pType);
    }
  }

  public SymbolicExpression divide(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new DivisionExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));

  }

  public SymbolicExpression modulo(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ModuloExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression shiftLeft(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ShiftLeftExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression shiftRightSigned(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ShiftRightExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType),
        ShiftRightExpression.ShiftType.SIGNED);
  }

  public SymbolicExpression shiftRightUnsigned(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new ShiftRightExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType),
        ShiftRightExpression.ShiftType.UNSIGNED);
  }

  public SymbolicExpression binaryAnd(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new BinaryAndExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression binaryOr(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new BinaryOrExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression binaryXor(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new BinaryXorExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public EqualsExpression equal(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new EqualsExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression lessThan(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new LessThanExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression lessThanOrEqual(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pType, Type pCalculationType) {
    return new LessThanOrEqualExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression notEqual(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return logicalNot(equal(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType)), pType);
  }

  public SymbolicExpression logicalAnd(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new LogicalAndExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression logicalOr(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {
    return new LogicalOrExpression(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression logicalNot(SymbolicExpression pOperand, Type pType) {

    if (pOperand instanceof LogicalNotExpression) {
      return ((LogicalNotExpression) pOperand).getOperand();

    } else {
      return new LogicalNotExpression(pOperand, getCanonicalType(pType));
    }
  }

  public SymbolicExpression binaryNot(SymbolicExpression pOperand, Type pType) {

    if (pOperand instanceof BinaryNotExpression) {
      return ((BinaryNotExpression) pOperand).getOperand();

    } else {
      return new BinaryNotExpression(pOperand, getCanonicalType(pType));
    }
  }

  public SymbolicExpression greaterThan(SymbolicExpression pOperand1, SymbolicExpression pOperand2, Type pType,
      Type pCalculationType) {

    // represent 'a > b' as 'b < a' so we do need less classes
    return new LessThanExpression(pOperand2, pOperand1, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression greaterThanOrEqual(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pType, Type pCalculationType) {

    // represent 'a >= b' as 'b <= a' so we do need less classes
    return new LessThanOrEqualExpression(pOperand2, pOperand1, getCanonicalType(pType), getCanonicalType(pCalculationType));
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
    Type canonicalTargetType = getCanonicalType(pTargetType);

    SymbolicExpression operand;

    if (!(pValue instanceof SymbolicExpression)) {
      return asConstant(pValue, canonicalTargetType);
    } else {
      operand = (SymbolicExpression) pValue;
    }

    if (operand.getType().equals(canonicalTargetType)) {
      return operand;

    } else {
      boolean isCast = operand instanceof CastExpression;

      operand = new CastExpression(operand, canonicalTargetType);

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
    return new PointerExpression(pOperand, getCanonicalType(pType));
  }

  public SymbolicExpression addressOf(SymbolicExpression pOperand, Type pType) {
    checkNotNull(pOperand);

    // &*a = a
    if (pOperand instanceof PointerExpression) {
      return ((PointerExpression) pOperand).getOperand();

    } else {
      return new AddressOfExpression(pOperand, getCanonicalType(pType));
    }
  }

  private Type getCanonicalType(Type pType) {
    if (pType instanceof CType) {
      return ((CType) pType).getCanonicalType();
    } else {
      return pType;
    }
  }
}
