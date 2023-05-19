// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of extending classes are parameterized visitors for invariants formulae which use a
 * generic visit method that can be used to handle general cases while special cases can be
 * overridden.
 *
 * @param <ConstantType> the type of the constants used in the visited formulae.
 * @param <ParamType> the type of the visit parameter.
 * @param <ReturnType> the type of the visit return values.
 */
abstract class DefaultParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType>
    implements ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> {

  /**
   * Provides a generic visit method that can be applied to any invariants formula type.
   *
   * @param pFormula the visited formula.
   * @param pParam the visit parameter.
   * @return the result of the generic visit.
   */
  protected abstract ReturnType visitDefault(
      NumeralFormula<ConstantType> pFormula, ParamType pParam);

  @Override
  public ReturnType visit(Add<ConstantType> pAdd, ParamType pParam) {
    return visitDefault(pAdd, pParam);
  }

  @Override
  public ReturnType visit(BinaryAnd<ConstantType> pAnd, ParamType pParam) {
    return visitDefault(pAnd, pParam);
  }

  @Override
  public ReturnType visit(BinaryNot<ConstantType> pNot, ParamType pParam) {
    return visitDefault(pNot, pParam);
  }

  @Override
  public ReturnType visit(BinaryOr<ConstantType> pOr, ParamType pParam) {
    return visitDefault(pOr, pParam);
  }

  @Override
  public ReturnType visit(BinaryXor<ConstantType> pXor, ParamType pParam) {
    return visitDefault(pXor, pParam);
  }

  @Override
  public ReturnType visit(Constant<ConstantType> pConstant, ParamType pParam) {
    return visitDefault(pConstant, pParam);
  }

  @Override
  public ReturnType visit(Divide<ConstantType> pDivide, ParamType pParam) {
    return visitDefault(pDivide, pParam);
  }

  @Override
  public ReturnType visit(Exclusion<ConstantType> pExclusion, ParamType pParam) {
    return visitDefault(pExclusion, pParam);
  }

  @Override
  public ReturnType visit(Modulo<ConstantType> pModulo, ParamType pParam) {
    return visitDefault(pModulo, pParam);
  }

  @Override
  public ReturnType visit(Multiply<ConstantType> pMultiply, ParamType pParam) {
    return visitDefault(pMultiply, pParam);
  }

  @Override
  public ReturnType visit(ShiftLeft<ConstantType> pShiftLeft, ParamType pParam) {
    return visitDefault(pShiftLeft, pParam);
  }

  @Override
  public ReturnType visit(ShiftRight<ConstantType> pShiftRight, ParamType pParam) {
    return visitDefault(pShiftRight, pParam);
  }

  @Override
  public ReturnType visit(Union<ConstantType> pUnion, ParamType pParam) {
    return visitDefault(pUnion, pParam);
  }

  @Override
  public ReturnType visit(Variable<ConstantType> pVariable, ParamType pParam) {
    return visitDefault(pVariable, pParam);
  }

  @Override
  public ReturnType visit(IfThenElse<ConstantType> pIfThenElse, ParamType pParam) {
    return visitDefault(pIfThenElse, pParam);
  }

  @Override
  public ReturnType visit(Cast<ConstantType> pCast, ParamType pParam) {
    return visitDefault(pCast, pParam);
  }
}
