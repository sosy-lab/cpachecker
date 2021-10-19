// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.state_transformer;

import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class CompositeAbstractStateTransformer extends AbstractStateTransformer<CompositeState> {

  private final AnyStateTransformer knownTransformations;

  public CompositeAbstractStateTransformer(AnyStateTransformer pKnownTransformations) {
    knownTransformations = pKnownTransformations;
  }

  @Override
  protected BooleanFormula transform(
      CompositeState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    return state.getWrappedStates().stream()
        .map(currState -> knownTransformations.safeTransform(currState, fmgr, direction, uniqueVariableId))
        .collect(fmgr.getBooleanFormulaManager().toConjunction());
  }

  @Override
  public Class<CompositeState> getAbstractStateClass() {
    return CompositeState.class;
  }
}
