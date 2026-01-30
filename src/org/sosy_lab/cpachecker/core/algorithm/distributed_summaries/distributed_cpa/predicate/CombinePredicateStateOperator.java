// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

public class CombinePredicateStateOperator implements CombineOperator {

  private final PredicateCPA predicateCPA;

  public CombinePredicateStateOperator(PredicateCPA pPredicateCPA) {
    predicateCPA = pPredicateCPA;
  }

  /**
   * Combine multiple PredicateAbstractStates into one by taking the disjunction of their
   * abstraction formulas. The resulting state is a non-abstraction state with a path formula where
   * all SSA indices are set to 1 and the PointerTargetSet is merged accordingly (delegated to
   * {@link PathFormulaManager#mergePts(PointerTargetSet, PointerTargetSet, SSAMapBuilder)})
   *
   * <p>This method assumes that all provided states are abstraction states.
   *
   * @param states the collection of states to combine
   * @return the combined PredicateAbstractState
   */
  @Override
  public AbstractState combine(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(!states.isEmpty(), "There must be at least one state to combine.");
    FluentIterable<@NonNull PredicateAbstractState> predicateAbstractStates =
        FluentIterable.from(states).filter(PredicateAbstractState.class);
    predicateAbstractStates =
        predicateAbstractStates.filter(PredicateAbstractState::isAbstractionState);
    Preconditions.checkArgument(
        states.size() == predicateAbstractStates.size(),
        "All states must be PredicateAbstractStates and abstraction states.");

    ImmutableList<@NonNull AbstractionFormula> formulas =
        predicateAbstractStates.transform(p -> p.getAbstractionFormula()).toList();

    AbstractionFormula first = formulas.getFirst();
    for (int i = 1; i < formulas.size(); i++) {
      first = predicateCPA.getPredicateManager().makeAnd(first, formulas.get(i));
    }

    return PredicateAbstractState.mkAbstractionState(
        predicateCPA.getPathFormulaManager().makeEmptyPathFormula(),
        first,
        PathCopyingPersistentTreeMap.of());
  }
}
