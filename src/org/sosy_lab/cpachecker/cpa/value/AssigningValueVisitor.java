// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.AtomicDouble;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Visitor that derives further information from an assume edge */
class AssigningValueVisitor extends ExpressionValueVisitor {

  private ExpressionValueVisitor nonAssigningValueVisitor;

  private ValueAnalysisState assignableState;

  private Collection<String> booleans;

  protected boolean truthValue = false;

  private final ValueTransferOptions options;

  public AssigningValueVisitor(
      ValueAnalysisState assignableState,
      boolean truthValue,
      Collection<String> booleanVariables,
      String functionName,
      ValueAnalysisState state,
      MachineModel machineModel,
      LogManagerWithoutDuplicates logger,
      ValueTransferOptions options) {
    super(state, functionName, machineModel, logger);
    nonAssigningValueVisitor =
        new ExpressionValueVisitor(state, functionName, machineModel, logger);
    this.assignableState = assignableState;
    booleans = booleanVariables;
    this.truthValue = truthValue;
    this.options = options;
  }

  private AExpression unwrap(AExpression expression) {
    // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!
    CSimpleType expType;
    CSimpleType castType;
    boolean stop = false;
    while (!stop && expression instanceof CCastExpression) {
      stop = true;
      // expression.getExpressionType() instanceof CType because expression CCastExpression
      if (((CType) expression.getExpressionType()).getCanonicalType() instanceof CSimpleType
          && ((CCastExpression) expression).getOperand().getExpressionType().getCanonicalType()
              instanceof CSimpleType) {
        castType = (CSimpleType) ((CType) expression.getExpressionType()).getCanonicalType();
        expType =
            (CSimpleType)
                ((CCastExpression) expression).getOperand().getExpressionType().getCanonicalType();
        if ((expType.getType().isIntegerType()
                && castType.getType().isIntegerType()
                && (expType.getType() == CBasicType.BOOL
                    || getMachineModel().getSizeof(expType) < getMachineModel().getSizeof(castType)
                    || (getMachineModel().getSizeof(expType)
                            == getMachineModel().getSizeof(castType)
                        && getMachineModel().isSigned(expType)
                            == getMachineModel().isSigned(castType))))
            || (expType.getType().isFloatingPointType()
                && castType.getType().isFloatingPointType()
                && getMachineModel().getSizeof(expType) < getMachineModel().getSizeof(castType))) {
          expression = ((CCastExpression) expression).getOperand();
          stop = false;
        }
      }
    }

    return expression;
  }

