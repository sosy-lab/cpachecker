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
import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

/*
 * Based on paper
 * Martina Marré, Antonia Bertolino: Using Spanning Sets for Coverage Testing.
 * IEEE TSE 29(11): 974-984 (2003)
 * https://doi.org/10.1109/TSE.2003.1245299
 *
 * TODO paper for DominatorGraph??
 */
public class TestTargetReductionSpanningSet_DominatorGraph {

  public Set<CFAEdge> reduceTargets(
      final Set<CFAEdge> pTargets,
      final CFA pCfa,
      final boolean useEdgeBasedTestGoalGraph,
      final boolean useDominatorGraph) {
    Preconditions.checkNotNull(pTargets);
    Preconditions.checkNotNull(pCfa);
    return getTestTargetsFromLeaves(
        reduceSubsumptionGraph(
            computeStronglyConnectedComponents(
                constructSubsumptionGraph(
                    pTargets, pCfa.getMainFunction(), useEdgeBasedTestGoalGraph)),
            useDominatorGraph));
  }

  private ImmutableList<CFAEdgeNode> constructSubsumptionGraph(
      final Set<CFAEdge> pTargets,
      FunctionEntryNode pStartNode,
      boolean pUseEdgeBasedTestGoalGraph) {
    if (pUseEdgeBasedTestGoalGraph) {
      return constructSubsumptionGraphWithEdgeBasedGoalGraph(pTargets, pStartNode);
    } else {
      return constructSubsumptionGraphWithNodeBasedGoalGraph(pTargets, pStartNode);
    }
  }

  private ImmutableList<CFAEdgeNode> constructSubsumptionGraphWithEdgeBasedGoalGraph(
      final Set<CFAEdge> pTargets, FunctionEntryNode pStartNode) {
    ImmutableList.Builder<CFAEdgeNode> nodeBuilder = ImmutableList.builder();

    CFAEdgeNode node;
    Map<CFAEdge, CFAEdgeNode> edgeToNode = Maps.newHashMapWithExpectedSize(pTargets.size());
    Map<CFAEdge, CFAEdge> copyToTarget = Maps.newHashMapWithExpectedSize(pTargets.size());

    Pair<CFANode, CFANode> entryExit =
        TestTargetReductionUtils.buildEdgeBasedTestGoalGraph(pTargets, copyToTarget, pStartNode);

    for (CFAEdge target : pTargets) {
      node = new CFAEdgeNode(target);
      edgeToNode.put(target, node);
      nodeBuilder.add(node);
    }

    SubsumptionOracleForTargetsAsEdges subOracle =
        new SubsumptionOracleForTargetsAsEdges(entryExit, copyToTarget);

    for (CFAEdge targetPred : pTargets) {
      for (CFAEdge targetSucc : pTargets) {
        if (targetPred == targetSucc) {
          continue;
        }
        // TODO currently only approximation via dominator trees on nodes, not on edges
        if (subOracle.subsumes(targetSucc, targetPred)) {
          edgeToNode.get(targetPred).addOrUpdateEdgeTo(edgeToNode.get(targetSucc), false);
        }
      }
    }

    return nodeBuilder.build();
  }

