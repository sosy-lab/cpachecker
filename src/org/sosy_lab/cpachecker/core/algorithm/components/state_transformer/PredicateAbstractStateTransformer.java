// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.state_transformer;

import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateAbstractStateTransformer extends AbstractStateTransformer<PredicateAbstractState> {

  public PredicateAbstractStateTransformer(String pId) {
    super(pId);
  }

  @Override
  protected BooleanFormula transform(
      PredicateAbstractState state, FormulaManagerView fmgr, AnalysisDirection direction, String uniqueVariableId) {
    PathFormula pathFormula = state.getPathFormula();
    SSAMap ssaMap = pathFormula.getSsa();
    return uninstantiate(fmgr, pathFormula.getFormula(), ssaMap, direction);
  }

  @Override
  public Class<PredicateAbstractState> getAbstractStateClass() {
    return PredicateAbstractState.class;
  }
}
