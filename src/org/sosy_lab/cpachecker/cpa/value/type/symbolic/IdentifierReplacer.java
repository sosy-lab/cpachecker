/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.SymbolicExpressionFactory;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.UnarySymbolicExpression;

/**
 * This visitor replaces all occurrences of a given {@link SymbolicIdentifier} in a
 * {@link SymbolicValue} with the given value.
 *
 * When visiting a SymbolicExpression, this class has to always return a SymbolicExpression,
 * no other {@link org.sosy_lab.cpachecker.cpa.value.type.Value} type is allowed.
 */
public class IdentifierReplacer implements SymbolicValueVisitor<Value> {

  private final SymbolicExpressionFactory factory = SymbolicExpressionFactory.getInstance();
  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;

  private long idToReplace;
  private Value newValue;

  public IdentifierReplacer(SymbolicIdentifier pIdentifierToReplace, Value pNewValue, MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {
    logger = pLogger;
    machineModel = pMachineModel;

    idToReplace = pIdentifierToReplace.getId();
    newValue = pNewValue;
  }

  @Override
  public Value visit(SymbolicIdentifier pSymbolicValue) {
    long id = pSymbolicValue.getId();

    return id == idToReplace ? newValue : pSymbolicValue;
  }

  private Value cast(Value pValue, Type toType) {

    if (toType instanceof JType) {
      JType fromType = getJFromType((JType) toType);
      return AbstractExpressionValueVisitor.castJValue(pValue, fromType, (JType) toType, logger, null);

    } else {
      assert toType instanceof CType;
      CType fromType = getCFromType((CType) toType);

      return AbstractExpressionValueVisitor.castCValue(pValue, fromType, (CType) toType, machineModel, logger, null);
    }
  }

  private JType getJFromType(JType pType) {
    if (!(pType instanceof JSimpleType)) {
      return pType;

    } else {
      final JBasicType basicType = ((JSimpleType)pType).getType();
      if (basicType.isFloatingPointType()) {
        return JSimpleType.getDouble();

      } else if (basicType.isIntegerType()) {
        return JSimpleType.getLong();

      } else {
        return pType;
      }
    }
  }

  private CType getCFromType(CType pType) {
    if (!(pType instanceof CSimpleType)) {
      return pType;
    } else {
      final CSimpleType simpleType = (CSimpleType)pType;
      final boolean isSigned = simpleType.isSigned();
      final CBasicType basicType = simpleType.getType();

      if (basicType.isFloatingPointType()) {
        return CNumericTypes.LONG_DOUBLE;
      } else if (basicType.isIntegerType()) {
        if (isSigned) {
          return CNumericTypes.LONG_LONG_INT;
        } else {
          return CNumericTypes.UNSIGNED_LONG_LONG_INT;
        }

      } else {
        return pType;
      }
    }
  }

  @Override
  public SymbolicExpression visit(ConstantSymbolicExpression pExpression) {
    Value newValue = pExpression.getValue();

    if (newValue instanceof SymbolicIdentifier) {
      newValue = ((SymbolicIdentifier) newValue).accept(this);

      if (newValue instanceof NumericValue) {
        newValue = cast(newValue, pExpression.getType());
      }
    }

    return factory.asConstant(newValue, pExpression.getType());
  }

  private SymbolicExpression replaceInBinaryExpression(BinarySymbolicExpression pExpression,
      BinaryFactoryFunction pFactoryFunction) {
    // This visitor always returns a SymbolicExpression when visiting a SymbolicExpression, so the cast
    // is fine
    SymbolicExpression leftOperand = (SymbolicExpression)pExpression.getOperand1().accept(this);
    SymbolicExpression rightOperand = (SymbolicExpression)pExpression.getOperand2().accept(this);

    Type expressionType = pExpression.getType();
    Type calculationType = pExpression.getCalculationType();

    return pFactoryFunction.create(leftOperand, rightOperand, expressionType, calculationType);
  }

  private SymbolicExpression replaceInUnaryExpression(UnarySymbolicExpression pExpression,
      UnaryFactoryFunction pFactoryFunction) {
    // This visitor always returns a SymbolicExpression when visiting a SymbolicExpression, so the cast
    // is fine
    SymbolicExpression operand = (SymbolicExpression)pExpression.getOperand().accept(this);

    return pFactoryFunction.create(operand, pExpression.getType());
  }

  @Override
  public SymbolicExpression visit(AdditionExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.add(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(MultiplicationExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.multiply(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(DivisionExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.divide(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(ModuloExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.modulo(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(BinaryAndExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.binaryAnd(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(BinaryNotExpression pExpression) {
    return replaceInUnaryExpression(pExpression, new UnaryFactoryFunction() {

      @Override
      public SymbolicExpression create(SymbolicExpression pOperand, Type pExpType) {
        return factory.binaryNot(pOperand, pExpType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(BinaryOrExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.binaryOr(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(BinaryXorExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.binaryXor(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(ShiftRightExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.shiftRight(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(ShiftLeftExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.shiftLeft(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(LogicalNotExpression pExpression) {
    return replaceInUnaryExpression(pExpression, new UnaryFactoryFunction() {

      @Override
      public SymbolicExpression create(SymbolicExpression pOperand, Type pExpType) {
        return factory.logicalNot(pOperand, pExpType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(LessThanOrEqualExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.lessThanOrEqual(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(LessThanExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.lessThan(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(EqualsExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.equal(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(LogicalOrExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.logicalOr(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public SymbolicExpression visit(LogicalAndExpression pExpression) {
    return replaceInBinaryExpression(pExpression, new BinaryFactoryFunction() {

      @Override
      public SymbolicExpression create(
          SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType) {
        return factory.logicalAnd(pLeftOp, pRightOp, pExpType, pCalcType);
      }
    });
  }

  @Override
  public Value visit(CastExpression pExpression) {
    Value newValue = pExpression.getOperand().accept(this);

    if (newValue instanceof SymbolicExpression) {
      return new CastExpression((SymbolicExpression) newValue, pExpression.getType());

    } else {
      assert !(newValue instanceof SymbolicValue);
      Type toType = pExpression.getType();
      Type fromType = pExpression.getOperand().getType();

      if (toType instanceof CType) {
        assert fromType instanceof CType;
        return AbstractExpressionValueVisitor.castCValue(
            newValue, (CType) fromType, (CType) toType, machineModel, logger, null);

      } else if (toType instanceof JType) {
        assert fromType instanceof JType;
        return AbstractExpressionValueVisitor.castJValue(newValue, (JType) fromType, (JType) toType, logger, null);

      } else {
        throw new AssertionError("Unhandled type " + toType);
      }
    }
  }

  private interface BinaryFactoryFunction {
    SymbolicExpression create(SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType);
  }

  private interface UnaryFactoryFunction {
    SymbolicExpression create(SymbolicExpression pOperand, Type pExpType);
  }
}
