// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;


public class FormulaDepthCountVisitor<ConstantType> implements NumeralFormulaVisitor<ConstantType, Integer>, BooleanFormulaVisitor<ConstantType, Integer> {

  @Override
  public Integer visit(Add<ConstantType> pAdd) {
    return Math.max(pAdd.getSummand1().accept(this), pAdd.getSummand2().accept(this)) + 1;
  }

  @Override
  public Integer visit(BinaryAnd<ConstantType> pAnd) {
    return Math.max(pAnd.getOperand1().accept(this), pAnd.getOperand2().accept(this)) + 1;
  }

  @Override
  public Integer visit(BinaryNot<ConstantType> pNot) {
    return pNot.getFlipped().accept(this) + 1;
  }

  @Override
  public Integer visit(BinaryOr<ConstantType> pOr) {
    return Math.max(pOr.getOperand1().accept(this), pOr.getOperand2().accept(this)) + 1;
  }

  @Override
  public Integer visit(BinaryXor<ConstantType> pXor) {
    return Math.max(pXor.getOperand1().accept(this), pXor.getOperand2().accept(this)) + 1;
  }

  @Override
  public Integer visit(Constant<ConstantType> pConstant) {
    return 1;
  }

  @Override
  public Integer visit(Divide<ConstantType> pDivide) {
    return Math.max(pDivide.getNumerator().accept(this), pDivide.getDenominator().accept(this)) + 1;
  }

  @Override
  public Integer visit(Equal<ConstantType> pEqual) {
    return Math.max(pEqual.getOperand1().accept(this), pEqual.getOperand2().accept(this)) + 1;
  }

  @Override
  public Integer visit(Exclusion<ConstantType> pExclusion) {
    return pExclusion.getExcluded().accept(this) + 1;
  }

  @Override
  public Integer visit(LessThan<ConstantType> pLessThan) {
    return Math.max(pLessThan.getOperand1().accept(this), pLessThan.getOperand2().accept(this)) + 1;
  }

  @Override
  public Integer visit(LogicalAnd<ConstantType> pAnd) {
    return Math.max(pAnd.getOperand1().accept(this), pAnd.getOperand2().accept(this)) + 1;
  }

  @Override
  public Integer visit(LogicalNot<ConstantType> pNot) {
    return pNot.getNegated().accept(this) + 1;
  }

  @Override
  public Integer visit(Modulo<ConstantType> pModulo) {
    return Math.max(pModulo.getNumerator().accept(this), pModulo.getDenominator().accept(this)) + 1;
  }

  @Override
  public Integer visit(Multiply<ConstantType> pMultiply) {
    return Math.max(pMultiply.getFactor1().accept(this), pMultiply.getFactor2().accept(this)) + 1;
  }

  @Override
  public Integer visit(ShiftLeft<ConstantType> pShiftLeft) {
    return Math.max(pShiftLeft.getShifted().accept(this), pShiftLeft.getShiftDistance().accept(this)) + 1;
  }

  @Override
  public Integer visit(ShiftRight<ConstantType> pShiftRight) {
    return Math.max(pShiftRight.getShifted().accept(this), pShiftRight.getShiftDistance().accept(this)) + 1;
  }

  @Override
  public Integer visit(Union<ConstantType> pUnion) {
    return Math.max(pUnion.getOperand1().accept(this), pUnion.getOperand2().accept(this)) + 1;
  }

  @Override
  public Integer visit(Variable<ConstantType> pVariable) {
    return 1;
  }

  @Override
  public Integer visitFalse() {
    return 1;
  }

  @Override
  public Integer visitTrue() {
    return 1;
  }

  @Override
  public Integer visit(IfThenElse<ConstantType> pIfThenElse) {
    return Math.max(
        pIfThenElse.getCondition().accept(this),
        Math.max(
            pIfThenElse.getPositiveCase().accept(this),
            pIfThenElse.getNegativeCase().accept(this))
        ) + 1;
  }

  @Override
  public Integer visit(Cast<ConstantType> pCast) {
    return pCast.getCasted().accept(this) + 1;
  }

}
