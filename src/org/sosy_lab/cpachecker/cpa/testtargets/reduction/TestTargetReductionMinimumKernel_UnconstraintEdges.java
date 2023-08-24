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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.testtargets.reduction.TestTargetReductionUtils.CFAEdgeNode;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

public class TestTargetReductionMinimumKernel_UnconstraintEdges {

  public Set<CFAEdge> reduceTargets(
      final Set<CFAEdge> pTargets, final CFA pCfa, final boolean computeMinimumKernel) {
    Preconditions.checkNotNull(pTargets);
    Preconditions.checkNotNull(pCfa);

    Map<CFAEdge, CFAEdgeNode> targetToGoalGraphNode =
        Maps.newHashMapWithExpectedSize(pTargets.size());
    Pair<CFAEdgeNode, CFAEdgeNode> entryExit =
        TestTargetReductionUtils.buildNodeBasedTestGoalGraph(
            pTargets, pCfa.getMainFunction(), targetToGoalGraphNode);

    DomTree<CFAEdgeNode> domTree =
        DomTree.forGraph(
            CFAEdgeNode::allPredecessorsOf, CFAEdgeNode::allSuccessorsOf, entryExit.getFirst());
    DomTree<CFAEdgeNode> inverseDomTree =
        DomTree.forGraph(
            CFAEdgeNode::allSuccessorsOf, CFAEdgeNode::allPredecessorsOf, entryExit.getSecond());

    Set<CFAEdgeNode> domLeaves = TestTargetReductionUtils.getLeavesOfDomintorTree(domTree);
    Set<CFAEdgeNode> postDomLeaves =
        TestTargetReductionUtils.getLeavesOfDomintorTree(inverseDomTree);

    // remove start and end node from graph, ensure that predecessor is considered
    // if it is not the root node of the tree
    CFAEdgeNode parent;
    domLeaves.remove(entryExit.getSecond());
    parent = domTree.getParent(entryExit.getSecond()).orElse(entryExit.getFirst());
    domLeaves.add(parent);
    domLeaves.remove(entryExit.getFirst());

    postDomLeaves.remove(entryExit.getFirst());
    parent = inverseDomTree.getParent(entryExit.getSecond()).orElse(entryExit.getSecond());
    postDomLeaves.add(parent);
    postDomLeaves.remove(entryExit.getSecond());

    // no reachable test targets
    if (domLeaves.isEmpty() || postDomLeaves.isEmpty()) {
      return ImmutableSet.of();
    }

    if (computeMinimumKernel) {
      if (domLeaves.size() < postDomLeaves.size()) {
        return findAndSubstractLD(domLeaves, inverseDomTree);
      } else {
        return findAndSubstractLD(postDomLeaves, domTree);
      }
    } else {
      // Paper unconstraint edges (unconstraint arcs):
      // Automatic Generation of Path Covers Based on the Control Flow Analysis of Computer Programs
      return new HashSet<>(
          transformedImmutableSetCopy(
              Sets.intersection(domLeaves, postDomLeaves), node -> node.getRepresentedEdge()));
    }
  }

  private Set<CFAEdge> findAndSubstractLD(
      Set<CFAEdgeNode> pLeaves, DomTree<CFAEdgeNode> pReverseDomTree) {
    Set<CFAEdgeNode> ldSet = Sets.newHashSetWithExpectedSize(pLeaves.size());
    for (CFAEdgeNode domTreeEntry : pReverseDomTree) {
      ldSet.addAll(Sets.intersection(pReverseDomTree.getAncestors(domTreeEntry), pLeaves));
    }
    // return pLeaves/ldSet
    return new HashSet<>(
        FluentIterable.from(pLeaves)
            .filter(node -> !ldSet.contains(node))
            .transform(node -> node.getRepresentedEdge())
            .toSet());
  }
}
