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
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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

public final class TestTargetReductionUtils {

  private TestTargetReductionUtils() {}

  public static Pair<CFANode, CFANode> buildTestGoalGraph(
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

  public static CFAEdge copyAsDummyEdge(final CFANode pred, final CFANode succ) {
    CFAEdge newEdge = new DummyCFAEdge(pred, succ);
    pred.addLeavingEdge(newEdge);
    succ.addEnteringEdge(newEdge);
    return newEdge;
  }

  static class CFAEdgeNode {
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

    public boolean isRoot() {
      return predecessors.isEmpty();
    }

    public boolean isLeave() {
      return successors.isEmpty();
    }

    public CFAEdge getRepresentedEdge() {
      return representativeTarget;
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
