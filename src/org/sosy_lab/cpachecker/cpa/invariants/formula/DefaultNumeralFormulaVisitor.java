// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of extending classes are visitors for invariants formulae which use a generic visit
 * method that can be used to handle general cases while special cases can be overridden.
 *
 * @param <ConstantType> the type of the constants used in the visited formulae.
 * @param <ReturnType> the type of the visit return values.
 */
abstract class DefaultNumeralFormulaVisitor<ConstantType, ReturnType>
    implements NumeralFormulaVisitor<ConstantType, ReturnType> {

  /**
   * Provides a generic visit method that can be applied to any invariants formula type.
   *
   * @param pFormula the visited formula.
   * @return the result of the generic visit.
   */
  protected abstract ReturnType visitDefault(NumeralFormula<ConstantType> pFormula);

  @Override
  public ReturnType visit(Add<ConstantType> pAdd) {
    return visitDefault(pAdd);
  }

  @Override
  public ReturnType visit(BinaryAnd<ConstantType> pAnd) {
    return visitDefault(pAnd);
  }

  @Override
  public ReturnType visit(BinaryNot<ConstantType> pNot) {
    return visitDefault(pNot);
  }

  @Override
  public ReturnType visit(BinaryOr<ConstantType> pOr) {
    return visitDefault(pOr);
  }

  @Override
  public ReturnType visit(BinaryXor<ConstantType> pXor) {
    return visitDefault(pXor);
  }

  @Override
  public ReturnType visit(Constant<ConstantType> pConstant) {
    return visitDefault(pConstant);
  }

  @Override
  public ReturnType visit(Divide<ConstantType> pDivide) {
    return visitDefault(pDivide);
  }

  @Override
  public ReturnType visit(Exclusion<ConstantType> pExclusion) {
    return visitDefault(pExclusion);
  }

  @Override
  public ReturnType visit(Modulo<ConstantType> pModulo) {
    return visitDefault(pModulo);
  }

  @Override
  public ReturnType visit(Multiply<ConstantType> pMultiply) {
    return visitDefault(pMultiply);
  }

  @Override
  public ReturnType visit(ShiftLeft<ConstantType> pShiftLeft) {
    return visitDefault(pShiftLeft);
  }

  @Override
  public ReturnType visit(ShiftRight<ConstantType> pShiftRight) {
    return visitDefault(pShiftRight);
  }

  @Override
  public ReturnType visit(Union<ConstantType> pUnion) {
    return visitDefault(pUnion);
  }

  @Override
  public ReturnType visit(Variable<ConstantType> pVariable) {
    return visitDefault(pVariable);
  }

  @Override
  public ReturnType visit(IfThenElse<ConstantType> pVariable) {
    return visitDefault(pVariable);
  }

  @Override
  public ReturnType visit(Cast<ConstantType> pCast) {
    return visitDefault(pCast);
  }
}
