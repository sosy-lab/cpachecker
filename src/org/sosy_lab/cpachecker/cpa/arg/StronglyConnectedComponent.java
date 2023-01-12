// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class StronglyConnectedComponent {

  private final ARGState rootNode;
  private final Deque<ARGState> nodes = new ArrayDeque<>();

  public StronglyConnectedComponent(ARGState pRootnode) {
    rootNode = pRootnode;
  }

  public ARGState getRootNode() {
    return rootNode;
  }

  public ImmutableList<ARGState> getNodes() {
    return ImmutableList.copyOf(nodes);
  }

  public void addNode(ARGState pState) {
    if (nodes.contains(pState)) {
      throw new UnsupportedOperationException("nodes must not be added twice");
    }
    nodes.push(pState);
  }

  public boolean hasTargetStates() {
    return nodes.stream().anyMatch(ARGState::isTarget);
  }

  @Override
  public String toString() {
    return String.format(
        "[%s]",
        nodes.stream()
            .map(x -> (x.getStateId() + ":" + AbstractStates.extractLocation(x)))
            .collect(Collectors.joining(", ")));
  }
}
