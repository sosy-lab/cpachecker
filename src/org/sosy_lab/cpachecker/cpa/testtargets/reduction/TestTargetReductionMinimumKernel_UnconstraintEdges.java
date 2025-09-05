// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.testtargets.reduction.TestTargetReductionUtils.CFAEdgeNode;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

/*
 * Based on papers
 * Antonia Bertolino, Martina Marré: Automatic Generation of Path Covers Based on the
 * Control Flow Analysis of Computer Programs. IEEE TSE. 20(12): 885-899 (1994)
 *
 * ... TODO paper minimum kernel
 */

public class TestTargetReductionMinimumKernel_UnconstraintEdges {

  public Set<CFAEdge> reduceTargets(
      final Set<CFAEdge> pTargets, final CFA pCfa, final boolean computeMinimumKernel) {
    Preconditions.checkNotNull(pTargets);
    Preconditions.checkNotNull(pCfa);

    Map<CFAEdge, CFAEdgeNode> targetToGoalGraphNode =
        Maps.newHashMapWithExpectedSize(pTargets.size());
    Pair<Pair<CFAEdgeNode, CFAEdgeNode>, ImmutableSet<Pair<CFAEdgeNode, CFAEdgeNode>>> result =
        TestTargetReductionUtils.buildNodeBasedTestGoalGraph(
            pTargets, pCfa.getMainFunction(), targetToGoalGraphNode);
    Pair<CFAEdgeNode, CFAEdgeNode> entryExit = result.getFirst();

    DomTree<CFAEdgeNode> domTree =
        DomTree.forGraph(
            CFAEdgeNode::allPredecessorsOf, CFAEdgeNode::allSuccessorsOf, entryExit.getFirst());
    DomTree<CFAEdgeNode> inverseDomTree =
        DomTree.forGraph(
            CFAEdgeNode::allSuccessorsOf, CFAEdgeNode::allPredecessorsOf, entryExit.getSecond());

    Set<CFAEdgeNode> domLeaves =
        new HashSet<>(
            getLeavesOfDomintorTree(
                domTree, false, targetToGoalGraphNode.values(), result.getSecond()));
    Set<CFAEdgeNode> postDomLeaves =
        new HashSet<>(
            getLeavesOfDomintorTree(
                inverseDomTree, true, targetToGoalGraphNode.values(), result.getSecond()));

    // remove start and end node from graph, ensure that predecessor is considered
    // if it is not the root node of the tree
    CFAEdgeNode parent;
    domLeaves.remove(entryExit.getSecond());
    parent = domTree.getParent(entryExit.getSecond()).orElse(entryExit.getFirst());
    domLeaves.add(parent);
    domLeaves.remove(entryExit.getFirst());

    postDomLeaves.remove(entryExit.getFirst());
    parent = inverseDomTree.getParent(entryExit.getFirst()).orElse(entryExit.getSecond());
    postDomLeaves.add(parent);
    postDomLeaves.remove(entryExit.getSecond());

    // no reachable test targets
    if (domLeaves.isEmpty() || postDomLeaves.isEmpty()) {
      return ImmutableSet.of();
    }

    if (computeMinimumKernel) {
      if (domLeaves.size() < postDomLeaves.size()) {
        return findAndSubstractLD(domLeaves, inverseDomTree, Optional.of(result.getSecond()));
      } else {
        return findAndSubstractLD(postDomLeaves, domTree, Optional.empty());
      }
    } else {
      // Paper unconstraint edges (unconstraint arcs):
      // Automatic Generation of Path Covers Based on the Control Flow Analysis of Computer Programs
      return new HashSet<>(
          transformedImmutableSetCopy(
              Sets.intersection(domLeaves, postDomLeaves), CFAEdgeNode::getRepresentedEdge));
    }
  }

  private ImmutableSet<CFAEdgeNode> getLeavesOfDomintorTree(
      final DomTree<CFAEdgeNode> pDomTree,
      final boolean isPostDomTree,
      final Collection<CFAEdgeNode> pAllTargets,
      final ImmutableSet<Pair<CFAEdgeNode, CFAEdgeNode>> pPathsWithInputs) {
    Set<CFAEdgeNode> nonLeaves = Sets.newHashSetWithExpectedSize(pDomTree.getNodeCount());
    for (CFAEdgeNode domTreeEntry : pDomTree) {
      pDomTree
          .getParent(domTreeEntry)
          .ifPresent(
              parent -> {
                if (!isPostDomTree || !pPathsWithInputs.contains(Pair.of(domTreeEntry, parent))) {
                  nonLeaves.add(parent);
                }
              });
    }

    // targets not reachable from entry/exit not part of the dominator tree
    // but must be considered as leave nodes as well
    return FluentIterable.from(pDomTree)
        .append(pAllTargets)
        .filter(node -> !nonLeaves.contains(node))
        .toSet();
  }

  private Set<CFAEdge> findAndSubstractLD(
      final Set<CFAEdgeNode> pLeaves,
      final DomTree<CFAEdgeNode> pReverseDomTree,
      final Optional<ImmutableSet<Pair<CFAEdgeNode, CFAEdgeNode>>> pPathsWithInputs) {
    Set<CFAEdgeNode> ldSet = Sets.newHashSetWithExpectedSize(pLeaves.size());
    for (CFAEdgeNode domTreeEntry : pReverseDomTree) {
      if (pPathsWithInputs.isPresent()) {
        ldSet.addAll(
            Sets.intersection(
                FluentIterable.from(pReverseDomTree.getAncestors(domTreeEntry))
                    .filter(
                        ancestor ->
                            !pPathsWithInputs
                                .orElseThrow()
                                .contains(Pair.of(domTreeEntry, ancestor)))
                    .toSet(),
                pLeaves));

      } else {
        ldSet.addAll(Sets.intersection(pReverseDomTree.getAncestors(domTreeEntry), pLeaves));
      }
    }
    // return pLeaves\ldSet
    return new HashSet<>(
        FluentIterable.from(pLeaves)
            .filter(node -> !ldSet.contains(node))
            .transform(CFAEdgeNode::getRepresentedEdge)
            .toSet());
  }
}
