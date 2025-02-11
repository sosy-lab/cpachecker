// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition;

import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.util.CFATraversal;

public class LinearBlockNodeDecomposition implements DssBlockDecomposition {

  private final BlockNodeCFAVisitor visitor;

  public LinearBlockNodeDecomposition(Predicate<CFANode> pIsBlockEnd) {
    visitor = new BlockNodeCFAVisitor(pIsBlockEnd);
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    CFATraversal.dfs().traverseEdgesOnce(cfa.getMainFunction(), visitor);
    return BlockGraph.fromBlockNodesWithoutGraphInformation(cfa, visitor.getBlockNodes());
  }
}
