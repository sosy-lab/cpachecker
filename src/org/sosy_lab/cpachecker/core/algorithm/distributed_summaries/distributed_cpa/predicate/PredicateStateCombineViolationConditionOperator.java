// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateStateCombineViolationConditionOperator
    implements CombineViolationConditionsOperator {

  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManagerView bfmgr;

  public PredicateStateCombineViolationConditionOperator(
      FormulaManagerView pFormulaManagerView, PathFormulaManager pPathFormulaManager) {
    pfmgr = pPathFormulaManager;
    bfmgr = pFormulaManagerView.getBooleanFormulaManager();
  }

  private Set<BooleanFormula> toAtoms(PredicateAbstractState state) {
    return bfmgr.toConjunctionArgs(state.getPathFormula().getFormula(), true);
  }

  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(
      Optional<AbstractState> origin, Collection<AbstractState> states)
      throws InterruptedException {
    PathFormula prev = null;
    PredicateAbstractState previousState = null;
    Set<@NonNull BooleanFormula> originalPrefix =
        origin.map(state -> toAtoms((PredicateAbstractState) state)).orElse(ImmutableSet.of());
    for (AbstractState state : states) {
      Preconditions.checkState(
          state instanceof PredicateAbstractState, "All states must be PredicateAbstractStates.");
      PredicateAbstractState predicateState = (PredicateAbstractState) state;
      Set<@NonNull BooleanFormula> atoms = toAtoms(predicateState);
      PathFormula pathFormula =
          predicateState
              .getPathFormula()
              .withFormula(bfmgr.and(Sets.difference(atoms, originalPrefix)));
      if (prev == null) {
        prev = pathFormula;
      } else {
        prev = pfmgr.makeOr(prev, pathFormula);
      }
      previousState = predicateState;
    }
    Preconditions.checkNotNull(previousState);
    BooleanFormula originalPrefixFormula = bfmgr.and(originalPrefix);
    BooleanFormula wholeFormula = bfmgr.and(originalPrefixFormula, prev.getFormula());
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        prev.withFormula(wholeFormula), previousState);
  }
}
