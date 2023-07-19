// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition;

//
import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockSummaryCFADecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.util.CFAEdgeDFSTraversal;

public class ParallelBlockNodeDecomposition implements BlockSummaryCFADecomposer {

  private final BlockNodeCFAVisitor visitor;

  public ParallelBlockNodeDecomposition() {
    visitor = new BlockNodeCFAVisitor();
  }

  @Override
  public ParallelBlockGraph decompose(CFA cfa) throws InterruptedException {
    for (FunctionEntryNode f : cfa.getAllFunctions().values()) {
      CFAEdgeDFSTraversal.dfs().traverseOnce(f, visitor);
      visitor.finish();
    }

    return ParallelBlockGraph.fromBlockNodesWithoutGraphInformation(cfa, visitor.getBlockNodes());
  }
}
