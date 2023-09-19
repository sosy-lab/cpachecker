// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.testtargets.reduction.TestTargetReductionUtils.CFAEdgeNode;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

public class TestTargetReductionSpanningForest {
  public Set<CFAEdge> reduceTargets(
      final Set<CFAEdge> pTargets, final CFA pCfa, final boolean useEdgeBasedTestGoalGraph) {
    Preconditions.checkNotNull(pTargets);
    Preconditions.checkNotNull(pCfa);
    if (useEdgeBasedTestGoalGraph) {
      return reduceTargetsWithEdgeBasedTestGoalGraph(pTargets, pCfa);
    } else {
      return reduceTargetsWithNodeBasedTestGoalGraph(pTargets, pCfa);
    }
  }

  private Set<CFAEdge> reduceTargetsWithEdgeBasedTestGoalGraph(
      final Set<CFAEdge> pTargets, final CFA pCfa) {
    Map<CFAEdge, CFAEdge> copyToTarget = Maps.newHashMapWithExpectedSize(pTargets.size());
    Pair<CFANode, CFANode> entryExit =
        TestTargetReductionUtils.buildEdgeBasedTestGoalGraph(
            pTargets, copyToTarget, pCfa.getMainFunction());

    Set<CFAEdge> visited = Sets.newHashSetWithExpectedSize(pTargets.size());
    Map<CFAEdge, CFAEdgeNode> targetsAsNodes = Maps.newHashMapWithExpectedSize(pTargets.size());

    for (CFAEdge target : pTargets) {
      targetsAsNodes.put(target, new CFAEdgeNode(target));
    }

    SubsumptionOracleForTargetsAsEdges oracle =
        new SubsumptionOracleForTargetsAsEdges(entryExit, copyToTarget);

    for (CFAEdge target : pTargets) {
      if (visited.contains(target)) {
        for (CFAEdge target2 : pTargets) {
          if (target == target2 || visited.contains(target2)) {
            continue;
          }
          // TODO currently only approximation via dominator trees on nodes, not on edges
          if (oracle.subsumes(target, target2)) { // target subsumes target2
            targetsAsNodes.get(target).addEdgeTo(targetsAsNodes.get(target2));
            visited.add(target2);
          }
        }
      }
    }
    return getRootNodes(targetsAsNodes.values());
  }

  private Set<CFAEdge> getRootNodes(final Collection<CFAEdgeNode> forestNodes) {
    return new HashSet<>(
        FluentIterable.from(forestNodes)
            .filter(CFAEdgeNode::isRoot)
            .transform(CFAEdgeNode::getRepresentedEdge)
            .toSet());
  }

  private Set<CFAEdge> reduceTargetsWithNodeBasedTestGoalGraph(
      final Set<CFAEdge> pTargets, final CFA pCfa) {
    Map<CFAEdge, CFAEdgeNode> targetToGoalGraphNode =
        Maps.newHashMapWithExpectedSize(pTargets.size());
    Pair<CFAEdgeNode, CFAEdgeNode> entryExit =
        TestTargetReductionUtils.buildNodeBasedTestGoalGraph(
            pTargets, pCfa.getMainFunction(), targetToGoalGraphNode);

    Set<CFAEdgeNode> visited = Sets.newHashSetWithExpectedSize(targetToGoalGraphNode.size());
    Map<CFAEdgeNode, CFAEdgeNode> forestNodes =
        Maps.newHashMapWithExpectedSize(targetToGoalGraphNode.size());

    for (CFAEdgeNode graphNode : targetToGoalGraphNode.values()) {
      forestNodes.put(graphNode, new CFAEdgeNode(graphNode.getRepresentedEdge()));
    }

    DomTree<CFAEdgeNode> domTree =
        DomTree.forGraph(
            CFAEdgeNode::allPredecessorsOf, CFAEdgeNode::allSuccessorsOf, entryExit.getFirst());
    DomTree<CFAEdgeNode> inverseDomTree =
        DomTree.forGraph(
            CFAEdgeNode::allSuccessorsOf, CFAEdgeNode::allPredecessorsOf, entryExit.getSecond());
    /*
     * Implementation of Arcs subsumes?. An arc e subsumes an arc e’ if every path from the
     * entry arc to e contains e’ or else if every path from e to the exit arc contains e’
     * [4], i.e., if AL(eo,e’,e) or AL(e,e’,e~).
     */
    for (CFAEdgeNode target : targetToGoalGraphNode.values()) {
      if (visited.contains(target)) {
        for (CFAEdgeNode target2 : targetToGoalGraphNode.values()) {
          if (target == target2 || visited.contains(target2)) {
            continue;
          }
          // target subsumes target2
          if (domTree.isAncestorOf(target2, target)
              || inverseDomTree.isAncestorOf(target2, target)) {
            forestNodes.get(target).addEdgeTo(forestNodes.get(target2));
            visited.add(target2);
          }
        }
      }
    }
    return getRootNodes(forestNodes.values());
  }
}
