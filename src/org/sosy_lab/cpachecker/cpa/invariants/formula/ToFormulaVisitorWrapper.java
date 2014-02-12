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

import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;


public class ToFormulaVisitorWrapper<ConstantType, FormulaType> extends ParameterizedInvariantsFormulaVisitorWrapper<ConstantType, Map<? extends String, ? extends InvariantsFormula<ConstantType>>, FormulaType> implements ToFormulaVisitor<ConstantType, FormulaType> {

  private ToFormulaVisitor<ConstantType, FormulaType> wrapped;

  public ToFormulaVisitorWrapper() {
    this(null);
  }

  public ToFormulaVisitorWrapper(ToFormulaVisitor<ConstantType, FormulaType> pToWrap) {
    super(pToWrap);
    setInner(pToWrap);
  }

  public void setInner(ToFormulaVisitor<ConstantType, FormulaType> pToWrap) {
    super.setInner(pToWrap);
    this.wrapped = pToWrap;
  }

  @Override
  public FormulaType getZero() {
    return this.wrapped.getZero();
  }

  @Override
  public FormulaType getOne() {
    return this.wrapped.getOne();
  }

  public ToFormulaVisitor<ConstantType, FormulaType> getWrapped() {
    return this.wrapped;
  }

  @Override
  public BooleanFormula lessThan(FormulaType pOp1, FormulaType pOp2) {
    return this.wrapped.lessThan(pOp1, pOp2);
  }

  @Override
  public BooleanFormula equal(FormulaType pOp1, FormulaType pOp2) {
    return this.wrapped.equal(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterThan(FormulaType pOp1, FormulaType pOp2) {
    return this.wrapped.greaterThan(pOp1, pOp2);
  }

  @Override
  public BooleanFormula lessOrEqual(FormulaType pOp1, FormulaType pOp2) {
    return this.wrapped.lessOrEqual(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterOrEqual(FormulaType pOp1, FormulaType pOp2) {
    return this.wrapped.greaterOrEqual(pOp1, pOp2);
  }



}
