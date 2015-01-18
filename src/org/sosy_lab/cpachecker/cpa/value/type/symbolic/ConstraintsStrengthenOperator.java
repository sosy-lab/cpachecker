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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryXorExpression;
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
import org.sosy_lab.cpachecker.exceptions.SolverException;

import com.google.common.base.Optional;

/**
 * Strengthener for ValueAnalysis with ConstraintsCPA.
 */
public class ConstraintsStrengthenOperator {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  
  private ConstraintsStrengthenOperator(
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  public static ConstraintsStrengthenOperator getInstance(
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {

    return new ConstraintsStrengthenOperator(pMachineModel, pLogger);
  }

  /**
   * Strengthen the given {@link ValueAnalysisState} with the given {@link ConstraintsState}.
   *
   * <p>The returned <code>Collection</code> contains all reachable states after strengthening.
   * A returned empty <code>Collection</code> represents 'bottom', a returned <code>null</code>
   * represents that no changes were made to the given <code>ValueAnalysisState</code>.</p>
   *
   *
   * @param pStateToStrengthen the state to strengthen
   * @param pStrengtheningState the state to strengthen the first state with
   * @return <code>null</code> if no changes were made to the given <code>ValueAnalysisState</code>,
   *    an empty <code>Collection</code>, if the resulting state is not reachable and
   *    a <code>Collection</code> containing all reachable states, otherwise
   */
  public Collection<ValueAnalysisState> strengthen(
      ValueAnalysisState pStateToStrengthen, ConstraintsState pStrengtheningState) {

    Optional<ValueAnalysisState> newElement = Optional.absent();

    try {
      if (pStrengtheningState.hasNewSatisfyingAssignment()) {
        newElement = evaluateAssignment(pStrengtheningState.getDefiniteAssignment(), pStateToStrengthen);
      }

    } catch (SolverException | InterruptedException e) {
      return null;
    }

    if (newElement == null) {
      return null;
    } else if (newElement.isPresent()) {
      return Collections.singleton(newElement.get());
    } else {
      return Collections.emptySet();
    }
  }

  private Optional<ValueAnalysisState> evaluateAssignment(
      IdentifierAssignment pAssignment, ValueAnalysisState pValueState) {

    ValueAnalysisState newElement = ValueAnalysisState.copyOf(pValueState);
    boolean somethingChanged = false;

    for (Map.Entry<? extends SymbolicIdentifier, Value> onlyValidAssignment : pAssignment.entrySet()) {
      final SymbolicIdentifier identifierToReplace = onlyValidAssignment.getKey();
      final Value newIdentifierValue = onlyValidAssignment.getValue();

      for (Map.Entry<ValueAnalysisState.MemoryLocation, Value> valueEntry : pValueState.getConstantsMapView().entrySet()) {
        final Value currentValue = valueEntry.getValue();
        final ValueAnalysisState.MemoryLocation memLoc = valueEntry.getKey();
        final Type storageType = pValueState.getTypeForMemoryLocation(memLoc);

        if (currentValue instanceof SymbolicValue) {
          IdentifierReplacer replacer = new IdentifierReplacer(identifierToReplace, newIdentifierValue);

          Value valueAfterReplacingIdentifier = ((SymbolicValue) currentValue).accept(replacer);

          // if the current variable can never obtain the only valid value, the path is infeasible
          /*if (isValueOutOfTypeBounds(newIdentifierValue, storageType)) {
            return Optional.absent();
          }*/ // We can only do this if the memorylocation is exactly the variable/field represented
              // by the ConstraintsCPA's constraint

          newElement.assignConstant(memLoc, valueAfterReplacingIdentifier, storageType);
          somethingChanged = true;
        }
      }
    }

    if (somethingChanged) {
      return Optional.of(newElement);
    } else {
      return null;
    }
  }

  private boolean isValueOutOfTypeBounds(Value pValue, Type pType) {
    if (pType instanceof JSimpleType) {
      return isValueOutOfJTypeBounds(pValue, (JSimpleType) pType);

    } else {
      assert pType instanceof CSimpleType;

      return isValueOutOfCTypeBounds(pValue, (CSimpleType) pType);
    }
  }

  private boolean isValueOutOfJTypeBounds(Value pValue, JSimpleType pType) {
    final JBasicType basicType = pType.getType();

    if (basicType == JBasicType.BOOLEAN) {
      assert pValue instanceof BooleanValue;
      return false;

    } else if (basicType.isFloatingPointType()) {
      if (basicType == JBasicType.DOUBLE) {
        return true;
      } else {
        final double concreteValue = ((NumericValue) pValue).doubleValue();
        float castValue = (float) concreteValue;

        return concreteValue == castValue;
      }
    } else {
      assert basicType.isIntegerType();
      final long concreteValue = pValue.asNumericValue().longValue();

      switch (basicType) {
        case BYTE:
          return concreteValue == ((byte) concreteValue);
        case SHORT:
          return concreteValue == ((short) concreteValue);
        case CHAR:
          return concreteValue == ((char) concreteValue);
        case INT:
          return concreteValue == ((int) concreteValue);
        case LONG:
          return true;

        default:
          throw new AssertionError("Unexpected type " + pType.getType() + " for integer value");
      }
    }
  }

  private boolean isValueOutOfCTypeBounds(Value pValue, CSimpleType pType) {
    final CBasicType basicType = pType.getType();

    if (basicType.isFloatingPointType()) {
      return false; // we can't handle this easily, as C float values can't be easily represented as bits
    } else {
      assert basicType.isIntegerType();

      final BigInteger concreteValue = BigInteger.valueOf(pValue.asNumericValue().longValue());
      final BigInteger maxValue = machineModel.getMaximalIntegerValue(pType);
      final BigInteger minValue = machineModel.getMinimalIntegerValue(pType);

      return concreteValue.compareTo(minValue) < 0 || maxValue.compareTo(concreteValue) < 0;
    }
  }

  /**
   * This visitor replaces all occurrences of a given {@link SymbolicIdentifier} in a
   * {@link SymbolicValue} with the given value.
   *
   * When visiting a SymbolicExpression, this class has to always return a SymbolicExpression,
   * no other {@link Value} type is allowed.
   */
  private class IdentifierReplacer implements SymbolicValueVisitor<Value> {

    private long idToReplace;
    private Value newValue;
    private final SymbolicExpressionFactory factory = SymbolicExpressionFactory.getInstance();

    public IdentifierReplacer(SymbolicIdentifier pIdentifierToReplace, Value pNewValue) {
      idToReplace = pIdentifierToReplace.getId();
      newValue = pNewValue;
    }

    @Override
    public Value visit(SymbolicIdentifier pSymbolicValue) {
      long id = pSymbolicValue.getId();

      return id == idToReplace ? cast(newValue, pSymbolicValue) : pSymbolicValue;
    }

    private Value cast(Value pValue, SymbolicIdentifier pIdentifierValue) {
      Type type = pIdentifierValue.getType();

      if (type instanceof JType) {
        JType toType = (JType) type;
        JType fromType = getJFromType(toType);
        return AbstractExpressionValueVisitor.castJValue(pValue, fromType, toType, logger, null);

      } else {
        assert type instanceof CType;

        CType toType = (CType) type;
        CType fromType = getCFromType(toType);
        return AbstractExpressionValueVisitor.castCValue(pValue, fromType, toType, machineModel, logger, null);
      }
    }

    private JType getJFromType(JType pType) {
      if (!(pType instanceof JSimpleType)) {
        return pType;

      } else {
        final JBasicType basicType = ((JSimpleType) pType).getType();
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
        final CSimpleType simpleType = (CSimpleType) pType;
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
      }

      return factory.asConstant(newValue, pExpression.getType());
    }

    private SymbolicExpression replaceInBinaryExpression(BinarySymbolicExpression pExpression,
        BinaryFactoryFunction pFactoryFunction) {
      // This visitor always returns a SymbolicExpression when visiting a SymbolicExpression, so the cast
      // is fine
      SymbolicExpression leftOperand = (SymbolicExpression) pExpression.getOperand1().accept(this);
      SymbolicExpression rightOperand = (SymbolicExpression) pExpression.getOperand2().accept(this);

      Type expressionType = pExpression.getType();
      Type calculationType = pExpression.getCalculationType();

      return pFactoryFunction.create(leftOperand, rightOperand, expressionType, calculationType);
    }

    private SymbolicExpression replaceInUnaryExpression(UnarySymbolicExpression pExpression,
        UnaryFactoryFunction pFactoryFunction) {
      // This visitor always returns a SymbolicExpression when visiting a SymbolicExpression, so the cast
      // is fine
      SymbolicExpression operand = (SymbolicExpression) pExpression.getOperand().accept(this);

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
  }

  private interface BinaryFactoryFunction {
    SymbolicExpression create(SymbolicExpression pLeftOp, SymbolicExpression pRightOp, Type pExpType, Type pCalcType);
  }

  private interface UnaryFactoryFunction {
    SymbolicExpression create(SymbolicExpression pOperand, Type pExpType);
  }
}
