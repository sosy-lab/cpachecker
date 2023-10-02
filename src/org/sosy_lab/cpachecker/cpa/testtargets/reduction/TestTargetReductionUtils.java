// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

public final class TestTargetReductionUtils {

  private TestTargetReductionUtils() {}

  public static Pair<CFANode, CFANode> buildEdgeBasedTestGoalGraph(
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> pCopiedEdgeToTestTargetsMap,
      final FunctionEntryNode pEntryNode) {
    // a set of nodes that has already been created to prevent duplicates
    Set<CFANode> successorNodes = Sets.newHashSetWithExpectedSize(pTestTargets.size() + 2);
    Set<CFANode> visited = new HashSet<>();
    Map<CFANode, CFANode> origCFANodeToCopyMap = new HashMap<>();
    CFANode currentNode;
    Set<CFANode> toExplore = Sets.newHashSetWithExpectedSize(pTestTargets.size() + 1);
    Deque<CFANode> waitlist = new ArrayDeque<>();

    origCFANodeToCopyMap.put(pEntryNode, CFANode.newDummyCFANode());
    toExplore.add(pEntryNode);
    Optional<FunctionExitNode> functionExitNode = pEntryNode.getExitNode();
    functionExitNode.ifPresent(
        exitNode -> {
          origCFANodeToCopyMap.put(exitNode, CFANode.newDummyCFANode(""));
          successorNodes.add(exitNode);
        });
    for (CFAEdge target : pTestTargets) {
      successorNodes.add(target.getPredecessor());
      toExplore.add(target.getSuccessor());

      if (!origCFANodeToCopyMap.containsKey(target.getPredecessor())) {
        origCFANodeToCopyMap.put(target.getPredecessor(), CFANode.newDummyCFANode(""));
      }
      if (!origCFANodeToCopyMap.containsKey(target.getSuccessor())) {
        origCFANodeToCopyMap.put(target.getSuccessor(), CFANode.newDummyCFANode(""));
      }

      pCopiedEdgeToTestTargetsMap.put(
          copyAsDummyEdge(
              origCFANodeToCopyMap.get(target.getPredecessor()),
              origCFANodeToCopyMap.get(target.getSuccessor())),
          target);
    }

    for (CFANode predecessor : toExplore) {
      if (!successorNodes.contains(predecessor)) {
        // get next node in the queue
        waitlist.add(predecessor);
        visited.clear();
      }

      while (!waitlist.isEmpty()) {
        currentNode = waitlist.poll();
        if (currentNode.getNumLeavingEdges() == 0) {
          functionExitNode.ifPresent(
              exitNode -> {
                if (!origCFANodeToCopyMap
                    .get(predecessor)
                    .hasEdgeTo(origCFANodeToCopyMap.get(exitNode))) {
                  copyAsDummyEdge(
                      origCFANodeToCopyMap.get(predecessor), origCFANodeToCopyMap.get(exitNode));
                }
              });
        }
        for (CFAEdge leaving : CFAUtils.leavingEdges(currentNode)) {
          if (successorNodes.contains(leaving.getSuccessor())) {
            if (!origCFANodeToCopyMap
                .get(predecessor)
                .hasEdgeTo(origCFANodeToCopyMap.get(leaving.getSuccessor()))) {
              copyAsDummyEdge(
                  origCFANodeToCopyMap.get(predecessor),
                  origCFANodeToCopyMap.get(leaving.getSuccessor()));
            }
          } else {
            if (visited.add(leaving.getSuccessor())) {
              waitlist.add(leaving.getSuccessor());
            }
          }
        }
      }
    }

    @Nullable CFANode exitNodeCopy =
        functionExitNode
            .filter(
                exitNode ->
                    removeUnreachableTestGoalsAndIsReachExit(
                        pTestTargets,
                        pCopiedEdgeToTestTargetsMap,
                        origCFANodeToCopyMap.get(pEntryNode),
                        origCFANodeToCopyMap.get(exitNode)))
            .map(exitNode -> origCFANodeToCopyMap.get(exitNode))
            .orElse(null);
    return Pair.of(origCFANodeToCopyMap.get(pEntryNode), exitNodeCopy);
  }

