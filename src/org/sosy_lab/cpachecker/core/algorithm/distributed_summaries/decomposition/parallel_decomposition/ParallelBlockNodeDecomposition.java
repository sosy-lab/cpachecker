// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition;
//
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockSummaryCFADecomposer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.util.CFAEdgeDFSTraversal;

public class ParallelBlockNodeDecomposition
    implements BlockSummaryCFADecomposer {

  private final BlockNodeCFAVisitor visitor;

  public ParallelBlockNodeDecomposition() {
    visitor = new BlockNodeCFAVisitor();
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    for (FunctionEntryNode f : cfa.getAllFunctions().values()) {
      CFAEdgeDFSTraversal.dfs().traverseOnce(f, visitor);
      visitor.finish();
    }

    return BlockGraph.fromBlockNodesWithoutGraphInformation(
        cfa, visitor.getBlockNodes());
  }
}
