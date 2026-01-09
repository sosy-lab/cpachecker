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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class CombinePredicateStateOperator implements CombineOperator {

  private final PredicateCPA predicateCPA;

  public CombinePredicateStateOperator(PredicateCPA pPredicateCPA) {
    predicateCPA = pPredicateCPA;
  }

  /**
   * Combine multiple PredicateAbstractStates into one by taking the disjunction of their
   * abstraction formulas.
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
    ImmutableSet<@NonNull BooleanFormula> booleanFormulas =
        predicateAbstractStates
            .transform(s -> s.getAbstractionFormula().asInstantiatedFormula())
            .toSet();

    // Merge SSAMaps
    SSAMapBuilder ssaMap = SSAMap.emptySSAMap().builder();
    for (PathFormula formula : predicateAbstractStates.transform(s -> s.getPathFormula())) {
      for (String variable : formula.getSsa().allVariables()) {
        ssaMap.setIndex(variable, formula.getSsa().getType(variable), 1);
      }
    }

    // Merge PointerTargetSets
    PathFormulaManager pathFormulaManager = predicateCPA.getPathFormulaManager();
    PointerTargetSet combined = null;
    for (PointerTargetSet pts :
        predicateAbstractStates.transform(p -> p.getPathFormula().getPointerTargetSet())) {
      if (combined == null) {
        combined = pts;
      } else {
        combined = pathFormulaManager.mergePts(combined, pts, ssaMap);
      }
    }
    Preconditions.checkNotNull(combined, "Combined PointerTargetSet should not be null.");
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        pathFormulaManager
            .makeEmptyPathFormula()
            .withContext(ssaMap.build(), combined)
            .withFormula(
                predicateCPA
                    .getSolver()
                    .getFormulaManager()
                    .getBooleanFormulaManager()
                    .or(booleanFormulas)),
        (PredicateAbstractState) Objects.requireNonNull(Iterables.get(states, 0)));
  }
}