  private static boolean removeUnreachableTestGoalsAndIsReachExit(
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> pCopiedEdgeToTestTargetsMap,
      final CFANode pEntry,
      final CFANode pExit) {
    Set<CFANode> visited = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    visited.add(pEntry);
    waitlist.add(pEntry);

    CFANode pred;
    while (!waitlist.isEmpty()) {
      pred = waitlist.poll();
      for (CFANode succ : CFAUtils.allSuccessorsOf(pred)) {
        if (visited.add(succ)) {
          waitlist.add(succ);
        }
      }
    }

    Collection<CFAEdge> toDelete = new ArrayList<>();
    for (Entry<CFAEdge, CFAEdge> mapEntry : pCopiedEdgeToTestTargetsMap.entrySet()) {
      if (!visited.contains(mapEntry.getKey().getPredecessor())) {
        pTestTargets.remove(mapEntry.getValue());
        toDelete.add(mapEntry.getKey());
      }
    }
    for (CFAEdge unreachTarget : toDelete) {
      pCopiedEdgeToTestTargetsMap.remove(unreachTarget);
    }

    toDelete.clear();
    for (CFANode succ : visited) {
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(succ)) {
        if (!visited.contains(enteringEdge.getPredecessor())) {
          toDelete.add(enteringEdge);
        }
      }
    }

    for (CFAEdge removeEdge : toDelete) {
      removeEdge.getPredecessor().removeLeavingEdge(removeEdge);
      removeEdge.getSuccessor().removeEnteringEdge(removeEdge);
    }

