// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of implementing classes are visitors for invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the visited formulae.
 * @param <ReturnType> the type of the visit results.
 */
interface NumeralFormulaVisitor<ConstantType, ReturnType> {

  /**
   * Visits the given addition invariants formula.
   *
   * @param pAdd the addition invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(Add<ConstantType> pAdd);

  /**
   * Visits the given binary and invariants formula.
   *
   * @param pAnd the binary and invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(BinaryAnd<ConstantType> pAnd);

  /**
   * Visits the given binary negation invariants formula.
   *
   * @param pNot the binary negation invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(BinaryNot<ConstantType> pNot);

  /**
   * Visits the given binary or invariants formula.
   *
   * @param pOr the binary or invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(BinaryOr<ConstantType> pOr);

  /**
   * Visits the given binary exclusive or invariants formula.
   *
   * @param pXor the binary exclusive or invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(BinaryXor<ConstantType> pXor);

  /**
   * Visits the given constant invariants formula.
   *
   * @param pConstant the constant invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(Constant<ConstantType> pConstant);

  /**
   * Visits the given fraction invariants formula.
   *
   * @param pDivide the fraction invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(Divide<ConstantType> pDivide);

  /**
   * Visits the given exclusion invariants formula.
   *
   * @param pExclusion the exclusion formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(Exclusion<ConstantType> pExclusion);

  /**
   * Visits the given modulo invariants formula.
   *
   * @param pModulo the modulo invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(Modulo<ConstantType> pModulo);

  /**
   * Visits the given multiplication invariants formula.
   *
   * @param pMultiply the multiplication invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(Multiply<ConstantType> pMultiply);

  /**
   * Visits the given left shift invariants formula.
   *
   * @param pShiftLeft the left shift invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(ShiftLeft<ConstantType> pShiftLeft);

  /**
   * Visits the given right shift invariants formula.
   *
   * @param pShiftRight the right shift invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(ShiftRight<ConstantType> pShiftRight);

  /**
   * Visits the given union invariants formula.
   *
   * @param pUnion the union invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(Union<ConstantType> pUnion);

  /**
   * Visits the given variable invariants formula.
   *
   * @param pVariable the variable invariants formula to visit..
   * @return the result of the visit.
   */
  ReturnType visit(Variable<ConstantType> pVariable);

  /**
   * Visits the given if-then-else invariants formula.
   *
   * @param pIfThenElse the if-then-else invariants formula to visit..
   * @return the result of the visit.
   */
  ReturnType visit(IfThenElse<ConstantType> pIfThenElse);

  /**
   * Visits the given cast invariants formula.
   *
   * @param pCast the cast invariants formula to visit..
   * @return the result of the visit.
   */
  ReturnType visit(Cast<ConstantType> pCast);
}
