// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Predicate;

/**
 * Instances of this class are visitors used to check if the visited formulae contain a specified
 * variable.
 *
 * @param <T> the type of the constants used in the visited formulae.
 */
public class ContainsVisitor<T>
    implements ParameterizedNumeralFormulaVisitor<T, Predicate<? super NumeralFormula<T>>, Boolean>,
        ParameterizedBooleanFormulaVisitor<T, Predicate<? super NumeralFormula<T>>, Boolean> {

  @Override
  public Boolean visit(Add<T> pAdd, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pAdd)
        || pAdd.getSummand1().accept(this, pPredicate)
        || pAdd.getSummand2().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(BinaryAnd<T> pAnd, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pAnd)
        || pAnd.getOperand1().accept(this, pPredicate)
        || pAnd.getOperand2().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(BinaryNot<T> pNot, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pNot) || pNot.getFlipped().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(BinaryOr<T> pOr, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pOr)
        || pOr.getOperand1().accept(this, pPredicate)
        || pOr.getOperand2().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(BinaryXor<T> pXor, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pXor)
        || pXor.getOperand1().accept(this, pPredicate)
        || pXor.getOperand2().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(Constant<T> pConstant, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pConstant);
  }

  @Override
  public Boolean visit(Divide<T> pDivide, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pDivide)
        || pDivide.getNumerator().accept(this, pPredicate)
        || pDivide.getDenominator().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(Equal<T> pEqual, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pEqual.getOperand1().accept(this, pPredicate)
        || pEqual.getOperand2().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(Exclusion<T> pExclusion, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pExclusion) || pExclusion.getExcluded().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pLessThan.getOperand1().accept(this, pPredicate)
        || pLessThan.getOperand2().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(LogicalAnd<T> pAnd, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pAnd.getOperand1().accept(this, pPredicate)
        || pAnd.getOperand2().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(LogicalNot<T> pNot, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pNot.getNegated().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(Modulo<T> pModulo, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pModulo)
        || pModulo.getNumerator().accept(this, pPredicate)
        || pModulo.getDenominator().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(Multiply<T> pMultiply, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pMultiply)
        || pMultiply.getFactor1().accept(this, pPredicate)
        || pMultiply.getFactor2().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(ShiftLeft<T> pShiftLeft, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pShiftLeft)
        || pShiftLeft.getShifted().accept(this, pPredicate)
        || pShiftLeft.getShiftDistance().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(ShiftRight<T> pShiftRight, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pShiftRight)
        || pShiftRight.getShifted().accept(this, pPredicate)
        || pShiftRight.getShiftDistance().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(Union<T> pUnion, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pUnion)
        || pUnion.getOperand1().accept(this, pPredicate)
        || pUnion.getOperand2().accept(this, pPredicate);
  }

  @Override
  public Boolean visitFalse(Predicate<? super NumeralFormula<T>> pParameter) {
    return false;
  }

  @Override
  public Boolean visitTrue(Predicate<? super NumeralFormula<T>> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Variable<T> pVariable, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pVariable);
  }

  @Override
  public Boolean visit(Cast<T> pCast, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pPredicate.apply(pCast) || pCast.getCasted().accept(this, pPredicate);
  }

  @Override
  public Boolean visit(IfThenElse<T> pIfThenElse, Predicate<? super NumeralFormula<T>> pPredicate) {
    return pIfThenElse.getCondition().accept(this, pPredicate)
        || pIfThenElse.getPositiveCase().accept(this, pPredicate)
        || pIfThenElse.getNegativeCase().accept(this, pPredicate);
  }
}