    return visited.contains(pExit);
  }

  public static Pair<CFAEdgeNode, CFAEdgeNode> buildNodeBasedTestGoalGraph(
      final Set<CFAEdge> pTestTargets,
      final FunctionEntryNode pEntryNode,
      final Map<CFAEdge, CFAEdgeNode> pTargetToGoalGraphNode) {
    Set<CFAEdge> reachableTargets = getReachableTestGoals(pEntryNode, pTestTargets);
    CFAEdgeNode graphStartNode = CFAEdgeNode.makeStartOrEndNode(true);
    CFAEdgeNode graphEndNode = CFAEdgeNode.makeStartOrEndNode(false);

    for (CFAEdge target : reachableTargets) {
      pTargetToGoalGraphNode.put(target, new CFAEdgeNode(target));
    }

    exploreSegment(pEntryNode, graphStartNode, graphEndNode, pTargetToGoalGraphNode);

    for (CFAEdge target : reachableTargets) {
      exploreSegment(target.getSuccessor(), graphStartNode, graphEndNode, pTargetToGoalGraphNode);
    }

    return Pair.of(graphStartNode, graphEndNode);
  }

  private static void exploreSegment(
      final CFANode pSegmentStartNode,
      final CFAEdgeNode pPredecessor,
      final CFAEdgeNode pGraphEndNode,
      final Map<CFAEdge, CFAEdgeNode> pTargetToGoalGraphNode) {
    Set<CFANode> visited = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    waitlist.add(pSegmentStartNode);
    visited.add(pSegmentStartNode);

    CFANode currentNode;
    boolean reachesEndNode = false;
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();
      if (currentNode.getNumLeavingEdges() == 0) {
        reachesEndNode = true;
      }
      for (CFAEdge leaving : CFAUtils.leavingEdges(currentNode)) {
        if (pTargetToGoalGraphNode.containsKey(leaving)) {
          pPredecessor.addEdgeTo(pTargetToGoalGraphNode.get(leaving));
        } else {
          if (visited.add(leaving.getSuccessor())) {
            waitlist.add(leaving.getSuccessor());
          }
        }
      }
    }
    if (reachesEndNode) {
      pPredecessor.addEdgeTo(pGraphEndNode);
    }
  }

  public static Set<CFAEdge> getReachableTestGoals(
      final FunctionEntryNode pEntryNode, final Set<CFAEdge> pTargets) {
    Set<CFAEdge> seenTargets = new HashSet<>();
    Set<CFANode> visited = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    waitlist.add(pEntryNode);
    visited.add(pEntryNode);

    CFANode currentNode;
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();
      for (CFAEdge leaving : CFAUtils.leavingEdges(currentNode)) {
        if (pTargets.contains(leaving)) {
          seenTargets.add(leaving);
        }
        if (visited.add(leaving.getSuccessor())) {
          waitlist.add(leaving.getSuccessor());
        }
      }
    }
    return seenTargets;
  }

  public static CFAEdge copyAsDummyEdge(final CFANode pred, final CFANode succ) {
    CFAEdge newEdge = new DummyCFAEdge(pred, succ);
    pred.addLeavingEdge(newEdge);
    succ.addEnteringEdge(newEdge);
    return newEdge;
  }

  public static <E> Set<E> getLeavesOfDomintorTree(final DomTree<E> pDomTree) {
    Set<E> nonLeaves = Sets.newHashSetWithExpectedSize(pDomTree.getNodeCount());
    for (E domTreeEntry : pDomTree) {
      pDomTree.getParent(domTreeEntry).ifPresent(parent -> nonLeaves.add(parent));
    }

    return FluentIterable.from(pDomTree).filter(node -> !nonLeaves.contains(node)).toSet();
  }

  static class CFAEdgeNode {
    private final CFAEdge representativeTarget;
    private final Collection<CFAEdgeNode> predecessors;
    private final Collection<CFAEdgeNode> successors;

    private CFAEdgeNode(final boolean isStart, final boolean isEnd) {
      Preconditions.checkArgument(isStart || isEnd);
      if (isStart) {
        predecessors = ImmutableList.of();
      } else {
        predecessors = new ArrayList<>();
      }

      if (isEnd) {
        successors = ImmutableList.of();
      } else {
        successors = new ArrayList<>();
      }
      representativeTarget = null;
    }

    public CFAEdgeNode(final CFAEdge pTarget) {
      Preconditions.checkNotNull(pTarget);
      representativeTarget = pTarget;
      predecessors = new ArrayList<>();
      successors = new ArrayList<>();
    }

    public void addEdgeTo(final CFAEdgeNode succ) {
      successors.add(succ);
      succ.predecessors.add(this);
    }

    public void removeEdgeTo(final CFAEdgeNode succ) {
      Preconditions.checkArgument(successors.contains(succ));
      successors.remove(succ);
      succ.predecessors.remove(succ);
    }

    public void removeDuplicateSuccessors() {
      List<CFAEdgeNode> orderedSuccessors = new ArrayList<>(successors);
      Collections.sort(orderedSuccessors, Comparator.comparingInt(CFAEdgeNode::hashCode));
      for (int i = 1; i < orderedSuccessors.size(); i++) {
        if (orderedSuccessors.get(i) == orderedSuccessors.get(i - 1)) {
          removeEdgeTo(orderedSuccessors.get(i));
          Preconditions.checkState(successors.contains(orderedSuccessors.get(i)));
        }
      }
    }

    public FluentIterable<CFAEdgeNode> edges(final boolean incoming) {
      return incoming ? FluentIterable.from(predecessors) : FluentIterable.from(successors);
    }

    public boolean isRoot() {
      return predecessors.isEmpty();
    }

    public boolean isLeave() {
      return successors.isEmpty();
    }

    public CFAEdge getRepresentedEdge() {
      return representativeTarget;
    }

    public static FluentIterable<CFAEdgeNode> allPredecessorsOf(final CFAEdgeNode node) {
      return FluentIterable.from(node.predecessors);
    }

    public static FluentIterable<CFAEdgeNode> allSuccessorsOf(final CFAEdgeNode node) {
      return FluentIterable.from(node.successors);
    }

    public static CFAEdgeNode makeStartOrEndNode(final boolean isStart) {
      return new CFAEdgeNode(isStart, !isStart);
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
