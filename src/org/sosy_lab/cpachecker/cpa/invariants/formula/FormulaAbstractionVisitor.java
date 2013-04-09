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

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;

public class FormulaAbstractionVisitor implements FormulaEvaluationVisitor<CompoundState> {

  private final Map<String, InvariantsFormula<CompoundState>> environment;

  private CompoundState weakAdd(CompoundState a, CompoundState b) {
    if (a.isSingleton() && a.containsZero()) {
      return b;
    }
    if (b.containsNegative()) {
      if (b.containsPositive()) {
        return CompoundState.top();
      } else {
        return a.extendToNegativeInfinity();
      }
    } else if (b.containsPositive()) {
      return a.extendToPositiveInfinity();
    } else {
      return a;
    }
  }

  private CompoundState weakMultiply(CompoundState a, CompoundState b) {
    if (a.isSingleton() && a.containsZero()) {
      return a;
    }
    if (b.isSingleton() && b.containsZero()) {
      return b;
    }
    if (a.isSingleton() && a.contains(1)) {
      return b;
    }
    if (b.isSingleton() && b.contains(1)) {
      return a;
    }
    if (a.containsNegative() && a.containsPositive()) {
      return CompoundState.top();
    }
    if (b.containsNegative()) {
      if (b.containsPositive()) {
        return CompoundState.top();
      } else {
        return weakMultiply(a.negate(), b.negate());
      }
    } else if (b.containsPositive()) {
      return a.extendToPositiveInfinity();
    } else {
      return a.extendToNegativeInfinity();
    }
  }

  public FormulaAbstractionVisitor(Map<String, InvariantsFormula<CompoundState>> environment) {
    this.environment = environment;
  }

  @Override
  public CompoundState visit(Add<CompoundState> pAdd) {
    return weakAdd(pAdd.getSummand1().accept(this), pAdd.getSummand2().accept(this));
  }

  @Override
  public CompoundState visit(BinaryAnd<CompoundState> pAnd) {
    return top();
  }

  @Override
  public CompoundState visit(BinaryNot<CompoundState> pNot) {
    return pNot.getFlipped().accept(this).binaryNot();
  }

  @Override
  public CompoundState visit(BinaryOr<CompoundState> pOr) {
    return top();
  }

  @Override
  public CompoundState visit(BinaryXor<CompoundState> pXor) {
    return top();
  }

  @Override
  public CompoundState visit(Constant<CompoundState> pConstant) {
    return pConstant.getValue();
  }

  @Override
  public CompoundState visit(Divide<CompoundState> pDivide) {
    return pDivide.getNumerator().accept(this).divide(pDivide.getDenominator().accept(this));
  }

  @Override
  public CompoundState visit(Equal<CompoundState> pEqual) {
    return pEqual.getOperand1().accept(this).logicalEquals(pEqual.getOperand2().accept(this));
  }

  @Override
  public CompoundState visit(LessThan<CompoundState> pLessThan) {
    return pLessThan.getOperand1().accept(this).lessThan(pLessThan.getOperand2().accept(this));
  }

  @Override
  public CompoundState visit(LogicalAnd<CompoundState> pAnd) {
    return pAnd.getOperand1().accept(this).logicalAnd(pAnd.getOperand2().accept(this));
  }

  @Override
  public CompoundState visit(LogicalNot<CompoundState> pNot) {
    return pNot.getNegated().accept(this).logicalNot();
  }

  @Override
  public CompoundState visit(Modulo<CompoundState> pModulo) {
    return pModulo.getNumerator().accept(this).modulo(pModulo.getDenominator().accept(this));
  }

  @Override
  public CompoundState visit(Multiply<CompoundState> pMultiply) {
    return weakMultiply(pMultiply.getFactor1().accept(this), pMultiply.getFactor2().accept(this));
  }

  @Override
  public CompoundState visit(Negate<CompoundState> pNegate) {
    return pNegate.accept(this).negate();
  }

  @Override
  public CompoundState visit(ShiftLeft<CompoundState> pShiftLeft) {
    CompoundState toShift = pShiftLeft.getShifted().accept(this);
    CompoundState shiftDistance = pShiftLeft.getShiftDistance().accept(this);
    if (!shiftDistance.containsPositive()) {
      return toShift.shiftLeft(shiftDistance);
    }
    if (!toShift.containsPositive()) {
      return CompoundState.singleton(0).extendToNegativeInfinity();
    }
    if (!toShift.containsNegative()) {
      return CompoundState.singleton(0).extendToPositiveInfinity();
    }
    return top();
  }

  @Override
  public CompoundState visit(ShiftRight<CompoundState> pShiftRight) {
    return pShiftRight.getShifted().accept(this).shiftRight(pShiftRight.getShiftDistance().accept(this));
  }

  @Override
  public CompoundState visit(Union<CompoundState> pUnion) {
    return pUnion.getOperand1().accept(this).unionWith(pUnion.getOperand2().accept(this));
  }

  @Override
  public CompoundState visit(Variable<CompoundState> pVariable) {
    InvariantsFormula<CompoundState> varState = environment.get(pVariable.getName());
    if (varState == null) {
      return CompoundState.top();
    }
    return varState.accept(this);
  }

  @Override
  public CompoundState top() {
    return CompoundState.top();
  }

  @Override
  public CompoundState bottom() {
    return CompoundState.bottom();
  }

}
