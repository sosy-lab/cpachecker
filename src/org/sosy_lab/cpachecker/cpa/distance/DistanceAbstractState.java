// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.distance;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class DistanceAbstractState implements AbstractState {

  private final int distance;
  private final ImmutableSet<CFANode> visitedNodes;
  private final int maxDistance;

  public DistanceAbstractState(int pDistance, Set<CFANode> pVisitedNodes, int pMaxDistance) {
    distance = pDistance;
    visitedNodes = ImmutableSet.copyOf(pVisitedNodes);
    maxDistance = pMaxDistance;
  }

  public int getDistance() {
    return distance;
  }

  public ImmutableSet<CFANode> getVisitedNodes() {
    return visitedNodes;
  }

  public int getMaxDistance() {
    return maxDistance;
  }

  @Override
  public String toString() {
    return "DistanceAbstractState{"
        + "distance="
        + distance
        + ", visitedNodes="
        + visitedNodes
        + '}';
  }
}
