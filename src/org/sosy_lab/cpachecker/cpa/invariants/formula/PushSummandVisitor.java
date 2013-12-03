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

import java.util.Collections;
import java.util.Map;

/**
 * Instances of this class are parameterized invariants formula visitors used
 * to push one constant summand of an addition into the formula of the other
 * summand. This is possible as long as the operand can validly commute across
 * the concerned formulae and is performed with the goal of consuming the
 * operand in a formula, which means that the operand was added to another
 * constant producing a new constant instead of a more complex formula.
 *
 * @param <T> the type of the constants used
 */
public class PushSummandVisitor<T> extends DefaultParameterizedFormulaVisitor<T, T, InvariantsFormula<T>>{

  private static final String SUMMAND_ALREADY_CONSUMED_MESSAGE = "Summand already consumed.";

  /**
   * The empty environment used for pushing the summand. No real environment
   * is required because evaluations are exclusively done on the addition and
   * negation of constants.
   */
  private final Map<? extends String, ? extends InvariantsFormula<T>> EMPTY_ENVIRONMENT =
      Collections.emptyMap();

  /**
   * The evaluation visitor used to evaluate the addition and negation of
   * constants.
   */
  private final FormulaEvaluationVisitor<T> evaluationVisitor;

  /**
   * This flag indicates whether or not this visitor managed to get a summand
   * consumed.
   */
  private boolean consumed = false;

  /**
   * Creates a new push summand visitor with the given evaluation visitor. This
   * visitor must not be reused after a summand has been consumed.
   *
   * @param pEvaluationVisitor the evaluation visitor used to evaluate the
   * addition and negation of constants.
   */
  public PushSummandVisitor(FormulaEvaluationVisitor<T> pEvaluationVisitor) {
    this.evaluationVisitor = pEvaluationVisitor;
  }

  /**
   * Checks if the visitor managed to get a summand consumed.
   *
   * @return <code>true</code> if the summand was consumed, <code>false</code>
   * otherwise.
   */
  public boolean isSummandConsumed() {
    return consumed;
  }

  /**
   * Throws an illegal state exception if the visitor already managed to get a
   * summand consumed, otherwise it does nothing.
   *
   * @throws IllegalStateException if the visitor already managed to get a
   * summand consumed, otherwise it does nothing.
   */
  private void checkNotConsumed() throws IllegalStateException {
    if (isSummandConsumed()) {
      throw new IllegalStateException(SUMMAND_ALREADY_CONSUMED_MESSAGE);
    }
  }

  /**
   * @throws IllegalStateException if the visitor already managed to get a
   * summand consumed, otherwise it does nothing.
   */
  @Override
  public InvariantsFormula<T> visit(Add<T> pAdd, T pToPush) throws IllegalStateException {
    checkNotConsumed();
    InvariantsFormula<T> candidateS1 = pAdd.getSummand1().accept(this, pToPush);
    if (isSummandConsumed()) {
      return InvariantsFormulaManager.INSTANCE.add(candidateS1, pAdd.getSummand2());
    }
    InvariantsFormula<T> summand2 = pAdd.getSummand2().accept(this, pToPush);
    return InvariantsFormulaManager.INSTANCE.add(pAdd.getSummand1(), summand2);
  }

  /**
   * @throws IllegalStateException if the visitor already managed to get a
   * summand consumed, otherwise it does nothing.
   */
  @Override
  public InvariantsFormula<T> visit(Constant<T> pConstant, T pToPush) throws IllegalStateException {
    checkNotConsumed();
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    InvariantsFormula<T> toPush = ifm.asConstant(pToPush);
    InvariantsFormula<T> sum = ifm.add(pConstant, toPush);
    this.consumed = true;
    T sumValue = sum.accept(evaluationVisitor, EMPTY_ENVIRONMENT);
    return InvariantsFormulaManager.INSTANCE.asConstant(sumValue);
  }

  /**
   * @throws IllegalStateException if the visitor already managed to get a
   * summand consumed, otherwise it does nothing.
   */
  @Override
  protected InvariantsFormula<T> visitDefault(InvariantsFormula<T> pFormula, T pToPush) throws IllegalStateException {
    checkNotConsumed();
    InvariantsFormulaManager ifm = InvariantsFormulaManager.INSTANCE;
    InvariantsFormula<T> toPush = ifm.asConstant(pToPush);
    return ifm.add(pFormula, toPush);
  }

}
