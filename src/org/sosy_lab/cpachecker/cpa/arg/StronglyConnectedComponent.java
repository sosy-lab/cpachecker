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
 */
package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class StronglyConnectedComponent {

  private ARGState rootNode;
  private Deque<ARGState> nodes = new ArrayDeque<>();

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
    List<String> formattedNodes =
        nodes
            .stream()
            .map(x -> (x.getStateId() + ":" + AbstractStates.extractLocation(x)))
            .collect(Collectors.toCollection(ArrayList::new));
    return String.valueOf(formattedNodes);
  }
}
