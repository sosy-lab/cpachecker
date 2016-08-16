/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntegralInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManager;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.MemoryLocationExtractor;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instances of this class are c expression visitors used to convert c
 * expressions to compound state invariants formulae.
 */
public class ExpressionToFormulaVisitor extends DefaultCExpressionVisitor<NumeralFormula<CompoundInterval>, UnrecognizedCodeException> implements CRightHandSideVisitor<NumeralFormula<CompoundInterval>, UnrecognizedCodeException>, JRightHandSideVisitor<NumeralFormula<CompoundInterval>, UnrecognizedCodeException> {

  /**
   * The variable name extractor used to extract variable names from c id
   * expressions.
   */
  private final MemoryLocationExtractor variableNameExtractor;

  private final Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> environment;

  private final MachineModel machineModel;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final CompoundIntervalFormulaManager compoundIntervalFormulaManager;

  private final BaseSizeofVisitor sizeofVisitor;

  /**
   * Creates a new visitor for converting c expressions to compound state
   * invariants formulae with the given variable name extractor.
   *
   * @param pCompoundIntervalManagerFactory the factory for compound interval managers.
   * @param pMachineModel the machine model.
   * @param pVariableNameExtractor the variable name extractor used to obtain
   * variable names for c id expressions.
   */
  public ExpressionToFormulaVisitor(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      MemoryLocationExtractor pVariableNameExtractor) {
    this(pCompoundIntervalManagerFactory, pMachineModel, pVariableNameExtractor, Collections.<MemoryLocation, NumeralFormula<CompoundInterval>>emptyMap());
  }

  /**
   * Creates a new visitor for converting c expressions to compound state
   * invariants formulae with the given variable name extractor.
   *
   * @param pCompoundIntervalManagerFactory the factory for compound interval managers.
   * @param pMachineModel the machine model.
   * @param pVariableNameExtractor the variable name extractor used to obtain
   * variable names for c id expressions.
   * @param pEnvironment the current environment information.
   */
  public ExpressionToFormulaVisitor(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      MemoryLocationExtractor pVariableNameExtractor,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    this.machineModel = pMachineModel;
    this.variableNameExtractor = pVariableNameExtractor;
    this.environment = pEnvironment;
    this.evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    this.compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    sizeofVisitor = new BaseSizeofVisitor(machineModel);
  }

  private CompoundIntervalManager getIntervalManager(Type pType) {
    return compoundIntervalManagerFactory.createCompoundIntervalManager(machineModel, pType);
  }

  private NumeralFormula<CompoundInterval> allPossibleValues(AExpression pExpression) {
    return allPossibleValues(pExpression.getExpressionType());
  }

  private NumeralFormula<CompoundInterval> allPossibleValues(Type pType) {
    return asConstant(pType, getIntervalManager(pType).allPossibleValues());
  }

  private NumeralFormula<CompoundInterval> asConstant(Type pType, boolean pValue) {
    return asConstant(pType, getIntervalManager(pType).fromBoolean(pValue));
  }

  private NumeralFormula<CompoundInterval> asConstant(Type pType, BigInteger pValue) {
    return asConstant(pType, getIntervalManager(pType).castedSingleton(pValue));
  }

  private NumeralFormula<CompoundInterval> asConstant(Type pType, long pValue) {
    return asConstant(pType, BigInteger.valueOf(pValue));
  }

  private NumeralFormula<CompoundInterval> asConstant(Type pType, CompoundInterval pValue) {
    return InvariantsFormulaManager.INSTANCE.asConstant(
        BitVectorInfo.from(machineModel, pType),
        pValue);
  }

  private NumeralFormula<CompoundInterval> asVariable(Type pType, MemoryLocation pMemoryLocation) {
    return InvariantsFormulaManager.INSTANCE.asVariable(
        BitVectorInfo.from(machineModel, pType),
        pMemoryLocation);
  }

