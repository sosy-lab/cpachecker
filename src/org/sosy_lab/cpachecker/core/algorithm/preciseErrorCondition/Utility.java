// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import com.google.common.collect.FluentIterable;
import static com.google.common.collect.FluentIterable.from;
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

public final class Utility {

  private Utility() {
    // Prevent instantiation
  }

  public static AbstractState getInitialState(
      ConfigurableProgramAnalysis pCpa,
      FormulaContext pContext) throws InterruptedException {
    return pCpa.getInitialState(
        pContext.getMutableCFA().getMainFunction(),
        StateSpacePartition.getDefaultPartition()
    );
  }

  public static ReachedSet updateReachedSet(
      ReachedSet pReachedSet,
      AbstractState pInitialState,
      int pCurrentIteration,
      ConfigurableProgramAnalysis cpa,
      FormulaContext context) throws InterruptedException {
    pReachedSet.clear();
    pReachedSet.add(pInitialState,
        cpa.getInitialPrecision(
            context.getMutableCFA().getMainFunction(),
            StateSpacePartition.getDefaultPartition()));

    context.getLogger().log(Level.FINE, String.format(
        "Iteration %d: Reached Set: \n : %s",
        pCurrentIteration, pReachedSet));
    return pReachedSet;
  }

  public static FluentIterable<CounterexampleInfo> getCounterexample(ReachedSet pReachedSet) {
    return Optionals.presentInstances(
        from(pReachedSet)
            .filter(AbstractStates::isTargetState)
            .filter(ARGState.class)
            .transform(ARGState::getCounterexampleInformation));
  }

  // Instrument the initial state of the program to exclude the already explored paths
  // (i.e., the error condition at this iteration representing the already discovered error-inducing
  // inputs)
  public static AbstractState updateInitialStateWithExclusions(
      AbstractState pInitialState,
      PathFormula pExclusionFormula,
      int pCurrentIteration,
      FormulaContext pContext) {
    Builder<AbstractState> initialAbstractStates = ImmutableList.builder();
    for (AbstractState abstractState : AbstractStates.asIterable(pInitialState)) {
      if (abstractState instanceof ARGState) {
        continue;
      }
      if (abstractState instanceof CompositeState) {
        continue;
      }
      if (abstractState instanceof PredicateAbstractState predicateState) {
        PersistentMap<CFANode, Integer> locations =
            predicateState.getAbstractionLocationsOnPath();
        initialAbstractStates.add(PredicateAbstractState.mkAbstractionState(
            pExclusionFormula,
            predicateState.getAbstractionFormula(),
            locations));
      } else {
        initialAbstractStates.add(abstractState);
      }
    }
    pContext.getLogger().log(Level.INFO,
        String.format(
            "Iteration %d: Updated Initial State With The Exclusion Formula For Next Iteration.",
            pCurrentIteration));
    pContext.getLogger()
        .log(Level.FINER, String.format("Iteration %s: Updated Initial State: ", pInitialState));
    return new ARGState(new CompositeState(initialAbstractStates.build()), null);
  }

  public static void logWithIteration(
      int pCurrentRefinementIteration,
      Level pLoggingLevel,
      FormulaContext pContext,
      String pMessage) {
    pContext.getLogger().log(pLoggingLevel,
        String.format("Iteration %d - %s \n",
            pCurrentRefinementIteration,
            pMessage));
  }
}