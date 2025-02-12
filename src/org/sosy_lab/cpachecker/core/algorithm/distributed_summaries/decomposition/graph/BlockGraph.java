// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static com.google.common.base.Preconditions.checkArgument;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.StronglyConnectedComponents;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockGraph {

  static final String GHOST_EDGE_DESCRIPTION = "<<ghost-edge>>";

  public static final String ROOT_ID = "root";
  private final BlockNode root;
  private final ImmutableSet<BlockNode> nodes;

  public BlockGraph(BlockNode pRoot, ImmutableSet<BlockNode> pNodes) {
    checkArgument(
        pRoot.getPredecessorIds().isEmpty(), "Node with ID: '%s' has predecessors.", ROOT_ID);
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
      Preconditions.checkState(
          !blockNode.isRoot(), "Only one root node per graph allowed (%s).", blockNode);
      Preconditions.checkState(
          !blockNode.getId().equals(BlockGraph.ROOT_ID)
              || (blockNode.getPredecessorIds().isEmpty() && blockNode.isRoot()),
          "Only root nodes should not have predecessors (%s).",
          blockNode);
      Preconditions.checkState(
          CFAUtils.existsPath(
              blockNode.getInitialLocation(),
              blockNode.getFinalLocation(),
              node -> CFAUtils.allLeavingEdges(node).toSet(),
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
      startNodes.put(blockNode.getInitialLocation(), blockNode);
      endNodes.put(blockNode.getFinalLocation(), blockNode);
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

    startNodes.put(root.getInitialLocation(), root);
    endNodes.put(root.getFinalLocation(), root);
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
    return new BlockGraph(root, blockNodes);
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
    ImmutableSet<BlockNode> allNodes = nodes.build();
    BlockNode root =
        Iterables.getOnlyElement(FluentIterable.from(allNodes).filter(BlockNode::isRoot));
    return new BlockGraph(root, FluentIterable.from(allNodes).filter(b -> !b.isRoot()).toSet());
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
    Iterables.concat(getNodes(), ImmutableList.of(getRoot()))
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
        + "rootNode="
        + root.getInitialLocation()
        + ", nodes="
        + nodes.stream().map(BlockNode::getId).collect(Collectors.joining(", "))
        + '}';
  }
}