  @Override
  public Value visit(CBinaryExpression pE) throws UnrecognizedCodeException {
    BinaryOperator binaryOperator = pE.getOperator();
    CExpression lVarInBinaryExp = (CExpression) unwrap(pE.getOperand1());
    CExpression rVarInBinaryExp = (CExpression) unwrap(pE.getOperand2());

    Value leftValue = lVarInBinaryExp.accept(nonAssigningValueVisitor);
    if (!(leftValue.isExplicitlyKnown()
        && leftValue.asNumericValue().getNumber() instanceof BigInteger
        && ((BigInteger) leftValue.asNumericValue().getNumber()).equals(BigInteger.ONE))) {
      leftValue =
          castCValue(
              leftValue,
              pE.getCalculationType(),
              getMachineModel(),
              getLogger(),
              pE.getFileLocation());
    }

    Value rightValue = rVarInBinaryExp.accept(nonAssigningValueVisitor);
    if (!(rightValue.isExplicitlyKnown()
        && rightValue.asNumericValue().getNumber() instanceof BigInteger
        && ((BigInteger) rightValue.asNumericValue().getNumber()).equals(BigInteger.ONE))) {
      rightValue =
          castCValue(
              rightValue,
              pE.getCalculationType(),
              getMachineModel(),
              getLogger(),
              pE.getFileLocation());
    }

    if (isEqualityAssumption(binaryOperator)) {
      if (leftValue.isExplicitlyKnown()) {
        Number lNum = leftValue.asNumericValue().getNumber();
        if (BigInteger.ONE.equals(lNum)) {
          rVarInBinaryExp.accept(this);
        }
      } else if (rightValue.isExplicitlyKnown()) {
        Number rNum = rightValue.asNumericValue().getNumber();
        if (BigInteger.ONE.equals(rNum)) {
          lVarInBinaryExp.accept(this);
        }
      }

      if (isEligibleForAssignment(leftValue)
          && rightValue.isExplicitlyKnown()
          && isAssignable(lVarInBinaryExp)) {
        assignConcreteValue(lVarInBinaryExp, leftValue, rightValue, pE.getCalculationType());
      } else if (isEligibleForAssignment(rightValue)
          && leftValue.isExplicitlyKnown()
          && isAssignable(rVarInBinaryExp)) {
        assignConcreteValue(rVarInBinaryExp, rightValue, leftValue, pE.getCalculationType());
      }
    }

    if (isNonEqualityAssumption(binaryOperator)) {
      if (assumingUnknownToBeZero(leftValue, rightValue) && isAssignable(lVarInBinaryExp)) {
        MemoryLocation leftMemLoc = getMemoryLocation(lVarInBinaryExp);

        if (options.isOptimizeBooleanVariables()
            && (booleans.contains(leftMemLoc.getExtendedQualifiedName())
                || options.isInitAssumptionVars())) {
          assignableState.assignConstant(
              leftMemLoc, new NumericValue(1L), pE.getOperand1().getExpressionType());
        }

      } else if (options.isOptimizeBooleanVariables()
          && (assumingUnknownToBeZero(rightValue, leftValue) && isAssignable(rVarInBinaryExp))) {
        MemoryLocation rightMemLoc = getMemoryLocation(rVarInBinaryExp);

        if (booleans.contains(rightMemLoc.getExtendedQualifiedName())
            || options.isInitAssumptionVars()) {
          assignableState.assignConstant(
              rightMemLoc, new NumericValue(1L), pE.getOperand2().getExpressionType());
        }
      }
    }

    return nonAssigningValueVisitor.visit(pE);
  }

  private boolean isEligibleForAssignment(final Value pValue) {
    return pValue.isUnknown() && options.isAssignEqualityAssumptions();
  }

  private void assignConcreteValue(
      final CExpression pVarInBinaryExp,
      final Value pOldValue,
      final Value pNewValue,
      final CType pValueType)
      throws UnrecognizedCodeException {
    checkState(
        !(pOldValue instanceof SymbolicValue),
        "Symbolic values should never be replaced by a concrete value");

    Preconditions.checkArgument(isValueInRangeOfType(pValueType, pNewValue));

    Value pInvertedCastValue =
        invertCast(pVarInBinaryExp.getExpressionType(), pValueType, pNewValue);
    if (pInvertedCastValue.isExplicitlyKnown()) {
      assignableState.assignConstant(
          getMemoryLocation(pVarInBinaryExp),
          pInvertedCastValue,
          pVarInBinaryExp.getExpressionType());
    }
  }

