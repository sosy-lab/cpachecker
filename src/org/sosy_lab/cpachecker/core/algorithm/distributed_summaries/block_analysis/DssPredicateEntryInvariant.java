// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.AbstractStates;

/** A proved entry invariant, serialized so workers never share solver-owned formulas. */
public record DssPredicateEntryInvariant(
    String formula, ImmutableMap<String, CType> variableTypes) {

  /**
   * Conjoins the invariant at the incoming SSA indices. Variables missing from an unconstrained
   * entry start at index one, just like the first read in the path-formula converter. Recording
   * their types and indices is essential: leaving a variable uninstantiated would disconnect the
   * invariant from program execution and could later confuse entry values with exit values.
   */
  public ARGState strengthen(ARGState state, PredicateCPA cpa) throws InterruptedException {
    var predicate =
        requireNonNull(AbstractStates.extractStateByType(state, PredicateAbstractState.class));
    checkArgument(predicate.isAbstractionState(), "Expected an abstract block-entry state");
    var fmgr = cpa.getSolver().getFormulaManager();
    var invariant = fmgr.parse(formula);
    var path = predicate.getPathFormula();
    var entryFormula =
        fmgr.getBooleanFormulaManager()
            .and(predicate.getAbstractionFormula().asInstantiatedFormula(), path.getFormula());
    if (!fmgr.instantiate(fmgr.uninstantiate(entryFormula), path.getSsa()).equals(entryFormula)) {
      // An unusual input can still contain intermediate SSA values. Turning those into an
      // abstraction would conflate them with the current values; skip the optional strengthening.
      return state;
    }
    var ssa = path.getSsa().builder();
    for (var variable : fmgr.extractVariableNames(invariant)) {
      if (!path.getSsa().containsVariable(variable)) {
        ssa.setIndex(variable, requireNonNull(variableTypes.get(variable)), 1);
      }
    }
    path = path.withContext(ssa.build(), path.getPointerTargetSet());
    path =
        path.withFormula(
            fmgr.getBooleanFormulaManager()
                .and(
                    predicate.getAbstractionFormula().asInstantiatedFormula(),
                    path.getFormula(),
                    fmgr.instantiate(invariant, path.getSsa())));
    var replacement =
        PredicateAbstractState.mkAbstractionState(
            path,
            cpa.getPredicateManager().asAbstraction(fmgr.uninstantiate(path.getFormula()), path),
            predicate.getAbstractionLocationsOnPath());
    var components = new ArrayList<AbstractState>();
    for (var component : ((CompositeState) state.getWrappedState()).getWrappedStates()) {
      components.add(component == predicate ? replacement : component);
    }
    return new ARGState(new CompositeState(components), null);
  }
}
