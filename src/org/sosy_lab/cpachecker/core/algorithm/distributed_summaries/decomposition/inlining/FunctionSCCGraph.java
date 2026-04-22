// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.StronglyConnectedComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.FunctionGraph.Call;

public class FunctionSCCGraph {

  public record FunctionSCC(ImmutableSet<BlockFunction> functions) {

    @Override
    public String toString() {
      if (functions.size() == 1) {
        return functions.iterator().next().toString();
      }
      return "SCC" + functions;
    }
  }

  private final FunctionSCC root;
  private final FunctionGraph originalGraph;
  private final ImmutableMap<BlockFunction, FunctionSCC> functionToSCC;

  private FunctionSCCGraph(
      FunctionSCC pRoot,
      FunctionGraph pGraph,
      ImmutableMap<BlockFunction, FunctionSCC> pFunctionToSCC) {
    root = pRoot;
    originalGraph = pGraph;
    functionToSCC = pFunctionToSCC;
  }

  public static FunctionSCCGraph from(FunctionGraph graph) {

    ImmutableList<List<BlockFunction>> sccList =
        StronglyConnectedComponents.performTarjanAlgorithm(graph.getRoot(), graph::getSuccessors);

    ImmutableMap.Builder<BlockFunction, FunctionSCC> functionToSCCBuilder = ImmutableMap.builder();

    FunctionSCC root = null;

    for (List<BlockFunction> scc : sccList) {
      FunctionSCC curr = new FunctionSCC(ImmutableSet.copyOf(scc));
      for (BlockFunction f : scc) {
        functionToSCCBuilder.put(f, curr);
      }
      if (curr.functions().contains(graph.getRoot())) {
        assert root == null;
        root = curr;
      }
    }

    ImmutableMap<BlockFunction, FunctionSCC> functionToSCC = functionToSCCBuilder.buildOrThrow();

    assert root != null;
    return new FunctionSCCGraph(root, graph, functionToSCC);
  }

  public ImmutableSet<FunctionSCC> getSCCs() {
    return ImmutableSet.copyOf(functionToSCC.values());
  }

  public Multimap<FunctionSCC, CallStack> findCallStacks() {

    Multimap<FunctionSCC, CallStack> stacks = HashMultimap.create();
    stacks.put(root, CallStack.empty());

    for (FunctionSCC currSCC : TopologicalTraversal.traverse(root, this::getSuccessors)) {

      for (Call c : getLeavingCalls(currSCC)) {
        FunctionSCC targetSCC = functionToSCC.get(c.target());

        for (CallStack currStack : stacks.get(currSCC)) {
          stacks.put(targetSCC, currStack.addCall(currSCC, c.location()));
        }
      }
    }

    return stacks;
  }

  public ImmutableSet<FunctionSCC> getSuccessors(FunctionSCC node) {
    return FluentIterable.from(node.functions)
        .transformAndConcat(f -> originalGraph.getSuccessors(f))
        .transform(f -> functionToSCC.get(f))
        .filter(scc -> !scc.equals(node))
        .toSet();
  }

  private FluentIterable<Call> getLeavingCalls(FunctionSCC scc) {
    return FluentIterable.from(scc.functions())
        .transformAndConcat(f -> originalGraph.getEdges().get(f))
        .filter(c -> !scc.equals(functionToSCC.get(c.target())));
  }

  public FunctionSCC getRoot() {
    return root;
  }
}
