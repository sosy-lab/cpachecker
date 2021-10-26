// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blockgraph.builder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

abstract class BlockBuilder {
  protected final ShutdownNotifier shutdownNotifier;

  protected final CFANode entry;

  protected final Set<CFANode> nodes = new HashSet<>();

  protected final ArrayDeque<CFANode> scope;

  protected final Set<BlockBuilder> exits = new HashSet<>();

  protected Optional<Block> result = Optional.empty();

  public BlockBuilder(
      final CFANode pStart, final ArrayDeque<CFANode> pScope, final ShutdownNotifier pShutdownNotifier
  ) {
    this.nodes.add(pStart);
    this.entry = pStart;
    this.scope = pScope.clone();
    this.shutdownNotifier = pShutdownNotifier;
  }

  protected void addNode(final CFANode pNode) {
    nodes.add(pNode);
  }

  public boolean contains(final CFANode pNode) {
    return nodes.contains(pNode);
  }

  public CFANode getStart() {
    return entry;
  }

  /**
   * Duplicates CFAUtils::existsPath
   */
  protected boolean existsPath(final CFANode source, final CFANode target) {
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    Deque<CFANode> callstack = scope.clone();

    waitlist.add(source);

    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();

      if (current.equals(target)) {
        return true;
      }

      if (!visited.contains(current)) {
        visited.add(current);
        for (CFAEdge outEdge : CFAUtils.leavingEdges(current)) {
          if (outEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
            callstack.push(current);
          } else if (outEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
            FunctionSummaryEdge summaryEdge = ((FunctionReturnEdge) outEdge).getSummaryEdge();

            if (callstack.peek() == summaryEdge.getPredecessor()) {
              callstack.pop();
            } else {
              continue;
            }
          }
          waitlist.add(outEdge.getSuccessor());
        }
      }
    }
    return false;
  }

  abstract Set<BlockBuilder> explore(final BlockOperator blk);

  abstract Block getBlock();
}
