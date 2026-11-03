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
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicateStateCombineViolationConditionOperator
    implements CombineViolationConditionsOperator {

  private final PathFormulaManager pfmgr;

  /** Projects the local variables out of the combined condition, or {@code null} to keep them. */
  private final @Nullable ExistentialProjection projection;

  public PredicateStateCombineViolationConditionOperator(
      PathFormulaManager pPfmgr, @Nullable ExistentialProjection pProjection) {
    pfmgr = pPfmgr;
    projection = pProjection;
  }

  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(Collection<AbstractState> states)
      throws InterruptedException, CPAException {
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
    Preconditions.checkNotNull(prev);
    Preconditions.checkNotNull(previousState);
    if (projection != null && states.size() > 1) {
      // makeOr aligns the SSA indices of the disjuncts by equalities between intermediate
      // variables, which the projection removes again
      try {
        prev = projection.projectLocalVariables(prev);
      } catch (SolverException e) {
        throw new CPAException("Could not project the combined violation condition", e);
      }
    }
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(prev, previousState);
  }
}
