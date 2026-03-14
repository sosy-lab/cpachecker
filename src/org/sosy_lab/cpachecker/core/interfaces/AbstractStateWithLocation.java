// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * This interface represents abstract states that somehow store information about which CFA location
 * the abstract state belongs to. The interface is intended to provide this knowledge about the
 * location to other components, such as other CPAs or algorithms.
 *
 * <p>The method {@link AbstractStates#extractLocation(AbstractState)} provides a convenient way to
 * access this information.
 */
public interface AbstractStateWithLocation extends AbstractStateWithLocations {

  /**
   * Get the {@link CFANode} that represents the location of this state.
   *
   * @return A node of the CFA.
   */
  CFANode getLocationNode();

  @Override
  default Iterable<CFANode> getLocationNodes() {
    return ImmutableSet.of(getLocationNode());
  }

  @Override
  default Iterable<CFAEdge> getOutgoingEdges() {
    return getLocationNode().getLeavingEdges();
  }

  @Override
  default Iterable<CFAEdge> getIncomingEdges() {
    return getLocationNode().getEnteringEdges();
  }

  @Override
  default @Nullable List<CFAEdge> getEdgesToChild(AbstractStateWithLocations pChild) {
    if (pChild instanceof AbstractStateWithLocation child) {
      return getEdgesToChild(child);
    }
    return null;
  }

  default @Nullable List<CFAEdge> getEdgesToChild(AbstractStateWithLocation pChild) {
    ImmutableList.Builder<CFAEdge> allEdges = ImmutableList.builder();
    CFANode currentLoc = getLocationNode();
    CFANode childLoc = pChild.getLocationNode();

    while (!currentLoc.equals(childLoc)) {
      // we didn't find a proper connection to the child so we return an empty list
      if (currentLoc.getNumLeavingEdges() != 1) {
        return null;
      }

      final CFAEdge leavingEdge = currentLoc.getLeavingEdge(0);
      allEdges.add(leavingEdge);
      currentLoc = leavingEdge.getSuccessor();
    }
    return allEdges.build();
  }
}
