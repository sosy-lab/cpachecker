// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class StatementBlock implements ACSLBlock {

  private final int startOffset;
  private final int endOffset;
  private final boolean isLoop;
  private final CFANode firstNode;
  private final Set<CFANode> endNodes = new HashSet<>();
  private final Set<CFANode> containedNodes = new HashSet<>();
  private final Set<CFAEdge> enteringEdges = new HashSet<>();
  private final Set<CFAEdge> leavingEdges = new HashSet<>();

  public StatementBlock(int pStartOffset, int pEndOffset, boolean pIsLoop, CFANode first, CFANode next) {
    startOffset = pStartOffset;
    endOffset = pEndOffset;
    isLoop = pIsLoop;
    firstNode = first;
    endNodes.add(next);
  }

  @Override
  public boolean isFunction() {
    return false;
  }

  @Override
  public boolean isLoop() {
    return isLoop;
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

  boolean computeContainedNodes(Collection<CFANode> nodes) {
    // remove nodes that were optimized away by CPAchecker
    endNodes.removeIf(node -> !nodes.contains(node));
    if (!nodes.contains(firstNode) || endNodes.isEmpty()) {
      return false;
    }

    enteringEdges.addAll(CFAUtils.enteringEdges(firstNode).toList());

    Set<CFAEdge> visited = new HashSet<>();
    Queue<CFAEdge> waitlist = new ArrayDeque<>(enteringEdges);
    while (!waitlist.isEmpty()) {
      CFAEdge currentEdge = waitlist.poll();
      if (visited.contains(currentEdge)) {
        continue;
      }
      visited.add(currentEdge);
      CFANode successor = currentEdge.getSuccessor();
      if (endNodes.contains(successor)) {
        leavingEdges.add(currentEdge);
      } else {
        containedNodes.add(successor);
        for (int i = 0; i < successor.getNumLeavingEdges(); i++) {
          waitlist.add(successor.getLeavingEdge(i));
        }
      }
    }
    return true;
  }

  public void addEndNode(CFANode node) {
    endNodes.add(node);
  }
}
