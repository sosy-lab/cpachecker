// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

// TODO TestBlockModification? Problem: does not follow all invariants anymore!
public class DssBlockDecompositionTestUtil {

  private static String[] testFiles = {
    // Simple file
    // bug in BridgeDecomposition; missing a viable path
    "doc/examples/example.c",
    // many constructs
    "test/programs/cfa-ast-relation/full-expression.c",
    // bug in SingleBlock: if a function is unreachable from main, the block should not contain
    // all CFANodes
    "test/programs/dss/unreachableFunction.c",
    // bug in SingleBlock: The whole program can contain CFATerminationNode, leaving multiple
    // blocks with no successor
    "test/programs/dss/multipleExits.c",
    // bug in BridgeDecomposition: endless loop
    "test/programs/dss/loop-multiple-condition.c"
  };

  public static List<Object[]> getFiles() {

    List<Object[]> list = new ArrayList<>(testFiles.length);

    for (String p : testFiles) {
      list.add(new Object[] {p});
    }

    return list;
  }

  public static void checkBlockGraph(BlockGraph graph, CFA cfa) throws InterruptedException {

    ImmutableSet.Builder<CFAEdge> edgesCFABuilder = ImmutableSet.builder();
    SequencedSet<CFANode> nodesCFA = new LinkedHashSet<>();

    collectReachableNodes(cfa, nodesCFA, edgesCFABuilder);
    ImmutableSet<CFAEdge> edgesCFA = edgesCFABuilder.build();

    checkBlockEdges(graph);
    checkCFANodes(graph, nodesCFA, edgesCFA);
    checkInternalEdges(graph, edgesCFA);

    graph.checkConsistency(ShutdownNotifier.createDummy());
  }

  /** checks if all block edges are between the same CFANode */
  private static void checkBlockEdges(BlockGraph graph) {

    for (BlockNode n : graph.getNodes()) {
      for (String id : n.getSuccessorIds()) {

        Optional<@NonNull BlockNode> succOpt =
            graph.getNodes().stream().filter(b -> b.getId().equals(id)).findAny();
        assertWithMessage("Couldn't find successor for block").that(succOpt).isPresent();
        BlockNode succ = succOpt.orElseThrow();

        assertWithMessage("Exit node of block does not match entry node of successor")
            .that(succ.getInitialLocation())
            .isEqualTo(n.getFinalLocation());
      }
    }
  }

  private static void checkInternalEdges(BlockGraph graph, Set<CFAEdge> reachableEdges) {

    for (BlockNode blockNode : graph.getNodes()) {

      for (CFANode cfaNode : blockNode.getNodes()) {

        if (cfaNode.equals(blockNode.getInitialLocation())) {
          if (blockNode.getInitialLocation().equals(blockNode.getFinalLocation())) {
            continue; // nothing to check for loop head
          }
          assertWithMessage(
                  "Block entry node %s should not have a predecessor in the block. \n"
                      + " Full block: %s \n"
                      + " Internal edges: %s",
                  cfaNode, blockNode, blockNode.getEdges())
              .that(
                  blockNode.getEdges().stream()
                      .filter(e -> e.getSuccessor().equals(cfaNode))
                      .map(e -> e.getPredecessor()))
              .containsNoneIn(blockNode.getNodes());
        } else if (cfaNode.equals(blockNode.getFinalLocation())) {
          assertWithMessage(
                  "Block exit node %s should not have a successor in the block. \n"
                      + " Full block: %s \n"
                      + " Internal edges: %s",
                  cfaNode, blockNode, blockNode.getEdges())
              .that(
                  blockNode.getEdges().stream()
                      .filter(e -> e.getPredecessor().equals(cfaNode))
                      .map(e -> e.getSuccessor()))
              .containsNoneIn(blockNode.getNodes());
        } else {
          cfaNode.getLeavingEdges().stream()
              .filter(e -> !(e instanceof CFunctionSummaryEdge))
              .map(e -> e.getSuccessor())
              .forEach(
                  n ->
                      assertWithMessage(
                              "Internal node has a successor in the cfa that is not in the block")
                          .that(n)
                          .isIn(blockNode.getNodes()));
          cfaNode.getEnteringEdges().stream()
              .filter(e -> reachableEdges.contains(e))
              .map(e -> e.getPredecessor())
              .forEach(
                  n ->
                      assertWithMessage(
                              "Internal node has predecessor in the cfa that is not in the block")
                          .that(n)
                          .isIn(blockNode.getNodes()));
        }
      }
    }
  }

  /**
   * checks if the BlockGraph contains exactly the nodes and edges of the CFA. entry / exit nodes
   * may occur multiple times, other may not.
   */
  private static void checkCFANodes(
      BlockGraph graph, Set<CFANode> nodesCFA, Set<CFAEdge> pEdgesCFA) {

    Multiset<CFANode> nodesGraph =
        graph.getNodes().stream()
            .flatMap(node -> node.getNodes().stream())
            .collect(Collectors.toCollection(LinkedHashMultiset::create));

    assertWithMessage("BlockGraph does not contain exactly the nodes of the CFA")
        .that(nodesGraph.elementSet())
        .containsExactlyElementsIn(nodesCFA);

    Set<CFAEdge> edgesGraph =
        graph.getNodes().stream()
            .flatMap(node -> node.getEdges().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));

    assertWithMessage("BlockGraph does not contain exactly the edges of the CFA")
        .that(edgesGraph)
        .containsExactlyElementsIn(pEdgesCFA);

    for (BlockNode blocknode : graph.getNodes()) {
      CFANode entry = blocknode.getInitialLocation();
      nodesGraph.remove(entry, nodesGraph.count(entry));
      CFANode exit = blocknode.getFinalLocation();
      nodesGraph.remove(exit, nodesGraph.count(exit));
    }

    // TODO what if a node appears once as an entry node and once as an internal one -> this will
    // not fail!
    assertWithMessage(
            "A node which is neither an entry nor an exit node appears multiple times in"
                + " decomposition")
        .that(nodesGraph.entrySet().stream().allMatch(e -> e.getCount() == 1))
        .isTrue();
  }

  private static void collectReachableNodes(CFA cfa, Set<CFANode> pNodes, Builder<CFAEdge> pEdges) {
    List<CFANode> waitlist = new ArrayList<>();
    waitlist.add(cfa.getMainFunction());
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.removeFirst();
      if (pNodes.contains(current)) {
        continue;
      }
      pNodes.add(current);
      for (CFAEdge leavingEdge : current.getAllLeavingEdges()) {
        if (!(leavingEdge instanceof CFunctionSummaryEdge)) {
          pEdges.add(leavingEdge);
          waitlist.add(leavingEdge.getSuccessor());
        }
      }
    }
  }
}
