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

  public AnyStateTransformer(String pId) {
    super(pId);
    transformerMap = new HashMap<>();
    register(new ARGAbstractStateTransformer(pId, this));
    register(new CompositeAbstractStateTransformer(pId, this));
    register(new PredicateAbstractStateTransformer(pId));
    register(new ValueAbstractStateTransformer(pId));
  }

  public void register(AbstractStateTransformer<?> transformer) {
    transformerMap.put(transformer.getAbstractStateClass(), transformer);
  }

  @Override
  protected BooleanFormula transform(
      AbstractState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    Class<?> superClasses = state.getClass();
    while (superClasses != null) {
      if (transformerMap.containsKey(superClasses)) {
        return transformerMap.get(superClasses).safeTransform(state, fmgr, direction, uniqueVariableId);
      }
      superClasses = superClasses.getSuperclass();
    }

    return fmgr.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public Class<AbstractState> getAbstractStateClass() {
    return AbstractState.class;
  }
}
