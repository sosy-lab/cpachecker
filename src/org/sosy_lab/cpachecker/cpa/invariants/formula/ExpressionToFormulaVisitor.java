/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class ExpressionToFormulaVisitor extends DefaultCExpressionVisitor<InvariantsFormula<CompoundState>, UnrecognizedCCodeException> implements CRightHandSideVisitor<InvariantsFormula<CompoundState>, UnrecognizedCCodeException> {

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

  private static final InvariantsFormula<CompoundState> BOTTOM = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.bottom());

  private final VariableNameExtractor variableNameExtractor;

  public ExpressionToFormulaVisitor(VariableNameExtractor pVariableNameExtractor) {
    this.variableNameExtractor = pVariableNameExtractor;
  }

  @Override
  protected InvariantsFormula<CompoundState> visitDefault(CExpression pExp) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<CompoundState> visit(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {
    return InvariantsFormulaManager.INSTANCE.asVariable(this.variableNameExtractor.extract(pCIdExpression));
  }

  @Override
  public InvariantsFormula<CompoundState> visit(CIntegerLiteralExpression pE) {
    return InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.singleton(pE.getValue()));
  }

  @Override
  public InvariantsFormula<CompoundState> visit(CCharLiteralExpression pE) {
    return InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.singleton(pE.getCharacter()));
  }

  @Override
  public InvariantsFormula<CompoundState> visit(CUnaryExpression pCUnaryExpression) throws UnrecognizedCCodeException {
    switch (pCUnaryExpression.getOperator()) {
    case MINUS:
      return InvariantsFormulaManager.INSTANCE.negate(pCUnaryExpression.getOperand().accept(this));
    case NOT:
      return InvariantsFormulaManager.INSTANCE.logicalNot(pCUnaryExpression.getOperand().accept(this));
    case PLUS:
      return pCUnaryExpression.getOperand().accept(this);
    case TILDE:
      return InvariantsFormulaManager.INSTANCE.binaryNot(pCUnaryExpression.getOperand().accept(this));
    default:
      return super.visit(pCUnaryExpression);
    }
  }

  @Override
  public InvariantsFormula<CompoundState> visit(CBinaryExpression pCBinaryExpression) throws UnrecognizedCCodeException {
    InvariantsFormula<CompoundState> left = pCBinaryExpression.getOperand1().accept(this);
    InvariantsFormula<CompoundState> right = pCBinaryExpression.getOperand2().accept(this);
    InvariantsFormulaManager fmgr = InvariantsFormulaManager.INSTANCE;
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
      assert allowedOperators.contains(pCBinaryExpression.getOperator())
          : ("Unexpected operator: " + pCBinaryExpression.getOperator());
      return BOTTOM;
    }
  }

  @Override
  public InvariantsFormula<CompoundState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCCodeException {
    return null;
  }

  public interface VariableNameExtractor {

    String extract(CIdExpression pCIdExpression) throws UnrecognizedCCodeException;

  }
}
