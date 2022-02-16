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
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class StatementBlock implements SyntacticBlock {

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
  public Iterable<CFAEdge> getEnteringEdges() {
    return enteringEdges;
  }

  @Override
  public Iterable<CFAEdge> getLeavingEdges() {
    return leavingEdges;
  }

  @Override
  public Set<CFANode> getContainedNodes() {
    return containedNodes;
  }

  boolean computeSets(Collection<CFANode> nodes) {
    // remove nodes that were optimized away by CPAchecker
    endNodes.removeIf(node -> !nodes.contains(node));
    if (!nodes.contains(firstNode) || endNodes.isEmpty()) {
      return false;
    }

    CFAUtils.enteringEdges(firstNode).copyInto(enteringEdges);
    if (endNodes.size() == 1 && endNodes.contains(firstNode)) {
      // Block is empty, so there is no need to compute contained nodes or leaving edges.
      // However, do not discard this block as it may contain ACSL assertions
      return true;
    }

    Set<CFAEdge> visited = new HashSet<>();
    Queue<CFAEdge> waitlist = new ArrayDeque<>(enteringEdges);
    while (!waitlist.isEmpty()) {
      CFAEdge currentEdge = waitlist.poll();
      if (visited.contains(currentEdge)) {
        continue;
      }
      visited.add(currentEdge);
      if (currentEdge instanceof CFunctionCallEdge) {
        // If currentEdge is a function call, then continue with the return edge and skip
        // everything in between
        CFunctionSummaryEdge summaryEdge = ((CFunctionCallEdge) currentEdge).getSummaryEdge();
        CFAUtils.enteringEdges(summaryEdge.getSuccessor()).copyInto(waitlist);
        continue;
      }
      CFANode successor = currentEdge.getSuccessor();
      if (endNodes.contains(successor)) {
        leavingEdges.add(currentEdge);
      } else {
        containedNodes.add(successor);
        CFAUtils.leavingEdges(successor).copyInto(waitlist);
      }
    }
    return true;
  }

  public void addEndNode(CFANode node) {
    endNodes.add(node);
  }
}
