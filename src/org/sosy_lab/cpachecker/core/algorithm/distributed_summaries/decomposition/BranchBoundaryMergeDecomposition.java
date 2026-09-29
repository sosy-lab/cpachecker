// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

/** Removes acyclic branch boundaries by composing every incoming and outgoing block. */
final class BranchBoundaryMergeDecomposition implements DssBlockDecomposition {
  private final DssBlockDecomposition child;
  private int nextId;

  BranchBoundaryMergeDecomposition(DssBlockDecomposition pChild) {
    child = pChild;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    Collection<BlockNode> nodes = child.decompose(cfa).getNodes();
    Set<CFANode> protectedBoundaries =
        new LinkedHashSet<>(cfa.getLoopStructure().orElseThrow().getAllLoopHeads());
    Map<String, Set<String>> renamed = new LinkedHashMap<>();
    nodes.forEach(n -> renamed.put(n.getId(), ImmutableSet.of("ORIGINAL_" + n.getId())));
    nodes =
        nodes.stream()
            .map(
                n ->
                    new BlockNode(
                        "ORIGINAL_" + n.getId(),
                        n.getInitialLocation(),
                        n.getFinalLocation(),
                        n.getNodes(),
                        n.getEdges(),
                        replace(n.getPredecessorIds(), renamed),
                        replace(n.getSuccessorIds(), renamed)))
            .toList();
    HorizontalMergeDecomposition horizontal =
        new HorizontalMergeDecomposition(child, 2, -1, null, true);
    for (int round = 0; round < 1000; round++) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      Collection<BlockNode> merged = mergeRound(nodes, protectedBoundaries);
      if (merged == null) {
        break;
      }
      nodes = horizontal.mergeHorizontally(merged);
    }
    return new BlockGraph(ImmutableSet.copyOf(nodes));
  }

  private @Nullable Collection<BlockNode> mergeRound(
      Collection<BlockNode> nodes, Set<CFANode> protectedBoundaries) {
    Map<String, BlockNode> byId = new LinkedHashMap<>();
    Map<Set<String>, List<BlockNode>> groups = new LinkedHashMap<>();
    for (BlockNode node : nodes) {
      byId.put(node.getId(), node);
      groups.computeIfAbsent(node.getSuccessorIds(), unused -> new ArrayList<>()).add(node);
    }
    // Disjoint compositions commute. Batch them so a large inlined graph does not require
    // rebuilding every block once for each individual boundary.
    Map<String, Set<String>> replacements = new LinkedHashMap<>();
    List<BlockNode> result = new ArrayList<>();
    for (Entry<Set<String>, List<BlockNode>> entry : groups.entrySet()) {
      List<BlockNode> incoming = entry.getValue();
      if (entry.getKey().isEmpty() || incoming.stream().anyMatch(BlockNode::isRoot)) {
        continue;
      }
      Set<String> before = new LinkedHashSet<>();
      incoming.forEach(n -> before.add(n.getId()));
      List<BlockNode> outgoing = entry.getKey().stream().map(byId::get).toList();
      if (incoming.stream().anyMatch(n -> replacements.containsKey(n.getId()))
          || outgoing.stream().anyMatch(n -> replacements.containsKey(n.getId()))
          || (long) incoming.size() * outgoing.size() > 64
          || outgoing.stream()
              .anyMatch(n -> !n.getPredecessorIds().equals(before) || before.contains(n.getId()))) {
        continue;
      }
      CFANode boundary = incoming.getFirst().getFinalLocation();
      if (protectedBoundaries.contains(boundary)
          || incoming.stream().anyMatch(n -> n.getFinalLocation() != boundary)
          || outgoing.stream().anyMatch(n -> n.getInitialLocation() != boundary)) {
        continue;
      }
      boolean valid = true;
      for (BlockNode p : incoming) {
        for (BlockNode s : outgoing) {
          for (CFANode node : p.getNodes()) {
            if (s.getNodes().contains(node)
                && node != boundary
                && !(node == p.getInitialLocation() && node == s.getFinalLocation())) {
              valid = false;
            }
          }
        }
      }
      if (!valid) {
        continue;
      }
      for (BlockNode p : incoming) {
        for (BlockNode s : outgoing) {
          String id = "MBR" + nextId++;
          replacements.computeIfAbsent(p.getId(), unused -> new LinkedHashSet<>()).add(id);
          replacements.computeIfAbsent(s.getId(), unused -> new LinkedHashSet<>()).add(id);
          result.add(
              new BlockNode(
                  id,
                  p.getInitialLocation(),
                  s.getFinalLocation(),
                  ImmutableSet.<CFANode>builder().addAll(p.getNodes()).addAll(s.getNodes()).build(),
                  ImmutableSet.<CFAEdge>builder().addAll(p.getEdges()).addAll(s.getEdges()).build(),
                  p.getPredecessorIds(),
                  s.getSuccessorIds()));
        }
      }
    }
    if (replacements.isEmpty()) {
      return null;
    }
    for (BlockNode n : nodes) {
      if (!replacements.containsKey(n.getId())) {
        result.add(n);
      }
    }
    return result.stream()
        .map(
            n ->
                new BlockNode(
                    n.getId(),
                    n.getInitialLocation(),
                    n.getFinalLocation(),
                    n.getNodes(),
                    n.getEdges(),
                    replace(n.getPredecessorIds(), replacements),
                    replace(n.getSuccessorIds(), replacements)))
        .toList();
  }

  private static ImmutableSet<String> replace(
      Set<String> ids, Map<String, Set<String>> replacements) {
    ImmutableSet.Builder<String> result = ImmutableSet.<String>builder();
    for (String id : ids) {
      result.addAll(replacements.getOrDefault(id, ImmutableSet.of(id)));
    }
    return result.build();
  }
}
