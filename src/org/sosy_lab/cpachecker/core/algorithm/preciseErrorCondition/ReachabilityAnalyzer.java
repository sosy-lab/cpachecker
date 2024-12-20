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
import java.util.logging.Level;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ReachabilityAnalyzer {

  private final ConfigurableProgramAnalysis cpa;
  private final FormulaContext context;
  private final int currentIteration;

  public ReachabilityAnalyzer(
      ConfigurableProgramAnalysis pCPA,
      FormulaContext pContext,
      int pCurrentIteration) {
    this.cpa = pCPA;
    this.context = pContext;
    currentIteration = pCurrentIteration;
  }

  public AbstractState getInitialState() throws InterruptedException {
    return cpa.getInitialState(context.getMutableCFA().getMainFunction(),
        StateSpacePartition.getDefaultPartition());
  }

  public ReachedSet updateReachedSet(ReachedSet reachedSet, AbstractState initialState)
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
}
