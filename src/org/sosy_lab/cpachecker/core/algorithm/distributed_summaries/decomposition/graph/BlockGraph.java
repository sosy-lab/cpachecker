// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.Tarjan;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockGraph {

  public static final String ROOT_ID = "root";
  private final BlockNode root;
  private final ImmutableSet<BlockNode> nodes;

  public BlockGraph(BlockNode pRoot, ImmutableSet<BlockNode> pNodes) {
    Preconditions.checkArgument(
        pRoot.getPredecessorIds().isEmpty(), "Node with ID: '" + ROOT_ID + "' has predecessors.");
    Preconditions.checkArgument(
        pNodes.stream().noneMatch(b -> b.equals(pRoot) || b.getId().equals(ROOT_ID)),
        "Root nodes are ambiguous.");
    nodes = pNodes;
    root = pRoot;
  }

  public BlockNode getRoot() {
    return root;
  }

  public ImmutableSet<BlockNode> getNodes() {
    return nodes;
  }

  public void checkConsistency(ShutdownNotifier pShutdownNotifier) throws InterruptedException {
    for (BlockNode blockNode : nodes) {
      if (blockNode.isRoot()) {
        throw new IllegalStateException("Only one root per BlockGraph allowed.");
      }
      Preconditions.checkState(
          !blockNode.getId().equals(BlockGraph.ROOT_ID)
              || (blockNode.getPredecessorIds().isEmpty() && blockNode.isRoot()),
          "Only root nodes should not have predecessors.");
      Preconditions.checkState(
          CFAUtils.existsPath(
              blockNode.getFirst(),
              blockNode.getLast(),
              node -> CFAUtils.allLeavingEdges(node).toSet(),
              pShutdownNotifier),
          "pNodesInBlock (%s) "
              + "must list all nodes but misses either the root node (%s) "
              + "or the last node (%s).",
          blockNode.getNodes(),
          blockNode.getFirst(),
          blockNode.getLast());
      // block node is not root implies that there is at least one edge in the block
      Preconditions.checkState(
          !blockNode.getEdges().isEmpty() || blockNode.getPredecessorIds().isEmpty(),
          "Every block needs at least one edge");
      Preconditions.checkState(
          isBlockNodeValid(blockNode.getFirst(), blockNode.getEdges()),
          "BlockNodes require to have exactly one exit node.");
      Preconditions.checkState(
          blockNode.getPredecessorIds().containsAll(blockNode.getLoopPredecessorIds()),
          "Found loop predecessors that are not in the set of predecessors?");
    }
  }

  private boolean isBlockNodeValid(CFANode pStartNode, Set<CFAEdge> pEdgesInBlock) {
    ArrayDeque<CFANode> waiting = new ArrayDeque<>();
    waiting.push(pStartNode);
    Set<CFANode> covered = new LinkedHashSet<>();
    int count = 0;
    while (!waiting.isEmpty()) {
      CFANode curr = waiting.pop();
      boolean hasSuccessor = false;
      for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(curr)) {
        if (pEdgesInBlock.contains(leavingEdge)) {
          if (covered.contains(leavingEdge.getSuccessor())) {
            waiting.push(leavingEdge.getSuccessor());
          }
          hasSuccessor = true;
        }
      }
      if (!hasSuccessor) {
        count++;
      }
      covered.add(curr);
    }
    return count <= 1;
  }

  public static BlockGraph fromBlockNodesWithoutGraphInformation(
      CFA pCFA, Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<CFANode, BlockNodeWithoutGraphInformation> startNodes = ArrayListMultimap.create();
    Multimap<CFANode, BlockNodeWithoutGraphInformation> endNodes = ArrayListMultimap.create();
    for (BlockNodeWithoutGraphInformation blockNode : pNodes) {
      startNodes.put(blockNode.getFirst(), blockNode);
      endNodes.put(blockNode.getLast(), blockNode);
    }
    BlockNode root =
        new BlockNode(
            BlockGraph.ROOT_ID,
            pCFA.getMainFunction(),
            pCFA.getMainFunction(),
            ImmutableSet.of(pCFA.getMainFunction()),
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            FluentIterable.from(startNodes.get(pCFA.getMainFunction()))
                .transform(BlockNodeWithoutGraphInformation::getId)
                .filter(id -> !id.equals(ROOT_ID))
                .toSet());

    Multimap<BlockNodeWithoutGraphInformation, BlockNodeWithoutGraphInformation> loopPredecessors =
        findLoopPredecessors(root, pNodes);

    startNodes.put(root.getFirst(), root);
    endNodes.put(root.getLast(), root);
    ImmutableSet<BlockNode> blockNodes =
        FluentIterable.from(pNodes)
            .transform(
                b ->
                    new BlockNode(
                        b.getId(),
                        b.getFirst(),
                        b.getLast(),
                        b.getNodes(),
                        b.getEdges(),
                        transformedImmutableSetCopy(
                            endNodes.get(b.getFirst()), BlockNodeWithoutGraphInformation::getId),
                        Sets.intersection(
                                transformedImmutableSetCopy(
                                    endNodes.get(b.getFirst()),
                                    BlockNodeWithoutGraphInformation::getId),
                                transformedImmutableSetCopy(
                                    loopPredecessors.get(b),
                                    BlockNodeWithoutGraphInformation::getId))
                            .immutableCopy(),
                        transformedImmutableSetCopy(
                            startNodes.get(b.getLast()), BlockNodeWithoutGraphInformation::getId)))
            .toSet();
    return new BlockGraph(root, blockNodes);
  }

  private static Multimap<BlockNodeWithoutGraphInformation, BlockNodeWithoutGraphInformation>
      findLoopPredecessors(
          BlockNodeWithoutGraphInformation pRoot,
          Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<BlockNodeWithoutGraphInformation, BlockNodeWithoutGraphInformation> predecessors =
        ArrayListMultimap.create();
    Multimap<CFANode, BlockNodeWithoutGraphInformation> startNodeToBlockNodes =
        ArrayListMultimap.create();
    pNodes.forEach(p -> startNodeToBlockNodes.put(p.getFirst(), p));
    ImmutableList<ImmutableList<BlockNodeWithoutGraphInformation>> stronglyConnected =
        Tarjan.performTarjanAlgorithm(pRoot, n -> startNodeToBlockNodes.get(n.getLast()));
    for (List<BlockNodeWithoutGraphInformation> connections : stronglyConnected) {
      for (BlockNodeWithoutGraphInformation connection : connections) {
        predecessors.putAll(connection, connections);
      }
    }
    return predecessors;
  }

  @Override
  public String toString() {
    return "BlockGraph{"
        + "rootNode="
        + root.getFirst()
        + ", nodes="
        + nodes.stream().map(BlockNode::getId).collect(Collectors.joining(", "))
        + '}';
  }
}
