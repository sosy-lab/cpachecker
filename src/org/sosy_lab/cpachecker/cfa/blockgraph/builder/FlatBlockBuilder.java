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
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;


class FlatBlockBuilder extends BlockBuilder {
  protected FlatBlockBuilder(
      final CFANode pStart, final ArrayDeque<CFANode> pScope, final ShutdownNotifier pShutdownNotifier
  ) {
    super(pStart, pScope, pShutdownNotifier);
  }

  protected boolean isForeignBlockMergePoint(final CFANode pNode) {
    if(pNode.getNumEnteringEdges() <= 1 || pNode.isLoopStart() || pNode.getNumLeavingEdges() == 0) {
      return false;
    }

    for (CFAEdge inEdge : CFAUtils.enteringEdges(pNode)) {
      if (inEdge.getEdgeType() != CFAEdgeType.FunctionCallEdge) {
        CFANode predecessor = inEdge.getPredecessor();

        /*
         * Todo: Fix 
         * This check (obviously?) doesn't work in some situations, if CFA exploration has not yet
         * left the current block, but also not yet expanded a separate path which also leads to the
         * the current node pNode.
         * Impact: Block might get terminated unnecessarily; correctness of the analysis should not 
         * be in danger.
         */
        if (!contains(predecessor)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override public Set<BlockBuilder> explore(final BlockOperator blk) {
    Set<CFANode> seen = new HashSet<>();
    seen.add(entry);

    Deque<CFANode> stack = new ArrayDeque<>();
    stack.add(entry);

    while(!stack.isEmpty()) {
      CFANode node = stack.pop();

      /*
       * (Only) If current node is not block start, check for loop start and block merge point.
       */
      if(node != entry) {
        if(entry.isLoopStart() && !existsPath(node, entry)) {
          continue;
        } else if (node.isLoopStart()) {
          nodes.add(node);
          exits.add(new LoopBlockBuilder(node, scope, shutdownNotifier));
          continue;
        } else if (isForeignBlockMergePoint(node)) {
          nodes.add(node);
          exits.add(new FlatBlockBuilder(node, scope, shutdownNotifier));
          continue;
        }
      }

      nodes.add(node);

      /*
       * Process outgoing edges
       */
      for (CFAEdge out : CFAUtils.leavingEdges(node)) {
        CFANode nextNode = out.getSuccessor();

        if(node != entry) {
          if (out instanceof CFunctionSummaryStatementEdge) {
            throw new AssertionError();
          } else if (out.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
            scope.push(node);
          } else if (out.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
            FunctionSummaryEdge summaryEdge = ((FunctionReturnEdge) out).getSummaryEdge();

            if (scope.peek() == summaryEdge.getPredecessor()) {
              scope.pop();
            } else {
              continue;
            }
          }

          if (blk.isBlockEnd(nextNode, 0)) {
            if (nextNode.isLoopStart()) {
              nodes.add(nextNode);
              exits.add(new LoopBlockBuilder(nextNode, scope, shutdownNotifier));
            } else if(out.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
              nodes.add(nextNode);
              exits.add(new FlatBlockBuilder(nextNode, scope, shutdownNotifier));
            } else {
              exits.add(new FlatBlockBuilder(node, scope, shutdownNotifier));
            }
            continue;
          }
        }

        if (!seen.contains(nextNode)) {
          stack.push(nextNode);
          seen.add(nextNode);
        }
      }
    }

    return exits;
  }

  @Override public Block getBlock() {
    if(result.isEmpty()) {
      result = Optional.of(new Block(entry, nodes));
    }

    return result.orElseThrow();
  }
}