  @Override
  protected NumeralFormula<CompoundInterval> visitDefault(CExpression pCExpression) throws UnrecognizedCodeException {
    return allPossibleValues(pCExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CIdExpression pCIdExpression) throws UnrecognizedCodeException {
    return asVariable(pCIdExpression.getExpressionType(), this.variableNameExtractor.getMemoryLocation(pCIdExpression));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CFieldReference pCFieldReference) throws UnrecognizedCodeException {
    return asVariable(pCFieldReference.getExpressionType(), this.variableNameExtractor.getMemoryLocation(pCFieldReference));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CArraySubscriptExpression pCArraySubscriptExpression) throws UnrecognizedCodeException {
    return asVariable(pCArraySubscriptExpression.getExpressionType(), this.variableNameExtractor.getMemoryLocation(pCArraySubscriptExpression));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CIntegerLiteralExpression pE) {
    return asConstant(pE.getExpressionType(), pE.getValue());
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CCharLiteralExpression pE) {
    return asConstant(pE.getExpressionType(), pE.getCharacter());
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CImaginaryLiteralExpression pE) throws UnrecognizedCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CUnaryExpression pCUnaryExpression) throws UnrecognizedCodeException {
    CExpression operandExpression = pCUnaryExpression.getOperand();
    if (pCUnaryExpression.getOperator() != UnaryOperator.AMPER) {
      operandExpression =
          makeCastFromArrayToPointerIfNecessary(
              operandExpression, pCUnaryExpression.getExpressionType());
    }
    NumeralFormula<CompoundInterval> operand = operandExpression.accept(this);
    TypeInfo typeInfo = BitVectorInfo.from(machineModel, pCUnaryExpression.getExpressionType());
    operand = compoundIntervalFormulaManager.cast(typeInfo, operand);
    final NumeralFormula<CompoundInterval> result;
    switch (pCUnaryExpression.getOperator()) {
    case MINUS:
      result = compoundIntervalFormulaManager.negate(operand);
      break;
    case TILDE:
      result = compoundIntervalFormulaManager.binaryNot(operand);
      break;
    case AMPER:
      result = allPossibleValues(pCUnaryExpression);
      break;
    default:
      result = super.visit(pCUnaryExpression);
      break;
    }
    return compoundIntervalFormulaManager.cast(typeInfo, result);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CPointerExpression pCPointerExpression) throws UnrecognizedCodeException {
    return asVariable(pCPointerExpression.getExpressionType(), variableNameExtractor.getMemoryLocation(pCPointerExpression));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CCastExpression pCCastExpression) throws UnrecognizedCodeException {
    CExpression expression =
        makeCastFromArrayToPointerIfNecessary(
            pCCastExpression.getOperand(), pCCastExpression.getCastType());
    TypeInfo typeInfo = BitVectorInfo.from(machineModel, pCCastExpression.getCastType());
    return compoundIntervalFormulaManager.cast(typeInfo, expression.accept(this));
  }

  private CType getPromotedCType(CType t) {
    t = t.getCanonicalType();
    if (t instanceof CSimpleType) {
      // Integer types smaller than int are promoted when an operation is performed on them.
      return machineModel.getPromotedCType((CSimpleType) t);
    }
    return t;
  }

  /**
   * Returns the size in bytes of the given type.
   * Always use this method instead of machineModel.getSizeOf,
   * because this method can handle dereference-types.
   * @param pType the type to calculate the size of.
   * @return the size in bytes of the given type.
   */
  private int getSizeof(CType pType) {
    return pType.accept(sizeofVisitor);
  }

