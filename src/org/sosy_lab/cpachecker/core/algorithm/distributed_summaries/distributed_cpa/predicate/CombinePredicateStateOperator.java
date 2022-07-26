// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

public class CombinePredicateStateOperator implements CombineOperator {

  private final PathFormulaManager manager;

  public CombinePredicateStateOperator(PathFormulaManager pPathFormulaManager) {
    manager = pPathFormulaManager;
  }

  @Override
  public List<AbstractState> combine(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    PredicateAbstractState state1 = (PredicateAbstractState) pState1;
    PredicateAbstractState state2 = (PredicateAbstractState) pState2;

    PathFormula newFormula = manager.makeOr(state1.getPathFormula(), state2.getPathFormula());
    return ImmutableList.of(
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            newFormula, state1, state2.getPreviousAbstractionState()));
  }
}
