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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * Instances of this class are c expression visitors used to convert c
 * expressions to compound state invariants formulae.
 */
public class ExpressionToFormulaVisitor extends DefaultCExpressionVisitor<InvariantsFormula<CompoundInterval>, UnrecognizedCCodeException> implements CRightHandSideVisitor<InvariantsFormula<CompoundInterval>, UnrecognizedCCodeException> {

  /**
   * The set of allowed operators. Logical AND and logical OR are not allowed
   * because they are deprecated.
   */
  private static final List<CBinaryExpression.BinaryOperator> allowedOperators = Arrays.asList(
    BinaryOperator.BINARY_AND,
    BinaryOperator.BINARY_OR,
    BinaryOperator.BINARY_XOR,
    BinaryOperator.DIVIDE,
    BinaryOperator.EQUALS,
    BinaryOperator.GREATER_EQUAL,
    BinaryOperator.GREATER_THAN,
    BinaryOperator.LESS_EQUAL,
    BinaryOperator.LESS_THAN,
    BinaryOperator.MINUS,
    BinaryOperator.MODULO,
    BinaryOperator.MULTIPLY,
    BinaryOperator.NOT_EQUALS,
    BinaryOperator.PLUS,
    BinaryOperator.SHIFT_LEFT,
    BinaryOperator.SHIFT_RIGHT
  );

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
   * @param pEnvironment the current environment information.
   */
  public ExpressionToFormulaVisitor(VariableNameExtractor pVariableNameExtractor, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    this.variableNameExtractor = pVariableNameExtractor;
    this.environment = pEnvironment;
  }

  @Override
  protected InvariantsFormula<CompoundInterval> visitDefault(CExpression pExp) throws UnrecognizedCCodeException {
    return TOP;
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.extract(pCIdExpression));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CFieldReference pCFieldReference) throws UnrecognizedCCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.extract(pCFieldReference));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CArraySubscriptExpression pCArraySubscriptExpression) throws UnrecognizedCCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.extract(pCArraySubscriptExpression));
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
  public InvariantsFormula<CompoundInterval> visit(CImaginaryLiteralExpression pE) throws UnrecognizedCCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CUnaryExpression pCUnaryExpression) throws UnrecognizedCCodeException {
    switch (pCUnaryExpression.getOperator()) {
    case MINUS:
      return CompoundIntervalFormulaManager.INSTANCE.negate(pCUnaryExpression.getOperand().accept(this));
    case TILDE:
      return CompoundIntervalFormulaManager.INSTANCE.binaryNot(pCUnaryExpression.getOperand().accept(this));
    default:
      return super.visit(pCUnaryExpression);
    }
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CPointerExpression pCPointerExpression) throws UnrecognizedCCodeException {
    return CompoundIntervalFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.extract(pCPointerExpression));
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CCastExpression pCCastExpression) throws UnrecognizedCCodeException {
    return pCCastExpression.getOperand().accept(this);
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CBinaryExpression pCBinaryExpression) throws UnrecognizedCCodeException {
    CompoundIntervalFormulaManager fmgr = CompoundIntervalFormulaManager.INSTANCE;
    InvariantsFormula<CompoundInterval> left = pCBinaryExpression.getOperand1().accept(this);
    InvariantsFormula<CompoundInterval> right = pCBinaryExpression.getOperand2().accept(this);
    left = topIfProblematicType(pCBinaryExpression.getCalculationType(), left);
    right = topIfProblematicType(pCBinaryExpression.getCalculationType(), right);
    switch (pCBinaryExpression.getOperator()) {
    case BINARY_AND:
      return fmgr.binaryAnd(left, right);
    case BINARY_OR:
      return fmgr.binaryOr(left, right);
    case BINARY_XOR:
      return fmgr.binaryXor(left, right);
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
      /*
       * While invariants formulae support logical AND and logical NOT and thus
       * also logical OR, logical AND and logical OR are deprecated c binary
       * operators in CPAchecker.
       */
      assert allowedOperators.contains(pCBinaryExpression.getOperator())
          : ("Unexpected operator: " + pCBinaryExpression.getOperator());
      return TOP;
    }
  }

  @Override
  public InvariantsFormula<CompoundInterval> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCCodeException {
    return TOP;
  }

  /**
   * Instances of implementing classes are used to obtain variable names for c
   * id expressions.
   */
  public interface VariableNameExtractor {

    /**
     * Provides a variable name for the given c expression.
     *
     * @param pCExpression the c id expression to provide a variable name
     * for.
     *
     * @return the variable name for the given c id expression.
     * @throws UnrecognizedCCodeException if the extraction process cannot be
     * completed because involved c code is unrecognized.
     */
    String extract(CExpression pCExpression) throws UnrecognizedCCodeException;

  }

  private InvariantsFormula<CompoundInterval> topIfProblematicType(CType pType, InvariantsFormula<CompoundInterval> pFormula) {
    if ((pType instanceof CSimpleType) && ((CSimpleType) pType).isUnsigned()) {
      CompoundInterval value =  pFormula.accept(evaluationVisitor, environment);
      if (value.containsNegative()) {
        return TOP;
      }
    }
    return pFormula;
  }
}
