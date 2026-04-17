// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Map.Entry;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.FunctionSCCGraph.FunctionSCC;

public class InliningDecomposition implements DssBlockDecomposition {

  private DssBlockDecomposition childDecomposition;

  public InliningDecomposition(DssBlockDecomposition pChildDecomposition) {
    childDecomposition = pChildDecomposition;
  }

  @Override
  public BlockGraph decompose(CFA pCfa) throws InterruptedException {

    BlockGraph childGraph = childDecomposition.decompose(pCfa);
    FunctionGraph functionGraph = FunctionGraph.from(childGraph);
    FunctionSCCGraph sccGraph = FunctionSCCGraph.from(functionGraph);

    Multimap<FunctionSCC, CallStack> callStacks = sccGraph.findCallStacks();

    ImmutableSet<BlockNode> nodes = copyNodesWithCallStacks(callStacks);

    return new BlockGraph(nodes);
  }

  private ImmutableSet<@NonNull BlockNode> copyNodesWithCallStacks(
      Multimap<FunctionSCC, CallStack> pCallStacks) {
    ImmutableSet.Builder<BlockNode> blocks = ImmutableSet.builder();

    for (Entry<FunctionSCC, CallStack> entry : pCallStacks.entries()) {
      blocks.addAll(mapBlocksWithCallStack(entry.getKey(), entry.getValue()));
    }

    return blocks.build();
  }

  private Iterable<BlockNode> mapBlocksWithCallStack(FunctionSCC scc, CallStack stack) {

    Function<String, String> idMapper = s -> s + "@" + stack;

    CallStack previousStack = stack.pop();

    Set<String> idsInScc =
        FluentIterable.from(scc.functions())
            .transformAndConcat(f -> f.blockNodes())
            .transform(n -> n.getId())
            .toSet();

    Function<String, String> predMapper =
        s -> {
          if (idsInScc.contains(s)) {
            return s + "@" + stack;
          } else {
            return s + "@" + previousStack;
          }
        };

    return FluentIterable.from(scc.functions())
        .transformAndConcat(f -> f.blockNodes())
        .transform(
            node -> {
              Function<String, String> succMapper =
                  s -> {
                    if (idsInScc.contains(s)) {
                      return s + "@" + stack;
                    } else {
                      // This is a call to outside this SCC -> successor has additional stack
                      return s + "@" + stack.addCall(scc, node);
                    }
                  };

              return node.withMappedIds(idMapper, predMapper, succMapper);
            });
  }
}
