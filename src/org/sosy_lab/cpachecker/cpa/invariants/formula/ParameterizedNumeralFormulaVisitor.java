// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of implementing classes are visitors for invariants formulae that accept an additional
 * parameter to take into consideration on visiting a formula.
 *
 * @param <ConstantType> the type of the constants used in the visited formulae.
 * @param <ParameterType> the type of the additional parameter.
 * @param <ReturnType> the type of the visit results.
 */
interface ParameterizedNumeralFormulaVisitor<ConstantType, ParameterType, ReturnType> {

  /**
   * Visits the given addition invariants formula.
   *
   * @param pAdd the addition invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Add<ConstantType> pAdd, ParameterType pParameter);

  /**
   * Visits the given binary and invariants formula.
   *
   * @param pAnd the binary and invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(BinaryAnd<ConstantType> pAnd, ParameterType pParameter);

  /**
   * Visits the given binary negation invariants formula.
   *
   * @param pNot the binary negation invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(BinaryNot<ConstantType> pNot, ParameterType pParameter);

  /**
   * Visits the given binary or invariants formula.
   *
   * @param pOr the binary or invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(BinaryOr<ConstantType> pOr, ParameterType pParameter);

  /**
   * Visits the given binary exclusive or invariants formula.
   *
   * @param pXor the binary exclusive or invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(BinaryXor<ConstantType> pXor, ParameterType pParameter);

  /**
   * Visits the given constant invariants formula.
   *
   * @param pConstant the constant invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Constant<ConstantType> pConstant, ParameterType pParameter);

  /**
   * Visits the given fraction invariants formula.
   *
   * @param pDivide the fraction invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Divide<ConstantType> pDivide, ParameterType pParameter);

  /**
   * Visits the given exclusion invariants formula.
   *
   * @param pExclusion the exclusion formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Exclusion<ConstantType> pExclusion, ParameterType pParameter);

  /**
   * Visits the given modulo invariants formula.
   *
   * @param pModulo the modulo invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Modulo<ConstantType> pModulo, ParameterType pParameter);

  /**
   * Visits the given multiplication invariants formula.
   *
   * @param pMultiply the multiplication invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Multiply<ConstantType> pMultiply, ParameterType pParameter);

  /**
   * Visits the given left shift invariants formula.
   *
   * @param pShiftLeft the left shift invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(ShiftLeft<ConstantType> pShiftLeft, ParameterType pParameter);

  /**
   * Visits the given right shift invariants formula.
   *
   * @param pShiftRight the right shift invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(ShiftRight<ConstantType> pShiftRight, ParameterType pParameter);

  /**
   * Visits the given union invariants formula.
   *
   * @param pUnion the union invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Union<ConstantType> pUnion, ParameterType pParameter);

  /**
   * Visits the given variable invariants formula.
   *
   * @param pVariable the variable invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Variable<ConstantType> pVariable, ParameterType pParameter);

  /**
   * Visits the given if-then-else invariants formula.
   *
   * @param pIfThenElse the if-then-else invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(IfThenElse<ConstantType> pIfThenElse, ParameterType pParameter);

  /**
   * Visits the given cast invariants formula.
   *
   * @param pCast the cast invariants formula to visit.
   * @param pParameter the additional parameter to take into consideration.
   * @return the result of the visit.
   */
  ReturnType visit(Cast<ConstantType> pCast, ParameterType pParameter);
}
