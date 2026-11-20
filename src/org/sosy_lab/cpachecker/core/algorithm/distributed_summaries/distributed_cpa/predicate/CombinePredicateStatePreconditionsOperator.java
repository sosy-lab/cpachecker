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
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class CombinePredicateStatePreconditionsOperator implements CombinePreconditionsOperator {

  private final PredicateCPA predicateCPA;

  public CombinePredicateStatePreconditionsOperator(PredicateCPA pPredicateCPA) {
    predicateCPA = pPredicateCPA;
  }

  @Override
  public Optional<AbstractState> combineIfPossible(Collection<AbstractState> states)
      throws InterruptedException {
    if (states.isEmpty()
        || states.stream()
            .anyMatch(s -> !(s instanceof PredicateAbstractState p) || !p.isAbstractionState())) {
      return Optional.empty();
    }
    return Optional.of(combinePreconditions(states));
  }

  /**
   * Combine multiple PredicateAbstractStates into one by taking the disjunction of their
   * abstraction formulas. The resulting abstraction state retains the merged SSA and pointer
   * context of the block formulas.
   *
   * <p>This method assumes that all provided states are abstraction states.
   *
   * @param states the collection of states to combine
   * @return the combined PredicateAbstractState
   */
  @Override
  public AbstractState combinePreconditions(Collection<AbstractState> states)
      throws InterruptedException {
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
      first = predicateCPA.getPredicateManager().makeOr(first, formulas.get(i));
    }

    FormulaManagerView formulaManager = predicateCPA.getSolver().getFormulaManager();
    PathFormula pathFormula =
        predicateCPA
            .getPathFormulaManager()
            .makeEmptyPathFormulaWithContextFrom(first.getBlockFormula());
    // Counterexample checking uses the path formulas after the ARG root, without its abstraction.
    // Keep the entry constraint in the first path formula so refinement can refute paths that are
    // feasible only outside the received precondition. Reinstantiate at the merged SSA indices.
    pathFormula =
        pathFormula.withFormula(
            formulaManager.instantiate(first.asFormula(), pathFormula.getSsa()));
    return PredicateAbstractState.mkAbstractionState(
        pathFormula,
        predicateCPA.getPredicateManager().asAbstraction(first.asFormula(), pathFormula),
        PathCopyingPersistentTreeMap.of());
  }
}
