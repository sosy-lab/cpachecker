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
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
      // A block without a successor does not need a unique exit node, as it will never send a
      // relevant postcondition and never receive a violation condition. Relaxing this check allows
      // the SingleBlockDecomposition to work for programs with CFATerminationNodes
      if (!blockNode.getSuccessorIds().isEmpty()) {
        Preconditions.checkState(
            isBlockNodeValid(blockNode.getInitialLocation(), blockNode.getEdges()),
            "BlockNodes require to have exactly one exit node (%s).",
            blockNode);
      }
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
          if (!covered.contains(leavingEdge.getSuccessor())) {
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

    ImmutableSet<@NonNull BlockNode> blockNodes =
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
                    transformedImmutableSetCopy(
                        startNodes.get(b.getFinalLocation()),
                        BlockNodeWithoutGraphInformation::getId),
                    b.getFinalLocation()));
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
              ImmutableSet.copyOf(importedBlock.successors()),
              pIdToNodeMap.get(importedBlock.abstractionLocation())));
    }
    return new BlockGraph(nodes.build());
  }

  public void export(Path blockCFAFile, CFA cfa) throws IOException {
    Map<String, Map<String, Object>> treeMap = new HashMap<>();
    int minCfaNodeNumber =
        cfa.nodes().stream().mapToInt(CFANode::getNodeNumber).min().orElseThrow();
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
                              shiftedNodeNumber(
                                  e.getPredecessor().getNodeNumber(), minCfaNodeNumber),
                              shiftedNodeNumber(
                                  e.getSuccessor().getNodeNumber(), minCfaNodeNumber))));
              attributes.put(
                  "startNode",
                  shiftedNodeNumber(n.getInitialLocation().getNodeNumber(), minCfaNodeNumber));
              attributes.put(
                  "endNode",
                  shiftedNodeNumber(n.getFinalLocation().getNodeNumber(), minCfaNodeNumber));
              attributes.put(
                  "abstractionLocation",
                  shiftedNodeNumber(
                      n.getViolationConditionLocation().getNodeNumber(), minCfaNodeNumber));
              treeMap.put(n.getId(), attributes);
            });
    JSON.writeJSONString(treeMap, blockCFAFile);
  }

  // All node IDs are shifted such that they start from 0
  private int shiftedNodeNumber(int originalNodeNumber, int shift) {
    return originalNodeNumber - shift;
  }

  @Override
  public String toString() {
    return "BlockGraph{"
        + "nodes="
        + nodes.stream().map(BlockNode::getId).collect(Collectors.joining(", "))
        + '}';
  }

  @Override
  public boolean equals(@Nullable Object pOther) {
    if (this == pOther) {
      return true;
    }

    return pOther instanceof BlockGraph other
        && nodes.equals(other.nodes)
        && root.equals(other.root);
  }

  @Override
  public int hashCode() {
    return nodes.hashCode();
  }
}
