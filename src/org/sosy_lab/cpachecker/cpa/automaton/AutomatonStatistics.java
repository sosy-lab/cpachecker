/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
