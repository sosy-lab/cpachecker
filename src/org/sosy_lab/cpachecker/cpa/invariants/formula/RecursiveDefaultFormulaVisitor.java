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

/**
 * Instances of extending classes traverse a structure of invariants formula
 * in post order.
 *
 * @param <T>
 */
public abstract class RecursiveDefaultFormulaVisitor<T> implements InvariantsFormulaVisitor<T, InvariantsFormula<T>> {

  /**
   * Visits the (possibly modified) formula after its child formulae were
   * visited by this visitor.
   *
   * @param pFormula the formula to visit.
   * @return the (possible modified) visited formula.
   */
  protected abstract InvariantsFormula<T> visitPost(InvariantsFormula<T> pFormula);

  @Override
  public InvariantsFormula<T> visit(Add<T> pAdd) {
    InvariantsFormula<T> summand1 = pAdd.getSummand1().accept(this);
    InvariantsFormula<T> summand2 = pAdd.getSummand2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (summand1 == pAdd.getSummand1() && summand2 == pAdd.getSummand2()) {
      toVisit = pAdd;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.add(summand1, summand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryAnd<T> pAnd) {
    InvariantsFormula<T> operand1 = pAnd.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pAnd.getOperand2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      toVisit = pAnd;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.binaryAnd(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryNot<T> pNot) {
    InvariantsFormula<T> operand = pNot.getFlipped().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand == pNot.getFlipped()) {
      toVisit = pNot;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.binaryNot(operand);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryOr<T> pOr) {
    InvariantsFormula<T> operand1 = pOr.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pOr.getOperand2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand1 == pOr.getOperand1() && operand2 == pOr.getOperand2()) {
      toVisit = pOr;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.binaryOr(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryXor<T> pXor) {
    InvariantsFormula<T> operand1 = pXor.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pXor.getOperand2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand1 == pXor.getOperand1() && operand2 == pXor.getOperand2()) {
      toVisit = pXor;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.binaryXor(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(Constant<T> pConstant) {
    return visitPost(pConstant);
  }

  @Override
  public InvariantsFormula<T> visit(Divide<T> pDivide) {
    InvariantsFormula<T> numerator = pDivide.getNumerator().accept(this);
    InvariantsFormula<T> denominator = pDivide.getDenominator().accept(this);
    final InvariantsFormula<T> toVisit;
    if (numerator == pDivide.getNumerator() && denominator == pDivide.getDenominator()) {
      toVisit = pDivide;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.divide(numerator, denominator);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(Equal<T> pEqual) {
    InvariantsFormula<T> operand1 = pEqual.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pEqual.getOperand2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand1 == pEqual.getOperand1() && operand2 == pEqual.getOperand2()) {
      toVisit = pEqual;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.equal(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(LessThan<T> pLessThan) {
    InvariantsFormula<T> operand1 = pLessThan.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pLessThan.getOperand2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand1 == pLessThan.getOperand1() && operand2 == pLessThan.getOperand2()) {
      toVisit = pLessThan;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.lessThan(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(LogicalAnd<T> pAnd) {
    InvariantsFormula<T> operand1 = pAnd.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pAnd.getOperand2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      toVisit = pAnd;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.logicalAnd(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(LogicalNot<T> pNot) {
    InvariantsFormula<T> operand = pNot.getNegated().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand == pNot.getNegated()) {
      toVisit = pNot;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.logicalNot(operand);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(Modulo<T> pModulo) {
    InvariantsFormula<T> numerator = pModulo.getNumerator().accept(this);
    InvariantsFormula<T> denominator = pModulo.getDenominator().accept(this);
    final InvariantsFormula<T> toVisit;
    if (numerator == pModulo.getNumerator() && denominator == pModulo.getDenominator()) {
      toVisit = pModulo;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.modulo(numerator, denominator);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(Multiply<T> pMultiply) {
    InvariantsFormula<T> factor1 = pMultiply.getFactor1().accept(this);
    InvariantsFormula<T> factor2 = pMultiply.getFactor2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (factor1 == pMultiply.getFactor1() && factor2 == pMultiply.getFactor2()) {
      toVisit = pMultiply;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.multiply(factor1, factor2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(ShiftLeft<T> pShiftLeft) {
    InvariantsFormula<T> shifted = pShiftLeft.getShifted().accept(this);
    InvariantsFormula<T> shiftDistance = pShiftLeft.getShiftDistance().accept(this);
    final InvariantsFormula<T> toVisit;
    if (shifted == pShiftLeft.getShifted() && shiftDistance == pShiftLeft.getShiftDistance()) {
      toVisit = pShiftLeft;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.shiftLeft(shifted, shiftDistance);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(ShiftRight<T> pShiftRight) {
    InvariantsFormula<T> shifted = pShiftRight.getShifted().accept(this);
    InvariantsFormula<T> shiftDistance = pShiftRight.getShiftDistance().accept(this);
    final InvariantsFormula<T> toVisit;
    if (shifted == pShiftRight.getShifted() && shiftDistance == pShiftRight.getShiftDistance()) {
      toVisit = pShiftRight;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.shiftRight(shifted, shiftDistance);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(Union<T> pUnion) {
    InvariantsFormula<T> operand1 = pUnion.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pUnion.getOperand2().accept(this);
    final InvariantsFormula<T> toVisit;
    if (operand1 == pUnion.getOperand1() && operand2 == pUnion.getOperand2()) {
      toVisit = pUnion;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.union(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public InvariantsFormula<T> visit(Variable<T> pVariable) {
    return visitPost(pVariable);
  }

}
