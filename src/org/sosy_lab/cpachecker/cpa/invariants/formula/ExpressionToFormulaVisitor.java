// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Instances of this class are c expression visitors used to convert c expressions to compound state
 * invariants formulae.
 */
public class ExpressionToFormulaVisitor
    extends DefaultCExpressionVisitor<NumeralFormula<CompoundInterval>, UnrecognizedCodeException>
    implements CRightHandSideVisitor<NumeralFormula<CompoundInterval>, UnrecognizedCodeException>,
        JRightHandSideVisitor<NumeralFormula<CompoundInterval>, UnrecognizedCodeException> {

  /** The variable name extractor used to extract variable names from c id expressions. */
  private final MemoryLocationExtractor variableNameExtractor;

  private final Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>
      environment;

  private final MachineModel machineModel;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final CompoundIntervalFormulaManager compoundIntervalFormulaManager;

  /**
   * Creates a new visitor for converting c expressions to compound state invariants formulae with
   * the given variable name extractor.
   *
   * @param pCompoundIntervalManagerFactory the factory for compound interval managers.
   * @param pMachineModel the machine model.
   * @param pVariableNameExtractor the variable name extractor used to obtain variable names for c
   *     id expressions.
   */
  public ExpressionToFormulaVisitor(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      MemoryLocationExtractor pVariableNameExtractor) {
    this(pCompoundIntervalManagerFactory, pMachineModel, pVariableNameExtractor, ImmutableMap.of());
  }

  /**
   * Creates a new visitor for converting c expressions to compound state invariants formulae with
   * the given variable name extractor.
   *
   * @param pCompoundIntervalManagerFactory the factory for compound interval managers.
   * @param pMachineModel the machine model.
   * @param pVariableNameExtractor the variable name extractor used to obtain variable names for c
   *     id expressions.
   * @param pEnvironment the current environment information.
   */
  public ExpressionToFormulaVisitor(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      MemoryLocationExtractor pVariableNameExtractor,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    machineModel = pMachineModel;
    variableNameExtractor = pVariableNameExtractor;
    environment = pEnvironment;
    evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory);
    compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
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
    return InvariantsFormulaManager.INSTANCE.asConstant(TypeInfo.from(machineModel, pType), pValue);
  }

  private NumeralFormula<CompoundInterval> asVariable(Type pType, MemoryLocation pMemoryLocation) {
    if (TypeInfo.isSupported(pType)) {
      return InvariantsFormulaManager.INSTANCE.asVariable(
          TypeInfo.from(machineModel, pType), pMemoryLocation);
    } else {
      // Use dummy type. Would be better to not use a 0-size bitvector type,
      // but at least this is better than a wrong non-zero size.
      return InvariantsFormulaManager.INSTANCE.asVariable(
          BitVectorInfo.from(0, false), pMemoryLocation);
    }
  }

  @Override
  protected NumeralFormula<CompoundInterval> visitDefault(CExpression pCExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pCExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CIdExpression pCIdExpression)
      throws UnrecognizedCodeException {
    return asVariable(
        pCIdExpression.getExpressionType(),
        variableNameExtractor.getMemoryLocation(pCIdExpression));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CFieldReference pCFieldReference)
      throws UnrecognizedCodeException {
    return asVariable(
        pCFieldReference.getExpressionType(),
        variableNameExtractor.getMemoryLocation(pCFieldReference));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(
      CArraySubscriptExpression pCArraySubscriptExpression) throws UnrecognizedCodeException {
    return asVariable(
        pCArraySubscriptExpression.getExpressionType(),
        variableNameExtractor.getMemoryLocation(pCArraySubscriptExpression));
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
  public NumeralFormula<CompoundInterval> visit(CImaginaryLiteralExpression pE)
      throws UnrecognizedCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CUnaryExpression pCUnaryExpression)
      throws UnrecognizedCodeException {
    CExpression operandExpression = pCUnaryExpression.getOperand();
    if (pCUnaryExpression.getOperator() != UnaryOperator.AMPER) {
      operandExpression =
          makeCastFromArrayToPointerIfNecessary(
              operandExpression, pCUnaryExpression.getExpressionType());
    }
    NumeralFormula<CompoundInterval> operand = operandExpression.accept(this);
    TypeInfo typeInfo = TypeInfo.from(machineModel, pCUnaryExpression.getExpressionType());
    operand = compoundIntervalFormulaManager.cast(typeInfo, operand);
    final NumeralFormula<CompoundInterval> result =
        switch (pCUnaryExpression.getOperator()) {
          case MINUS -> compoundIntervalFormulaManager.negate(operand);
          case TILDE -> compoundIntervalFormulaManager.binaryNot(operand);
          case AMPER -> allPossibleValues(pCUnaryExpression);
          default -> super.visit(pCUnaryExpression);
        };
    return compoundIntervalFormulaManager.cast(typeInfo, result);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CPointerExpression pCPointerExpression)
      throws UnrecognizedCodeException {
    return asVariable(
        pCPointerExpression.getExpressionType(),
        variableNameExtractor.getMemoryLocation(pCPointerExpression));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CCastExpression pCCastExpression)
      throws UnrecognizedCodeException {
    CExpression expression =
        makeCastFromArrayToPointerIfNecessary(
            pCCastExpression.getOperand(), pCCastExpression.getCastType());
    TypeInfo typeInfo = TypeInfo.from(machineModel, pCCastExpression.getCastType());
    return compoundIntervalFormulaManager.cast(typeInfo, expression.accept(this));
  }

  private CType getPromotedCType(CType t) {
    t = t.getCanonicalType();
    if (CTypes.isIntegerType(t)) {
      // Integer types smaller than int are promoted when an operation is performed on them.
      return machineModel.applyIntegerPromotion(t);
    }
    return t;
  }

  private NumeralFormula<CompoundInterval> getPointerTargetSizeLiteral(
      final CPointerType pointerType, final CType implicitType) {
    final BigInteger pointerTargetSize = machineModel.getSizeof(pointerType.getType());
    return asConstant(implicitType, pointerTargetSize);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(CBinaryExpression pCBinaryExpression)
      throws UnrecognizedCodeException {
    final CType calculationType = pCBinaryExpression.getCalculationType();
    final CType t1 = pCBinaryExpression.getOperand1().getExpressionType();
    final CType t2 = pCBinaryExpression.getOperand2().getExpressionType();
    final CType promLeft = getPromotedCType(t1).getCanonicalType();
    final CType promRight = getPromotedCType(t2).getCanonicalType();

    TypeInfo typeInfo = TypeInfo.from(machineModel, calculationType);
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

    final NumeralFormula<CompoundInterval> result =
        switch (pCBinaryExpression.getOperator()) {
          case BINARY_AND -> allPossibleValues(pCBinaryExpression);
          case BINARY_OR -> allPossibleValues(pCBinaryExpression);
          case BINARY_XOR -> allPossibleValues(pCBinaryExpression);
          case DIVIDE -> compoundIntervalFormulaManager.divide(left, right);
          case EQUALS ->
              compoundIntervalFormulaManager.fromBoolean(
                  typeInfo, compoundIntervalFormulaManager.equal(left, right));
          case GREATER_EQUAL ->
              compoundIntervalFormulaManager.fromBoolean(
                  typeInfo, compoundIntervalFormulaManager.greaterThanOrEqual(left, right));
          case GREATER_THAN ->
              compoundIntervalFormulaManager.fromBoolean(
                  typeInfo, compoundIntervalFormulaManager.greaterThan(left, right));
          case LESS_EQUAL ->
              compoundIntervalFormulaManager.fromBoolean(
                  typeInfo, compoundIntervalFormulaManager.lessThanOrEqual(left, right));
          case LESS_THAN ->
              compoundIntervalFormulaManager.fromBoolean(
                  typeInfo, compoundIntervalFormulaManager.lessThan(left, right));
          case MINUS -> {
            if (!(promLeft instanceof CPointerType)
                && !(promRight instanceof CPointerType)) { // Just a subtraction e.g. 6 - 7
              yield compoundIntervalFormulaManager.subtract(left, right);
            } else if (!(promRight instanceof CPointerType)) {
              // operand1 is a pointer => we should multiply the subtrahend by the size of the
              // pointer target
              yield compoundIntervalFormulaManager.subtract(
                  left,
                  compoundIntervalFormulaManager.multiply(
                      right,
                      getPointerTargetSizeLiteral((CPointerType) promLeft, calculationType)));
            } else if (promLeft instanceof CPointerType cPointerType) {
              // Pointer subtraction => (operand1 - operand2) / sizeof (*operand1)
              if (promLeft.equals(promRight)) {
                yield compoundIntervalFormulaManager.divide(
                    compoundIntervalFormulaManager.subtract(left, right),
                    getPointerTargetSizeLiteral(cPointerType, calculationType));
              } else {
                throw new UnrecognizedCodeException(
                    "Can't subtract pointers of different types", pCBinaryExpression);
              }
            } else {
              throw new UnrecognizedCodeException(
                  "Can't subtract a pointer from a non-pointer", pCBinaryExpression);
            }
          }
          case MODULO -> compoundIntervalFormulaManager.modulo(left, right);
          case MULTIPLY -> compoundIntervalFormulaManager.multiply(left, right);
          case NOT_EQUALS ->
              compoundIntervalFormulaManager.fromBoolean(
                  typeInfo,
                  compoundIntervalFormulaManager.logicalNot(
                      compoundIntervalFormulaManager.equal(left, right)));
          case PLUS -> {
            if (!(promLeft instanceof CPointerType)
                && !(promRight instanceof CPointerType)) { // Just an addition e.g. 6 + 7
              yield compoundIntervalFormulaManager.add(left, right);
            } else if (!(promRight instanceof CPointerType cPointerType)) {
              // operand1 is a pointer => we should multiply the second summand by the size of the
              // pointer target
              yield compoundIntervalFormulaManager.add(
                  left,
                  compoundIntervalFormulaManager.multiply(
                      right,
                      getPointerTargetSizeLiteral((CPointerType) promLeft, calculationType)));
            } else if (!(promLeft instanceof CPointerType)) {
              yield compoundIntervalFormulaManager.add(
                  right,
                  compoundIntervalFormulaManager.multiply(
                      left, getPointerTargetSizeLiteral(cPointerType, calculationType)));
            } else {
              throw new UnrecognizedCodeException("Can't add pointers", pCBinaryExpression);
            }
          }
          case SHIFT_LEFT -> compoundIntervalFormulaManager.shiftLeft(left, right);
          case SHIFT_RIGHT -> compoundIntervalFormulaManager.shiftRight(left, right);
        };
    return compoundIntervalFormulaManager.cast(typeInfo, result);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(
      CFunctionCallExpression pIastFunctionCallExpression) {
    return allPossibleValues(pIastFunctionCallExpression.getExpressionType());
  }

  private NumeralFormula<CompoundInterval> topIfProblematicType(
      CType pType, NumeralFormula<CompoundInterval> pFormula) {
    if ((pType instanceof CSimpleType cSimpleType)
        && cSimpleType.getCanonicalType().hasUnsignedSpecifier()) {
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
    return asConstant(
        pCharLiteralExpression.getExpressionType(), pCharLiteralExpression.getCharacter());
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
    BooleanFormula<CompoundInterval> logicalRight =
        compoundIntervalFormulaManager.fromNumeral(right);
    TypeInfo typeInfo = TypeInfo.from(machineModel, pBinaryExpression.getExpressionType());
    return switch (pBinaryExpression.getOperator()) {
      case BINARY_AND -> allPossibleValues(pBinaryExpression);

      case BINARY_OR -> allPossibleValues(pBinaryExpression);

      case BINARY_XOR -> allPossibleValues(pBinaryExpression);

      case CONDITIONAL_AND -> allPossibleValues(pBinaryExpression);

      case CONDITIONAL_OR -> allPossibleValues(pBinaryExpression);

      case DIVIDE -> compoundIntervalFormulaManager.divide(left, right);

      case EQUALS ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo, compoundIntervalFormulaManager.equal(left, right));

      case GREATER_EQUAL ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo, compoundIntervalFormulaManager.greaterThanOrEqual(left, right));

      case GREATER_THAN ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo, compoundIntervalFormulaManager.greaterThan(left, right));

      case LESS_EQUAL ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo, compoundIntervalFormulaManager.lessThanOrEqual(left, right));

      case LESS_THAN ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo, compoundIntervalFormulaManager.lessThan(left, right));

      case LOGICAL_AND ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo, compoundIntervalFormulaManager.logicalAnd(logicalLeft, logicalRight));

      case LOGICAL_OR ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo, compoundIntervalFormulaManager.logicalOr(logicalLeft, logicalRight));

      case LOGICAL_XOR ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo,
              compoundIntervalFormulaManager.logicalOr(
                  compoundIntervalFormulaManager.logicalAnd(
                      logicalLeft, compoundIntervalFormulaManager.logicalNot(logicalRight)),
                  compoundIntervalFormulaManager.logicalAnd(
                      compoundIntervalFormulaManager.logicalNot(logicalLeft), logicalRight)));

      case MINUS -> compoundIntervalFormulaManager.subtract(left, right);

      case MODULO -> compoundIntervalFormulaManager.modulo(left, right);

      case MULTIPLY -> compoundIntervalFormulaManager.multiply(left, right);

      case NOT_EQUALS ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo,
              compoundIntervalFormulaManager.logicalNot(
                  compoundIntervalFormulaManager.equal(left, right)));

      case PLUS -> compoundIntervalFormulaManager.add(left, right);

      case SHIFT_LEFT -> {
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        yield compoundIntervalFormulaManager.shiftLeft(left, right);
      }
      case SHIFT_RIGHT_SIGNED -> {
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        yield compoundIntervalFormulaManager.shiftRight(left, right);
      }
      case SHIFT_RIGHT_UNSIGNED -> {
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        CompoundInterval leftEval = left.accept(evaluationVisitor, environment);
        NumeralFormula<CompoundInterval> forPositiveLeft =
            compoundIntervalFormulaManager.shiftRight(left, right);
        if (!leftEval.containsNegative()) {
          yield forPositiveLeft;
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
          yield forNegativeLeft;
        }
        yield compoundIntervalFormulaManager.union(forPositiveLeft, forNegativeLeft);
      }
      case STRING_CONCATENATION -> allPossibleValues(pBinaryExpression);
    };
  }

  private NumeralFormula<CompoundInterval> truncateShiftOperand(
      JType pExpressionType, NumeralFormula<CompoundInterval> pOperand) {
    if (pExpressionType instanceof JSimpleType simpleType) {
      if (simpleType == JSimpleType.INT) {
        return compoundIntervalFormulaManager.binaryAnd(
            pOperand, asConstant(pExpressionType, 0x1F));
      } else if (simpleType == JSimpleType.LONG) {
        return compoundIntervalFormulaManager.binaryAnd(
            pOperand, asConstant(pExpressionType, 0x3F));
      }
    }
    return pOperand;
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JUnaryExpression pUnaryExpression)
      throws UnrecognizedCodeException {
    TypeInfo typeInfo = TypeInfo.from(machineModel, pUnaryExpression.getExpressionType());
    return switch (pUnaryExpression.getOperator()) {
      case MINUS ->
          compoundIntervalFormulaManager.negate(pUnaryExpression.getOperand().accept(this));
      case COMPLEMENT -> allPossibleValues(pUnaryExpression);
      case NOT ->
          compoundIntervalFormulaManager.fromBoolean(
              typeInfo,
              compoundIntervalFormulaManager.logicalNot(
                  compoundIntervalFormulaManager.fromNumeral(
                      pUnaryExpression.getOperand().accept(this))));
      case PLUS -> pUnaryExpression.getOperand().accept(this);
    };
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JIntegerLiteralExpression pIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return asConstant(
        pIntegerLiteralExpression.getExpressionType(), pIntegerLiteralExpression.getValue());
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JBooleanLiteralExpression pBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    return asConstant(
        pBooleanLiteralExpression.getExpressionType(), pBooleanLiteralExpression.getBoolean());
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
  public NumeralFormula<CompoundInterval> visit(JCastExpression pCastExpression)
      throws UnrecognizedCodeException {
    TypeInfo typeInfo = TypeInfo.from(machineModel, pCastExpression.getCastType());
    return compoundIntervalFormulaManager.cast(typeInfo, pCastExpression.getOperand().accept(this));
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JThisExpression pThisExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pThisExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JClassLiteralExpression pJClassLiteralExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pJClassLiteralExpression.getExpressionType());
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JArraySubscriptExpression pArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return allPossibleValues(pArraySubscriptExpression);
  }

  @Override
  public NumeralFormula<CompoundInterval> visit(JIdExpression pIdExpression)
      throws UnrecognizedCodeException {
    return asVariable(
        pIdExpression.getExpressionType(), variableNameExtractor.getMemoryLocation(pIdExpression));
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

    TypeInfo typeInfo = TypeInfo.from(pMachineModel, pTargetType);

    CompoundIntervalFormulaManager cifm =
        new CompoundIntervalFormulaManager(pCompoundIntervalManagerFactory);

    NumeralFormula<CompoundInterval> formula = cifm.cast(typeInfo, pFormula);

    CompoundIntervalManager cim =
        pCompoundIntervalManagerFactory.createCompoundIntervalManager(typeInfo);

    FormulaCompoundStateEvaluationVisitor evaluator =
        new FormulaCompoundStateEvaluationVisitor(pCompoundIntervalManagerFactory);
    CompoundInterval value = formula.accept(evaluator, pEnvironment);
    if (value instanceof CompoundIntegralInterval integralValue
        && typeInfo instanceof BitVectorInfo bitVectorInfo) {
      BigInteger lowerInclusiveBound = bitVectorInfo.getMinValue();
      BigInteger upperExclusiveBound = bitVectorInfo.getMaxValue().add(BigInteger.ONE);

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
        if (pCompoundIntervalManagerFactory
                instanceof
                CompoundBitVectorIntervalManagerFactory compoundBitVectorIntervalManagerFactory
            && !compoundBitVectorIntervalManagerFactory.isSignedWrapAroundAllowed()) {
          CompoundInterval ci = pFormula.accept(evaluator, pEnvironment);
          if (ci instanceof CompoundBitVectorInterval cbvi) {
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
    if (pExpression instanceof CExpression cExpression) {
      return makeCastFromArrayToPointerIfNecessary(cExpression, pTargetType);
    }
    return pExpression;
  }

  public static AExpression makeCastFromArrayToPointerIfNecessary(
      AExpression pExpression, Type pTargetType) {
    if (pExpression instanceof CExpression cExpression && pTargetType instanceof CType cType) {
      return makeCastFromArrayToPointerIfNecessary(cExpression, cType);
    }
    return pExpression;
  }

  public static ARightHandSide makeCastFromArrayToPointerIfNecessary(
      ARightHandSide pExpression, Type pTargetType) {
    if (pExpression instanceof CExpression cExpression && pTargetType instanceof CType cType) {
      return makeCastFromArrayToPointerIfNecessary(cExpression, cType);
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
    CPointerType pointerType = arrayType.asPointerType();

    return new CUnaryExpression(
        pArrayExpression.getFileLocation(), pointerType, pArrayExpression, UnaryOperator.AMPER);
  }
}