  Value invertCast(final CType pOriginalType, final CType pCastType, final Value pValue) {
    Preconditions.checkArgument(pValue.isExplicitlyKnown());

    if (pOriginalType.getCanonicalType().equals(pCastType.getCanonicalType())) {
      return pValue;
    }

    if (pOriginalType.getCanonicalType() instanceof CSimpleType
        && pCastType.getCanonicalType() instanceof CSimpleType) {
      CSimpleType origType = (CSimpleType) pOriginalType.getCanonicalType();
      CSimpleType castType = (CSimpleType) pCastType.getCanonicalType();

      if (origType.getType().isFloatingPointType()) { // orig type floating point
        Preconditions.checkArgument(castType.getType().isFloatingPointType());
        Preconditions.checkArgument(
            getMachineModel().getSizeof(castType) != getMachineModel().getSizeof(origType));

        if (getMachineModel().getSizeof(origType) < getMachineModel().getSizeof(castType)) {
          // TODO set null value or delete parameter because not used anyways
          Value downCastVal = castCValue(pValue, origType, getMachineModel(), getLogger(), null);
          if (downCastVal.isExplicitlyKnown()
              && !(downCastVal.asNumericValue().getNumber() instanceof Rational)
              && downCastVal
                  .asNumericValue()
                  .bigDecimalValue()
                  .equals(pValue.asNumericValue().bigDecimalValue())) {
            return pValue;
          }
        }

        // potential precision loss, be conservative
        return UnknownValue.getInstance();
      } else if (castType.getType().isFloatingPointType()) { // cast type floating point,
        Preconditions.checkArgument(pValue instanceof NumericValue); // but orig type not
        NumericValue numVal = (NumericValue) pValue;

        Number number = numVal.getNumber();
        if (number.equals(Float.NaN)
            || number.equals(Double.NaN)
            || NegativeNaN.VALUE.equals(number)
            || number.equals(Double.POSITIVE_INFINITY)
            || number.equals(Double.NEGATIVE_INFINITY)
            || number.equals(Float.POSITIVE_INFINITY)
            || number.equals(Float.NEGATIVE_INFINITY)) { // NaN, -NaN, +/-infinity
          return UnknownValue.getInstance(); // no integer value exists
        } else {
          NumericValue resVal;
          if (numVal.getNumber() instanceof Rational) {
            if (((Rational) numVal.getNumber()).isIntegral()) {
              resVal = new NumericValue(((Rational) numVal.getNumber()).getNum());
            } else { // Rational always normalized,
              // thus, denominator cannot be a divisor of nominator
              return UnknownValue.getInstance(); // no integer value exists
            }
          } else {
            try {
              resVal = new NumericValue(numVal.bigDecimalValue().toBigIntegerExact());
            } catch (ArithmeticException e) {
              return UnknownValue.getInstance(); // no integer value exists
            }
          }

          return invertCastFromInteger(origType, castType, resVal, false);
        }

      } else { // both integer type
        Preconditions.checkArgument(
            getMachineModel().getSizeof(castType) >= getMachineModel().getSizeof(origType));

        if (pValue.isNumericValue()) {
          return invertCastFromInteger(
              origType,
              castType,
              pValue.asNumericValue(),
              !getMachineModel().isSigned(castType) && getMachineModel().isSigned(origType));
        } else {
          return UnknownValue.getInstance();
        }
      }
    }
    return pValue; // TODO behaves as before, might be unsound, better unknown?
  }

  Value invertCastFromInteger(
      final CSimpleType pTargetTypeOfValue,
      final CSimpleType pCastType,
      final NumericValue pValue,
      final boolean invertToUnsignedConversion) {
    Preconditions.checkArgument(pTargetTypeOfValue.getType().isIntegerType());
    Preconditions.checkArgument(!(pValue.getNumber() instanceof Rational));
    Preconditions.checkArgument(
        !invertToUnsignedConversion
            || (pCastType.getType().isIntegerType()
                && !getMachineModel().isSigned(pCastType)
                && getMachineModel().isSigned(pTargetTypeOfValue)));

    Number num = pValue.getNumber();
    if (num instanceof Double
        || num instanceof Float
        || num instanceof AtomicDouble
        || num instanceof BigDecimal
        || num instanceof NegativeNaN) {
      return UnknownValue.getInstance();
    }

    if (pCastType.getType().isIntegerType()
        && getMachineModel().getSizeof(pTargetTypeOfValue)
            > getMachineModel().getSizeof(pCastType)) {
      return UnknownValue.getInstance();
    }

    if (isValueInRangeOfType(pTargetTypeOfValue, pValue)) {
      return pValue;
    } else if (!getMachineModel().isSigned(pTargetTypeOfValue)
        && pValue.bigDecimalValue().compareTo(BigDecimal.valueOf(0)) < 0) {
      // invert conversion to signed type
      if (pCastType.getType().isFloatingPointType()
          || getMachineModel().getSizeof(pTargetTypeOfValue)
              != getMachineModel().getSizeof(pCastType)) {
        return UnknownValue.getInstance();
      } else {
        // getMachineModel().getSizeof(pTargetTypeOfValue) ==
        // getMachineModel().getSizeof(pCastType))

        BigInteger toAdd;
        if (num instanceof BigInteger) {
          toAdd = (BigInteger) num;
        } else if (num instanceof UnsignedInteger) {
          toAdd = ((UnsignedInteger) num).bigIntegerValue();
        } else if (num instanceof UnsignedLong) {
          toAdd = ((UnsignedLong) num).bigIntegerValue();
        } else {
          toAdd = BigInteger.valueOf(num.longValue());
        }

        return invertCastFromInteger(
            pTargetTypeOfValue,
            pCastType,
            new NumericValue(
                BigInteger.ONE
                    .shiftLeft(getMachineModel().getSizeofInBits(pTargetTypeOfValue))
                    .add(toAdd)),
            false);
      }
    } else if (invertToUnsignedConversion) {
      // invert conversion to unsigned type
      checkState(getMachineModel().isSigned(pTargetTypeOfValue));
      checkState(!getMachineModel().isSigned(pCastType));
      checkState(!pCastType.getType().isFloatingPointType());
      checkState(
          getMachineModel().getSizeof(pTargetTypeOfValue)
              <= getMachineModel().getSizeof(pCastType));

      BigInteger toAdd;
      if (num instanceof BigInteger) {
        toAdd = (BigInteger) num;
      } else if (num instanceof UnsignedInteger) {
        toAdd = ((UnsignedInteger) num).bigIntegerValue();
      } else if (num instanceof UnsignedLong) {
        toAdd = ((UnsignedLong) num).bigIntegerValue();
      } else {
        toAdd = BigInteger.valueOf(num.longValue());
      }

      return invertCastFromInteger(
          pTargetTypeOfValue,
          pCastType,
          new NumericValue(
              toAdd.subtract(
                  BigInteger.ONE.shiftLeft(getMachineModel().getSizeofInBits(pCastType)))),
          false);
    } else {
      return UnknownValue.getInstance();
    }
  }

