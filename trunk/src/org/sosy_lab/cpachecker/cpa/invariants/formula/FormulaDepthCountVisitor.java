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


public class FormulaDepthCountVisitor<ConstantType> implements InvariantsFormulaVisitor<ConstantType, Integer> {

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

}
