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

import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;


public class CanExtractVariableRelationVisitor implements ParameterizedInvariantsFormulaVisitor<CompoundState, FormulaEvaluationVisitor<CompoundState>, Boolean> {

  private static final CollectVarsVisitor<CompoundState> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private final Map<? extends String, ? extends InvariantsFormula<CompoundState>> environment;

  public CanExtractVariableRelationVisitor(Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    this.environment = pEnvironment;
  }

  @Override
  public Boolean visit(Add<CompoundState> pAdd, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pAdd.getSummand1().accept(this, pParameter) && pAdd.getSummand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(BinaryAnd<CompoundState> pAnd, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pAnd.getOperand1().accept(COLLECT_VARS_VISITOR).isEmpty() && pAnd.getOperand2().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(BinaryNot<CompoundState> pNot, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pNot.getFlipped().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(BinaryOr<CompoundState> pOr, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pOr.getOperand1().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(BinaryXor<CompoundState> pXor, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pXor.getOperand1().accept(COLLECT_VARS_VISITOR).isEmpty() && pXor.getOperand2().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(Constant<CompoundState> pConstant, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return true;
  }

  @Override
  public Boolean visit(Divide<CompoundState> pDivide, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pDivide.getNumerator().accept(this, pParameter) && pDivide.getDenominator().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Equal<CompoundState> pEqual, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pEqual.getOperand1().accept(this, pParameter) && pEqual.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(LessThan<CompoundState> pLessThan, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pLessThan.getOperand1().accept(this, pParameter) && pLessThan.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundState> pAnd, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pAnd.getOperand1().accept(this, pParameter) && pAnd.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(LogicalNot<CompoundState> pNot, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pNot.getNegated().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Modulo<CompoundState> pModulo, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pModulo.getNumerator().accept(this, pParameter) && pModulo.getDenominator().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Multiply<CompoundState> pMultiply, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pMultiply.getFactor1().accept(this, pParameter) && pMultiply.getFactor2().accept(this, pParameter)
        && (pMultiply.getFactor1().accept(COLLECT_VARS_VISITOR).isEmpty() || !pMultiply.getFactor2().accept(pParameter, this.environment).containsZero())
        && (pMultiply.getFactor2().accept(COLLECT_VARS_VISITOR).isEmpty() || !pMultiply.getFactor1().accept(pParameter, this.environment).containsZero());
  }

  @Override
  public Boolean visit(Negate<CompoundState> pNegate, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pNegate.getNegated().accept(this, pParameter);
  }

  @Override
  public Boolean visit(ShiftLeft<CompoundState> pShiftLeft, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pShiftLeft.getShifted().accept(this, pParameter) && pShiftLeft.getShiftDistance().accept(this, pParameter);
  }

  @Override
  public Boolean visit(ShiftRight<CompoundState> pShiftRight, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pShiftRight.getShifted().accept(COLLECT_VARS_VISITOR).isEmpty() && pShiftRight.getShiftDistance().accept(COLLECT_VARS_VISITOR).isEmpty() || !pShiftRight.getShiftDistance().accept(pParameter, this.environment).containsPositive();
  }

  @Override
  public Boolean visit(Union<CompoundState> pUnion, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return pUnion.getOperand1().accept(this, pParameter) && pUnion.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Variable<CompoundState> pVariable, FormulaEvaluationVisitor<CompoundState> pParameter) {
    return true;
  }

}
