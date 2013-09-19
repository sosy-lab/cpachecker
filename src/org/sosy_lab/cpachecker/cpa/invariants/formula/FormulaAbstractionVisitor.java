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
public class FormulaAbstractionVisitor extends DefaultParameterizedFormulaVisitor<CompoundState, Map<? extends String, ? extends InvariantsFormula<CompoundState>>, CompoundState> implements FormulaEvaluationVisitor<CompoundState> {

  private static final FormulaCompoundStateEvaluationVisitor EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

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
  private CompoundState weakAdd(CompoundState pA, CompoundState pB) {
    if (pA.isSingleton() && pA.containsZero()) {
      return pB;
    }
    if (pB.isSingleton() && pB.containsZero()) {
      return pA;
    }
    return abstractionOf(pA.add(pB));
  }

  private static CompoundState abstractionOf(CompoundState pValue) {
    if (pValue.isBottom() || pValue.isTop()) {
      return pValue;
    }
    CompoundState result = pValue.signum();
    boolean extendToNeg = false;
    if (!pValue.lessThan(result).isDefinitelyFalse()) {
      extendToNeg = true;
    }
    if (!pValue.greaterThan(result).isDefinitelyFalse()) {
      result = result.extendToPositiveInfinity();
    }
    if (extendToNeg) {
      result = result.extendToNegativeInfinity();
    }
    assert result.unionWith(pValue).equals(result);
    return result;
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
    return abstractionOf(a.multiply(b));
  }

  @Override
  public CompoundState visit(Add<CompoundState> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return weakAdd(pAdd.getSummand1().accept(EVALUATION_VISITOR, pEnvironment), pAdd.getSummand2().accept(EVALUATION_VISITOR, pEnvironment));
  }

  @Override
  public CompoundState visit(Constant<CompoundState> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return pConstant.getValue();
  }

  @Override
  public CompoundState visit(Multiply<CompoundState> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return weakMultiply(pMultiply.getFactor1().accept(this, pEnvironment), pMultiply.getFactor2().accept(this, pEnvironment));
  }

  @Override
  public CompoundState visit(ShiftLeft<CompoundState> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState toShift = pShiftLeft.getShifted().accept(this, pEnvironment);
    CompoundState shiftDistance = pShiftLeft.getShiftDistance().accept(this, pEnvironment);
    CompoundState evaluation = toShift.shiftLeft(shiftDistance);
    if (!shiftDistance.containsPositive()) {
      return evaluation;
    }
    return abstractionOf(evaluation);
  }

  @Override
  public CompoundState visit(ShiftRight<CompoundState> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return CompoundStateFormulaManager.INSTANCE.shiftLeft(pShiftRight.getShifted(), InvariantsFormulaManager.INSTANCE.negate(pShiftRight.getShiftDistance())).accept(this, pEnvironment);
  }

  @Override
  public CompoundState visit(Variable<CompoundState> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    InvariantsFormula<CompoundState> varState = pEnvironment.get(pVariable.getName());
    if (varState == null) {
      return CompoundState.top();
    }
    return varState.accept(this, pEnvironment);
  }

  @Override
  protected CompoundState visitDefault(InvariantsFormula<CompoundState> pFormula,
      Map<? extends String, ? extends InvariantsFormula<CompoundState>> pParam) {
    return abstractionOf(pFormula.accept(EVALUATION_VISITOR, pParam));
  }

}
