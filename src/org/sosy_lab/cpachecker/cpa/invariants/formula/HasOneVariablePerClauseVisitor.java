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



public class HasOneVariablePerClauseVisitor<ConstantType> implements InvariantsFormulaVisitor<ConstantType, Boolean> {

  private final CollectVarsVisitor<ConstantType> collectVarsVisitor = new CollectVarsVisitor<>();

  @Override
  public Boolean visit(Add<ConstantType> pAdd) {
    return pAdd.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(BinaryAnd<ConstantType> pAnd) {
    return pAnd.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(BinaryNot<ConstantType> pNot) {
    return pNot.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(BinaryOr<ConstantType> pOr) {
    return pOr.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(BinaryXor<ConstantType> pXor) {
    return pXor.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(Constant<ConstantType> pConstant) {
    return false;
  }

  @Override
  public Boolean visit(Divide<ConstantType> pDivide) {
    return pDivide.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(Equal<ConstantType> pEqual) {
    return pEqual.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(LessThan<ConstantType> pLessThan) {
    return pLessThan.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(LogicalAnd<ConstantType> pAnd) {
    return pAnd.getOperand1().accept(this) && pAnd.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(LogicalNot<ConstantType> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Boolean visit(Modulo<ConstantType> pModulo) {
    return pModulo.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(Multiply<ConstantType> pMultiply) {
    return pMultiply.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(ShiftLeft<ConstantType> pShiftLeft) {
    return pShiftLeft.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(ShiftRight<ConstantType> pShiftRight) {
    return pShiftRight.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(Union<ConstantType> pUnion) {
    return pUnion.accept(this.collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(Variable<ConstantType> pVariable) {
    return true;
  }

}
