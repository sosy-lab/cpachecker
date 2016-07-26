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

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

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
  private ARGCPA cpa;

  public CounterExampleReplayEngine(ARGCPA pCpa, LogManager pLogger) {
    cpa = pCpa;
    logger = pLogger;
  }

  public ARGPath replayCounterExample(CounterexampleInfo pCex) {
    List<ARGState> states = new ArrayList<>();
    List<CFAEdge> edges = new ArrayList<>();

    CFAPathWithAssumptions cfaPath = pCex.getCFAPathWithAssignments();

    CFANode initialNode = cfaPath.get(0).getCFAEdge().getPredecessor();
    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();

    AbstractState currentState = cpa.getInitialState(initialNode, partition);
    Precision currentPrecision = cpa.getInitialPrecision(initialNode, partition);

    for (int i = 0; i < cfaPath.size(); i++) {
      CFAEdgeWithAssumptions edge = cfaPath.get(0);

      CFAEdge cfaEdge = edge.getCFAEdge();

      Set<ARGState> succs = getChildren(cfaEdge, currentState, currentPrecision);

      assert (succs.size() == 1);

      currentState = succs.iterator().next();
      states.add((ARGState) currentState);
      edges.add(cfaEdge);

      // handle weaved states
    }

    ARGPath path = new ARGPath(states, edges);

    return path;
  }

  private Set<ARGState> getChildren(CFAEdge pCfaEdge, AbstractState pState,
      Precision pPrecision) {
    Set<ARGState> successors = new HashSet<>();

    Collection<? extends AbstractState> succs = null;
    try {
      succs = cpa.getTransferRelation().getAbstractSuccessors(pState, pPrecision);
    } catch (CPATransferException | InterruptedException e) {
      logger.logf(Level.WARNING,
          "Failed to get next abstract state when calculating presence conditions for test cases.");

      return null;
    }

    int targetLocation = pCfaEdge.getSuccessor().getNodeNumber();

    for (AbstractState succ : succs) {
      assert (succ instanceof ARGState);
      ARGState state = (ARGState) succ;

      AbstractState wrappedState = state.getWrappedState();
      assert (wrappedState instanceof CompositeState);

      int location = -1;

      CompositeState wState = (CompositeState) wrappedState;
      for (AbstractState child : wState.getWrappedStates()) {
        if (child instanceof LocationState) {
          LocationState locationState = (LocationState) child;
          location = locationState.getLocationNode().getNodeNumber();
          break;
        }
      }

      if (targetLocation == location) {
        successors.add(state);
      }
    }

    return successors;
  }

}
