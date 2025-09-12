// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ValueStateCoverageOperator implements CoverageOperator {

  private final FormulaManagerView formulaManagerView;

  public ValueStateCoverageOperator(FormulaManagerView pFormulaManagerView) {
    formulaManagerView = pFormulaManagerView;
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    BooleanFormula formula1 = ((ValueAnalysisState) state1).getFormulaApproximation(formulaManagerView);
    BooleanFormula formula2 = ((ValueAnalysisState) state2).getFormulaApproximation(formulaManagerView);

    BooleanFormula oneImpliesTwo = formulaManagerView.makeOr(formulaManagerView.makeNot(formula1), formula2);
    return formulaManagerView.getBooleanFormulaManager().isTrue(oneImpliesTwo);
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
