// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.state_transformer;

import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ValueAbstractStateTransformer extends AbstractStateTransformer<ValueAnalysisState>{

  @Override
  protected BooleanFormula transform(
      ValueAnalysisState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    return state.getFormulaApproximation(fmgr);
  }

  @Override
  public Class<ValueAnalysisState> getAbstractStateClass() {
    return ValueAnalysisState.class;
  }
}
