/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.presence;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Assume;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class PathReplayEngine implements VariabilityAwarePathReplay {

  private final LogManager logger;

  public PathReplayEngine(LogManager pLogger) {
    logger = Preconditions.checkNotNull(pLogger);
  }

  public ARGPathWithPresenceConditions replayPath(ARGPath pARGPath)
      throws CPATransferException, InterruptedException {

    Preconditions.checkNotNull(pARGPath);

    // We assume that the path is FEASIBLE!!!!

    List<ARGState> states = Lists.newArrayList();
    List<CFAEdge> edges = Lists.newArrayList();
    List<PresenceCondition> conditions = Lists.newArrayList();

    final PathIterator it = pARGPath.fullPathIterator();

    PresenceCondition lastPc = PresenceConditions.manager().makeTrue();

    states.add(it.getAbstractState());
    conditions.add(lastPc);

    while (it.hasNext()) {
      final CFAEdge edge = it.getOutgoingEdge();
      edges.add(edge);
      it.advance();
      final ARGState inputPathSuccessor = it.getAbstractState();

      Preconditions.checkState(inputPathSuccessor != null, "We require a path without leaks!");

      if (encodesPresenceCondition(edge)) {
        lastPc = PresenceConditions.manager().makeAnd(lastPc, edge);
      }

      conditions.add(lastPc);
      states.add(it.getAbstractState());
    }

    return new ARGPathWithPresenceConditions(states, conditions, edges);
  }

  private boolean encodesPresenceCondition(CFAEdge pEdge) {
    if (!(pEdge instanceof AssumeEdge)) {
      return false;
    }

    if (!pEdge.getCode().contains("FEATURE")) {
      return false;
    }

    return true;
  }

  private AbstractState filterMatchingState(
      Collection<? extends AbstractState> pSuccessorCandidates,
      ARGState pInputPathSuccessor) {

    if (pSuccessorCandidates.size() == 1) {
      return pSuccessorCandidates.iterator().next();
    } else {
      Set<AutomatonInternalState> inputAutomatonStates =
          extractInternalStates(pInputPathSuccessor);

      for (AbstractState candidate: pSuccessorCandidates) {
        // There might be several successors with the same
        //  location if one of the analysis decided to provide more than one successor state.
        //
        //  Typical example:
        //    The automaton CPA decides to provide two or more successors.
        Set<AutomatonInternalState> candidateAutomatonStates = extractInternalStates(candidate);

        if (inputAutomatonStates.containsAll(candidateAutomatonStates)) {
          return candidate;
        }
      }
    }

    return null;
  }

  private Set<AutomatonInternalState> extractInternalStates(AbstractState pState) {
    Collection<AutomatonState> automatonStates =
        AbstractStates.extractStatesByType(pState, AutomatonState.class);
    return Sets.newHashSet(Collections2.transform(automatonStates,
        AutomatonState::getInternalState));
  }


}
