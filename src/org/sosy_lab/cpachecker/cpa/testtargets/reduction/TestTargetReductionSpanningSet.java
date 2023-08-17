// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.testtargets.reduction.TestTargetReductionUtils.CFAEdgeNode;
import org.sosy_lab.cpachecker.util.Pair;

public class TestTargetReductionSpanningSet {

  public Set<CFAEdge> reduceTargets(final Set<CFAEdge> pTargets, final CFA pCfa) {
    Preconditions.checkNotNull(pTargets);
    return getTestTargetsFromLeaves(
        reduceSubsumptionGraph(
            computeStronglyConnectedComponents(
                constructSubsumptionGraph(pTargets, pCfa.getMainFunction()))));
  }

  private ImmutableList<CFAEdgeNode> constructSubsumptionGraph(
      final Set<CFAEdge> pTargets, FunctionEntryNode pStartNode) {
    ImmutableList.Builder<CFAEdgeNode> nodeBuilder = ImmutableList.builder();

    CFAEdgeNode node;
    Map<CFAEdge, CFAEdgeNode> edgeToNode = Maps.newHashMapWithExpectedSize(pTargets.size());
    Map<CFAEdge, CFAEdge> copyToTarget = Maps.newHashMapWithExpectedSize(pTargets.size());

    Pair<CFANode, CFANode> entryExit =
        TestTargetReductionUtils.buildTestGoalGraph(pTargets, copyToTarget, pStartNode);

    for (CFAEdge target : pTargets) {
      node = new CFAEdgeNode(target);
      edgeToNode.put(target, node);
      nodeBuilder.add(node);
    }

    SubsumptionOracle subOracle = new SubsumptionOracle(entryExit, copyToTarget);

    /*
    try {
      TestTargetReductionUtils.drawGraph(Paths.get("subSumGraph.dot"), entryExit.getFirst());
    } catch (IOException e) {
    }*/

    for (CFAEdge targetPred : pTargets) {
      for (CFAEdge targetSucc : pTargets) {
        if (targetPred == targetSucc) {
          continue;
        }
        // TODO currently only approximation via dominator trees on nodes, not on edges
        if (subOracle.subsumes(targetSucc, targetPred)) {
          edgeToNode.get(targetPred).addEdgeTo(edgeToNode.get(targetSucc));
        }
      }
    }

    return nodeBuilder.build();
  }

  private ImmutableList<Collection<CFAEdgeNode>> computeStronglyConnectedComponents(
      final ImmutableList<CFAEdgeNode> nodes) {
    ImmutableList.Builder<Collection<CFAEdgeNode>> componentsBuilder = ImmutableList.builder();
    Deque<CFAEdgeNode> ordered = new ArrayDeque<>(nodes.size());

    Set<CFAEdgeNode> visited = Sets.newHashSetWithExpectedSize(nodes.size());
    for (CFAEdgeNode node : nodes) {
      dfs(node, false, visited, ordered);
    }

    visited = Sets.newHashSetWithExpectedSize(nodes.size());
    for (CFAEdgeNode node : ordered) {
      Deque<CFAEdgeNode> componentElems = new ArrayDeque<>();
      dfs(node, true, visited, componentElems);
      if (!componentElems.isEmpty()) {
        componentsBuilder.add(componentElems);
      }
    }

    return componentsBuilder.build();
  }

  private void dfs(
      final CFAEdgeNode pNode,
      boolean pBackwards,
      final Set<CFAEdgeNode> pVisited,
      final Deque<CFAEdgeNode> componentElems) {
    if (pVisited.add(pNode)) {
      for (CFAEdgeNode succ : pNode.edges(pBackwards)) {
        dfs(succ, pBackwards, pVisited, componentElems);
      }
      componentElems.addFirst(pNode);
    }
  }

  private ImmutableList<CFAEdgeNode> reduceSubsumptionGraph(
      final ImmutableList<Collection<CFAEdgeNode>> pComponents) {
    ImmutableList.Builder<CFAEdgeNode> nodeBuilder = ImmutableList.builder();

    for (Collection<CFAEdgeNode> component : pComponents) {
      nodeBuilder.add(CFAEdgeNode.merge(component));
    }

    return nodeBuilder.build();
  }

  private Set<CFAEdge> getTestTargetsFromLeaves(final ImmutableList<CFAEdgeNode> pNodes) {
    // set must not be immutable
    return new HashSet<>(
        FluentIterable.from(pNodes)
            .filter(CFAEdgeNode::isLeave)
            .transform(node -> node.getRepresentedEdge())
            .toSet());
  }
}
