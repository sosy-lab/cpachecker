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

public class FormulaCompoundStateEvaluationVisitor implements FormulaEvaluationVisitor<CompoundState> {

  @Override
  public CompoundState visit(Add<CompoundState> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pAdd.getSummand1().accept(this, pEnvironment).add(pAdd.getSummand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(BinaryAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pAnd.getOperand1().accept(this, pEnvironment).binaryAnd(pAnd.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(BinaryNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pNot.getFlipped().accept(this, pEnvironment).binaryNot();
  }

  @Override
  public CompoundState visit(BinaryOr<CompoundState> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pOr.getOperand1().accept(this, pEnvironment).binaryOr(pOr.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(BinaryXor<CompoundState> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pXor.getOperand1().accept(this, pEnvironment).binaryXor(pXor.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Constant<CompoundState> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pConstant.getValue();
  }

  @Override
  public CompoundState visit(Divide<CompoundState> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pDivide.getNumerator().accept(this, pEnvironment).divide(pDivide.getDenominator().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Equal<CompoundState> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pEqual.getOperand1().accept(this, pEnvironment).logicalEquals(pEqual.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(LessThan<CompoundState> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pLessThan.getOperand1().accept(this, pEnvironment).lessThan(pLessThan.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(LogicalAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pAnd.getOperand1().accept(this, pEnvironment).logicalAnd(pAnd.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(LogicalNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pNot.getNegated().accept(this, pEnvironment).logicalNot();
  }

  @Override
  public CompoundState visit(Modulo<CompoundState> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pModulo.getNumerator().accept(this, pEnvironment).modulo(pModulo.getDenominator().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Multiply<CompoundState> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pMultiply.getFactor1().accept(this, pEnvironment).multiply(pMultiply.getFactor2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Negate<CompoundState> pNegate, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pNegate.accept(this, pEnvironment).negate();
  }

  @Override
  public CompoundState visit(ShiftLeft<CompoundState> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pShiftLeft.getShifted().accept(this, pEnvironment).shiftLeft(pShiftLeft.getShiftDistance().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(ShiftRight<CompoundState> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pShiftRight.getShifted().accept(this, pEnvironment).shiftRight(pShiftRight.getShiftDistance().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Union<CompoundState> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pUnion.getOperand1().accept(this, pEnvironment).unionWith(pUnion.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Variable<CompoundState> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    InvariantsFormula<CompoundState> varState = pEnvironment.get(pVariable.getName());
    if (varState == null) {
      return CompoundState.top();
    }
    return varState.accept(this, pEnvironment);
  }

  @Override
  public CompoundState top() {
    return CompoundState.top();
  }

  @Override
  public CompoundState bottom() {
    return CompoundState.bottom();
  }

}
