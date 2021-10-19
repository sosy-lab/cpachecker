// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.state_transformer;

import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ARGAbstractStateTransformer extends AbstractStateTransformer<ARGState> {

  private final AnyStateTransformer knownTransformations;

  public ARGAbstractStateTransformer(AnyStateTransformer pKnownTransformations) {
    knownTransformations = pKnownTransformations;
  }

  @Override
  protected BooleanFormula transform(
      ARGState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    if (state.getWrappedState() == null) {
      return fmgr.getBooleanFormulaManager().makeTrue();
    }
    return knownTransformations.safeTransform(state.getWrappedState(), fmgr, direction, uniqueVariableId);
  }

  @Override
  public Class<ARGState> getAbstractStateClass() {
    return ARGState.class;
  }
}
