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

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
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
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.VariableNameExtractor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Instances of this class are c expression visitors used to convert c
 * expressions to compound state invariants formulae.
 */
public class ExpressionToFormulaVisitor extends DefaultCExpressionVisitor<InvariantsFormula<CompoundInterval>, UnrecognizedCodeException> implements CRightHandSideVisitor<InvariantsFormula<CompoundInterval>, UnrecognizedCodeException>, JRightHandSideVisitor<InvariantsFormula<CompoundInterval>, UnrecognizedCodeException> {

  /**
   * The compound state invariants formula representing the top state.
   */
  private static final InvariantsFormula<CompoundInterval> TOP =
      CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.top());

  /**
   * The variable name extractor used to extract variable names from c id
   * expressions.
   */
  private final VariableNameExtractor variableNameExtractor;

  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor = new FormulaCompoundStateEvaluationVisitor();

  private final Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> environment;

  /**
   * Creates a new visitor for converting c expressions to compound state
   * invariants formulae with the given variable name extractor.
   *
   * @param pVariableNameExtractor the variable name extractor used to obtain
   * variable names for c id expressions.
   */
  public ExpressionToFormulaVisitor(VariableNameExtractor pVariableNameExtractor) {
    this(pVariableNameExtractor, Collections.<String, InvariantsFormula<CompoundInterval>>emptyMap());
  }

  /**
   * Creates a new visitor for converting c expressions to compound state
   * invariants formulae with the given variable name extractor.
   *
   * @param pVariableNameExtractor the variable name extractor used to obtain
   * variable names for c id expressions.
   * @param pEnvironment the current environment information.
   */
  public ExpressionToFormulaVisitor(VariableNameExtractor pVariableNameExtractor, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    this.variableNameExtractor = pVariableNameExtractor;
    this.environment = pEnvironment;
  }

  @Override
  protected InvariantsFormula<CompoundInterval> visitDefault(CExpression pExp) throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CIdExpression pCIdExpression) throws UnrecognizedCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.getVarName(pCIdExpression));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CFieldReference pCFieldReference) throws UnrecognizedCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.getVarName(pCFieldReference));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CArraySubscriptExpression pCArraySubscriptExpression) throws UnrecognizedCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.getVarName(pCArraySubscriptExpression));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CIntegerLiteralExpression pE) {
    return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.singleton(pE.getValue()));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CCharLiteralExpression pE) {
    return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.singleton(pE.getCharacter()));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CImaginaryLiteralExpression pE) throws UnrecognizedCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CUnaryExpression pCUnaryExpression) throws UnrecognizedCodeException {
    switch (pCUnaryExpression.getOperator()) {
    case MINUS:
      return CompoundIntervalFormulaManager.INSTANCE.negate(pCUnaryExpression.getOperand().accept(this));
    case TILDE:
      return TOP;
    default:
      return super.visit(pCUnaryExpression);
    }
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CPointerExpression pCPointerExpression) throws UnrecognizedCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.getVarName(pCPointerExpression));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CCastExpression pCCastExpression) throws UnrecognizedCodeException {
    return pCCastExpression.getOperand().accept(this);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CBinaryExpression pCBinaryExpression) throws UnrecognizedCodeException {
    CompoundIntervalFormulaManager fmgr = CompoundIntervalFormulaManager.INSTANCE;
    InvariantsFormula<CompoundInterval> left = pCBinaryExpression.getOperand1().accept(this);
    InvariantsFormula<CompoundInterval> right = pCBinaryExpression.getOperand2().accept(this);
    left = topIfProblematicType(pCBinaryExpression.getCalculationType(), left);
    right = topIfProblematicType(pCBinaryExpression.getCalculationType(), right);
    switch (pCBinaryExpression.getOperator()) {
    case BINARY_AND:
      return TOP;
    case BINARY_OR:
      return TOP;
    case BINARY_XOR:
      return TOP;
    case DIVIDE:
      return fmgr.divide(left, right);
    case EQUALS:
      return fmgr.equal(left, right);
    case GREATER_EQUAL:
      return fmgr.greaterThanOrEqual(left, right);
    case GREATER_THAN:
      return fmgr.greaterThan(left, right);
    case LESS_EQUAL:
      return fmgr.lessThanOrEqual(left, right);
    case LESS_THAN:
      return fmgr.lessThan(left, right);
    case MINUS:
      return fmgr.subtract(left, right);
    case MODULO:
      return fmgr.modulo(left, right);
    case MULTIPLY:
      return fmgr.multiply(left, right);
    case NOT_EQUALS:
      return fmgr.logicalNot(fmgr.equal(left, right));
    case PLUS:
      return fmgr.add(left, right);
    case SHIFT_LEFT:
      return fmgr.shiftLeft(left, right);
    case SHIFT_RIGHT:
      return fmgr.shiftRight(left, right);
    default:
      return TOP;
    }
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CFunctionCallExpression pIastFunctionCallExpression) {
    return TOP;
  }

  private InvariantsFormula<CompoundInterval> topIfProblematicType(CType pType, InvariantsFormula<CompoundInterval> pFormula) {
    if ((pType instanceof CSimpleType) && ((CSimpleType) pType).getCanonicalType().isUnsigned()) {
      CompoundInterval value = pFormula.accept(evaluationVisitor, environment);
      if (value.isTop()) {
        return pFormula;
      }
      if (value.containsNegative()) {
        return TOP;
      }
    }
    return pFormula;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JCharLiteralExpression pCharLiteralExpression)
      throws UnrecognizedCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asConstant(
        CompoundInterval.singleton(pCharLiteralExpression.getCharacter()));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JStringLiteralExpression pStringLiteralExpression)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JBinaryExpression pBinaryExpression)
      throws UnrecognizedCodeException {
    CompoundIntervalFormulaManager fmgr = CompoundIntervalFormulaManager.INSTANCE;
    InvariantsFormula<CompoundInterval> left = pBinaryExpression.getOperand1().accept(this);
    InvariantsFormula<CompoundInterval> right = pBinaryExpression.getOperand2().accept(this);
    switch (pBinaryExpression.getOperator()) {
      case BINARY_AND:
        return TOP;
      case BINARY_OR:
        return TOP;
      case BINARY_XOR:
        return TOP;
      case CONDITIONAL_AND:
        return TOP;
      case CONDITIONAL_OR:
        return TOP;
      case DIVIDE:
        return fmgr.divide(left, right);
      case EQUALS:
        return fmgr.equal(left, right);
      case GREATER_EQUAL:
        return fmgr.greaterThanOrEqual(left, right);
      case GREATER_THAN:
        return fmgr.greaterThan(left, right);
      case LESS_EQUAL:
        return fmgr.lessThan(left, right);
      case LESS_THAN:
        return fmgr.lessThanOrEqual(left, right);
      case LOGICAL_AND:
        return fmgr.logicalAnd(left, right);
      case LOGICAL_OR:
        return fmgr.logicalOr(left, right);
      case LOGICAL_XOR:
        return fmgr.logicalOr(
            fmgr.logicalAnd(left, fmgr.logicalNot(right)),
            fmgr.logicalAnd(fmgr.logicalNot(left), right));
      case MINUS:
        return fmgr.subtract(left, right);
      case MODULO:
        return fmgr.modulo(left, right);
      case MULTIPLY:
        return fmgr.multiply(left, right);
      case NOT_EQUALS:
        return fmgr.logicalNot(fmgr.equal(left, right));
      case PLUS:
        return fmgr.add(left, right);
      case SHIFT_LEFT:
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        return fmgr.shiftLeft(left, right);
      case SHIFT_RIGHT_SIGNED:
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        return fmgr.shiftRight(left, right);
      case SHIFT_RIGHT_UNSIGNED:
        right = truncateShiftOperand(pBinaryExpression.getExpressionType(), right);
        CompoundInterval leftEval = left.accept(evaluationVisitor, environment);
        InvariantsFormula<CompoundInterval> forPositiveLeft = fmgr.shiftRight(left, right);
        if (!leftEval.containsNegative()) {
          return forPositiveLeft;
        }
        InvariantsFormula<CompoundInterval> forNegativeLeft =
            fmgr.add(forPositiveLeft,
                fmgr.shiftLeft(
                    fmgr.asConstant(CompoundInterval.singleton(2)),
                    fmgr.binaryNot(right)));
        if (!leftEval.containsPositive()) {
          return forNegativeLeft;
        }
        return fmgr.union(forPositiveLeft, forNegativeLeft);
      case STRING_CONCATENATION:
        return TOP;
    }
    return TOP;
  }

  private InvariantsFormula<CompoundInterval> truncateShiftOperand(JType pExpressionType, InvariantsFormula<CompoundInterval> pOperand) {
    CompoundIntervalFormulaManager fmgr = CompoundIntervalFormulaManager.INSTANCE;
    if (pExpressionType instanceof JSimpleType) {
      JSimpleType simpleType = (JSimpleType) pExpressionType;
      if (simpleType.getType() == JBasicType.INT) {
        return fmgr.binaryAnd(pOperand, fmgr.asConstant(CompoundInterval.singleton(0x1F)));
      } else if (simpleType.getType() == JBasicType.LONG) {
        return fmgr.binaryAnd(pOperand, fmgr.asConstant(CompoundInterval.singleton(0x3F)));
      }
    }
    return pOperand;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JUnaryExpression pUnaryExpression) throws UnrecognizedCodeException {
    switch (pUnaryExpression.getOperator()) {
    case MINUS:
      return CompoundIntervalFormulaManager.INSTANCE.negate(pUnaryExpression.getOperand().accept(this));
    case COMPLEMENT:
      return TOP;
    case NOT:
      return CompoundIntervalFormulaManager.INSTANCE.logicalNot(pUnaryExpression.getOperand().accept(this));
    case PLUS:
      return pUnaryExpression.getOperand().accept(this);
    default:
      return TOP;
    }
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JIntegerLiteralExpression pIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.singleton(pIntegerLiteralExpression.getValue()));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JBooleanLiteralExpression pBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.fromBoolean(pBooleanLiteralExpression.getValue()));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JFloatLiteralExpression pBooleanLiteralExpression)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JArrayCreationExpression pArrayCreationExpression)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JArrayInitializer pArrayInitializer)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JArrayLengthExpression pArrayLengthExpression) {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JVariableRunTimeType pThisRunTimeType)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JRunTimeTypeEqualsType pRunTimeTypeEqualsType)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JNullLiteralExpression pNullLiteralExpression)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JEnumConstantExpression pEnumConstantExpression)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JCastExpression pCastExpression) throws UnrecognizedCodeException {
    return pCastExpression.getOperand().accept(this);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JThisExpression pThisExpression) throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JArraySubscriptExpression pArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JIdExpression pIdExpression) throws UnrecognizedCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(variableNameExtractor.getVarName(pIdExpression));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JMethodInvocationExpression pFunctionCallExpression)
      throws UnrecognizedCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(JClassInstanceCreation pClassInstanceCreation)
      throws UnrecognizedCodeException {
    return TOP;
  }

  public static InvariantsFormula<CompoundInterval> handlePotentialOverflow(
      InvariantsFormula<CompoundInterval> pFormula,
      MachineModel pMachineModel,
      Type pTargetType,
      Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundIntervalFormulaManager fm = CompoundIntervalFormulaManager.INSTANCE;
    final boolean isSigned;
    final int bitLength;

    Type type = pTargetType;
    if (type instanceof CType) {
      type = ((CType) type).getCanonicalType();
    }

    if (type instanceof CSimpleType) {
      CSimpleType targetType = ((CSimpleType) type).getCanonicalType();
      isSigned = pMachineModel.isSigned(targetType);
      bitLength = pMachineModel.getSizeof(targetType) * pMachineModel.getSizeofCharInBits();
    } else if (type instanceof CType) {
      isSigned = false;
      bitLength = pMachineModel.getSizeof((CType) type) * pMachineModel.getSizeofCharInBits();
    } else {
      // TODO java types
      return TOP;
    }

    BigInteger lowerInclusiveBound = BigInteger.ZERO;
    BigInteger upperExclusiveBound = BigInteger.ONE.shiftLeft(bitLength);

    CompoundInterval value = pFormula.accept(new FormulaCompoundStateEvaluationVisitor(), pEnvironment);

    if (isSigned) {
      upperExclusiveBound = upperExclusiveBound.shiftRight(1);
      lowerInclusiveBound = upperExclusiveBound.negate();
      if (!value.hasLowerBound() || !value.hasUpperBound()) {
        return TOP;
      }
      if (value.getLowerBound().compareTo(lowerInclusiveBound) < 0) {
        return TOP;
      }
      if (value.getUpperBound().compareTo(upperExclusiveBound) >= 0) {
        return TOP;
      }
      return pFormula;
    }

    assert lowerInclusiveBound.compareTo(upperExclusiveBound) < 0;

    if (!value.hasLowerBound()) {
      return TOP;
    }

    if (value.getLowerBound().compareTo(lowerInclusiveBound) >= 0
        && value.hasUpperBound()
        && value.getUpperBound().compareTo(upperExclusiveBound) < 0) {
      return pFormula;
    }

    CompoundInterval negativePart = value.intersectWith(CompoundInterval.one().negate().extendToNegativeInfinity());
    CompoundInterval negativePartMod = negativePart.modulo(upperExclusiveBound);
    CompoundInterval negativePartResult = CompoundInterval.singleton(upperExclusiveBound).add(negativePartMod);

    CompoundInterval nonNegativePart = value.intersectWith(CompoundInterval.zero().extendToPositiveInfinity());
    CompoundInterval nonNegativePartResult = nonNegativePart.modulo(upperExclusiveBound);

    return fm.asConstant(negativePartResult.unionWith(nonNegativePartResult));
  }

}
