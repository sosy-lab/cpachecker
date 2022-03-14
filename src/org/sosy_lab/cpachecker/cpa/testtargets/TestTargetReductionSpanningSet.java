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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

public class TestTargetReductionSpanningSet {

  public Set<CFAEdge> reduceTargets(final Set<CFAEdge> pTargets, final CFA pCfa) {
    Preconditions.checkNotNull(pTargets);
    return getTestTargetsFromLeaves(
        reduceSubsumptionGraph(
            computeStronglyConnectedComponents(
                constructSubsumptionGraph(pTargets, pCfa.getMainFunction()))));
  }

  private ImmutableSet<CFAEdgeNode> constructSubsumptionGraph(
      final Set<CFAEdge> pTargets, FunctionEntryNode pStartNode) {
    ImmutableSet.Builder<CFAEdgeNode> nodeBuilder = ImmutableSet.builder();

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

    /*try {
      TestTargetReductionUtils.drawGraph(Paths.get("subSumGraph.dot"), entryExit.getFirst());
    } catch (IOException e) {
    }*/

    DomTree<CFANode>
        domTree =
            Dominance.createDomTree(
                entryExit.getFirst(), CFAUtils::allSuccessorsOf, CFAUtils::allPredecessorsOf),
        inverseDomTree =
            entryExit.getSecond() != null
                ? Dominance.createDomTree(
                    entryExit.getSecond(), CFAUtils::allPredecessorsOf, CFAUtils::allSuccessorsOf)
                : null;

    for (CFAEdge targetPred : pTargets) {
      for (CFAEdge targetSucc : pTargets) {
        if (targetPred == targetSucc) {
          continue;
        }
        // TODO currently only approximation via dominator trees on nodes, not on edges
        if (targetPred.getSuccessor().getNumEnteringEdges() == 1
            && targetSucc.getSuccessor().getNumEnteringEdges() == 1
            && (domTree.isAncestorOf( // pred is ancestor/dominator of succ
                    domTree.getId(targetToCopy.get(targetPred).getSuccessor()),
                    domTree.getId(targetToCopy.get(targetSucc).getSuccessor()))
                || (inverseDomTree != null
                    && inverseDomTree.isAncestorOf(
                        inverseDomTree.getId(targetToCopy.get(targetPred).getSuccessor()),
                        inverseDomTree.getId(targetToCopy.get(targetSucc).getSuccessor()))))) {
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

  private ImmutableSet<Collection<CFAEdgeNode>> computeStronglyConnectedComponents(
      final ImmutableSet<CFAEdgeNode> nodes) {
    ImmutableSet.Builder<Collection<CFAEdgeNode>> componentsBuilder = ImmutableSet.builder();
    Deque<CFAEdgeNode> componentElems, ordered = new ArrayDeque<>(nodes.size());

    Set<CFAEdgeNode> visited = Sets.newHashSetWithExpectedSize(nodes.size());
    for (CFAEdgeNode node : nodes) {
      dfs(node, false, visited, ordered);
    }

    visited = Sets.newHashSetWithExpectedSize(nodes.size());
    for (CFAEdgeNode node : ordered) {
      componentElems = new ArrayDeque<>();
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

  private ImmutableSet<CFAEdgeNode> reduceSubsumptionGraph(
      final ImmutableSet<Collection<CFAEdgeNode>> pComponents) {
    ImmutableSet.Builder<CFAEdgeNode> nodeBuilder = ImmutableSet.builder();

    for (Collection<CFAEdgeNode> component : pComponents) {
      nodeBuilder.add(CFAEdgeNode.merge(component));
    }

    return nodeBuilder.build();
  }

  private Set<CFAEdge> getTestTargetsFromLeaves(final ImmutableSet<CFAEdgeNode> pNodes) {
    // set must not be immutable
    return new HashSet<>(
        FluentIterable.from(pNodes)
            .filter(node -> node.isLeave())
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

      Set<CFAEdgeNode> newPred = new HashSet<>(), newSucc = new HashSet<>();
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
          + Joiner.on('\t')
              .join(from(predecessors).transform(edgeNode -> edgeNode.representativeTarget))
          + "\n successors:"
          + Joiner.on('\t')
              .join(from(successors).transform(edgeNode -> edgeNode.representativeTarget))
          + "\n";
    }
  }
}
