// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

class StatementBlock implements Block {

  private final int startOffset;
  private final int endOffset;
  private final Set<CFANode> containedNodes = new HashSet<>();
  // Entering edges are not inside the block, but the next concrete edge is
  private final Set<CFAEdge> enteringEdges = new HashSet<>();
  // Leaving edges are still inside the block, but next concrete edge is not
  private final Set<CFAEdge> leavingEdges = new HashSet<>();

  StatementBlock(int start, int end) {
    startOffset = start;
    endOffset = end;
  }

  @Override
  public boolean isFunction() {
    return false;
  }

  @Override
  public boolean isLoop() {
    return false;
  }

  @Override
  public int getStartOffset() {
    return startOffset;
  }

  @Override
  public int getEndOffset() {
    return endOffset;
  }

  @Override
  public Set<CFAEdge> getEnteringEdges() {
    return enteringEdges;
  }

  @Override
  public Set<CFAEdge> getLeavingEdges() {
    return leavingEdges;
  }

  @Override
  public Set<CFANode> getContainedNodes() {
    return containedNodes;
  }

  void computeContainedNodes() {
    Set<CFAEdge> visited = new HashSet<>();
    Queue<CFAEdge> waitlist = new ArrayDeque<>(enteringEdges);
    while (!waitlist.isEmpty()) {
      CFAEdge currentEdge = waitlist.poll();
      if (visited.contains(currentEdge) || leavingEdges.contains(currentEdge)) {
        continue;
      }
      visited.add(currentEdge);
      CFANode successor = currentEdge.getSuccessor();
      containedNodes.add(successor);
      for (int i = 0; i < successor.getNumLeavingEdges(); i++) {
        waitlist.add(successor.getLeavingEdge(i));
      }
    }
  }

  @Override
  public void addEnteringEdge(CFAEdge edge) {
    enteringEdges.add(edge);
  }

  @Override
  public void addLeavingEdge(CFAEdge edge) {
    leavingEdges.add(edge);
  }
}
