// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.PrintStream;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatIntHist;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;

class AutomatonStatistics implements Statistics {

  private final Automaton automaton;

  ThreadSafeTimerContainer totalPostTime       = new ThreadSafeTimerContainer("Total time for successor computation");
  ThreadSafeTimerContainer matchTime           = new ThreadSafeTimerContainer("Time for transition matches");
  ThreadSafeTimerContainer assertionsTime      = new ThreadSafeTimerContainer("Time for transition assertions");
  ThreadSafeTimerContainer actionTime          = new ThreadSafeTimerContainer("Time for transition actions");
  ThreadSafeTimerContainer totalStrengthenTime = new ThreadSafeTimerContainer("Total time for strengthen operator");
  StatIntHist automatonSuccessors = new StatIntHist(StatKind.AVG, "Automaton transfer successors");

  public AutomatonStatistics(Automaton pAutomaton) {
    automaton = pAutomaton;
  }

  @Override
  public String getName() {
    return "AutomatonAnalysis (" + automaton.getName() + ")";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    put(out, 0, "Number of states", automaton.getNumberOfStates());
    put(out, 0, totalPostTime);

    if (totalPostTime.getSumTime().compareTo(TimeSpan.ofMillis(500)) >= 0) {
      // normally automaton is very fast, and time measurements are very imprecise
      // so don't care about very small times
      put(out, 1, matchTime);
      put(out, 1, assertionsTime);
      put(out, 1, actionTime);
    }

    if (totalStrengthenTime.getUpdateCount() > 0) {
      put(out, 0, totalStrengthenTime);
    }

    long stateBranchings =
        automatonSuccessors.getValueCount()
            - automatonSuccessors.getTimesWithValue(0)
            - automatonSuccessors.getTimesWithValue(1);
    put(out, 0, "Automaton transfers with branching", stateBranchings);
    put(out, 0, automatonSuccessors);

    int statesWithAssumptionTransitions = 0;
    for (AutomatonInternalState state : automaton.getStates()) {
      if (state.getTransitions().stream().anyMatch(p -> p.isTransitionWithAssumptions())) {
        statesWithAssumptionTransitions++;
      }
    }
    put(out, 0, "Number of states with assumption transitions", statesWithAssumptionTransitions);
  }
}
