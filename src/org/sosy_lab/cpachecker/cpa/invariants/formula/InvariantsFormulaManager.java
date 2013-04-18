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




public enum InvariantsFormulaManager {

  INSTANCE;

  public <T> InvariantsFormula<T> add(InvariantsFormula<T> pSummand1, InvariantsFormula<T> pSummand2) {
    return Add.of(pSummand1, pSummand2);
  }

  public <T> InvariantsFormula<T> binaryAnd(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return BinaryAnd.of(pOperand1, pOperand2);
  }

  public <T> InvariantsFormula<T> binaryNot(InvariantsFormula<T> pToFlip) {
    return BinaryNot.of(pToFlip);
  }

  public <T> InvariantsFormula<T> binaryOr(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return BinaryOr.of(pOperand1, pOperand2);
  }

  public <T> InvariantsFormula<T> binaryXor(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return BinaryXor.of(pOperand1, pOperand2);
  }

  public <T> InvariantsFormula<T> asConstant(T pValue) {
    return Constant.of(pValue);
  }

  public <T> InvariantsFormula<T> divide(InvariantsFormula<T> pNumerator, InvariantsFormula<T> pDenominator) {
    return Divide.of(pNumerator, pDenominator);
  }

  public <T> InvariantsFormula<T> equal(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return Equal.of(pOperand1, pOperand2);
  }

  public <T> InvariantsFormula<T> greaterThan(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return lessThan(pOperand2, pOperand1);
  }

  public <T> InvariantsFormula<T> greaterThanOrEqual(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return logicalNot(lessThan(pOperand1, pOperand2));
  }

  public <T> InvariantsFormula<T> lessThan(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return LessThan.of(pOperand1, pOperand2);
  }

  public <T> InvariantsFormula<T> lessThanOrEqual(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return greaterThanOrEqual(pOperand2, pOperand1);
  }

  public <T> InvariantsFormula<T> logicalAnd(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return LogicalAnd.of(pOperand1, pOperand2);
  }

  public <T> InvariantsFormula<T> logicalNot(InvariantsFormula<T> pToNegate) {
    if (pToNegate instanceof Negate<?>) {
      return ((Negate<T>) pToNegate).getNegated();
    }
    return LogicalNot.of(pToNegate);
  }

  public <T> InvariantsFormula<T> logicalOr(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return logicalNot(LogicalAnd.of(logicalNot(pOperand1), logicalNot(pOperand2)));
  }

  public <T> InvariantsFormula<T> logicalImplies(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return logicalNot(LogicalAnd.of(pOperand1, logicalNot(pOperand2)));
  }

  public <T> InvariantsFormula<T> modulo(InvariantsFormula<T> pNumerator, InvariantsFormula<T> pDenominator) {
    return Modulo.of(pNumerator, pDenominator);
  }

  public <T> InvariantsFormula<T> multiply(InvariantsFormula<T> pFactor1, InvariantsFormula<T> pFactor2) {
    return Multiply.of(pFactor1, pFactor2);
  }

  public <T> Negate<T> negate(InvariantsFormula<T> pToNegate) {
    return Negate.of(pToNegate);
  }

  public <T> InvariantsFormula<T> subtract(InvariantsFormula<T> pMinuend, InvariantsFormula<T> pSubtrahend) {
    return Add.of(pMinuend, negate(pSubtrahend));
  }

  public <T> InvariantsFormula<T> shiftLeft(InvariantsFormula<T> pToShift, InvariantsFormula<T> pShiftDistance) {
    return ShiftLeft.of(pToShift, pShiftDistance);
  }

  public <T> InvariantsFormula<T> shiftRight(InvariantsFormula<T> pToShift, InvariantsFormula<T> pShiftDistance) {
    return ShiftRight.of(pToShift, pShiftDistance);
  }

  public <T> InvariantsFormula<T> union(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return Union.of(pOperand1, pOperand2);
  }

  public <T> InvariantsFormula<T> asVariable(String varName) {
    return Variable.of(varName);
  }

}
