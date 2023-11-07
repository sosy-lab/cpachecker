// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class FunctionDecomposer implements BlockSummaryCFADecomposer {

  private int idCount = 0;

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    ImmutableSet.Builder<BlockNodeWithoutGraphInformation> builder = ImmutableSet.builder();

    Set<CFANode> nodes = new HashSet<>();
    Set<CFAEdge> edges = new HashSet<>();
    List<CFANode> waitlist = new ArrayList<>();
    CFANode temp;
    CFANode exitNode = null;
    boolean foundExitNode = false;
    for (FunctionEntryNode value : cfa.getAllFunctions().values()) {
      if (value.getFunctionName().equals("reach_error")
          || value.getFunctionName().equals("__VERIFIER_assert")) {
        continue;
      }
      waitlist.add(value);
      while (!waitlist.isEmpty()) {
        temp = waitlist.remove(0);
        if (nodes.contains(temp)) {
          continue;
        }
        if ((temp.getNumLeavingEdges() == 0 || temp instanceof FunctionExitNode)
            && temp.getFunctionName().equals(value.getFunctionName())
            && !foundExitNode) {
          if (temp instanceof FunctionExitNode) {
            foundExitNode = true;
          }
          exitNode = temp;
          nodes.add(temp);
          continue;
        }
        nodes.add(temp);
        for (CFAEdge e : CFAUtils.allLeavingEdges(temp)) {

          // Check if Function Call Edge or Duplicate
          if (edges.contains(e)) {
            continue;
          } else if (e instanceof FunctionCallEdge fce) {
            if (fce.getSuccessor().getFunctionName().equals("reach_error")
                || fce.getSuccessor().getFunctionName().equals("__VERIFIER_assert")) {
              edges.add(e);
              waitlist.add(e.getSuccessor());
            }
          } else {
            edges.add(e);
            waitlist.add(e.getSuccessor());
          }
        }
      }
      assert exitNode != null;
      BlockNodeWithoutGraphInformation nodeMetaData =
          new BlockNodeWithoutGraphInformation(
              "Function " + value.getFunctionName() + " " + idCount++,
              value,
              exitNode,
              ImmutableSet.copyOf(nodes),
              ImmutableSet.copyOf(edges));
      builder.add(nodeMetaData);

      // Clear Nodes & Edges HashMap
      nodes.clear();
      edges.clear();
      foundExitNode = false;
    }

    return BlockGraph.fromBlockNodesWithoutGraphInformation(cfa, builder.build());
  }
}
