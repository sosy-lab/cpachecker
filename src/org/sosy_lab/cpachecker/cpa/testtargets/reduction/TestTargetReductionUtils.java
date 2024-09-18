// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.harness.PredefinedTypes.isPredefinedFunction;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
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
    Set<Pair<CFANode, Boolean>> visited = new HashSet<>();
    Map<CFANode, CFANode> origCFANodeToCopyMap = new HashMap<>();
    CFANode currentNode;
    Pair<CFANode, Boolean> currentNodeInfo;
    boolean requireInput;
    Set<CFANode> toExplore = Sets.newHashSetWithExpectedSize(pTestTargets.size() + 1);
    Deque<Pair<CFANode, Boolean>> waitlist = new ArrayDeque<>();

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
              origCFANodeToCopyMap.get(target.getSuccessor()),
              false),
          target);
    }

    for (CFANode predecessor : toExplore) {
      if (!successorNodes.contains(predecessor)) {
        // get next node in the queue
        waitlist.add(Pair.of(predecessor, false));
        visited.clear();
      }

      while (!waitlist.isEmpty()) {
        currentNodeInfo = waitlist.poll();
        currentNode = currentNodeInfo.getFirst();

        if (currentNode.getNumLeavingEdges() == 0) {
          boolean requiredInput = currentNodeInfo.getSecond();
          functionExitNode.ifPresent(
              exitNode -> {
                if (!origCFANodeToCopyMap
                    .get(predecessor)
                    .hasEdgeTo(origCFANodeToCopyMap.get(exitNode))) {
                  copyAsDummyEdge(
                      origCFANodeToCopyMap.get(predecessor),
                      origCFANodeToCopyMap.get(exitNode),
                      requiredInput);
                }
              });
        }
        for (CFAEdge leaving : CFAUtils.leavingEdges(currentNode)) {
          requireInput = currentNodeInfo.getSecond() || isInputEdge(leaving);
          if (successorNodes.contains(leaving.getSuccessor())) {
            if (!origCFANodeToCopyMap
                .get(predecessor)
                .hasEdgeTo(origCFANodeToCopyMap.get(leaving.getSuccessor()))) {
              copyAsDummyEdge(
                  origCFANodeToCopyMap.get(predecessor),
                  origCFANodeToCopyMap.get(leaving.getSuccessor()),
                  requireInput);
            } else if (requireInput) {
              ((DummyInputCFAEdge)
                      origCFANodeToCopyMap
                          .get(predecessor)
                          .getEdgeTo(origCFANodeToCopyMap.get(leaving.getSuccessor())))
                  .addInput();
            }
          } else {
            if (visited.add(Pair.of(leaving.getSuccessor(), requireInput))) {
              waitlist.add(Pair.of(leaving.getSuccessor(), requireInput));
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

  public static Pair<Pair<CFAEdgeNode, CFAEdgeNode>, ImmutableSet<Pair<CFAEdgeNode, CFAEdgeNode>>>
      buildNodeBasedTestGoalGraph(
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

    return Pair.of(
        Pair.of(graphStartNode, graphEndNode),
        determinePathsWithRequiredInputs(pTargetToGoalGraphNode.values()));
  }

  private static void exploreSegment(
      final CFANode pSegmentStartNode,
      final CFAEdgeNode pPredecessor,
      final CFAEdgeNode pGraphEndNode,
      final Map<CFAEdge, CFAEdgeNode> pTargetToGoalGraphNode) {
    Set<Pair<CFANode, Boolean>> visited = new HashSet<>();
    Deque<Pair<CFANode, Boolean>> waitlist = new ArrayDeque<>();
    waitlist.add(Pair.of(pSegmentStartNode, false));
    visited.add(Pair.of(pSegmentStartNode, false));

    Pair<CFANode, Boolean> currentNodeInfo;
    CFANode currentNode;
    boolean viaInput;
    boolean reachesEndNode = false;
    while (!waitlist.isEmpty()) {
      currentNodeInfo = waitlist.poll();
      currentNode = currentNodeInfo.getFirst();
      if (currentNode.getNumLeavingEdges() == 0) {
        reachesEndNode = true;
      }
      for (CFAEdge leaving : CFAUtils.leavingEdges(currentNode)) {
        viaInput = currentNodeInfo.getSecond() || isInputEdge(leaving);
        if (pTargetToGoalGraphNode.containsKey(leaving)) {
          pPredecessor.addOrUpdateEdgeTo(pTargetToGoalGraphNode.get(leaving), viaInput);
        } else {
          if (visited.add(Pair.of(leaving.getSuccessor(), viaInput))) {
            waitlist.add(Pair.of(leaving.getSuccessor(), viaInput));
          }
        }
      }
    }
    if (reachesEndNode) {
      pPredecessor.addEdgeTo(pGraphEndNode, false);
    }
  }

  private static ImmutableSet<Pair<CFAEdgeNode, CFAEdgeNode>> determinePathsWithRequiredInputs(
      Collection<CFAEdgeNode> pNodes) {
    Map<Pair<CFAEdgeNode, CFAEdgeNode>, Boolean> pathsToRequiredInputs = new HashMap<>();

    Deque<Pair<CFAEdgeNode, CFAEdgeNode>> waitlist = new ArrayDeque<>();
    Pair<CFAEdgeNode, CFAEdgeNode> path, newPath;
    boolean viaInput;

    for (CFAEdgeNode predTarget : pNodes) {
      for (CFAEdgeNode succTarget : CFAEdgeNode.allSuccessorsOf(predTarget)) {
        newPath = Pair.of(predTarget, succTarget);
        pathsToRequiredInputs.put(newPath, predTarget.mayReachViaInputs(succTarget));
        waitlist.add(newPath);
      }
    }

    while (!waitlist.isEmpty()) {
      path = waitlist.pop();
      for (CFAEdgeNode succTarget : CFAEdgeNode.allSuccessorsOf(path.getSecond())) {
        newPath = Pair.of(path.getFirst(), succTarget);
        viaInput = path.getFirst().mayReachViaInputs(succTarget);
        if (!pathsToRequiredInputs.containsKey(newPath)
            || (!pathsToRequiredInputs.get(newPath) && viaInput)) {
          pathsToRequiredInputs.put(newPath, viaInput);
          waitlist.add(newPath);
        }
      }
    }
    return ImmutableSet.copyOf(
        FluentIterable.from(pathsToRequiredInputs.keySet())
            .filter(pathPair -> pathsToRequiredInputs.get(pathPair)));
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
    return copyAsDummyEdge(pred, succ, false);
  }

  public static CFAEdge copyAsDummyEdge(
      final CFANode pred, final CFANode succ, final boolean withInput) {
    CFAEdge newEdge = new DummyInputCFAEdge(pred, succ, withInput);
    pred.addLeavingEdge(newEdge);
    succ.addEnteringEdge(newEdge);
    return newEdge;
  }

  public static Set<CFAEdgeNode> getLeavesOfDomintorTree(
      final DomTree<CFAEdgeNode> pDomTree, final boolean isPostDomTree) {
    Set<CFAEdgeNode> nonLeaves = Sets.newHashSetWithExpectedSize(pDomTree.getNodeCount());
    for (CFAEdgeNode domTreeEntry : pDomTree) {
      pDomTree
          .getParent(domTreeEntry)
          .ifPresent(
              parent -> {
                if (!isPostDomTree || !domTreeEntry.mayReachViaInputs(parent)) {
                  nonLeaves.add(parent);
                }
              });
    }

    return FluentIterable.from(pDomTree).filter(node -> !nonLeaves.contains(node)).toSet();
  }

  public static boolean isInputEdge(CFAEdge pEdge) {
    if (pEdge instanceof DummyInputCFAEdge) {
      return true;
    }
    if (pEdge instanceof AStatementEdge
        && ((AStatementEdge) pEdge).getStatement() instanceof AFunctionCall functionCall) {
      AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
      AFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

      if (!isPredefinedFunction(functionDeclaration)
          && !(functionCallExpression.getExpressionType() instanceof CVoidType)
          && (functionCallExpression.getExpressionType() != JSimpleType.getVoid())) {
        return true;
      }
    }
    // TODO also support extern C variable declaration?

    return false;
  }

  static class DummyInputCFAEdge extends DummyCFAEdge {

    private boolean providesInput;

    public DummyInputCFAEdge(
        final CFANode pPredecessor, final CFANode pSuccessor, final boolean pProvidesInput) {
      super(pPredecessor, pSuccessor);
      providesInput = pProvidesInput;
    }

    public boolean providesInput() {
      return providesInput;
    }

    public void addInput() {
      providesInput = true;
    }
  }

  static class CFAEdgeNode {
    private final CFAEdge representativeTarget;
    private final Collection<CFAEdgeNode> predecessors;
    private final Collection<CFAEdgeNode> predecessorsViaInputs;
    private final Collection<CFAEdgeNode> successors;
    private final Collection<CFAEdgeNode> successorsViaInputs;

    private CFAEdgeNode(final boolean isStart, final boolean isEnd) {
      Preconditions.checkArgument(isStart || isEnd);
      if (isStart) {
        predecessors = ImmutableList.of();
        predecessorsViaInputs = ImmutableList.of();
      } else {
        predecessors = new ArrayList<>();
        predecessorsViaInputs = new ArrayList<>();
      }

      if (isEnd) {
        successors = ImmutableList.of();
        successorsViaInputs = ImmutableList.of();
      } else {
        successors = new ArrayList<>();
        successorsViaInputs = new ArrayList<>();
      }
      representativeTarget = null;
    }

    public CFAEdgeNode(final CFAEdge pTarget) {
      Preconditions.checkNotNull(pTarget);
      representativeTarget = pTarget;
      predecessors = new ArrayList<>();
      predecessorsViaInputs = new ArrayList<>();
      successors = new ArrayList<>();
      successorsViaInputs = new ArrayList<>();
    }

    public void addEdgeTo(final CFAEdgeNode succ, final boolean pViaInput) {
      if (pViaInput) {
        successorsViaInputs.add(succ);
        succ.predecessorsViaInputs.add(this);
      } else {
        successors.add(succ);
        succ.predecessors.add(this);
      }
    }

    public void addOrUpdateEdgeTo(final CFAEdgeNode succ, final boolean pViaInput) {
      if (pViaInput) {
        if (successors.contains(succ)) {
          successors.remove(succ);
          succ.predecessors.remove(succ);
        }
        successorsViaInputs.add(succ);
        succ.predecessorsViaInputs.add(this);
      } else {
        successors.add(succ);
        succ.predecessors.add(this);
      }
    }

    public void removeEdgeTo(final CFAEdgeNode succ) {
      Preconditions.checkArgument(successors.contains(succ) || successorsViaInputs.contains(succ));
      if (successors.contains(succ)) {
        successors.remove(succ);
        succ.predecessors.remove(succ);
      }

      if (successorsViaInputs.contains(succ)) {
        successorsViaInputs.remove(succ);
        succ.predecessorsViaInputs.remove(succ);
      }
    }

    public void removeEdgeTo(final CFAEdgeNode succ, final boolean pFromNonInputEdges) {
      if (pFromNonInputEdges) {
        Preconditions.checkArgument(successors.contains(succ));
        successors.remove(succ);
        succ.predecessors.remove(succ);
      } else {
        Preconditions.checkArgument(successorsViaInputs.contains(succ));
        successorsViaInputs.remove(succ);
        succ.predecessorsViaInputs.remove(succ);
      }
    }

    public void removeDuplicateSuccessors() {
      List<CFAEdgeNode> orderedSuccessors = new ArrayList<>(successors);
      orderedSuccessors.addAll(successorsViaInputs);
      Collections.sort(orderedSuccessors, Comparator.comparingInt(CFAEdgeNode::hashCode));
      for (int i = 1; i < orderedSuccessors.size(); i++) {
        if (orderedSuccessors.get(i) == orderedSuccessors.get(i - 1)) {
          removeEdgeTo(orderedSuccessors.get(i), !successors.contains(orderedSuccessors.get(i)));
          Preconditions.checkState(
              successors.contains(orderedSuccessors.get(i))
                  || successorsViaInputs.contains(orderedSuccessors.get(i)));
        }
      }
    }

    public FluentIterable<CFAEdgeNode> edges(final boolean incoming) {
      return incoming ? allPredecessorsOf(this) : allSuccessorsOf(this);
    }

    public boolean isRoot() {
      return predecessors.isEmpty() && predecessorsViaInputs.isEmpty();
    }

    public boolean isLeave() {
      return successors.isEmpty() && successorsViaInputs.isEmpty();
    }

    public boolean mayReachViaInputs(final CFAEdgeNode pSuccessor) {
      if (successorsViaInputs.contains(pSuccessor)) {
        return true;
      }
      Preconditions.checkState(successors.contains(pSuccessor));
      return false;
    }

    public CFAEdge getRepresentedEdge() {
      return representativeTarget;
    }

    public static FluentIterable<CFAEdgeNode> allPredecessorsOf(final CFAEdgeNode node) {
      return FluentIterable.from(node.predecessors).append(node.predecessorsViaInputs);
    }

    public static FluentIterable<CFAEdgeNode> allSuccessorsOf(final CFAEdgeNode node) {
      return FluentIterable.from(node.successors).append(node.successorsViaInputs);
    }

    public static CFAEdgeNode makeStartOrEndNode(final boolean isStart) {
      return new CFAEdgeNode(isStart, !isStart);
    }

    public static CFAEdgeNode merge(final Collection<CFAEdgeNode> pComponent) {
      Preconditions.checkArgument(!pComponent.isEmpty());
      CFAEdgeNode superNode = new CFAEdgeNode(pComponent.iterator().next().representativeTarget);

      Set<CFAEdgeNode> newPred = new HashSet<>();
      Set<CFAEdgeNode> newPredVIn = new HashSet<>();
      Set<CFAEdgeNode> newSucc = new HashSet<>();
      Set<CFAEdgeNode> newSuccVIn = new HashSet<>();
      Collection<CFAEdgeNode> toRemove = new ArrayList<>();
      for (CFAEdgeNode elem : pComponent) {
        newPred.addAll(elem.predecessors);
        newPredVIn.addAll(elem.predecessorsViaInputs);
        newSucc.addAll(elem.successors);
        newSuccVIn.addAll(elem.successorsViaInputs);

        toRemove.clear();
        for (CFAEdgeNode pred : elem.predecessors) {
          pred.successors.remove(pred);
          if (newPredVIn.contains(pred)) {
            toRemove.add(pred);
          }
        }
        newPred.removeAll(toRemove);

        toRemove.clear();
        for (CFAEdgeNode succ : elem.successors) {
          succ.predecessors.remove(succ);
          if (newSuccVIn.contains(succ)) {
            toRemove.add(succ);
          }
        }
        newSucc.removeAll(toRemove);

        for (CFAEdgeNode pred : elem.predecessorsViaInputs) {
          pred.successorsViaInputs.remove(pred);
        }
        for (CFAEdgeNode succ : elem.successorsViaInputs) {
          succ.predecessorsViaInputs.remove(succ);
        }
      }

      newPred.removeAll(pComponent);
      newPredVIn.removeAll(pComponent);
      newSucc.removeAll(pComponent);
      newSuccVIn.removeAll(pComponent);

      for (CFAEdgeNode pred : newPred) {
        pred.addEdgeTo(superNode, false);
      }
      for (CFAEdgeNode pred : newPredVIn) {
        pred.addEdgeTo(superNode, true);
      }
      for (CFAEdgeNode succ : newSucc) {
        superNode.addEdgeTo(succ, false);
      }
      for (CFAEdgeNode succ : newSuccVIn) {
        superNode.addEdgeTo(succ, true);
      }

      return superNode;
    }

    @Override
    public String toString() {
      return representativeTarget
          + "\n predecessors:"
          + from(predecessors)
              .append(predecessorsViaInputs)
              .transform(edgeNode -> edgeNode.representativeTarget)
              .join(Joiner.on('\t'))
          + "\n successors:"
          + from(successors)
              .append(successorsViaInputs)
              .transform(edgeNode -> edgeNode.representativeTarget)
              .join(Joiner.on('\t'))
          + "\n";
    }
  }
}
