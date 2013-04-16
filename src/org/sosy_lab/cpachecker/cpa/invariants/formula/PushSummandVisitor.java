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

public class PushSummandVisitor<T> extends DefaultParameterizedFormulaVisitor<T, Map<? extends String, ? extends InvariantsFormula<T>>, InvariantsFormula<T>> implements ParameterizedInvariantsFormulaVisitor<T, Map<? extends String, ? extends InvariantsFormula<T>>, InvariantsFormula<T>>{

  private final Constant<T> toPush;

  private final FormulaEvaluationVisitor<T> evaluationVisitor;

  private boolean consumed = false;

  public PushSummandVisitor(Constant<T> pToPush, FormulaEvaluationVisitor<T> pEvaluationVisitor) {
    this.toPush = pToPush;
    this.evaluationVisitor = pEvaluationVisitor;
  }

  public boolean isSummandConsumed() {
    return consumed;
  }

  @Override
  public InvariantsFormula<T> visit(Add<T> pAdd, Map<? extends String, ? extends InvariantsFormula<T>> pEnvironment) {
    InvariantsFormula<T> candidateS1 = pAdd.getSummand1().accept(this, pEnvironment);
    if (isSummandConsumed()) {
      return InvariantsFormulaManager.INSTANCE.add(candidateS1, pAdd.getSummand2());
    }
    InvariantsFormula<T> summand2 = pAdd.getSummand2().accept(this, pEnvironment);
    return InvariantsFormulaManager.INSTANCE.add(pAdd.getSummand1(), summand2);
  }

  @Override
  public InvariantsFormula<T> visit(Constant<T> pConstant, Map<? extends String, ? extends InvariantsFormula<T>> pEnvironment) {
    InvariantsFormula<T> sum = InvariantsFormulaManager.INSTANCE.add(pConstant, toPush);
    return InvariantsFormulaManager.INSTANCE.asConstant(sum.accept(evaluationVisitor, pEnvironment));
  }

  @Override
  public InvariantsFormula<T> visit(Negate<T> pNegate, Map<? extends String, ? extends InvariantsFormula<T>> pEnvironment) {
    InvariantsFormula<T> negatedSummand =
        InvariantsFormulaManager.INSTANCE.negate(toPush);
    T negatedValue = negatedSummand.accept(this.evaluationVisitor, pEnvironment);
    negatedSummand = InvariantsFormulaManager.INSTANCE.asConstant(negatedValue);
    if (negatedSummand instanceof Constant<?>) {
      PushSummandVisitor<T> negatedVisitor = new PushSummandVisitor<>((Constant<T>) negatedSummand, evaluationVisitor);
      InvariantsFormula<T> candidate = pNegate.accept(negatedVisitor, pEnvironment);
      if (negatedVisitor.isSummandConsumed()) {
        this.consumed = true;
      }
      return candidate;
    }
    return visitDefault(pNegate, pEnvironment);
  }

  @Override
  protected InvariantsFormula<T> visitDefault(InvariantsFormula<T> pFormula, Map<? extends String, ? extends InvariantsFormula<T>> pEnvironment) {
    return InvariantsFormulaManager.INSTANCE.add(pFormula, this.toPush);
  }

}
