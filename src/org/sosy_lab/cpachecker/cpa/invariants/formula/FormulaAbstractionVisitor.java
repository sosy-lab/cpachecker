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

/**
 * Instances of this class are visitors for compound state invariants formulae
 * which are used to evaluate the visited formulae to compound states. This
 * visitor deliberately uses a weaker evaluation strategy than a
 * {@link FormulaCompoundStateEvaluationVisitor} in order to enable the CPA
 * strategy to prevent infeasible interpretation of the analyzed code.
 */
public class FormulaAbstractionVisitor implements FormulaEvaluationVisitor<CompoundState> {

  /**
   * Compute a compound state representing possible results of adding the
   * given summand compound states up. This method provides a much weaker
   * implementation of compound state addition than
   * {@link CompoundState#add(CompoundState)} and will thus usually return a
   * much larger result range.
   *
   * @param a the first summand.
   * @param b the second summand.
   * @return a state representing possible results of adding the given summand
   * compound states up.
   */
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

  /**
   * Compute a compound state representing possible results of multiplying the
   * given factor compound states. This method provides a much weaker
   * implementation of compound state addition than
   * {@link CompoundState#multiply(CompoundState)} and will thus usually return
   * a much larger result range.
   *
   * @param a the first factor.
   * @param b the second factor.
   * @return a state representing possible results of multiplying the given
   * factor compound states.
   */
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

  @Override
  public CompoundState visit(Add<CompoundState> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return weakAdd(pAdd.getSummand1().accept(this, pEnvironment), pAdd.getSummand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(BinaryAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return CompoundState.top();
  }

  @Override
  public CompoundState visit(BinaryNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pNot.getFlipped().accept(this, pEnvironment).binaryNot();
  }

  @Override
  public CompoundState visit(BinaryOr<CompoundState> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return CompoundState.top();
  }

  @Override
  public CompoundState visit(BinaryXor<CompoundState> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return CompoundState.top();
  }

  @Override
  public CompoundState visit(Constant<CompoundState> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pConstant.getValue();
  }

  @Override
  public CompoundState visit(Divide<CompoundState> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pDivide.getNumerator().accept(this, pEnvironment).divide(pDivide.getDenominator().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Equal<CompoundState> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pEqual.getOperand1().accept(this, pEnvironment).logicalEquals(pEqual.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(LessThan<CompoundState> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pLessThan.getOperand1().accept(this, pEnvironment).lessThan(pLessThan.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(LogicalAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pAnd.getOperand1().accept(this, pEnvironment).logicalAnd(pAnd.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(LogicalNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pNot.getNegated().accept(this, pEnvironment).logicalNot();
  }

  @Override
  public CompoundState visit(Modulo<CompoundState> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pModulo.getNumerator().accept(this, pEnvironment).modulo(pModulo.getDenominator().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Multiply<CompoundState> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return weakMultiply(pMultiply.getFactor1().accept(this, pEnvironment), pMultiply.getFactor2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Negate<CompoundState> pNegate, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pNegate.getNegated().accept(this, pEnvironment).negate();
  }

  @Override
  public CompoundState visit(ShiftLeft<CompoundState> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState toShift = pShiftLeft.getShifted().accept(this, pEnvironment);
    CompoundState shiftDistance = pShiftLeft.getShiftDistance().accept(this, pEnvironment);
    if (!shiftDistance.containsPositive()) {
      return toShift.shiftLeft(shiftDistance);
    }
    if (!toShift.containsPositive()) {
      return CompoundState.singleton(0).extendToNegativeInfinity();
    }
    if (!toShift.containsNegative()) {
      return CompoundState.singleton(0).extendToPositiveInfinity();
    }
    return CompoundState.top();
  }

  @Override
  public CompoundState visit(ShiftRight<CompoundState> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pShiftRight.getShifted().accept(this, pEnvironment).shiftRight(pShiftRight.getShiftDistance().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Union<CompoundState> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return CompoundState.span(pUnion.getOperand1().accept(this, pEnvironment), pUnion.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(Variable<CompoundState> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    InvariantsFormula<CompoundState> varState = pEnvironment.get(pVariable.getName());
    if (varState == null) {
      return CompoundState.top();
    }
    return varState.accept(this, pEnvironment);
  }

}
