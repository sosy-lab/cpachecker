// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.tests;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORState;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORThread;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORTests {

  MPORAlgorithm algorithm;

  public MPORTests(MPORAlgorithm pAlgorithm) {
    algorithm = pAlgorithm;
  }

  // computes all possible program states (i.e. all interleavings) and the corresponding preference
  // orders, no matter if they are actually feasible
  public void computeAllStates() {
    ImmutableMap.Builder<MPORThread, CFANode> stateBuilder = ImmutableMap.builder();
    for (MPORThread thread : algorithm.getThreads()) {
      stateBuilder.put(thread, thread.entryNode);
    }
    MPORState initialState = new MPORState(stateBuilder.buildOrThrow(), null);
    Set<MPORState> visitedStates = new HashSet<>();
    computeStates(visitedStates, initialState, null);
  }

  private void computeStates(
      Set<MPORState> pVisitedStates, MPORState pCurrentState, CFANode pFunctionReturnNode) {

    if (isStateVisited(pVisitedStates, pCurrentState)) {
      return;
    }
    for (var entry : pCurrentState.threadNodes.entrySet()) {
      MPORThread currentThread = entry.getKey();
      CFANode currentNode = entry.getValue();
      if (currentThread.exitNode.isPresent()) {
        if (currentNode.equals(currentThread.exitNode.orElseThrow())) {
          continue;
        }
      }
      pVisitedStates.add(pCurrentState);
      for (CFAEdge cfaEdge :
          MPORAlgorithm.contextSensitiveLeavingEdges(currentNode, pFunctionReturnNode)) {
        MPORState nextState =
            pCurrentState.createUpdatedState(currentThread, cfaEdge.getSuccessor());
        computeStates(
            pVisitedStates,
            nextState,
            MPORAlgorithm.getFunctionReturnNode(
                entry.getValue(), pFunctionReturnNode, algorithm.getFunctionCallMap()));
      }
    }
  }

  private boolean isStateVisited(Set<MPORState> pVisitedStates, MPORState pCurrentState) {
    for (MPORState state : pVisitedStates) {
      for (var entry : state.threadNodes.entrySet()) {
        for (var innerEntry : pCurrentState.threadNodes.entrySet()) {
          if (entry.getKey().equals(innerEntry.getKey())
              && entry.getValue().equals(innerEntry.getValue())) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
