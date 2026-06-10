// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

public class PredicateStateCombineViolationConditionOperator
    implements CombineViolationConditionsOperator {

  private final PathFormulaManager pfmgr;

  public PredicateStateCombineViolationConditionOperator(PathFormulaManager pPfmgr) {
    pfmgr = pPfmgr;
  }

  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(Collection<AbstractState> states)
      throws InterruptedException {
    PathFormula prev = null;
    PredicateAbstractState previousState = null;
    for (AbstractState state : states) {
      Preconditions.checkState(
          state instanceof PredicateAbstractState, "All states must be PredicateAbstractStates.");
      PathFormula pathFormula = ((PredicateAbstractState) state).getPathFormula();
      if (prev == null) {
        prev = pathFormula;
      } else {
        prev = pfmgr.makeOr(prev, pathFormula);
      }
      previousState = (PredicateAbstractState) state;
    }
    Preconditions.checkNotNull(previousState);
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(prev, previousState);
  }
}