  private static boolean assumingUnknownToBeZero(Value value1, Value value2) {
    return value1.isUnknown() && value2.equals(new NumericValue(BigInteger.ZERO));
  }

  private boolean isEqualityAssumption(BinaryOperator binaryOperator) {
    return truthValue
        ? binaryOperator == BinaryOperator.EQUALS
        : binaryOperator == BinaryOperator.NOT_EQUALS;
  }

  private boolean isNonEqualityAssumption(BinaryOperator binaryOperator) {
    return truthValue
        ? binaryOperator == BinaryOperator.NOT_EQUALS
        : binaryOperator == BinaryOperator.EQUALS;
  }

  @Override
  public Value visit(JBinaryExpression pE) {
    JBinaryExpression.BinaryOperator binaryOperator = pE.getOperator();

    JExpression lVarInBinaryExp = pE.getOperand1();

    lVarInBinaryExp = (JExpression) unwrap(lVarInBinaryExp);

    JExpression rVarInBinaryExp = pE.getOperand2();

    Value leftValueV = lVarInBinaryExp.accept(nonAssigningValueVisitor);
    Value rightValueV = rVarInBinaryExp.accept(nonAssigningValueVisitor);

    if (truthValue
        ? binaryOperator == JBinaryExpression.BinaryOperator.EQUALS
        : binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS) {

      if (leftValueV.isUnknown()
          && rightValueV.isExplicitlyKnown()
          && isAssignableVariable(lVarInBinaryExp)) {
        assignValueToState((AIdExpression) lVarInBinaryExp, rightValueV);

      } else if (rightValueV.isUnknown()
          && leftValueV.isExplicitlyKnown()
          && isAssignableVariable(rVarInBinaryExp)) {
        assignValueToState((AIdExpression) rVarInBinaryExp, leftValueV);
      }
    }

    if (options.isInitAssumptionVars()) {
      // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
      // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
      if (truthValue
          ? binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS
          : binaryOperator == JBinaryExpression.BinaryOperator.EQUALS) {

        if (leftValueV.isUnknown()
            && rightValueV.isExplicitlyKnown()
            && isAssignableVariable(lVarInBinaryExp)) {

          // we only want BooleanValue objects for boolean values in the future
          assert rightValueV instanceof BooleanValue;
          BooleanValue booleanValueRight = BooleanValue.valueOf(rightValueV).orElseThrow();

          if (!booleanValueRight.isTrue()) {
            assignValueToState((AIdExpression) lVarInBinaryExp, BooleanValue.valueOf(true));
          }

        } else if (rightValueV.isUnknown()
            && leftValueV.isExplicitlyKnown()
            && isAssignableVariable(rVarInBinaryExp)) {

          // we only want BooleanValue objects for boolean values in the future
          assert leftValueV instanceof BooleanValue;
          BooleanValue booleanValueLeft = BooleanValue.valueOf(leftValueV).orElseThrow();

          if (!booleanValueLeft.isTrue()) {
            assignValueToState((AIdExpression) rVarInBinaryExp, BooleanValue.valueOf(true));
          }
        }
      }
    }
    return super.visit(pE);
  }

