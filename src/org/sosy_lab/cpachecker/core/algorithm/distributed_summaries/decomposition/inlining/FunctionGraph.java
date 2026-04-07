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

  public record Call(BlockNode location, Function target) {}

  /**
   * When visiting the graph, not all function objects exist, so we collect this instead and convert
   * it to a {@link Call} later
   */
  private record CallEdge(BlockNode source, BlockNode target) {}

  private record FunctionInfo(Function function, ImmutableSet<CallEdge> calls) {}

  private final ImmutableSet<Function> functions;
  private final Function root;
  private final ImmutableMap<Function, ImmutableSet<Call>> edges;

  private FunctionGraph(
      ImmutableSet<Function> pFunctions,
      Function pRoot,
      ImmutableMap<Function, ImmutableSet<Call>> pEdges) {
    functions = pFunctions;
    root = pRoot;
    edges = pEdges;
  }

  public ImmutableSet<Function> getFunctions() {
    return functions;
  }

  public Function getRoot() {
    return root;
  }

  public ImmutableMap<Function, ImmutableSet<Call>> getEdges() {
    return edges;
  }

  public ImmutableSet<Function> getSuccessors(Function f) {
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

    ImmutableSet.Builder<Function> functions = ImmutableSet.builder();
    Map<Function, ImmutableSet<CallEdge>> edges = new HashMap<>();

    Function first = null;

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
    ImmutableSet<Function> fs = functions.build();

    return new FunctionGraph(fs, first, convertCallEdgesToCalls(edges, fs));
  }

  private static ImmutableMap<Function, ImmutableSet<Call>> convertCallEdgesToCalls(
      Map<Function, ImmutableSet<CallEdge>> edges, ImmutableSet<Function> fs) {
    Map<BlockNode, Function> headMap = new HashMap<>();

    for (Function f : fs) {
      headMap.put(f.entryNode(), f);
    }

    ImmutableMap.Builder<Function, ImmutableSet<Call>> callBuilder = ImmutableMap.builder();
    for (Entry<Function, ImmutableSet<CallEdge>> entry : edges.entrySet()) {
      callBuilder.put(
          entry.getKey(),
          transformedImmutableSetCopy(
              entry.getValue(), ce -> new Call(ce.source, headMap.get(ce.target))));
    }

    ImmutableMap<Function, ImmutableSet<Call>> calls = callBuilder.buildOrThrow();
    return calls;
  }

  /**
   * Visits all blocks of a function
   *
   * @param graph the whole block graph
   * @param pEntryNode the first block inside this function
   * @return the information for the {@link Function} record as well all calls to different function
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

    Function f =
        new Function(
            pEntryNode.getInitialLocation().getFunctionName(),
            ImmutableSet.copyOf(seen),
            pEntryNode,
            exitNodes.build());

    return new FunctionInfo(f, calls.build());
  }
}
