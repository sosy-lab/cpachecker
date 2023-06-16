// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SingleBlockDecomposition implements BlockSummaryCFADecomposer {

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    return BlockGraph.fromBlockNodesWithoutGraphInformation(
        cfa,
        ImmutableSet.of(
            new BlockNodeWithoutGraphInformation(
                "SB1",
                cfa.getMainFunction(),
                cfa.getMainFunction()
                    .getExitNode()
                    .orElseThrow(
                        () -> new AssertionError("Main function has no unique exit node.")),
                ImmutableSet.copyOf(cfa.nodes()),
                FluentIterable.from(cfa.nodes())
                    .transformAndConcat(n -> CFAUtils.allLeavingEdges(n))
                    .toSet())));
  }
}
