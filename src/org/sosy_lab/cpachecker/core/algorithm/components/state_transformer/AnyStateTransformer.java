// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.state_transformer;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AnyStateTransformer extends AbstractStateTransformer<AbstractState>{

  private final Map<Class<? extends AbstractState>, AbstractStateTransformer<? extends AbstractState>> transformerMap;

  public AnyStateTransformer() {
    transformerMap = new HashMap<>();
    register(new ARGAbstractStateTransformer(this));
    register(new CompositeAbstractStateTransformer(this));
    register(new PredicateAbstractStateTransformer());
    register(new ValueAbstractStateTransformer());
  }

  public void register(AbstractStateTransformer<?> transformer) {
    transformerMap.put(transformer.getAbstractStateClass(), transformer);
  }

  @Override
  protected BooleanFormula transform(
      AbstractState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    if (transformerMap.containsKey(state.getClass())) {
      return transformerMap.get(state.getClass()).safeTransform(state, fmgr, direction, uniqueVariableId);
    }
    return fmgr.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public Class<AbstractState> getAbstractStateClass() {
    return AbstractState.class;
  }
}
