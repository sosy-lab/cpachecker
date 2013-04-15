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



public class ContainsVarVisitor<T> implements InvariantsFormulaVisitor<T, Boolean> {

  private final String varName;

  public ContainsVarVisitor(String pVarName) {
    this.varName = pVarName;
  }

  @Override
  public Boolean visit(Add<T> pAdd) {
    return pAdd.getSummand1().accept(this) || pAdd.getSummand2().accept(this);
  }

  @Override
  public Boolean visit(BinaryAnd<T> pAnd) {
    return pAnd.getOperand1().accept(this) || pAnd.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(BinaryNot<T> pNot) {
    return pNot.getFlipped().accept(this);
  }

  @Override
  public Boolean visit(BinaryOr<T> pOr) {
    return pOr.getOperand1().accept(this) || pOr.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(BinaryXor<T> pXor) {
    return pXor.getOperand1().accept(this) || pXor.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(Constant<T> pConstant) {
    return false;
  }

  @Override
  public Boolean visit(Divide<T> pDivide) {
    return pDivide.getNumerator().accept(this) || pDivide.getDenominator().accept(this);
  }

  @Override
  public Boolean visit(Equal<T> pEqual) {
    return pEqual.getOperand1().accept(this) || pEqual.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan) {
    return pLessThan.getOperand1().accept(this) || pLessThan.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(LogicalAnd<T> pAnd) {
    return pAnd.getOperand1().accept(this) || pAnd.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(LogicalNot<T> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Boolean visit(Modulo<T> pModulo) {
    return pModulo.getNumerator().accept(this) || pModulo.getDenominator().accept(this);
  }

  @Override
  public Boolean visit(Multiply<T> pMultiply) {
    return pMultiply.getFactor1().accept(this) || pMultiply.getFactor2().accept(this);
  }

  @Override
  public Boolean visit(Negate<T> pNegate) {
    return pNegate.getNegated().accept(this);
  }

  @Override
  public Boolean visit(ShiftLeft<T> pShiftLeft) {
    return pShiftLeft.getShifted().accept(this) || pShiftLeft.getShiftDistance().accept(this);
  }

  @Override
  public Boolean visit(ShiftRight<T> pShiftRight) {
    return pShiftRight.getShifted().accept(this) || pShiftRight.getShiftDistance().accept(this);
  }

  @Override
  public Boolean visit(Union<T> pUnion) {
    return pUnion.getOperand1().accept(this) || pUnion.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(Variable<T> pVariable) {
    return pVariable.getName().equals(this.varName);
  }

}
