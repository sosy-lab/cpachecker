// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public class FunctionGraph {

  public record Call(BlockNode location, BlockFunction target) {}

  /**
   * When visiting the graph, not all function objects exist, so we collect this instead and convert
   * it to a {@link Call} later
   */
  private record CallEdge(BlockNode source, BlockNode target) {}

  private record FunctionInfo(BlockFunction function, ImmutableSet<CallEdge> calls) {}

  private final ImmutableSet<BlockFunction> functions;
  private final BlockFunction root;
  private final ImmutableMap<BlockFunction, ImmutableSet<Call>> edges;

  private FunctionGraph(
      ImmutableSet<BlockFunction> pFunctions,
      BlockFunction pRoot,
      ImmutableMap<BlockFunction, ImmutableSet<Call>> pEdges) {
    functions = pFunctions;
    root = pRoot;
    edges = pEdges;
  }

  public ImmutableSet<BlockFunction> getFunctions() {
    return functions;
  }

  public BlockFunction getRoot() {
    return root;
  }

  public ImmutableMap<BlockFunction, ImmutableSet<Call>> getEdges() {
    return edges;
  }

  public ImmutableSet<BlockFunction> getSuccessors(BlockFunction f) {
    ImmutableSet<Call> calls = edges.get(f);
    if (calls == null) {
      return ImmutableSet.of();
    }
    return transformedImmutableSetCopy(calls, c -> c.target());
  }

  /**
   * Partitions the blocks into the functions they belong to. Assumes that all {@link
   * FunctionEntryNode} and {@link FunctionExitNode} are abstraction points and therefore only ever
   * are entry and exit nodes of a block
   *
   * <p>TODO: enforce this assumption?
   *
   * @return the found functions as well as the edges of this graph
   */
  public static FunctionGraph from(BlockGraph graph) {

    ImmutableSet.Builder<BlockFunction> functions = ImmutableSet.builder();
    Map<BlockFunction, ImmutableSet<CallEdge>> edges = new HashMap<>();

    BlockFunction first = null;

    List<BlockNode> waitlist = new ArrayList<>();
    SequencedSet<BlockNode> seen = new LinkedHashSet<>();
    waitlist.add(graph.getRoot());
    while (!waitlist.isEmpty()) {
      BlockNode current = waitlist.removeFirst();
      if (seen.contains(current)) {
        continue;
      }
      seen.add(current);

      FunctionInfo fs = exploreFunction(graph, current);
      functions.add(fs.function);
      if (first == null) {
        first = fs.function;
      }
      for (CallEdge e : fs.calls) {
        waitlist.add(e.target);
      }
      edges.put(fs.function, fs.calls);
    }
    // We should have found at least one function
    assert first != null;
    ImmutableSet<BlockFunction> fs = functions.build();

    return new FunctionGraph(fs, first, convertCallEdgesToCalls(edges, fs));
  }

  private static ImmutableMap<BlockFunction, ImmutableSet<Call>> convertCallEdgesToCalls(
      Map<BlockFunction, ImmutableSet<CallEdge>> edges, ImmutableSet<BlockFunction> fs) {
    Map<BlockNode, BlockFunction> headMap = new HashMap<>();

    for (BlockFunction f : fs) {
      headMap.put(f.entryNode(), f);
    }

    ImmutableMap.Builder<BlockFunction, ImmutableSet<Call>> callBuilder = ImmutableMap.builder();
    for (Entry<BlockFunction, ImmutableSet<CallEdge>> entry : edges.entrySet()) {
      callBuilder.put(
          entry.getKey(),
          transformedImmutableSetCopy(
              entry.getValue(), ce -> new Call(ce.source, headMap.get(ce.target))));
    }

    ImmutableMap<BlockFunction, ImmutableSet<Call>> calls = callBuilder.buildOrThrow();
    return calls;
  }

  /**
   * Visits all blocks of a function
   *
   * @param graph the whole block graph
   * @param pEntryNode the first block inside this function
   * @return the information for the {@link BlockFunction} record as well all calls to different
   *     function
   */
  private static FunctionInfo exploreFunction(BlockGraph graph, BlockNode pEntryNode) {

    assert pEntryNode.getInitialLocation() instanceof FunctionEntryNode;

    ImmutableSet.Builder<BlockNode> exitNodes = ImmutableSet.builder();
    ImmutableSet.Builder<CallEdge> calls = ImmutableSet.builder();

    List<BlockNode> waitlist = new ArrayList<>();
    SequencedSet<BlockNode> seen = new LinkedHashSet<>();
    waitlist.add(pEntryNode);
    while (!waitlist.isEmpty()) {
      BlockNode current = waitlist.removeFirst();
      if (seen.contains(current)) {
        continue;
      }
      seen.add(current);

      if (current.getFinalLocation() instanceof FunctionExitNode) {
        exitNodes.add(current);
      } else if (current.getFinalLocation() instanceof FunctionEntryNode) {

        FluentIterable<BlockNode> succs = graph.getSuccessorsOf(current);
        // Currently, this assumption should hold, because the decompositions do not put edges into
        // multiple blocks and the function entry node always has only the function start dummy edge
        // leaving it -> the first block in a function is unique
        assert succs.size() == 1;
        calls.add(new CallEdge(current, Iterables.getOnlyElement(succs)));
        // find where the current function actually continues
        // TODO surely I am not the first who needs this -> is this implemented somewhere else?
        Set<CFAEdge> returnEdges =
            current
                .getFinalLocation()
                .getAllEnteringEdges()
                .filter(e -> current.getEdges().contains(e)) // all call edges in this block
                .transformAndConcat(e -> e.getPredecessor().getAllLeavingEdges())
                .filter(e -> e instanceof CFunctionSummaryEdge) // the corresponding summary edges
                .transform(
                    se ->
                        Iterables.getOnlyElement(
                            se.getSuccessor()
                                .getAllEnteringEdges()
                                .filter(re -> !(re instanceof CFunctionSummaryEdge))))
                .toSet(); // the corresponding return edge

        // visit all the blocks that contain such an edge
        Iterables.addAll(
            waitlist,
            FluentIterable.from(graph.getNodes())
                .filter(
                    n -> {
                      for (CFAEdge e : returnEdges) {
                        if (n.getEdges().contains(e)) {
                          return true;
                        }
                      }
                      return false;
                    }));

      } else {
        Iterables.addAll(waitlist, graph.getSuccessorsOf(current));
      }
    }

    BlockFunction f =
        new BlockFunction(
            pEntryNode.getInitialLocation().getFunctionName(),
            ImmutableSet.copyOf(seen),
            pEntryNode,
            exitNodes.build());

    return new FunctionInfo(f, calls.build());
  }
}
