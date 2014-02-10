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

import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;


public class CanExtractVariableRelationVisitor implements ParameterizedInvariantsFormulaVisitor<CompoundInterval, FormulaEvaluationVisitor<CompoundInterval>, Boolean> {

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  private final Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> environment;

  public CanExtractVariableRelationVisitor(Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    this.environment = pEnvironment;
  }

  @Override
  public Boolean visit(Add<CompoundInterval> pAdd, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pAdd.getSummand1().accept(this, pParameter) && pAdd.getSummand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(BinaryAnd<CompoundInterval> pAnd, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pAnd.getOperand1().accept(COLLECT_VARS_VISITOR).isEmpty() && pAnd.getOperand2().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(BinaryNot<CompoundInterval> pNot, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pNot.getFlipped().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(BinaryOr<CompoundInterval> pOr, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pOr.getOperand1().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(BinaryXor<CompoundInterval> pXor, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pXor.getOperand1().accept(COLLECT_VARS_VISITOR).isEmpty() && pXor.getOperand2().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(Constant<CompoundInterval> pConstant, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return true;
  }

  @Override
  public Boolean visit(Divide<CompoundInterval> pDivide, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pDivide.getNumerator().accept(COLLECT_VARS_VISITOR).isEmpty()
        && pDivide.getNumerator().accept(this, pParameter) && pDivide.getDenominator().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Equal<CompoundInterval> pEqual, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pEqual.getOperand1().accept(this, pParameter) && pEqual.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(LessThan<CompoundInterval> pLessThan, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pLessThan.getOperand1().accept(this, pParameter) && pLessThan.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundInterval> pAnd, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pAnd.getOperand1().accept(this, pParameter) && pAnd.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(LogicalNot<CompoundInterval> pNot, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pNot.getNegated().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Modulo<CompoundInterval> pModulo, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pModulo.getNumerator().accept(COLLECT_VARS_VISITOR).isEmpty()
        && pModulo.getNumerator().accept(this, pParameter) && pModulo.getDenominator().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Multiply<CompoundInterval> pMultiply, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pMultiply.getFactor1().accept(COLLECT_VARS_VISITOR).isEmpty()
        && pMultiply.getFactor2().accept(COLLECT_VARS_VISITOR).isEmpty();
  }

  @Override
  public Boolean visit(ShiftLeft<CompoundInterval> pShiftLeft, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pShiftLeft.getShifted().accept(this, pParameter) && pShiftLeft.getShiftDistance().accept(this, pParameter);
  }

  @Override
  public Boolean visit(ShiftRight<CompoundInterval> pShiftRight, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pShiftRight.getShifted().accept(COLLECT_VARS_VISITOR).isEmpty() && pShiftRight.getShiftDistance().accept(COLLECT_VARS_VISITOR).isEmpty() || !pShiftRight.getShiftDistance().accept(pParameter, this.environment).containsPositive();
  }

  @Override
  public Boolean visit(Union<CompoundInterval> pUnion, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return pUnion.getOperand1().accept(this, pParameter) && pUnion.getOperand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(Variable<CompoundInterval> pVariable, FormulaEvaluationVisitor<CompoundInterval> pParameter) {
    return true;
  }

}
