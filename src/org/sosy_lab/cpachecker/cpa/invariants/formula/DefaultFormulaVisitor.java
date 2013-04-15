/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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


public abstract class DefaultFormulaVisitor<ConstantType, ReturnType> implements InvariantsFormulaVisitor<ConstantType, ReturnType> {

  protected abstract ReturnType visitDefault(InvariantsFormula<ConstantType> pFormula);

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
  public ReturnType visit(Equal<ConstantType> pEqual) {
    return visitDefault(pEqual);
  }

  @Override
  public ReturnType visit(LessThan<ConstantType> pLessThan) {
    return visitDefault(pLessThan);
  }

  @Override
  public ReturnType visit(LogicalAnd<ConstantType> pAnd) {
    return visitDefault(pAnd);
  }

  @Override
  public ReturnType visit(LogicalNot<ConstantType> pNot) {
    return visitDefault(pNot);
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
  public ReturnType visit(Negate<ConstantType> pNegate) {
    return visitDefault(pNegate);
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

}
