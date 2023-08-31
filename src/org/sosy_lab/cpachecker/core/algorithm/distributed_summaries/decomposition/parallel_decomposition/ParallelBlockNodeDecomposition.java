// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockSummaryCFADecomposer;
import org.sosy_lab.cpachecker.util.CFAEdgeDFSTraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class ParallelBlockNodeDecomposition implements BlockSummaryCFADecomposer {

  private final BlockNodeCFAVisitor visitor;
  private final ShutdownNotifier shutdownNotifier;

  public ParallelBlockNodeDecomposition(ShutdownNotifier pShutdownNotifier) {
    visitor = new BlockNodeCFAVisitor();
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public ParallelBlockGraph decompose(CFA cfa) throws InterruptedException {
    FunctionEntryNode entry = cfa.getMainFunction();
    Builder<FunctionEntryNode> functionBlocks = new ImmutableList.Builder<>();

    for(FunctionEntryNode fn : cfa.getAllFunctions().values()){
      boolean existsPath = CFAUtils.existsPath(entry, fn, CFAUtils::leavingEdges, shutdownNotifier);
      if(existsPath && !fn.getFunctionName().equals("reach_error")){
        functionBlocks.add(fn);
      }
    }

    for (FunctionEntryNode f : functionBlocks.build()) {
      CFAEdgeDFSTraversal.dfs().traverseOnce(f, visitor);
      visitor.finish();
    }

    return ParallelBlockGraph.fromBlockNodesWithoutGraphInformation(cfa, visitor.getBlockNodes());
  }
}
