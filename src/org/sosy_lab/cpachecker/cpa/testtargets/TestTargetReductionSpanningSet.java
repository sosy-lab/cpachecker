// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.uni_freiburg.informatik.ultimate.util.datastructures.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

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
    Map<CFAEdge, CFAEdge> targetToCopy = Maps.newHashMapWithExpectedSize(copyToTarget.size());
    for (Entry<CFAEdge, CFAEdge> entry : copyToTarget.entrySet()) {
      targetToCopy.put(entry.getValue(), entry.getKey());
    }

    for (CFAEdge target : pTargets) {
      node = new CFAEdgeNode(target);
      edgeToNode.put(target, node);
      nodeBuilder.add(node);
    }

    /*
    try {
      TestTargetReductionUtils.drawGraph(Paths.get("subSumGraph.dot"), entryExit.getFirst());
    } catch (IOException e) {
    }*/

    DomTree<CFANode> domTree =
        DomTree.forGraph(
            CFAUtils::allPredecessorsOf, CFAUtils::allSuccessorsOf, entryExit.getFirst());
    DomTree<CFANode> inverseDomTree =
        entryExit.getSecond() != null
            ? DomTree.forGraph(
                CFAUtils::allSuccessorsOf, CFAUtils::allPredecessorsOf, entryExit.getSecond())
            : null;

    ImmutableSet<CFAEdge> reachedFromExit;
    if (entryExit.getSecond() == null) {
      reachedFromExit = ImmutableSet.empty();
    } else {
      reachedFromExit = getReachableFromExit(copyToTarget, entryExit.getSecond());
    }

    for (CFAEdge targetPred : pTargets) {
      for (CFAEdge targetSucc : pTargets) {
        if (targetPred == targetSucc) {
          continue;
        }
        // TODO currently only approximation via dominator trees on nodes, not on edges
        if (targetPred.getSuccessor().getNumEnteringEdges() == 1
            && targetSucc.getSuccessor().getNumEnteringEdges() == 1
            // pred is ancestor/dominator of succ
            && (domTree.isAncestorOf(
                    targetToCopy.get(targetPred).getSuccessor(),
                    targetToCopy.get(targetSucc).getSuccessor())
                || (inverseDomTree != null
                    && reachedFromExit.contains(targetPred)
                    && reachedFromExit.contains(targetSucc)
                    && inverseDomTree.isAncestorOf(
                        targetToCopy.get(targetPred).getSuccessor(),
                        targetToCopy.get(targetSucc).getSuccessor())))) {
          /*
           * Implementation of Arcs subsumes?. An arc e subsumes an arc e’ if every path from the
           * entry arc to e contains e’ or else if every path from e to the exit arc contains e’
           * [4], i.e., if AL(eo,e’,e) or AL(e,e’,e~).
           */
          edgeToNode.get(targetPred).addEdgeTo(edgeToNode.get(targetSucc));
        }
      }
    }

    return nodeBuilder.build();
  }

  private ImmutableSet<CFAEdge> getReachableFromExit(
      final Map<CFAEdge, CFAEdge> pCopyToTarget, final CFANode pCopiedExit) {
    Set<CFAEdge> reachableTargets = new HashSet<>(pCopyToTarget.size());
    Set<CFANode> visited = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    visited.add(pCopiedExit);
    waitlist.add(pCopiedExit);

    CFANode succ;
    while (!waitlist.isEmpty()) {
      succ = waitlist.poll();
      for (CFANode pred : CFAUtils.allPredecessorsOf(succ)) {
        if (visited.add(pred)) {
          waitlist.add(pred);
        }
      }
    }

    for (Entry<CFAEdge, CFAEdge> mapEntry : pCopyToTarget.entrySet()) {
      if (visited.contains(mapEntry.getKey().getSuccessor())) {
        reachableTargets.add(mapEntry.getValue());
      }
    }

    return ImmutableSet.of(reachableTargets);
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
            .transform(node -> node.representativeTarget)
            .toSet());
  }

  private static class CFAEdgeNode {
    private final CFAEdge representativeTarget;
    private final Collection<CFAEdgeNode> predecessors;
    private final Collection<CFAEdgeNode> successors;

    public CFAEdgeNode(final CFAEdge pTarget) {
      representativeTarget = pTarget;
      predecessors = new ArrayList<>();
      successors = new ArrayList<>();
    }

    public void addEdgeTo(final CFAEdgeNode succ) {
      successors.add(succ);
      succ.predecessors.add(this);
    }

    public FluentIterable<CFAEdgeNode> edges(final boolean incoming) {
      return incoming ? FluentIterable.from(predecessors) : FluentIterable.from(successors);
    }

    public boolean isLeave() {
      return successors.isEmpty();
    }

    public static CFAEdgeNode merge(final Collection<CFAEdgeNode> pComponent) {
      Preconditions.checkArgument(!pComponent.isEmpty());
      CFAEdgeNode superNode = new CFAEdgeNode(pComponent.iterator().next().representativeTarget);

      Set<CFAEdgeNode> newPred = new HashSet<>();
      Set<CFAEdgeNode> newSucc = new HashSet<>();
      for (CFAEdgeNode elem : pComponent) {
        newPred.addAll(elem.predecessors);
        newSucc.addAll(elem.successors);
        for (CFAEdgeNode pred : elem.predecessors) {
          pred.successors.remove(pred);
        }
        for (CFAEdgeNode succ : elem.successors) {
          succ.predecessors.remove(succ);
        }
      }

      newPred.removeAll(pComponent);
      newSucc.removeAll(pComponent);
      for (CFAEdgeNode pred : newPred) {
        pred.addEdgeTo(superNode);
      }
      for (CFAEdgeNode succ : newSucc) {
        superNode.addEdgeTo(succ);
      }

      return superNode;
    }

    @Override
    public String toString() {
      return representativeTarget
          + "\n predecessors:"
          + from(predecessors)
              .transform(edgeNode -> edgeNode.representativeTarget)
              .join(Joiner.on('\t'))
          + "\n successors:"
          + from(successors)
              .transform(edgeNode -> edgeNode.representativeTarget)
              .join(Joiner.on('\t'))
          + "\n";
    }
  }
}
