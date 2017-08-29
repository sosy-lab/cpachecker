/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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



public class IsLinearVisitor<T> implements ParameterizedNumeralFormulaVisitor<T, Variable<T>, Boolean> {

  private final ContainsVarVisitor<T> containsVarVisitor = new ContainsVarVisitor<>();

  @Override
  public Boolean visit(Add<T> pAdd, Variable<T> pParameter) {
    return pAdd.getSummand1().accept(this, pParameter) && pAdd.getSummand2().accept(this, pParameter);
  }

  @Override
  public Boolean visit(BinaryAnd<T> pAnd, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(BinaryNot<T> pNot, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(BinaryOr<T> pOr, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(BinaryXor<T> pXor, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Constant<T> pConstant, Variable<T> pParameter) {
    return true;
  }

  @Override
  public Boolean visit(Divide<T> pDivide, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Exclusion<T> pExclusion, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Modulo<T> pModulo, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Multiply<T> pMultiply, Variable<T> pParameter) {
    return !pMultiply.accept(containsVarVisitor, pParameter.getMemoryLocation())
        && (pMultiply.getOperand1() instanceof Constant || pMultiply.getOperand2() instanceof Constant);
  }

  @Override
  public Boolean visit(ShiftLeft<T> pShiftLeft, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(ShiftRight<T> pShiftRight, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Union<T> pUnion, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Variable<T> pVariable, Variable<T> pParameter) {
    return true;
  }

  @Override
  public Boolean visit(IfThenElse<T> pIfThenElse, Variable<T> pParameter) {
    return false;
  }

  @Override
  public Boolean visit(Cast<T> pCast, Variable<T> pParameter) {
    return false;
  }

}
