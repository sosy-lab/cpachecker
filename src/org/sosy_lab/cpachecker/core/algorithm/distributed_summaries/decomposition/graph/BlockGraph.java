// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.StronglyConnectedComponents;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockGraph {

  public static final String GHOST_EDGE_DESCRIPTION = "<<ghost-edge>>";

  private final ImmutableSet<@NonNull BlockNode> nodes;
  private final BlockNode root;

  public BlockGraph(ImmutableSet<@NonNull BlockNode> pNodes) {
    nodes = pNodes;
    root =
        Iterables.getOnlyElement(
            FluentIterable.from(pNodes).filter(n -> n.getPredecessorIds().isEmpty()));
  }

  public BlockNode getRoot() {
    return root;
  }

  public ImmutableSet<@NonNull BlockNode> getNodes() {
    return nodes;
  }

  public void checkConsistency(ShutdownNotifier pShutdownNotifier) throws InterruptedException {
    for (BlockNode blockNode : nodes) {
      Preconditions.checkState(
          CFAUtils.existsPath(
              blockNode.getInitialLocation(),
              blockNode.getFinalLocation(),
              node -> node.getAllLeavingEdges().toSet(),
              pShutdownNotifier),
          "pNodesInBlock (%s) "
              + "must list all nodes but misses either the root node (%s) "
              + "or the last node (%s).",
          blockNode.getNodes(),
          blockNode.getInitialLocation(),
          blockNode.getFinalLocation());
      // block node is not root implies that there is at least one edge in the block
      Preconditions.checkState(
          !blockNode.getEdges().isEmpty() || blockNode.getPredecessorIds().isEmpty(),
          "Every block needs at least one edge (%s).",
          blockNode);
      Preconditions.checkState(
          isBlockNodeValid(blockNode.getInitialLocation(), blockNode.getEdges()),
          "BlockNodes require to have exactly one exit node (%s).",
          blockNode);
      Preconditions.checkState(
          blockNode.getPredecessorIds().containsAll(blockNode.getLoopPredecessorIds()),
          "Found loop predecessors that are not in the set of predecessors (%s).",
          blockNode);
    }
  }

  private boolean isBlockNodeValid(CFANode pStartNode, Set<CFAEdge> pEdgesInBlock) {
    ArrayDeque<CFANode> waiting = new ArrayDeque<>();
    waiting.push(pStartNode);
    SequencedSet<CFANode> covered = new LinkedHashSet<>();
    int count = 0;
    while (!waiting.isEmpty()) {
      CFANode curr = waiting.pop();
      boolean hasSuccessor = false;
      for (CFAEdge leavingEdge : curr.getAllLeavingEdges()) {
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
      Collection<? extends @NonNull BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<CFANode, @NonNull BlockNodeWithoutGraphInformation> startNodes =
        ArrayListMultimap.create();
    Multimap<CFANode, @NonNull BlockNodeWithoutGraphInformation> endNodes =
        ArrayListMultimap.create();
    for (BlockNodeWithoutGraphInformation blockNode : pNodes) {
      startNodes.put(blockNode.getInitialLocation(), blockNode);
      endNodes.put(blockNode.getFinalLocation(), blockNode);
    }
    BlockNodeWithoutGraphInformation root =
        Iterables.getOnlyElement(
            FluentIterable.from(pNodes)
                .filter(n -> endNodes.get(n.getInitialLocation()).isEmpty()));
    Multimap<@NonNull BlockNodeWithoutGraphInformation, @NonNull BlockNodeWithoutGraphInformation>
        loopPredecessors = findLoopPredecessors(root, pNodes);

    ImmutableSet<BlockNode> blockNodes =
        transformedImmutableSetCopy(
            pNodes,
            b ->
                new BlockNode(
                    b.getId(),
                    b.getInitialLocation(),
                    b.getFinalLocation(),
                    b.getNodes(),
                    b.getEdges(),
                    transformedImmutableSetCopy(
                        endNodes.get(b.getInitialLocation()),
                        BlockNodeWithoutGraphInformation::getId),
                    Sets.intersection(
                            transformedImmutableSetCopy(
                                endNodes.get(b.getInitialLocation()),
                                BlockNodeWithoutGraphInformation::getId),
                            transformedImmutableSetCopy(
                                loopPredecessors.get(b), BlockNodeWithoutGraphInformation::getId))
                        .immutableCopy(),
                    transformedImmutableSetCopy(
                        startNodes.get(b.getFinalLocation()),
                        BlockNodeWithoutGraphInformation::getId)));
    return new BlockGraph(blockNodes);
  }

  public static BlockGraph fromImportedNodes(
      Set<BlockNodeWithoutGraphInformation> pBlockNodes,
      Map<String, ImportedBlock> pImportedBlockMap,
      Map<Integer, CFANode> pIdToNodeMap) {
    ImmutableSet.Builder<BlockNode> nodes = ImmutableSet.builder();
    for (BlockNodeWithoutGraphInformation blockNode : pBlockNodes) {
      ImportedBlock importedBlock = pImportedBlockMap.get(blockNode.getId());
      nodes.add(
          new BlockNode(
              blockNode.getId(),
              blockNode.getInitialLocation(),
              blockNode.getFinalLocation(),
              blockNode.getNodes(),
              blockNode.getEdges(),
              ImmutableSet.copyOf(importedBlock.predecessors()),
              ImmutableSet.copyOf(importedBlock.loopPredecessors()),
              ImmutableSet.copyOf(importedBlock.successors()),
              pIdToNodeMap.get(importedBlock.abstractionLocation())));
    }
    return new BlockGraph(nodes.build());
  }

  private static Multimap<BlockNodeWithoutGraphInformation, BlockNodeWithoutGraphInformation>
      findLoopPredecessors(
          BlockNodeWithoutGraphInformation pRoot,
          Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<BlockNodeWithoutGraphInformation, BlockNodeWithoutGraphInformation> predecessors =
        ArrayListMultimap.create();
    Multimap<CFANode, BlockNodeWithoutGraphInformation> startNodeToBlockNodes =
        ArrayListMultimap.create();
    pNodes.forEach(p -> startNodeToBlockNodes.put(p.getInitialLocation(), p));
    ImmutableList<List<BlockNodeWithoutGraphInformation>> stronglyConnected =
        StronglyConnectedComponents.performTarjanAlgorithm(
            pRoot, n -> startNodeToBlockNodes.get(n.getFinalLocation()));
    for (List<BlockNodeWithoutGraphInformation> connections : stronglyConnected) {
      for (BlockNodeWithoutGraphInformation connection : connections) {
        predecessors.putAll(connection, connections);
      }
    }
    return predecessors;
  }

  public void export(Path blockCFAFile) throws IOException {
    Map<String, Map<String, Object>> treeMap = new HashMap<>();
    getNodes()
        .forEach(
            n -> {
              Map<String, Object> attributes = new HashMap<>();
              attributes.put("code", Splitter.on("\n").splitToList(n.getCode()));
              attributes.put("predecessors", ImmutableList.copyOf(n.getPredecessorIds()));
              attributes.put("successors", ImmutableList.copyOf(n.getSuccessorIds()));
              attributes.put(
                  "edges",
                  transformedImmutableListCopy(
                      n.getEdges(),
                      e ->
                          ImmutableList.of(
                              e.getPredecessor().getNodeNumber(),
                              e.getSuccessor().getNodeNumber())));
              attributes.put("startNode", n.getInitialLocation().getNodeNumber());
              attributes.put("endNode", n.getFinalLocation().getNodeNumber());
              attributes.put("loopPredecessors", n.getLoopPredecessorIds());
              attributes.put(
                  "abstractionLocation", n.getViolationConditionLocation().getNodeNumber());
              treeMap.put(n.getId(), attributes);
            });
    JSON.writeJSONString(treeMap, blockCFAFile);
  }

  @Override
  public String toString() {
    return "BlockGraph{"
        + "nodes="
        + nodes.stream().map(BlockNode::getId).collect(Collectors.joining(", "))
        + '}';
  }
}
