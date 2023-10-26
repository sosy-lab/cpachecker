// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.distance;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.distance.DistanceCPA.TransferMode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class DistanceTransferRelation extends SingleEdgeTransferRelation {

  public DistanceTransferRelation(TransferMode pTransferMode) {
    transferMode = pTransferMode;
  }

  private final TransferMode transferMode;

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    DistanceAbstractState distanceState = (DistanceAbstractState) state;
    if (distanceState.getVisitedNodes().contains(cfaEdge.getSuccessor())) {
      return ImmutableSet.of(
          new DistanceAbstractState(
              0, distanceState.getVisitedNodes(), distanceState.getMaxDistance()));
    }
    OptionalInt result =
        switch (transferMode) {
          case CHEAP -> calculateDistanceCheap(cfaEdge, distanceState);
          case EXPENSIVE -> calculateDistanceExpensive(cfaEdge, distanceState);
        };
    if (result.isPresent()) {
      if (result.getAsInt() > distanceState.getMaxDistance()) {
        return ImmutableSet.of();
      }
      return ImmutableSet.of(
          new DistanceAbstractState(
              result.getAsInt(), distanceState.getVisitedNodes(), distanceState.getMaxDistance()));
    }
    if (distanceState.getDistance() > distanceState.getMaxDistance()) {
      return ImmutableSet.of();
    }
    return ImmutableSet.of(
        new DistanceAbstractState(
            distanceState.getDistance(),
            distanceState.getVisitedNodes(),
            distanceState.getMaxDistance()));
  }

  private record NodeDistance(CFANode node, int assumeCount) {}

  // get the shortest path from a CFANode to another CFANode
  private int getShortestPath(CFANode start, CFANode end) {
    List<NodeDistance> waitlist = new ArrayList<>();
    waitlist.add(new NodeDistance(start, 0));
    int shortestDistance = Integer.MAX_VALUE;
    Set<CFANode> visitedNodes = new LinkedHashSet<>();
    while (!waitlist.isEmpty()) {
      NodeDistance current = waitlist.remove(0);
      if (visitedNodes.contains(current.node())) {
        continue;
      }
      visitedNodes.add(current.node());
      if (current.node().equals(end)) {
        shortestDistance = Integer.min(shortestDistance, current.assumeCount());
        continue;
      }
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current.node())) {
        int distance = current.assumeCount() + (leavingEdge instanceof AssumeEdge ? 1 : 0);
        waitlist.add(new NodeDistance(leavingEdge.getSuccessor(), distance));
      }
    }
    return shortestDistance;
  }

  private OptionalInt calculateDistanceExpensive(CFAEdge edge, DistanceAbstractState state) {
    int distance = calculateDistanceCheap(edge, state).orElse(Integer.MAX_VALUE);
    for (CFANode visitedNode : state.getVisitedNodes()) {
      distance = Integer.min(distance, getShortestPath(edge.getSuccessor(), visitedNode));
      if (distance <= 1) {
        break;
      }
    }
    if (distance <= state.getMaxDistance()) {
      return OptionalInt.of(distance);
    }
    return OptionalInt.empty();
  }

  private OptionalInt calculateDistanceCheap(CFAEdge edge, DistanceAbstractState state) {
    if (edge instanceof AssumeEdge) {
      return OptionalInt.of(state.getDistance() + 1);
    }
    return OptionalInt.of(state.getDistance());
  }
}
