/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Replays a counter example (ARG path) and adds a presence condition
 * over features to every state based on the BDD CPA.
 */
public class CounterExampleReplayEngine {

  private final LogManager logger;
  private final ARGCPA cpa;

  public CounterExampleReplayEngine(ARGCPA pCpaForReplay, LogManager pLogger) {
    cpa = Preconditions.checkNotNull(pCpaForReplay);
    logger = Preconditions.checkNotNull(pLogger);
  }

  public ARGPath replayCounterExample(CounterexampleInfo pCex)
      throws CPATransferException, InterruptedException {

    List<ARGState> states = Lists.newArrayList();
    List<CFAEdge> edges = Lists.newArrayList();

    final PathIterator it = pCex.getTargetPath().fullPathIterator();
    final CFANode rootLocation = it.getLocation();
    final StateSpacePartition partition = StateSpacePartition.getDefaultPartition();

    ARGState currentState = (ARGState) cpa.getInitialState(rootLocation, partition);
    Precision currentPrecision = cpa.getInitialPrecision(rootLocation, partition);

    states.add(currentState);

    while (it.hasNext()) {
      final CFAEdge edge = it.getOutgoingEdge();
      edges.add(edge);
      it.advance();
      final ARGState inputPathSuccessor = it.getAbstractState();

      Preconditions.checkState(inputPathSuccessor != null, "We require a path without leaks!");

      final CFANode expectedSuccessorLocation = AbstractStates.extractLocationMaybeWeavedOn(it.getAbstractState());

      Collection<? extends AbstractState> successors =
          cpa.getTransferRelation().getAbstractSuccessors(currentState, currentPrecision);
      Preconditions.checkState(successors.size() > 0, "There should be always a successor! (For: " + edge.toString());

      Collection<? extends AbstractState> successorsOnPath = Lists.newArrayList(AbstractStates
          .filterLocationMaybeWeavedOn(successors, expectedSuccessorLocation));
      Preconditions.checkState(successorsOnPath.iterator().hasNext(), "Filtering resulted in no successor candidates: " + edge.toString());

      currentState = (ARGState) filterMatchingState(successorsOnPath, inputPathSuccessor);
      Preconditions.checkState(currentState != null, "No matching successor along path!");
      states.add(currentState);
    }

    ARGPath path = new ARGPath(states, edges);

    return path;
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
        Set<AutomatonInternalState> candidateAutomatonStates =
            extractInternalStates(candidate);

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