  private NumeralFormula<CompoundInterval> getPointerTargetSizeLiteral(
      final CPointerType pointerType, final CType implicitType) {
    final int pointerTargetSize = getSizeof(pointerType.getType());
    return asConstant(implicitType, pointerTargetSize);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CBinaryExpression pCBinaryExpression) throws UnrecognizedCodeException {
    final CType calculationType = pCBinaryExpression.getCalculationType();
    final CType t1 = pCBinaryExpression.getOperand1().getExpressionType();
    final CType t2 = pCBinaryExpression.getOperand2().getExpressionType();
    final CType promLeft = getPromotedCType(t1).getCanonicalType();
    final CType promRight = getPromotedCType(t2).getCanonicalType();

    TypeInfo typeInfo = BitVectorInfo.from(machineModel, calculationType);
    NumeralFormula<CompoundInterval> left =
        makeCastFromArrayToPointerIfNecessary(pCBinaryExpression.getOperand1(), calculationType)
            .accept(this);
    NumeralFormula<CompoundInterval> right =
        makeCastFromArrayToPointerIfNecessary(pCBinaryExpression.getOperand2(), calculationType)
            .accept(this);
    left = compoundIntervalFormulaManager.cast(typeInfo, left);
    right = compoundIntervalFormulaManager.cast(typeInfo, right);
    left = topIfProblematicType(calculationType, left);
    right = topIfProblematicType(calculationType, right);

    final NumeralFormula<CompoundInterval> result;
    switch (pCBinaryExpression.getOperator()) {
    case BINARY_AND:
      result = allPossibleValues(pCBinaryExpression);
      break;
    case BINARY_OR:
      result = allPossibleValues(pCBinaryExpression);
      break;
    case BINARY_XOR:
      result = allPossibleValues(pCBinaryExpression);
      break;
    case DIVIDE:
      result = compoundIntervalFormulaManager.divide(left, right);
      break;
      case EQUALS:
        result =
            compoundIntervalFormulaManager.fromBoolean(
                typeInfo, compoundIntervalFormulaManager.equal(left, right));
        break;
      case GREATER_EQUAL:
        result =
            compoundIntervalFormulaManager.fromBoolean(
                typeInfo, compoundIntervalFormulaManager.greaterThanOrEqual(left, right));
        break;
      case GREATER_THAN:
        result =
            compoundIntervalFormulaManager.fromBoolean(
                typeInfo, compoundIntervalFormulaManager.greaterThan(left, right));
        break;
      case LESS_EQUAL:
        result =
            compoundIntervalFormulaManager.fromBoolean(
                typeInfo, compoundIntervalFormulaManager.lessThanOrEqual(left, right));
        break;
      case LESS_THAN:
        result =
            compoundIntervalFormulaManager.fromBoolean(
                typeInfo, compoundIntervalFormulaManager.lessThan(left, right));
        break;
      case MINUS:
        if (!(promLeft instanceof CPointerType)
            && !(promRight instanceof CPointerType)) { // Just a subtraction e.g. 6 - 7
          result = compoundIntervalFormulaManager.subtract(left, right);
        } else if (!(promRight instanceof CPointerType)) {
          // operand1 is a pointer => we should multiply the subtrahend by the size of the pointer target
          result =
              compoundIntervalFormulaManager.subtract(
                  left,
                  compoundIntervalFormulaManager.multiply(
                      right,
                      getPointerTargetSizeLiteral((CPointerType) promLeft, calculationType)));
        } else if (promLeft instanceof CPointerType) {
          // Pointer subtraction => (operand1 - operand2) / sizeof (*operand1)
          if (promLeft.equals(promRight)) {
            result =
                compoundIntervalFormulaManager.divide(
                    compoundIntervalFormulaManager.subtract(left, right),
                    getPointerTargetSizeLiteral((CPointerType) promLeft, calculationType));
          } else {
            throw new UnrecognizedCCodeException(
                "Can't subtract pointers of different types", pCBinaryExpression);
          }
        } else {
          throw new UnrecognizedCCodeException(
              "Can't subtract a pointer from a non-pointer", pCBinaryExpression);
        }
        break;
    case MODULO:
      result = compoundIntervalFormulaManager.modulo(left, right);
      break;
    case MULTIPLY:
      result = compoundIntervalFormulaManager.multiply(left, right);
      break;
      case NOT_EQUALS:
        result =
            compoundIntervalFormulaManager.fromBoolean(
                typeInfo,
                compoundIntervalFormulaManager.logicalNot(
                    compoundIntervalFormulaManager.equal(left, right)));
        break;
      case PLUS:
        if (!(promLeft instanceof CPointerType)
            && !(promRight instanceof CPointerType)) { // Just an addition e.g. 6 + 7
          result = compoundIntervalFormulaManager.add(left, right);
        } else if (!(promRight instanceof CPointerType)) {
          // operand1 is a pointer => we should multiply the second summand by the size of the pointer target
          result =
              compoundIntervalFormulaManager.add(
                  left,
                  compoundIntervalFormulaManager.multiply(
                      right,
                      getPointerTargetSizeLiteral((CPointerType) promLeft, calculationType)));
        } else if (!(promLeft instanceof CPointerType)) {
          result =
              compoundIntervalFormulaManager.add(
                  right,
                  compoundIntervalFormulaManager.multiply(
                      left,
                      getPointerTargetSizeLiteral((CPointerType) promRight, calculationType)));
        } else {
          throw new UnrecognizedCCodeException("Can't add pointers", pCBinaryExpression);
        }
        break;
    case SHIFT_LEFT:
      result = compoundIntervalFormulaManager.shiftLeft(left, right);
      break;
    case SHIFT_RIGHT:
      result = compoundIntervalFormulaManager.shiftRight(left, right);
      break;
    default:
      result = allPossibleValues(pCBinaryExpression);
      break;
    }
    return compoundIntervalFormulaManager.cast(typeInfo, result);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CFunctionCallExpression pIastFunctionCallExpression) {
    return allPossibleValues(pIastFunctionCallExpression.getExpressionType());
  }

