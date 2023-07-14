// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class FunctionDecomposer implements BlockSummaryCFADecomposer {

  private static int idCount = 0;

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    ImmutableSet.Builder<BlockNodeWithoutGraphInformation> builder = ImmutableSet.builder();

    Set<CFANode> nodes = new HashSet<>();
    Set<CFAEdge> edges = new HashSet<>();
    Set<CFANode> waitlist = new HashSet<>();
    CFANode temp;
    CFANode exitNode = null;

    for (FunctionEntryNode value : cfa.getAllFunctions().values()) {
      // Complete Solution:
      waitlist.add(value);
      while (!waitlist.isEmpty()) {
        temp = waitlist.iterator().next();
        waitlist.remove(waitlist.iterator().next());
        if (temp instanceof FunctionExitNode) {
          exitNode = temp;
          nodes.add(temp);
          continue;
        }
        for (CFAEdge e : CFAUtils.allLeavingEdges(temp)) {


          //Check if Function Call Edge or Duplicate
          if (nodes.contains(temp) || e instanceof FunctionCallEdge) {
            //Ignore this case

            //Check if Summary Edge
          } else if (e instanceof CFunctionSummaryEdge) {
            edges.add(e);
            nodes.add(temp);
            waitlist.add(e.getSuccessor());


          } else {
            edges.add(e);
            nodes.add(temp);
            waitlist.add(e.getSuccessor());
          }

        }
      }
      Preconditions.checkArgument(nodes.contains(value.getExitNode().orElseThrow()));
      BlockNodeWithoutGraphInformation nodeMetaData =
          new BlockNodeWithoutGraphInformation("Function " + value.getFunctionName() + " " + idCount++, value, exitNode,
              ImmutableSet.copyOf(nodes), ImmutableSet.copyOf(edges));
      builder.add(nodeMetaData);

      //Clear Nodes & Edges HashMap
      nodes.clear();
      edges.clear();
    }

    return BlockGraph.fromBlockNodesWithoutGraphInformation(cfa, builder.build());
  }
}