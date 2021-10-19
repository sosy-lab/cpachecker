// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.state_transformer;

import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public abstract class AbstractStateTransformer<T extends AbstractState> {

  protected abstract BooleanFormula transform(T state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId);

  public abstract Class<T> getAbstractStateClass();

  public final BooleanFormula safeTransform(AbstractState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    if (!getAbstractStateClass().isAssignableFrom(state.getClass())) {
      throw new AssertionError("Cannot transform member of class " + state.getClass() + " in a "
          + getAbstractStateClass() + " transformer");
    }
    return fmgr.uninstantiate(transform(getAbstractStateClass().cast(state), fmgr, direction, uniqueVariableId));
  }

}
