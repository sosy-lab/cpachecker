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


public class ParameterizedInvariantsFormulaVisitorWrapper<ConstantType, ParameterType, ReturnType> implements ParameterizedInvariantsFormulaVisitor<ConstantType, ParameterType, ReturnType> {

  private ParameterizedInvariantsFormulaVisitor<ConstantType, ParameterType, ReturnType> wrapped = null;

  public ParameterizedInvariantsFormulaVisitorWrapper() {
    this(null);
  }

  public ParameterizedInvariantsFormulaVisitorWrapper(ParameterizedInvariantsFormulaVisitor<ConstantType, ParameterType, ReturnType> pToWrap) {
    setInner(pToWrap);
  }

  public void setInner(ParameterizedInvariantsFormulaVisitor<ConstantType, ParameterType, ReturnType> pToWrap) {
    this.wrapped = pToWrap;
  }

  @Override
  public ReturnType visit(Add<ConstantType> pAdd, ParameterType pParameter) {
    return this.wrapped.visit(pAdd, pParameter);
  }

  @Override
  public ReturnType visit(BinaryAnd<ConstantType> pAnd, ParameterType pParameter) {
    return this.wrapped.visit(pAnd, pParameter);
  }

  @Override
  public ReturnType visit(BinaryNot<ConstantType> pNot, ParameterType pParameter) {
    return this.wrapped.visit(pNot, pParameter);
  }

  @Override
  public ReturnType visit(BinaryOr<ConstantType> pOr, ParameterType pParameter) {
    return this.wrapped.visit(pOr, pParameter);
  }

  @Override
  public ReturnType visit(BinaryXor<ConstantType> pXor, ParameterType pParameter) {
    return this.wrapped.visit(pXor, pParameter);
  }

  @Override
  public ReturnType visit(Constant<ConstantType> pConstant, ParameterType pParameter) {
    return this.wrapped.visit(pConstant, pParameter);
  }

  @Override
  public ReturnType visit(Divide<ConstantType> pDivide, ParameterType pParameter) {
    return this.wrapped.visit(pDivide, pParameter);
  }

  @Override
  public ReturnType visit(Equal<ConstantType> pEqual, ParameterType pParameter) {
    return this.wrapped.visit(pEqual, pParameter);
  }

  @Override
  public ReturnType visit(LessThan<ConstantType> pLessThan, ParameterType pParameter) {
    return this.wrapped.visit(pLessThan, pParameter);
  }

  @Override
  public ReturnType visit(LogicalAnd<ConstantType> pAnd, ParameterType pParameter) {
    return this.wrapped.visit(pAnd, pParameter);
  }

  @Override
  public ReturnType visit(LogicalNot<ConstantType> pNot, ParameterType pParameter) {
    return this.wrapped.visit(pNot, pParameter);
  }

  @Override
  public ReturnType visit(Modulo<ConstantType> pModulo, ParameterType pParameter) {
    return this.wrapped.visit(pModulo, pParameter);
  }

  @Override
  public ReturnType visit(Multiply<ConstantType> pMultiply, ParameterType pParameter) {
    return this.wrapped.visit(pMultiply, pParameter);
  }

  @Override
  public ReturnType visit(ShiftLeft<ConstantType> pShiftLeft, ParameterType pParameter) {
    return this.wrapped.visit(pShiftLeft, pParameter);
  }

  @Override
  public ReturnType visit(ShiftRight<ConstantType> pShiftRight, ParameterType pParameter) {
    return this.wrapped.visit(pShiftRight, pParameter);
  }

  @Override
  public ReturnType visit(Union<ConstantType> pUnion, ParameterType pParameter) {
    return this.wrapped.visit(pUnion, pParameter);
  }

  @Override
  public ReturnType visit(Variable<ConstantType> pVariable, ParameterType pParameter) {
    return this.wrapped.visit(pVariable, pParameter);
  }

}