  private NumeralFormula<CompoundInterval> topIfProblematicType(CType pType, NumeralFormula<CompoundInterval> pFormula) {
    if ((pType instanceof CSimpleType) && ((CSimpleType) pType).getCanonicalType().isUnsigned()) {
      CompoundInterval value = pFormula.accept(evaluationVisitor, environment);
      if (value.containsAllPossibleValues()) {
        return pFormula;
      }
      if (value.containsNegative()) {
        return allPossibleValues(pType);
      }
    }
    return pFormula;
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JCharLiteralExpression pCharLiteralExpression)
      throws UnrecognizedCodeException {
    return asConstant(pCharLiteralExpression.getExpressionType(), pCharLiteralExpression.getCharacter());
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JStringLiteralExpression pStringLiteralExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pStringLiteralExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JBinaryExpression pBinaryExpression)
      throws UnrecognizedCodeException {
    NumeralFormula<CompoundInterval> left = pBinaryExpression.getOperand1().accept(this);
    NumeralFormula<CompoundInterval> right = pBinaryExpression.getOperand2().accept(this);
    BooleanFormula<CompoundInterval> logicalLeft = compoundIntervalFormulaManager.fromNumeral(left);
    BooleanFormula<CompoundInterval> logicalRight = compoundIntervalFormulaManager.fromNumeral(right);
    TypeInfo typeInfo = BitVectorInfo.from(machineModel, pBinaryExpression.getExpressionType());
    switch (pBinaryExpression.getOperator()) {
      case BINARY_AND:
        return allPossibleValues(pBinaryExpression);
      case BINARY_OR:
        return allPossibleValues(pBinaryExpression);
      case BINARY_XOR:
        return allPossibleValues(pBinaryExpression);
      case CONDITIONAL_AND:
        return allPossibleValues(pBinaryExpression);
      case CONDITIONAL_OR:
        return allPossibleValues(pBinaryExpression);
      case DIVIDE:
        return compoundIntervalFormulaManager.divide(left, right);
      case EQUALS:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo, compoundIntervalFormulaManager.equal(left, right));
      case GREATER_EQUAL:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo, compoundIntervalFormulaManager.greaterThanOrEqual(left, right));
      case GREATER_THAN:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo, compoundIntervalFormulaManager.greaterThan(left, right));
      case LESS_EQUAL:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo, compoundIntervalFormulaManager.lessThan(left, right));
      case LESS_THAN:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo, compoundIntervalFormulaManager.lessThanOrEqual(left, right));
      case LOGICAL_AND:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo, compoundIntervalFormulaManager.logicalAnd(logicalLeft, logicalRight));
      case LOGICAL_OR:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo, compoundIntervalFormulaManager.logicalOr(logicalLeft, logicalRight));
      case LOGICAL_XOR:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo,
            compoundIntervalFormulaManager.logicalOr(
                compoundIntervalFormulaManager.logicalAnd(
                    logicalLeft, compoundIntervalFormulaManager.logicalNot(logicalRight)),
                compoundIntervalFormulaManager.logicalAnd(
                    compoundIntervalFormulaManager.logicalNot(logicalLeft), logicalRight)));
      case MINUS:
        return compoundIntervalFormulaManager.subtract(left, right);
      case MODULO:
        return compoundIntervalFormulaManager.modulo(left, right);
      case MULTIPLY:
        return compoundIntervalFormulaManager.multiply(left, right);
      case NOT_EQUALS:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo,
            compoundIntervalFormulaManager.logicalNot(
                compoundIntervalFormulaManager.equal(left, right)));
      case PLUS:
        return compoundIntervalFormulaManager.add(left, right);
      case SHIFT_LEFT:
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        return compoundIntervalFormulaManager.shiftLeft(left, right);
      case SHIFT_RIGHT_SIGNED:
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        return compoundIntervalFormulaManager.shiftRight(left, right);
      case SHIFT_RIGHT_UNSIGNED:
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        CompoundInterval leftEval = left.accept(evaluationVisitor, environment);
        NumeralFormula<CompoundInterval> forPositiveLeft = compoundIntervalFormulaManager.shiftRight(left, right);
        if (!leftEval.containsNegative()) {
          return forPositiveLeft;
        }
        NumeralFormula<CompoundInterval> forNegativeLeft =
            compoundIntervalFormulaManager.add(
                forPositiveLeft,
                compoundIntervalFormulaManager.shiftLeft(
                    asConstant(
                        pBinaryExpression.getExpressionType(),
                        compoundIntervalManagerFactory
                            .createCompoundIntervalManager(left.getTypeInfo())
                            .singleton(2)),
                    compoundIntervalFormulaManager.binaryNot(right)));
        if (!leftEval.containsPositive()) {
          return forNegativeLeft;
        }
        return compoundIntervalFormulaManager.union(forPositiveLeft, forNegativeLeft);
      case STRING_CONCATENATION:
        return allPossibleValues(pBinaryExpression);
      default:
        throw new AssertionError("Unhandled enum value in switch: " + pBinaryExpression.getOperator());
    }
  }

  private NumeralFormula<CompoundInterval> truncateShiftOperand(JType pExpressionType, NumeralFormula<CompoundInterval> pOperand) {
    if (pExpressionType instanceof JSimpleType) {
      JSimpleType simpleType = (JSimpleType) pExpressionType;
      if (simpleType.getType() == JBasicType.INT) {
        return compoundIntervalFormulaManager.binaryAnd(pOperand, asConstant(pExpressionType, 0x1F));
      } else if (simpleType.getType() == JBasicType.LONG) {
        return compoundIntervalFormulaManager.binaryAnd(pOperand, asConstant(pExpressionType, 0x3F));
      }
    }
    return pOperand;
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JUnaryExpression pUnaryExpression) throws UnrecognizedCodeException {
    TypeInfo typeInfo = BitVectorInfo.from(machineModel, pUnaryExpression.getExpressionType());
    switch (pUnaryExpression.getOperator()) {
    case MINUS:
      return compoundIntervalFormulaManager.negate(pUnaryExpression.getOperand().accept(this));
    case COMPLEMENT:
      return allPossibleValues(pUnaryExpression);
      case NOT:
        return compoundIntervalFormulaManager.fromBoolean(
            typeInfo,
            compoundIntervalFormulaManager.logicalNot(
                compoundIntervalFormulaManager.fromNumeral(
                    pUnaryExpression.getOperand().accept(this))));
    case PLUS:
      return pUnaryExpression.getOperand().accept(this);
    default:
      return allPossibleValues(pUnaryExpression);
    }
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JIntegerLiteralExpression pIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return asConstant(pIntegerLiteralExpression.getExpressionType(), pIntegerLiteralExpression.getValue());
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JBooleanLiteralExpression pBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    return asConstant(pBooleanLiteralExpression.getExpressionType(), pBooleanLiteralExpression.getValue());
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JFloatLiteralExpression pFloatLiteralExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pFloatLiteralExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JArrayCreationExpression pArrayCreationExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pArrayCreationExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JArrayInitializer pArrayInitializer)
      throws UnrecognizedCodeException {
    return allPossibleValues(pArrayInitializer);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JArrayLengthExpression pArrayLengthExpression) {
    return allPossibleValues(pArrayLengthExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JVariableRunTimeType pThisRunTimeType)
      throws UnrecognizedCodeException {
    return allPossibleValues(pThisRunTimeType);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JRunTimeTypeEqualsType pRunTimeTypeEqualsType)
      throws UnrecognizedCodeException {
    return allPossibleValues(pRunTimeTypeEqualsType);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JNullLiteralExpression pNullLiteralExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pNullLiteralExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JEnumConstantExpression pEnumConstantExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pEnumConstantExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JCastExpression pCastExpression) throws UnrecognizedCodeException {
    TypeInfo typeInfo = BitVectorInfo.from(machineModel, pCastExpression.getCastType());
    return compoundIntervalFormulaManager.cast(typeInfo, pCastExpression.getOperand().accept(this));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JThisExpression pThisExpression) throws UnrecognizedCodeException {
    return allPossibleValues(pThisExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JArraySubscriptExpression pArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pArraySubscriptExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JIdExpression pIdExpression) throws UnrecognizedCodeException {
    return asVariable(pIdExpression.getExpressionType(), variableNameExtractor.getMemoryLocation(pIdExpression));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JMethodInvocationExpression pFunctionCallExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pFunctionCallExpression.getExpressionType());
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JClassInstanceCreation pClassInstanceCreation)
      throws UnrecognizedCodeException {
    return allPossibleValues(pClassInstanceCreation.getExpressionType());
  }

  public static NumeralFormula<CompoundInterval> handlePotentialOverflow(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      NumeralFormula<CompoundInterval> pFormula,
      MachineModel pMachineModel,
      Type pTargetType,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {

    TypeInfo typeInfo = BitVectorInfo.from(pMachineModel, pTargetType);

    CompoundIntervalFormulaManager cifm = new CompoundIntervalFormulaManager(pCompoundIntervalManagerFactory);

    NumeralFormula<CompoundInterval> formula = cifm.cast(typeInfo, pFormula);

    CompoundIntervalManager cim =
        pCompoundIntervalManagerFactory.createCompoundIntervalManager(typeInfo);

    FormulaCompoundStateEvaluationVisitor evaluator =
        new FormulaCompoundStateEvaluationVisitor(pCompoundIntervalManagerFactory);
    CompoundInterval value = formula.accept(evaluator, pEnvironment);
    if (value instanceof CompoundIntegralInterval && typeInfo instanceof BitVectorInfo) {
      BitVectorInfo bitVectorInfo = (BitVectorInfo) typeInfo;
      BigInteger lowerInclusiveBound = bitVectorInfo.getMinValue();
      BigInteger upperExclusiveBound = bitVectorInfo.getMaxValue().add(BigInteger.ONE);
      CompoundIntegralInterval integralValue = (CompoundIntegralInterval) value;

      if (typeInfo.isSigned()) {
        if (!value.hasLowerBound() || !value.hasUpperBound()) {
          return InvariantsFormulaManager.INSTANCE.asConstant(typeInfo, cim.allPossibleValues());
        }
        if (integralValue.getLowerBound().compareTo(lowerInclusiveBound) < 0) {
          return InvariantsFormulaManager.INSTANCE.asConstant(typeInfo, cim.allPossibleValues());
        }
        if (integralValue.getUpperBound().compareTo(upperExclusiveBound) >= 0) {
          return InvariantsFormulaManager.INSTANCE.asConstant(typeInfo, cim.allPossibleValues());
        }
        // Handle implementation-defined cast to signed
        if (pCompoundIntervalManagerFactory instanceof CompoundBitVectorIntervalManagerFactory
            && !((CompoundBitVectorIntervalManagerFactory) pCompoundIntervalManagerFactory)
                .isSignedWrapAroundAllowed()) {
          CompoundInterval ci = pFormula.accept(evaluator, pEnvironment);
          if (ci instanceof CompoundBitVectorInterval) {
            CompoundBitVectorInterval cbvi = (CompoundBitVectorInterval) ci;
            final AtomicBoolean overflows = new AtomicBoolean();
            OverflowEventHandler overflowEventHandler = () -> overflows.set(true);
            // cast to check for overflow, result is unused
            cbvi.cast(bitVectorInfo, false, overflowEventHandler);
            if (overflows.get()) {
              return InvariantsFormulaManager.INSTANCE.asConstant(
                  typeInfo, cim.allPossibleValues());
            }
          }
          // TODO handle floats
        }
        return formula;
      }

      assert lowerInclusiveBound.compareTo(upperExclusiveBound) < 0;

      if (!value.hasLowerBound()) {
        return InvariantsFormulaManager.INSTANCE.asConstant(typeInfo, cim.allPossibleValues());
      }

      if (integralValue.getLowerBound().compareTo(lowerInclusiveBound) >= 0
          && value.hasUpperBound()
          && integralValue.getUpperBound().compareTo(upperExclusiveBound) < 0) {
        return formula;
      }

      CompoundInterval negativePart =
          cim.intersect(value, cim.negate(cim.singleton(1)).extendToMinValue());
      CompoundInterval negativePartMod =
          cim.modulo(negativePart, cim.singleton(upperExclusiveBound));
      CompoundInterval negativePartResult =
          cim.add(cim.singleton(upperExclusiveBound), negativePartMod);

      CompoundInterval nonNegativePart = cim.intersect(value, cim.singleton(0).extendToMaxValue());
      CompoundInterval nonNegativePartResult =
          cim.modulo(nonNegativePart, cim.singleton(upperExclusiveBound));

      return InvariantsFormulaManager.INSTANCE.asConstant(
          typeInfo, cim.union(negativePartResult, nonNegativePartResult));
    }
    return formula;
  }

  public static CRightHandSide makeCastFromArrayToPointerIfNecessary(
      CRightHandSide pExpression, CType pTargetType) {
    if (pExpression instanceof CExpression) {
      return makeCastFromArrayToPointerIfNecessary((CExpression) pExpression, pTargetType);
    }
    return pExpression;
  }

  public static AExpression makeCastFromArrayToPointerIfNecessary(
      AExpression pExpression, Type pTargetType) {
    if (pExpression instanceof CExpression && pTargetType instanceof CType) {
      return makeCastFromArrayToPointerIfNecessary((CExpression) pExpression, (CType) pTargetType);
    }
    return pExpression;
  }

  public static ARightHandSide makeCastFromArrayToPointerIfNecessary(
      ARightHandSide pExpression, Type pTargetType) {
    if (pExpression instanceof CExpression && pTargetType instanceof CType) {
      return makeCastFromArrayToPointerIfNecessary((CExpression) pExpression, (CType) pTargetType);
    }
    return pExpression;
  }

  public static CExpression makeCastFromArrayToPointerIfNecessary(
      CExpression pExpression, CType pTargetType) {
    if (pExpression.getExpressionType().getCanonicalType() instanceof CArrayType) {
      CType targetType = pTargetType.getCanonicalType();
      if (targetType instanceof CPointerType || targetType instanceof CSimpleType) {
        return makeCastFromArrayToPointer(pExpression);
      }
    }
    return pExpression;
  }

  private static CExpression makeCastFromArrayToPointer(CExpression pArrayExpression) {
    // array-to-pointer conversion
    CArrayType arrayType = (CArrayType) pArrayExpression.getExpressionType().getCanonicalType();
    CPointerType pointerType =
        new CPointerType(arrayType.isConst(), arrayType.isVolatile(), arrayType.getType());

    return new CUnaryExpression(
        pArrayExpression.getFileLocation(), pointerType, pArrayExpression, UnaryOperator.AMPER);
  }

}
