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
 * Instances of extending classes are parameterized visitors for invariants
 * formulae which use a generic visit method that can be used to handle general
 * cases while special cases can be overridden.
 *
 * @param <ConstantType> the type of the constants used in the visited
 * formulae.
 * @param <ParamType> the type of the visit parameter.
 * @param <ReturnType> the type of the visit return values.
 */
public abstract class DefaultParameterizedFormulaVisitor<ConstantType, ParamType, ReturnType> implements ParameterizedInvariantsFormulaVisitor<ConstantType, ParamType, ReturnType> {

  /**
   * Provides a generic visit method that can be applied to any invariants
   * formula type.
   *
   * @param pFormula the visited formula.
   * @param pParam the visit parameter.
   *
   * @return the result of the generic visit.
   */
  protected abstract ReturnType visitDefault(InvariantsFormula<ConstantType> pFormula, ParamType pParam);

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
  public ReturnType visit(Equal<ConstantType> pEqual, ParamType pParam) {
    return visitDefault(pEqual, pParam);
  }

  @Override
  public ReturnType visit(Exclusion<ConstantType> pExclusion, ParamType pParam) {
    return visitDefault(pExclusion, pParam);
  }

  @Override
  public ReturnType visit(LessThan<ConstantType> pLessThan, ParamType pParam) {
    return visitDefault(pLessThan, pParam);
  }

  @Override
  public ReturnType visit(LogicalAnd<ConstantType> pAnd, ParamType pParam) {
    return visitDefault(pAnd, pParam);
  }

  @Override
  public ReturnType visit(LogicalNot<ConstantType> pNot, ParamType pParam) {
    return visitDefault(pNot, pParam);
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

}