  // Assign the given value of the given IdExpression to the state of this TransferRelation
  private void assignValueToState(AIdExpression pIdExpression, Value pValue) {
    ASimpleDeclaration declaration = pIdExpression.getDeclaration();

    if (declaration != null) {
      assignableState.assignConstant(declaration.getQualifiedName(), pValue);
    } else {
      MemoryLocation memLoc =
          MemoryLocation.forLocalVariable(getFunctionName(), pIdExpression.getName());
      assignableState.assignConstant(memLoc, pValue, pIdExpression.getExpressionType());
    }
  }

  private MemoryLocation getMemoryLocation(CExpression pLValue) throws UnrecognizedCodeException {
    ExpressionValueVisitor v = getVisitor();
    assert pLValue instanceof CLeftHandSide;
    return checkNotNull(v.evaluateMemoryLocation(pLValue));
  }

  private static boolean isAssignableVariable(JExpression expression) {

    if (expression instanceof JIdExpression) {
      JSimpleDeclaration decl = ((JIdExpression) expression).getDeclaration();

      if (decl == null) {
        return false;
      } else if (decl instanceof JFieldDeclaration) {
        return ((JFieldDeclaration) decl).isStatic();
      } else {
        return true;
      }
    }

    return false;
  }

  private boolean isAssignable(CExpression expression) throws UnrecognizedCodeException {

    if (expression instanceof CIdExpression) {
      return true;
    }

    if (expression instanceof CFieldReference || expression instanceof CArraySubscriptExpression) {
      ExpressionValueVisitor evv = getVisitor();
      return evv.canBeEvaluated(expression);
    }

    return false;
  }

  /**
   * Determines for integer types whether the given value is within the bounds of the type. Note
   * that this does not mean that the value is necessary a valid value of the type.
   *
   * @param pExpectedTypeOfValue - the type defining the range of values
   * @param pValue - an explicitly known value
   * @return false - if <code>pValue</code> is a numeric value, which is not a rational, and <code>
   *     pExpectedTypeOfValue</code> is a CSimpleType representing an integer type and <code>pValue
   *     </code> is smaller than the minimum value or larger than the maximum value representable by
   *     <code>pExpectedTypeOfValue</code> true - otherwise
   */
  boolean isValueInRangeOfType(final CType pExpectedTypeOfValue, final Value pValue) {
    Preconditions.checkNotNull(pExpectedTypeOfValue);
    Preconditions.checkNotNull(pValue);
    Preconditions.checkArgument(pValue.isExplicitlyKnown());

    if (pValue instanceof NumericValue) {
      if (pExpectedTypeOfValue instanceof CSimpleType) {
        CSimpleType type = (CSimpleType) pExpectedTypeOfValue;
        if (type.getType().isIntegerType()
            && !(((NumericValue) pValue).getNumber() instanceof Rational)) {
          BigDecimal val = ((NumericValue) pValue).bigDecimalValue();

          return val.compareTo(new BigDecimal(getMachineModel().getMaximalIntegerValue(type))) <= 0
              && val.compareTo(new BigDecimal(getMachineModel().getMinimalIntegerValue(type))) >= 0;
        }
      }
    }
    return true;
  }

  /** returns an initialized, empty visitor */
  ExpressionValueVisitor getVisitor() {
    if (options.isIgnoreFunctionValue()) {
      return new ExpressionValueVisitor(
          getState(), getFunctionName(), getMachineModel(), getLogger());
    } else {
      return new FunctionPointerExpressionValueVisitor(
          getState(), getFunctionName(), getMachineModel(), getLogger());
    }
  }
}
