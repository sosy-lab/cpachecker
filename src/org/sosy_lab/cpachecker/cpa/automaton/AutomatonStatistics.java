/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

class AutomatonStatistics extends AbstractStatistics {

  private final ControlAutomatonCPA mCpa;

  public AutomatonStatistics(ControlAutomatonCPA pCpa) {
    mCpa = pCpa;
  }

  @Override
  public String getName() {
    return "AutomatonAnalysis (" + mCpa.getAutomaton().getName() + ")";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    AutomatonTransferRelation trans = mCpa.getTransferRelation();

    put(out, 0, "Number of states", mCpa.getAutomaton().getNumberOfStates());
    put(out, 0, "Total time for successor computation", trans.totalPostTime);

    if (trans.totalPostTime.getSumTime().compareTo(TimeSpan.ofMillis(500)) >= 0) {
      // normally automaton is very fast, and time measurements are very imprecise
      // so don't care about very small times
      put(out, 1, "Time for transition matches", trans.matchTime);
      put(out, 1, "Time for transition assertions", trans.assertionsTime);
      put(out, 1, "Time for transition actions", trans.actionTime);
    }

    if (trans.totalStrengthenTime.getNumberOfIntervals() > 0) {
      put(out, 0, "Total time for strengthen operator", trans.totalStrengthenTime);
    }

    int stateBranchings = trans.automatonSuccessors.getValueCount()
        - trans.automatonSuccessors.getTimesWithValue(0)
        - trans.automatonSuccessors.getTimesWithValue(1);
    put(out, 0, "Automaton transfers with branching", stateBranchings);
    put(out, 0, "Automaton transfer successors", trans.automatonSuccessors);
  }
}
