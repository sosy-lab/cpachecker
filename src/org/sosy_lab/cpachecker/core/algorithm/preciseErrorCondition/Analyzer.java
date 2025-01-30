// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.logging.Level;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public class Analyzer {

  private final ConfigurableProgramAnalysis cpa;
  private final FormulaContext context;

  public Analyzer(
      ConfigurableProgramAnalysis pCPA,
      FormulaContext pContext) {
    cpa = pCPA;
    context = pContext;
  }

  public AbstractState getInitialState() throws InterruptedException {
    return cpa.getInitialState(context.getMutableCFA().getMainFunction(),
        StateSpacePartition.getDefaultPartition());
  }

  public ReachedSet updateReachedSet(ReachedSet reachedSet, AbstractState initialState, int currentIteration)
      throws InterruptedException {
    reachedSet.clear();
    reachedSet.add(initialState,
        cpa.getInitialPrecision(context.getMutableCFA().getMainFunction(),
            StateSpacePartition.getDefaultPartition()));

    context.getLogger().log(Level.FINE, String.format(
        "Iteration %d: Reached Set: \n : %s",
        currentIteration, reachedSet));
    return reachedSet;
  }

  public FluentIterable<CounterexampleInfo> getCounterexamples(ReachedSet reachedSet) {
    return Optionals.presentInstances(
        from(reachedSet)
            .filter(AbstractStates::isTargetState)
            .filter(ARGState.class)
            .transform(ARGState::getCounterexampleInformation));
  }

  // Update the initial state with exclusion formulas for the next run
  public AbstractState updateInitialStateWithExclusions(
      AbstractState initialState,
      PathFormula exclusionFormula,
      int currentIteration) {
    Builder<AbstractState> initialAbstractStates = ImmutableList.builder();
    for (AbstractState abstractState : AbstractStates.asIterable(initialState)) {
      if (abstractState instanceof ARGState) {
        // TODO handle ARGState instances
        continue;
      }
      if (abstractState instanceof CompositeState) {
        // TODO handle CompositeState instances
        continue;
      }
      if (abstractState instanceof PredicateAbstractState predicateState) {
        PersistentMap<CFANode, Integer> locations =
            predicateState.getAbstractionLocationsOnPath();
        initialAbstractStates.add(PredicateAbstractState.mkAbstractionState(exclusionFormula,
            predicateState.getAbstractionFormula(), locations));
      } else {
        initialAbstractStates.add(abstractState);
      }
    }
    context.getLogger().log(Level.INFO,
        String.format(
            "Iteration %d: Updated initial state with the exclusion formula for next iteration.",
            currentIteration));
    context.getLogger().log(Level.FINE, String.format("Iteration %s: Updated initial state: ", initialState));
    return new ARGState(new CompositeState(initialAbstractStates.build()), null);
  }
}
