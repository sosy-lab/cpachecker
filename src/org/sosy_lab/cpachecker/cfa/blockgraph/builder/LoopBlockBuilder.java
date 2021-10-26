// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blockgraph.builder;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.blockgraph.BlockGraph;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

class LoopBlockBuilder extends BlockBuilder {
  private BlockGraph nested = null;

  protected LoopBlockBuilder(
      final CFANode pStart, final ArrayDeque<CFANode> pScope, final ShutdownNotifier pShutdownNotifier
  ) {
    super(pStart, pScope, pShutdownNotifier);
  }

  @Override Set<BlockBuilder> explore(final BlockOperator blk) {
    Preconditions.checkState(getStart().isLoopStart());

    Set<CFANode> seen = new HashSet<>();
    seen.add(entry);

    Deque<CFANode> stack = new ArrayDeque<>();
    stack.add(entry);

    while(!stack.isEmpty()) {
      CFANode node = stack.pop();
      nodes.add(node);

      if(node != entry && !existsPath(node, getStart())) {
        if(node.isLoopStart()) {
          exits.add(new LoopBlockBuilder(node, scope, shutdownNotifier));
        } else {
          exits.add(new FlatBlockBuilder(node, scope, shutdownNotifier));
        }
        continue;
      }

      for (CFAEdge out : CFAUtils.leavingEdges(node)) {
        if(node != entry) {
          if (out instanceof CFunctionSummaryStatementEdge) {
            assert false;
          } else if (out.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
            scope.push(node);
          } else if (out.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
            FunctionSummaryEdge summaryEdge = ((FunctionReturnEdge) out).getSummaryEdge();

            if (scope.peek() == summaryEdge.getPredecessor()) {
              scope.pop();
            } else continue;
          }
        }

        CFANode nextNode = out.getSuccessor();

        if (!seen.contains(nextNode)) {
          stack.push(nextNode);
          seen.add(nextNode);
        }
      }
    }

    try {
      nested = BlockGraphBuilder.create(shutdownNotifier).build(entry, blk);
    } catch(InterruptedException ignored) {
      // todo: Add handling
    }

    return exits;
  }

  @Override public Block getBlock() {
    if(result.isEmpty()) {
      result = Optional.of(new Block(entry, nodes));
      result.get().setNestedGraph(nested);
    }

    return result.get();
  }
}
