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
 */
public abstract class RecursiveNumeralFormulaVisitor<T> implements NumeralFormulaVisitor<T, NumeralFormula<T>> {

  /**
   * Visits the (possibly modified) formula after its child formulae were
   * visited by this visitor.
   *
   * @param pFormula the formula to visit.
   * @return the (possible modified) visited formula.
   */
  protected abstract NumeralFormula<T> visitPost(NumeralFormula<T> pFormula);

  @Override
  public NumeralFormula<T> visit(Add<T> pAdd) {
    NumeralFormula<T> summand1 = pAdd.getSummand1().accept(this);
    NumeralFormula<T> summand2 = pAdd.getSummand2().accept(this);
    final NumeralFormula<T> toVisit;
    if (summand1 == pAdd.getSummand1() && summand2 == pAdd.getSummand2()) {
      toVisit = pAdd;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.add(summand1, summand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(BinaryAnd<T> pAnd) {
    NumeralFormula<T> operand1 = pAnd.getOperand1().accept(this);
    NumeralFormula<T> operand2 = pAnd.getOperand2().accept(this);
    final NumeralFormula<T> toVisit;
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      toVisit = pAnd;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.binaryAnd(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(BinaryNot<T> pNot) {
    NumeralFormula<T> operand = pNot.getFlipped().accept(this);
    final NumeralFormula<T> toVisit;
    if (operand == pNot.getFlipped()) {
      toVisit = pNot;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.binaryNot(operand);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(BinaryOr<T> pOr) {
    NumeralFormula<T> operand1 = pOr.getOperand1().accept(this);
    NumeralFormula<T> operand2 = pOr.getOperand2().accept(this);
    final NumeralFormula<T> toVisit;
    if (operand1 == pOr.getOperand1() && operand2 == pOr.getOperand2()) {
      toVisit = pOr;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.binaryOr(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(BinaryXor<T> pXor) {
    NumeralFormula<T> operand1 = pXor.getOperand1().accept(this);
    NumeralFormula<T> operand2 = pXor.getOperand2().accept(this);
    final NumeralFormula<T> toVisit;
    if (operand1 == pXor.getOperand1() && operand2 == pXor.getOperand2()) {
      toVisit = pXor;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.binaryXor(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(Constant<T> pConstant) {
    return visitPost(pConstant);
  }

  @Override
  public NumeralFormula<T> visit(Divide<T> pDivide) {
    NumeralFormula<T> numerator = pDivide.getNumerator().accept(this);
    NumeralFormula<T> denominator = pDivide.getDenominator().accept(this);
    final NumeralFormula<T> toVisit;
    if (numerator == pDivide.getNumerator() && denominator == pDivide.getDenominator()) {
      toVisit = pDivide;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.divide(numerator, denominator);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(Exclusion<T> pExclusion) {
    NumeralFormula<T> operand = pExclusion.getExcluded().accept(this);
    final NumeralFormula<T> toVisit;
    if (operand == pExclusion.getExcluded()) {
      toVisit = pExclusion;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.exclude(operand);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(Modulo<T> pModulo) {
    NumeralFormula<T> numerator = pModulo.getNumerator().accept(this);
    NumeralFormula<T> denominator = pModulo.getDenominator().accept(this);
    final NumeralFormula<T> toVisit;
    if (numerator == pModulo.getNumerator() && denominator == pModulo.getDenominator()) {
      toVisit = pModulo;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.modulo(numerator, denominator);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(Multiply<T> pMultiply) {
    NumeralFormula<T> factor1 = pMultiply.getFactor1().accept(this);
    NumeralFormula<T> factor2 = pMultiply.getFactor2().accept(this);
    final NumeralFormula<T> toVisit;
    if (factor1 == pMultiply.getFactor1() && factor2 == pMultiply.getFactor2()) {
      toVisit = pMultiply;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.multiply(factor1, factor2);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(ShiftLeft<T> pShiftLeft) {
    NumeralFormula<T> shifted = pShiftLeft.getShifted().accept(this);
    NumeralFormula<T> shiftDistance = pShiftLeft.getShiftDistance().accept(this);
    final NumeralFormula<T> toVisit;
    if (shifted == pShiftLeft.getShifted() && shiftDistance == pShiftLeft.getShiftDistance()) {
      toVisit = pShiftLeft;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.shiftLeft(shifted, shiftDistance);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(ShiftRight<T> pShiftRight) {
    NumeralFormula<T> shifted = pShiftRight.getShifted().accept(this);
    NumeralFormula<T> shiftDistance = pShiftRight.getShiftDistance().accept(this);
    final NumeralFormula<T> toVisit;
    if (shifted == pShiftRight.getShifted() && shiftDistance == pShiftRight.getShiftDistance()) {
      toVisit = pShiftRight;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.shiftRight(shifted, shiftDistance);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(Union<T> pUnion) {
    NumeralFormula<T> operand1 = pUnion.getOperand1().accept(this);
    NumeralFormula<T> operand2 = pUnion.getOperand2().accept(this);
    final NumeralFormula<T> toVisit;
    if (operand1 == pUnion.getOperand1() && operand2 == pUnion.getOperand2()) {
      toVisit = pUnion;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.union(operand1, operand2);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(Variable<T> pVariable) {
    return visitPost(pVariable);
  }

  @Override
  public NumeralFormula<T> visit(IfThenElse<T> pIfThenElse) {
    NumeralFormula<T> positiveCase = pIfThenElse.getPositiveCase().accept(this);
    NumeralFormula<T> negativeCase = pIfThenElse.getNegativeCase().accept(this);
    final NumeralFormula<T> toVisit;
    if (positiveCase == pIfThenElse.getPositiveCase() && negativeCase == pIfThenElse.getNegativeCase()) {
      toVisit = pIfThenElse;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.ifThenElse(
          pIfThenElse.getCondition(),
          positiveCase,
          negativeCase);
    }
    return visitPost(toVisit);
  }

  @Override
  public NumeralFormula<T> visit(Cast<T> pCast) {
    NumeralFormula<T> operand = pCast.getCasted().accept(this);
    final NumeralFormula<T> toVisit;
    if (operand == pCast.getCasted()) {
      toVisit = pCast;
    } else {
      toVisit = InvariantsFormulaManager.INSTANCE.cast(pCast.getTypeInfo(), operand);
    }
    return visitPost(toVisit);
  }

}