  private ImmutableList<CFAEdgeNode> constructSubsumptionGraphWithNodeBasedGoalGraph(
      Set<CFAEdge> pTargets, FunctionEntryNode pStartNode) {
    ImmutableList.Builder<CFAEdgeNode> nodeBuilder = ImmutableList.builder();

    CFAEdgeNode node;
    Map<CFAEdge, CFAEdgeNode> targetToNode = Maps.newHashMapWithExpectedSize(pTargets.size());

    Pair<Pair<CFAEdgeNode, CFAEdgeNode>, ImmutableSet<Pair<CFAEdgeNode, CFAEdgeNode>>> result =
        TestTargetReductionUtils.buildNodeBasedTestGoalGraph(pTargets, pStartNode, targetToNode);
    Pair<CFAEdgeNode, CFAEdgeNode> entryExit = result.getFirst();
    Map<CFAEdgeNode, CFAEdgeNode> graphNodeToNodeSpanningSet =
        Maps.newHashMapWithExpectedSize(targetToNode.size());

    for (CFAEdgeNode target : targetToNode.values()) {
      node = new CFAEdgeNode(target.getRepresentedEdge());
      graphNodeToNodeSpanningSet.put(target, node);
      nodeBuilder.add(node);
    }

    DomTree<CFAEdgeNode> domTree =
        DomTree.forGraph(
            CFAEdgeNode::allPredecessorsOf, CFAEdgeNode::allSuccessorsOf, entryExit.getFirst());
    DomTree<CFAEdgeNode> inverseDomTree =
        DomTree.forGraph(
            CFAEdgeNode::allSuccessorsOf, CFAEdgeNode::allPredecessorsOf, entryExit.getSecond());

    ImmutableSet<CFAEdgeNode> reachableFromExit = getReachableFromExit(entryExit.getSecond());

    for (CFAEdgeNode targetPred : targetToNode.values()) {
      for (CFAEdgeNode targetSucc : targetToNode.values()) {
        if (targetPred == targetSucc) {
          continue;
        }

        // targetSucc subsumes targetPred
        if (domTree.isAncestorOf(targetPred, targetSucc)
            || (reachableFromExit.contains(targetPred)
                && reachableFromExit.contains(targetSucc)
                && inverseDomTree.isAncestorOf(targetPred, targetSucc)
                && !result.getSecond().contains(Pair.of(targetSucc, targetPred)))) {
          // TODO never entered for example
          graphNodeToNodeSpanningSet
              .get(targetPred)
              .addOrUpdateEdgeTo(graphNodeToNodeSpanningSet.get(targetSucc), false);
        }
      }
    }

    return nodeBuilder.build();
  }

  private ImmutableSet<CFAEdgeNode> getReachableFromExit(final CFAEdgeNode pExit) {
    Set<CFAEdgeNode> visited = new HashSet<>();
    Deque<CFAEdgeNode> waitlist = new ArrayDeque<>();
    visited.add(pExit);
    waitlist.add(pExit);

    CFAEdgeNode succ;
    while (!waitlist.isEmpty()) {
      succ = waitlist.poll();
      for (CFAEdgeNode pred : CFAEdgeNode.allPredecessorsOf(succ)) {
        if (visited.add(pred)) {
          waitlist.add(pred);
        }
      }
    }

    return ImmutableSet.copyOf(visited);
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
      final ImmutableList<Collection<CFAEdgeNode>> pComponents, final boolean pUseDominatorGraph) {
    ImmutableList.Builder<CFAEdgeNode> nodeBuilder = ImmutableList.builder();

    for (Collection<CFAEdgeNode> component : pComponents) {
      nodeBuilder.add(CFAEdgeNode.merge(component));
    }

    ImmutableList<CFAEdgeNode> nodes = nodeBuilder.build();

    if (pUseDominatorGraph) {
      // remove composite edges
      for (CFAEdgeNode node : nodes) {
        // remove duplicate successors
        node.removeDuplicateSuccessors();
        for (CFAEdgeNode succ : node.edges(false).toList()) {
          if (reachableWithoutDirectEdge(node, succ)) {
            node.removeEdgeTo(succ);
          }
        }
      }
    }

    return nodes;
  }

  private boolean reachableWithoutDirectEdge(CFAEdgeNode pPred, CFAEdgeNode pSucc) {
    Set<CFAEdgeNode> visited = new HashSet<>();
    Deque<CFAEdgeNode> waitlist = new ArrayDeque<>();
    waitlist.add(pPred);
    visited.add(pPred);

    CFAEdgeNode currentNode;
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();
      for (CFAEdgeNode succ : currentNode.edges(false)) {
        if (currentNode == pPred && succ == pSucc) {
          continue;
        }
        if (succ == pSucc) {
          return true;
        }
        if (visited.add(succ)) {
          waitlist.add(succ);
        }
      }
    }
    return false;
  }

  private Set<CFAEdge> getTestTargetsFromLeaves(final ImmutableList<CFAEdgeNode> pNodes) {
    // set must not be immutable
    return new HashSet<>(
        FluentIterable.from(pNodes)
            .filter(CFAEdgeNode::isLeave)
            .transform(CFAEdgeNode::getRepresentedEdge)
            .toSet());
  }
}
