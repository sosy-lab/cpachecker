// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Factory for creating {@link SymbolicValue}s. All {@link SymbolicExpression}s created with this
 * factory use canonical C types, as provided by {@link CType#getCanonicalType()}.
 */
public class SymbolicValueFactory {

  private static final SymbolicValueFactory SINGLETON = new SymbolicValueFactory();
  private int idCounter = 0;

  private SymbolicValueFactory() {
    // DO NOTHING
  }

  public static SymbolicValueFactory getInstance() {
    return SINGLETON;
  }

  public static void reset() {
    SINGLETON.idCounter = 0;
  }

  public SymbolicIdentifier newIdentifier(MemoryLocation pMemoryLocation) {
    return new SymbolicIdentifier(idCounter++, pMemoryLocation);
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

  public SymbolicExpression multiply(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new MultiplicationExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression add(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new AdditionExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression minus(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {

    Type canonicalCalcType = getCanonicalType(pCalculationType);

    return new SubtractionExpression(
        pOperand1, pOperand2, getCanonicalType(pType), canonicalCalcType);
  }

  public SymbolicExpression negate(SymbolicExpression pFormula, Type pType) {
    checkNotNull(pFormula);
    if (pFormula instanceof NegationExpression) {
      return ((NegationExpression) pFormula).getOperand();

    } else {
      return new NegationExpression(pFormula, pType);
    }
  }

  public SymbolicExpression divide(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new DivisionExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression modulo(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new ModuloExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression shiftLeft(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new ShiftLeftExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression shiftRightSigned(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new ShiftRightExpression(
        pOperand1,
        pOperand2,
        getCanonicalType(pType),
        getCanonicalType(pCalculationType),
        ShiftRightExpression.ShiftType.SIGNED);
  }

  public SymbolicExpression shiftRightUnsigned(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new ShiftRightExpression(
        pOperand1,
        pOperand2,
        getCanonicalType(pType),
        getCanonicalType(pCalculationType),
        ShiftRightExpression.ShiftType.UNSIGNED);
  }

  public SymbolicExpression binaryAnd(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new BinaryAndExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression binaryOr(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new BinaryOrExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression binaryXor(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new BinaryXorExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public EqualsExpression equal(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new EqualsExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression lessThan(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new LessThanExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression lessThanOrEqual(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new LessThanOrEqualExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression notEqual(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return logicalNot(
        equal(pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType)),
        pType);
  }

  public SymbolicExpression logicalAnd(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new LogicalAndExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression logicalOr(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return new LogicalOrExpression(
        pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType));
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

  public SymbolicExpression greaterThan(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {

    // represent 'a > b' as 'b < a' so we do need less classes
    return new LessThanExpression(
        pOperand2, pOperand1, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  public SymbolicExpression greaterThanOrEqual(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {

    // represent 'a >= b' as 'b <= a' so we do need less classes
    return new LessThanOrEqualExpression(
        pOperand2, pOperand1, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }

  /**
   * Creates a {@link SymbolicExpression} representing the cast of the given value to the given
   * type. If multiple casts occur sequentially, it is tried to simplify them.
   *
   * @param pValue the value to cast
   * @param pTargetType the type to cast to
   * @return a <code>SymbolicExpression</code> representing the cast of the given value to the given
   *     type
   */
  public SymbolicExpression cast(SymbolicValue pValue, Type pTargetType) {
    Type canonicalTargetType = getCanonicalType(pTargetType);

    SymbolicExpression operand;

    if (pValue instanceof AddressExpression) {
      // TODO:
      // We want to cast AddressExpressions only if the cast type is smaller than signed int
      // (default for pointers) because only then the cast would make a difference.
      // In all smaller cases we need to make sure that there are 2 possibilities later on.
      // One where due to the cast the values still match and one where they don't.
      return (SymbolicExpression) pValue;
    }

    if (!(pValue instanceof SymbolicExpression)) {
      return asConstant(pValue, canonicalTargetType);
    } else {
      operand = (SymbolicExpression) pValue;
    }

    if (operand.getType().equals(canonicalTargetType)) {
      return operand;

    } else {
      return new CastExpression(operand, canonicalTargetType);
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
