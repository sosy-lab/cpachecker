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

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;

public enum PartialEvaluator implements InvariantsFormulaVisitor<CompoundState, InvariantsFormula<CompoundState>> {

  INSTANCE;

  @Override
  public InvariantsFormula<CompoundState> visit(Add<CompoundState> pAdd) {
    InvariantsFormula<CompoundState> summand1 = pAdd.getSummand1().accept(this);
    InvariantsFormula<CompoundState> summand2 = pAdd.getSummand2().accept(this);
    if (summand1 instanceof Constant<?> && summand2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) summand1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) summand2;
      CompoundState result = c1.getValue().add(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (summand1 == pAdd.getSummand1() && summand2 == pAdd.getSummand2()) {
      return pAdd;
    }
    return InvariantsFormulaManager.INSTANCE.add(summand1, summand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryAnd<CompoundState> pAnd) {
    InvariantsFormula<CompoundState> operand1 = pAnd.getOperand1().accept(this);
    InvariantsFormula<CompoundState> operand2 = pAnd.getOperand2().accept(this);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) operand1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) operand2;
      CompoundState result = c1.getValue().binaryAnd(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return InvariantsFormulaManager.INSTANCE.binaryAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryNot<CompoundState> pNot) {
    InvariantsFormula<CompoundState> operand = pNot.getFlipped().accept(this);
    if (operand instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) operand;
      CompoundState result = c.getValue().binaryNot();
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand == pNot.getFlipped()) {
      return pNot;
    }
    return InvariantsFormulaManager.INSTANCE.binaryNot(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryOr<CompoundState> pOr) {
    InvariantsFormula<CompoundState> operand1 = pOr.getOperand1().accept(this);
    InvariantsFormula<CompoundState> operand2 = pOr.getOperand2().accept(this);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) operand1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) operand2;
      CompoundState result = c1.getValue().binaryOr(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand1 == pOr.getOperand1() && operand2 == pOr.getOperand2()) {
      return pOr;
    }
    return InvariantsFormulaManager.INSTANCE.binaryOr(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryXor<CompoundState> pXor) {
    InvariantsFormula<CompoundState> operand1 = pXor.getOperand1().accept(this);
    InvariantsFormula<CompoundState> operand2 = pXor.getOperand2().accept(this);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) operand1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) operand2;
      CompoundState result = c1.getValue().binaryXor(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand1 == pXor.getOperand1() && operand2 == pXor.getOperand2()) {
      return pXor;
    }
    return InvariantsFormulaManager.INSTANCE.binaryXor(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Constant<CompoundState> pConstant) {
    return pConstant;
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Divide<CompoundState> pDivide) {
    InvariantsFormula<CompoundState> numerator = pDivide.getNumerator().accept(this);
    InvariantsFormula<CompoundState> denominator = pDivide.getDenominator().accept(this);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) numerator;
      Constant<CompoundState> c2 = (Constant<CompoundState>) denominator;
      CompoundState result = c1.getValue().divide(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (numerator == pDivide.getNumerator() && denominator == pDivide.getDenominator()) {
      return pDivide;
    }
    return InvariantsFormulaManager.INSTANCE.divide(numerator, denominator);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Equal<CompoundState> pEqual) {
    InvariantsFormula<CompoundState> operand1 = pEqual.getOperand1().accept(this);
    InvariantsFormula<CompoundState> operand2 = pEqual.getOperand2().accept(this);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) operand1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) operand2;
      CompoundState result = c1.getValue().logicalEquals(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand1 == pEqual.getOperand1() && operand2 == pEqual.getOperand2()) {
      return pEqual;
    }
    return InvariantsFormulaManager.INSTANCE.equal(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LessThan<CompoundState> pLessThan) {
    InvariantsFormula<CompoundState> operand1 = pLessThan.getOperand1().accept(this);
    InvariantsFormula<CompoundState> operand2 = pLessThan.getOperand2().accept(this);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) operand1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) operand2;
      CompoundState result = c1.getValue().lessThan(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand1 == pLessThan.getOperand1() && operand2 == pLessThan.getOperand2()) {
      return pLessThan;
    }
    return InvariantsFormulaManager.INSTANCE.lessThan(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LogicalAnd<CompoundState> pAnd) {
    InvariantsFormula<CompoundState> operand1 = pAnd.getOperand1().accept(this);
    InvariantsFormula<CompoundState> operand2 = pAnd.getOperand2().accept(this);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) operand1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) operand2;
      CompoundState result = c1.getValue().logicalAnd(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return InvariantsFormulaManager.INSTANCE.logicalAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LogicalNot<CompoundState> pNot) {
    InvariantsFormula<CompoundState> operand = pNot.getNegated().accept(this);
    if (operand instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) operand;
      CompoundState result = c.getValue().logicalNot();
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand == pNot.getNegated()) {
      return pNot;
    }
    return InvariantsFormulaManager.INSTANCE.logicalNot(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Modulo<CompoundState> pModulo) {
    InvariantsFormula<CompoundState> numerator = pModulo.getNumerator().accept(this);
    InvariantsFormula<CompoundState> denominator = pModulo.getDenominator().accept(this);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) numerator;
      Constant<CompoundState> c2 = (Constant<CompoundState>) denominator;
      CompoundState result = c1.getValue().modulo(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (numerator == pModulo.getNumerator() && denominator == pModulo.getDenominator()) {
      return pModulo;
    }
    return InvariantsFormulaManager.INSTANCE.modulo(numerator, denominator);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Multiply<CompoundState> pMultiply) {
    InvariantsFormula<CompoundState> factor1 = pMultiply.getFactor1().accept(this);
    InvariantsFormula<CompoundState> factor2 = pMultiply.getFactor2().accept(this);
    if (factor1 instanceof Constant<?> && factor2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) factor1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) factor2;
      CompoundState result = c1.getValue().multiply(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (factor1 == pMultiply.getFactor1() && factor2 == pMultiply.getFactor2()) {
      return pMultiply;
    }
    return InvariantsFormulaManager.INSTANCE.multiply(factor1, factor2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Negate<CompoundState> pNegate) {
    InvariantsFormula<CompoundState> operand = pNegate.getNegated().accept(this);
    if (operand instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) operand;
      CompoundState result = c.getValue().negate();
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand == pNegate.getNegated()) {
      return pNegate;
    }
    return InvariantsFormulaManager.INSTANCE.negate(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(ShiftLeft<CompoundState> pShiftLeft) {
    InvariantsFormula<CompoundState> shifted = pShiftLeft.getShifted().accept(this);
    InvariantsFormula<CompoundState> shiftDistance = pShiftLeft.getShiftDistance().accept(this);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) shifted;
      Constant<CompoundState> c2 = (Constant<CompoundState>) shiftDistance;
      CompoundState result = c1.getValue().shiftLeft(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (shifted == pShiftLeft.getShifted() && shiftDistance == pShiftLeft.getShiftDistance()) {
      return pShiftLeft;
    }
    return InvariantsFormulaManager.INSTANCE.shiftLeft(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(ShiftRight<CompoundState> pShiftRight) {
    InvariantsFormula<CompoundState> shifted = pShiftRight.getShifted().accept(this);
    InvariantsFormula<CompoundState> shiftDistance = pShiftRight.getShiftDistance().accept(this);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) shifted;
      Constant<CompoundState> c2 = (Constant<CompoundState>) shiftDistance;
      CompoundState result = c1.getValue().shiftRight(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (shifted == pShiftRight.getShifted() && shiftDistance == pShiftRight.getShiftDistance()) {
      return pShiftRight;
    }
    return InvariantsFormulaManager.INSTANCE.shiftRight(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Union<CompoundState> pUnion) {
    InvariantsFormula<CompoundState> operand1 = pUnion.getOperand1().accept(this);
    InvariantsFormula<CompoundState> operand2 = pUnion.getOperand2().accept(this);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      Constant<CompoundState> c1 = (Constant<CompoundState>) operand1;
      Constant<CompoundState> c2 = (Constant<CompoundState>) operand2;
      CompoundState result = c1.getValue().unionWith(c2.getValue());
      return InvariantsFormulaManager.INSTANCE.asConstant(result);
    }
    if (operand1 == pUnion.getOperand1() && operand2 == pUnion.getOperand2()) {
      return pUnion;
    }
    return InvariantsFormulaManager.INSTANCE.union(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Variable<CompoundState> pVariable) {
    return pVariable;
  }

}
