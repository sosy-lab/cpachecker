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

/**
 * Coarsens a block graph by removing eligible branch and join locations from its block boundaries.
 * This is a postprocessing step for the child decomposition, including an inlined decomposition;
 * this class does not itself inline functions.
 *
 * <p>At a candidate boundary {@code v}, let {@code P} be the incoming blocks and {@code S} the
 * outgoing blocks. Every block in {@code P} must have exactly {@code S} as its successors, and
 * every block in {@code S} must have exactly {@code P} as its predecessors. All incoming blocks end
 * at {@code v}, and all outgoing blocks start there. The transformation replaces these blocks with
 * every pairwise composition {@code p;s}, for {@code p} in {@code P} and {@code s} in {@code S}.
 * Each composition keeps the entry of {@code p}, the exit of {@code s}, and the union of their CFA
 * nodes and edges. Its external predecessors come from {@code p}, and its external successors come
 * from {@code s}; references to replaced blocks are expanded accordingly.
 *
 * <p>For example, {@code P -> {S1, S2}} becomes {@code {P;S1, P;S2}}. This duplicates the shared
 * prefix in the block representation, while retaining both branch alternatives. A subsequent {@link
 * HorizontalMergeDecomposition} combines compatible alternatives with the same surrounding blocks
 * and exit. Independent compositions are batched into one round, and rounds repeat because a merge
 * can expose further opportunities.
 *
 * <p><b>Why this preserves paths.</b> The argument assumes that the child supplies a valid block
 * decomposition, with no loops inside blocks. Every original traversal through {@code v} chooses
 * some incoming {@code p} followed by some outgoing {@code s}, and the corresponding composition
 * exists because the full cross product is retained. Conversely, an execution through a composed
 * block follows existing CFA edges from {@code p} and then {@code s}. The overlap check ensures
 * that their node sets intersect only at {@code v}, with the entry/exit exception below, so an
 * execution cannot switch between the two interiors elsewhere. Thus each composed traversal can be
 * split at {@code v} into an original traversal. Edge statements and assumptions are retained, so
 * this regrouping preserves the concrete executions as well as the control-flow alternatives.
 *
 * <p>The checks are important for this argument:
 *
 * <ul>
 *   <li>The complete predecessor/successor relation ensures that removing the old blocks loses no
 *       external use of either half and introduces no previously absent block pairing.
 *   <li>The root block and loop-head boundaries are retained. Other shared interior locations are
 *       rejected to prevent shortcuts or new internal cycles.
 *   <li>The entry of {@code p} may equal the exit of {@code s}. This yields a block whose entry and
 *       exit coincide, representing one traversal back to the boundary; repetition still takes
 *       place between block analyses. It is not an unrestricted loop inside a block.
 *   <li>A block participates in at most one composition group per round. Disjoint groups can be
 *       composed together, followed by one update of all predecessor and successor references.
 * </ul>
 *
 * <p><b>Why this helps DSS.</b> Removing an intermediate boundary lets a worker analyze a branch
 * together with its surrounding statements. This can avoid summary exchanges, boundary
 * abstractions, and refinements caused by losing correlations at that boundary. After horizontal
 * merging, fewer blocks can also mean fewer worker CPAs and solver contexts. The tradeoff is that a
 * composed block has more local work and offers less parallelism, while the cross product can
 * initially increase the number of blocks. Groups producing more than 64 compositions are skipped,
 * and the pass stops after at most 1000 rounds. These are effort limits: leaving a boundary in
 * place preserves the existing decomposition and does not discard any execution.
 */
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
