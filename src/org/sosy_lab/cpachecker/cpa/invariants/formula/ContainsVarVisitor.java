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

import java.util.Map;

/**
 * Instances of this class are visitors used to check if the visited formulae
 * contain a specified variable.
 *
 * @param <T> the type of the constants used in the visited formulae.
 */
public class ContainsVarVisitor<T> implements ParameterizedInvariantsFormulaVisitor<T, String, Boolean> {

  private final Map<? extends String, ? extends InvariantsFormula<T>> environment;

  public ContainsVarVisitor() {
    this(null);
  }

  public ContainsVarVisitor(Map<? extends String, ? extends InvariantsFormula<T>> pEnvironment) {
    this.environment = pEnvironment;
  }

  @Override
  public Boolean visit(Add<T> pAdd, String pVarName) {
    return pAdd.getSummand1().accept(this, pVarName)
        || pAdd.getSummand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(BinaryAnd<T> pAnd, String pVarName) {
    return pAnd.getOperand1().accept(this, pVarName)
        || pAnd.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(BinaryNot<T> pNot, String pVarName) {
    return pNot.getFlipped().accept(this, pVarName);
  }

  @Override
  public Boolean visit(BinaryOr<T> pOr, String pVarName) {
    return pOr.getOperand1().accept(this, pVarName)
        || pOr.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(BinaryXor<T> pXor, String pVarName) {
    return pXor.getOperand1().accept(this, pVarName)
        || pXor.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Constant<T> pConstant, String pVarName) {
    return false;
  }

  @Override
  public Boolean visit(Divide<T> pDivide, String pVarName) {
    return pDivide.getNumerator().accept(this, pVarName)
        || pDivide.getDenominator().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Equal<T> pEqual, String pVarName) {
    return pEqual.getOperand1().accept(this, pVarName)
        || pEqual.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan, String pVarName) {
    return pLessThan.getOperand1().accept(this, pVarName)
        || pLessThan.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(LogicalAnd<T> pAnd, String pVarName) {
    return pAnd.getOperand1().accept(this, pVarName)
        || pAnd.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(LogicalNot<T> pNot, String pVarName) {
    return pNot.getNegated().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Modulo<T> pModulo, String pVarName) {
    return pModulo.getNumerator().accept(this, pVarName) || pModulo.getDenominator().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Multiply<T> pMultiply, String pVarName) {
    return pMultiply.getFactor1().accept(this, pVarName)
        || pMultiply.getFactor2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(ShiftLeft<T> pShiftLeft, String pVarName) {
    return pShiftLeft.getShifted().accept(this, pVarName)
        || pShiftLeft.getShiftDistance().accept(this, pVarName);
  }

  @Override
  public Boolean visit(ShiftRight<T> pShiftRight, String pVarName) {
    return pShiftRight.getShifted().accept(this, pVarName)
        || pShiftRight.getShiftDistance().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Union<T> pUnion, String pVarName) {
    return pUnion.getOperand1().accept(this, pVarName)
        || pUnion.getOperand2().accept(this, pVarName);
  }

  @Override
  public Boolean visit(Variable<T> pVariable, String pVarName) {
    return pVariable.getName().equals(pVarName) || refersTo(pVariable, pVarName);
  }

  private boolean refersTo(Variable<T> pVariable, String pVarName) {
    if (this.environment == null) {
      return false;
    }
    InvariantsFormula<T> value = this.environment.get(pVariable.getName());
    if (value == null) {
      return false;
    }
    return value.accept(this, pVarName);
  }

}
