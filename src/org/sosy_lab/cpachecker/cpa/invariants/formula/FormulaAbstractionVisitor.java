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

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;

/**
 * Instances of this class are visitors for compound state invariants formulae
 * which are used to evaluate the visited formulae to compound states. This
 * visitor deliberately uses a weaker evaluation strategy than a
 * {@link FormulaCompoundStateEvaluationVisitor} in order to enable the CPA
 * strategy to prevent infeasible interpretation of the analyzed code.
 */
public class FormulaAbstractionVisitor extends DefaultParameterizedFormulaVisitor<CompoundInterval, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>>, CompoundInterval> implements FormulaEvaluationVisitor<CompoundInterval> {

  private static final FormulaCompoundStateEvaluationVisitor EVALUATION_VISITOR = new FormulaCompoundStateEvaluationVisitor();

  /**
   * Compute a compound state representing possible results of adding the
   * given summand compound states up. This method provides a much weaker
   * implementation of compound state addition than
   * {@link CompoundInterval#add(CompoundInterval)} and will thus usually return a
   * much larger result range.
   *
   * @param a the first summand.
   * @param b the second summand.
   * @return a state representing possible results of adding the given summand
   * compound states up.
   */
  private CompoundInterval weakAdd(CompoundInterval pA, CompoundInterval pB) {
    if (pA.isSingleton() && pA.containsZero()) {
      return pB;
    }
    if (pB.isSingleton() && pB.containsZero()) {
      return pA;
    }
    return abstractionOf(pA.add(pB));
  }

  private static CompoundInterval abstractionOf(CompoundInterval pValue) {
    if (pValue.isBottom() || pValue.isTop()) {
      return pValue;
    }
    CompoundInterval result = pValue.signum();
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
   * {@link CompoundInterval#multiply(CompoundInterval)} and will thus usually return
   * a much larger result range.
   *
   * @param a the first factor.
   * @param b the second factor.
   * @return a state representing possible results of multiplying the given
   * factor compound states.
   */
  private CompoundInterval weakMultiply(CompoundInterval a, CompoundInterval b) {
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
  public CompoundInterval visit(Add<CompoundInterval> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return weakAdd(pAdd.getSummand1().accept(EVALUATION_VISITOR, pEnvironment), pAdd.getSummand2().accept(EVALUATION_VISITOR, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Constant<CompoundInterval> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pConstant.getValue();
  }

  @Override
  public CompoundInterval visit(Multiply<CompoundInterval> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return weakMultiply(pMultiply.getFactor1().accept(this, pEnvironment), pMultiply.getFactor2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(ShiftLeft<CompoundInterval> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval toShift = pShiftLeft.getShifted().accept(this, pEnvironment);
    CompoundInterval shiftDistance = pShiftLeft.getShiftDistance().accept(this, pEnvironment);
    CompoundInterval evaluation = toShift.shiftLeft(shiftDistance);
    if (!shiftDistance.containsPositive()) {
      return evaluation;
    }
    return abstractionOf(evaluation);
  }

  @Override
  public CompoundInterval visit(Variable<CompoundInterval> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    InvariantsFormula<CompoundInterval> varState = pEnvironment.get(pVariable.getName());
    if (varState == null) {
      return CompoundInterval.top();
    }
    return varState.accept(this, pEnvironment);
  }

  @Override
  protected CompoundInterval visitDefault(InvariantsFormula<CompoundInterval> pFormula,
      Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pParam) {
    return abstractionOf(pFormula.accept(EVALUATION_VISITOR, pParam));
  }

}
