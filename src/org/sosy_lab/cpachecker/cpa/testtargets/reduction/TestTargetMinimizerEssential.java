// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.testtargets.reduction.TestTargetReductionUtils.DummyInputCFAEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

/*
 * Based on paper
 * Takeshi Chusho: Test Data Selection and Quality Estimation Based on the Concept
 * of Essential Branches for Path Testing. IEEE TSE 13(5): 509-517 (1987)
 * https://doi.org/10.1109/TSE.1987.233196
 */
public class TestTargetMinimizerEssential {

  // only works correctly if DummyInputCFAEdge, DummyCFAEdge uses Object equals
  public Set<CFAEdge> reduceTargets(
      final Set<CFAEdge> pTestTargets, final CFA pCfa, final boolean fullCFACopy) {
    // maps a copied edge to the testTarget that can be removed if it's dominated by another
    // testTarget
    Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap = new HashMap<>();
    Set<CFAEdge> testTargets = new HashSet<>(pTestTargets);

    // create a copy of the cfa graph that can be minimized using the essential Branch rules
    Pair<CFANode, CFANode> copiedFunctionEntryExit;
    if (fullCFACopy) {
      copiedFunctionEntryExit =
          copyCFA(testTargets, copiedEdgeToTestTargetsMap, pCfa.getMainFunction());
    } else {
      copiedFunctionEntryExit =
          TestTargetReductionUtils.buildEdgeBasedTestGoalGraph(
              testTargets, copiedEdgeToTestTargetsMap, pCfa.getMainFunction());
    }

    // handle all cases of nodes with a single outgoing edge
    applyRule1(testTargets, copiedEdgeToTestTargetsMap, copiedFunctionEntryExit);

    applyRule2(testTargets, copiedEdgeToTestTargetsMap, copiedFunctionEntryExit);

    applyRule3(testTargets, copiedEdgeToTestTargetsMap, copiedFunctionEntryExit);

    applyRule4(testTargets, copiedEdgeToTestTargetsMap, copiedFunctionEntryExit.getFirst());

    return testTargets;
  }

  private Pair<CFANode, CFANode> copyCFA(
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final FunctionEntryNode pEntryNode) {
    // a set of nodes that has already been created to prevent duplicates
    Set<CFANode> origNodesCopied = new HashSet<>();
    Map<CFANode, CFANode> origCFANodeToCopyMap = new HashMap<>();
    CFANode currentNode;

    Queue<CFANode> waitlist = new ArrayDeque<>();
    // start with function entry point
    origCFANodeToCopyMap.put(pEntryNode, CFANode.newDummyCFANode());
    waitlist.add(pEntryNode);
    origNodesCopied.add(pEntryNode);
    while (!waitlist.isEmpty()) {
      // get next node in the queue
      currentNode = waitlist.poll();

      if (currentNode.getNumLeavingEdges() == 0
          && pEntryNode.getExitNode().orElse(null) != currentNode) {
        CFANode currentNodeFinal = currentNode;
        pEntryNode
            .getExitNode()
            .ifPresent(
                exitNode -> {
                  if (!origNodesCopied.contains(exitNode)) {
                    origCFANodeToCopyMap.put(exitNode, CFANode.newDummyCFANode(""));
                    waitlist.add(exitNode);
                    origNodesCopied.add(exitNode);
                  }
                  CFAEdge copyEdge =
                      new DummyInputCFAEdge(
                          origCFANodeToCopyMap.get(currentNodeFinal),
                          origCFANodeToCopyMap.get(exitNode),
                          false);
                  origCFANodeToCopyMap.get(currentNodeFinal).addLeavingEdge(copyEdge);
                  origCFANodeToCopyMap.get(exitNode).addEnteringEdge(copyEdge);
                });
      }

      // create copies of all outgoing edges and the nodes they go into if they don't yet exist
      for (CFAEdge currentEdge : CFAUtils.leavingEdges(currentNode)) {
        CFANode copiedSuccessorNode;
        if (origNodesCopied.contains(currentEdge.getSuccessor())) {
          // node the edge goes to has been added already so we retrieve the mapped dummy node to
          // create the new edge
          copiedSuccessorNode = origCFANodeToCopyMap.get(currentEdge.getSuccessor());
        } else {
          // node the edge goes to hasn't been added yet so we add the a new copied node
          copiedSuccessorNode = CFANode.newDummyCFANode("");
          origCFANodeToCopyMap.put(currentEdge.getSuccessor(), copiedSuccessorNode);
          waitlist.add(currentEdge.getSuccessor());
          origNodesCopied.add(currentEdge.getSuccessor());
        }
        // create the new Edge and add it to its predecessor and successor nodes aswell as mapping
        // the original to it

        CFAEdge copyEdge =
            new DummyInputCFAEdge(
                origCFANodeToCopyMap.get(currentNode),
                copiedSuccessorNode,
                TestTargetReductionUtils.isInputEdge(currentEdge));
        origCFANodeToCopyMap.get(currentNode).addLeavingEdge(copyEdge);
        copiedSuccessorNode.addEnteringEdge(copyEdge);

        // if the original edge is part of the test targets we have to map the new dummy edge to
        // the test target that may be removed if its being dominated
        if (pTestTargets.contains(currentEdge)) {
          copiedEdgeToTestTargetsMap.put(copyEdge, currentEdge);
        }
      }
    }
    // complete dummy graph has been created

    @Nullable CFANode exitNodeCopy =
        pEntryNode.getExitNode().map(exitNode -> origCFANodeToCopyMap.get(exitNode)).orElse(null);
    return Pair.of(origCFANodeToCopyMap.get(pEntryNode), exitNodeCopy);
  }

  private boolean isSelfLoop(final CFAEdge pEdge) {
    return pEdge.getPredecessor() == pEdge.getSuccessor();
  }

  private boolean entersNode(final CFAEdge pEdge, final CFANode pNode) {
    return pEdge.getSuccessor() == pNode;
  }

  private boolean entersProgramStart(final CFAEdge pEdge, final CFANode pFunctionEntryNode) {
    return entersNode(pEdge, pFunctionEntryNode);
  }

  private boolean entersProgramEnd(CFAEdge pEdge, CFANode pFunctionExitNode) {
    return entersNode(pEdge, pFunctionExitNode);
  }

  private void redirectEdgeToNewPredecessor(
      final CFAEdge edgeToRedirect,
      final CFANode newPredecessor,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final boolean pProvidesInputToRedirect) {

    boolean providesInput =
        pProvidesInputToRedirect
            || (edgeToRedirect instanceof DummyInputCFAEdge
                && ((DummyInputCFAEdge) edgeToRedirect).providesInput());
    /*
     * redirect edges (construct a new edge and replace because predecessors and successors are
     * immutable
     */
    CFAEdge redirectedEdge;
    if (isSelfLoop(edgeToRedirect)) {
      redirectedEdge = new DummyInputCFAEdge(newPredecessor, newPredecessor, providesInput);
    } else {
      redirectedEdge =
          new DummyInputCFAEdge(newPredecessor, edgeToRedirect.getSuccessor(), providesInput);
    }
    edgeToRedirect.getSuccessor().removeEnteringEdge(edgeToRedirect);
    redirectedEdge.getSuccessor().addEnteringEdge(redirectedEdge);
    redirectedEdge.getPredecessor().addLeavingEdge(redirectedEdge);
    edgeToRedirect.getPredecessor().removeLeavingEdge(edgeToRedirect);

    copyTestTargetProperty(edgeToRedirect, redirectedEdge, copiedEdgeToTestTargetsMap);
  }

  private void redirectEdgeToNewSuccessor(
      CFAEdge edgeToRedirect,
      CFANode newSuccessor,
      Map<CFAEdge, CFAEdge> pCopiedEdgeToTestTargetsMap) {
    // create a new edge from current Edges start to the successor of its end
    CFAEdge redirectedEdge;
    if (isSelfLoop(edgeToRedirect)) {
      redirectedEdge =
          new DummyInputCFAEdge(
              newSuccessor,
              newSuccessor,
              edgeToRedirect instanceof DummyInputCFAEdge
                  && ((DummyInputCFAEdge) edgeToRedirect).providesInput());
    } else {
      redirectedEdge =
          new DummyInputCFAEdge(
              edgeToRedirect.getPredecessor(),
              newSuccessor,
              edgeToRedirect instanceof DummyInputCFAEdge
                  && ((DummyInputCFAEdge) edgeToRedirect).providesInput());
    }
    edgeToRedirect.getPredecessor().removeLeavingEdge(edgeToRedirect);
    redirectedEdge.getPredecessor().addLeavingEdge(redirectedEdge);
    redirectedEdge.getSuccessor().addEnteringEdge(redirectedEdge);
    edgeToRedirect.getSuccessor().removeEnteringEdge(edgeToRedirect);
    // remove the edge from its successor and add a new edge from current Node to said
    // successor
    copyTestTargetProperty(edgeToRedirect, redirectedEdge, pCopiedEdgeToTestTargetsMap);
  }

  private void copyTestTargetProperty(
      final CFAEdge copyFrom,
      final CFAEdge copyTo,
      final Map<CFAEdge, CFAEdge> pCopiedEdgeToTestTargetsMap) {
    if (pCopiedEdgeToTestTargetsMap.containsKey(copyFrom)) {
      // copy potential testTarget associations to new edge
      pCopiedEdgeToTestTargetsMap.put(copyTo, pCopiedEdgeToTestTargetsMap.get(copyFrom));
      pCopiedEdgeToTestTargetsMap.remove(copyFrom);
    }
  }

  private void updateTestGoalMappingAfterRemoval(
      final CFAEdge removedEdge,
      final FluentIterable<CFAEdge> dominatorCandidates,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final Set<CFAEdge> pTestTargets) {
    if (copiedEdgeToTestTargetsMap.containsKey(removedEdge)) {
      CFAEdge testTargetToBeDominated = copiedEdgeToTestTargetsMap.get(removedEdge);
      // first check if any test targets dominate the removed edges testtarget
      for (CFAEdge domEdgeCandidate : dominatorCandidates) {
        // if an edge is mapped to a different test target
        if (copiedEdgeToTestTargetsMap.containsKey(domEdgeCandidate)
            && !copiedEdgeToTestTargetsMap.get(domEdgeCandidate).equals(testTargetToBeDominated)) {
          // removed edge is getting dominated by this edge so remove the removed edges
          // testtarget from our list of testtargets
          pTestTargets.remove(testTargetToBeDominated);
          // need to remove all occurrences of the same value that has been dominated to clean
          // up
          // the mapping and prevent
          // accidental removal of test targets based on an already removed dominator due to the
          // order of nodes
          copiedEdgeToTestTargetsMap
              .values()
              .removeAll(Collections.singleton(testTargetToBeDominated));

          break;
        }
        copiedEdgeToTestTargetsMap.put(domEdgeCandidate, testTargetToBeDominated);
      }
      copiedEdgeToTestTargetsMap.remove(removedEdge);
    }
  }

  private void removeEdge(
      final CFAEdge toRemove,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final Set<CFAEdge> pTestTargets,
      final boolean isLeaving) {
    CFANode pred = toRemove.getPredecessor();
    CFANode succ = toRemove.getSuccessor();
    boolean providesInput =
        toRemove instanceof DummyInputCFAEdge && ((DummyInputCFAEdge) toRemove).providesInput();

    // remove the edge from its predecessor and successor
    pred.removeLeavingEdge(toRemove);
    succ.removeEnteringEdge(toRemove);

    updateTestGoalMappingAfterRemoval(
        toRemove,
        isLeaving ? CFAUtils.enteringEdges(pred) : CFAUtils.leavingEdges(succ),
        copiedEdgeToTestTargetsMap,
        pTestTargets);

    // add the exiting edges from the successor to the predecessor to keep the graph intact
    while (succ.getNumLeavingEdges() > 0) {
      redirectEdgeToNewPredecessor(
          succ.getLeavingEdge(0), pred, copiedEdgeToTestTargetsMap, providesInput);
    }

    // copy the incoming edges to the previous successor to the current Node
    while (succ.getNumEnteringEdges() > 0) {
      /*if (enteringRemovedNode == toRemove) {
        continue;
      }*/
      redirectEdgeToNewSuccessor(succ.getEnteringEdge(0), pred, copiedEdgeToTestTargetsMap);
    }
  }

  /** Remove edges from copied graph according to first rule. */
  private void applyRule1(
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final Pair<CFANode, CFANode> pCopiedFunctionEntryExit) {

    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    CFAEdge removedEdge;
    CFANode currentNode;
    CFAEdge leaving;

    waitlist.add(pCopiedFunctionEntryExit.getFirst());
    visitedNodes.add(pCopiedFunctionEntryExit.getFirst());
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();

      // Read the node to the queue to ensure longer paths arent overlooked
      // shrink graph if node has only one outgoing edge by removing the successor from the graph
      // unless successor is the root node.
      while (currentNode.getNumLeavingEdges() == 1) {
        leaving = currentNode.getLeavingEdge(0);
        if (entersProgramStart(leaving, pCopiedFunctionEntryExit.getFirst())
            || entersProgramEnd(leaving, pCopiedFunctionEntryExit.getSecond())
            || isSelfLoop(leaving)
            || (copiedEdgeToTestTargetsMap.containsKey(leaving)
                && TestTargetReductionUtils.isInputEdge(leaving))) {
          break;
        }

        removedEdge = currentNode.getLeavingEdge(0);
        removeEdge(removedEdge, copiedEdgeToTestTargetsMap, pTestTargets, true);

        // due to branching the node we remove might already be in the queue of nodes to look at.
        // In that case we need to remove it from the queue
        waitlist.remove(removedEdge.getSuccessor());
      }

      // register unexplored successors
      for (CFAEdge leaveEdge : CFAUtils.leavingEdges(currentNode)) {
        if (visitedNodes.add(leaveEdge.getSuccessor())) {
          waitlist.add(leaveEdge.getSuccessor());
        }
      }
    }
  }

  private void applyRule2(
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final Pair<CFANode, CFANode> pCopiedFunctionEntryExit) {
    Set<CFANode> vistedNodes = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    CFANode currentNode;

    // remove edges from dummy graph according to second rule
    waitlist.add(pCopiedFunctionEntryExit.getFirst());
    vistedNodes.add(pCopiedFunctionEntryExit.getFirst());
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();

      // shrink graph if node has only one outgoing edge by removing the successor from the graph.
      // skipping it
      if (currentNode.getNumEnteringEdges() == 1
          && !entersProgramStart(
              currentNode.getEnteringEdge(0), pCopiedFunctionEntryExit.getFirst())
          && !entersProgramEnd(currentNode.getEnteringEdge(0), pCopiedFunctionEntryExit.getSecond())
          && !isSelfLoop(currentNode.getEnteringEdge(0))) {

        // remove the current nodes entering edge from its predecessor and from the current node
        removeEdge(currentNode.getEnteringEdge(0), copiedEdgeToTestTargetsMap, pTestTargets, false);

        waitlist.remove(currentNode);
      }
      // add nodes to the queue that haven't been added yet
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentNode)) {
        if (vistedNodes.add(leavingEdge.getSuccessor())) {
          waitlist.add(leavingEdge.getSuccessor());
        }
      }
    }
  }

  private ImmutableSet<Pair<CFANode, CFANode>> determinePathsWithRequiredInputs(
      final CFANode pEntryNode) {
    Map<Pair<CFANode, CFANode>, Boolean> pathsToRequiredInputs = new HashMap<>();

    Deque<Pair<CFANode, CFANode>> waitlist = new ArrayDeque<>();
    Pair<CFANode, CFANode> path;
    Pair<CFANode, CFANode> newPath;
    boolean viaInput;

    for (CFAEdge leaving : CFAUtils.allLeavingEdges(pEntryNode)) {
      newPath = Pair.of(pEntryNode, leaving.getSuccessor());
      pathsToRequiredInputs.put(newPath, TestTargetReductionUtils.isInputEdge(leaving));
      waitlist.add(newPath);
    }

    while (!waitlist.isEmpty()) {
      path = waitlist.pop();
      for (CFAEdge leaving : CFAUtils.allLeavingEdges(path.getSecond())) {
        newPath = Pair.of(leaving.getPredecessor(), leaving.getSuccessor());
        viaInput = TestTargetReductionUtils.isInputEdge(leaving);
        if (!pathsToRequiredInputs.containsKey(newPath)
            || (!pathsToRequiredInputs.get(newPath) && viaInput)) {
          pathsToRequiredInputs.put(newPath, viaInput);
          waitlist.add(newPath);
        }

        newPath = Pair.of(path.getFirst(), leaving.getSuccessor());

        viaInput = pathsToRequiredInputs.get(path) || TestTargetReductionUtils.isInputEdge(leaving);
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

  private void applyRule3(
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final Pair<CFANode, CFANode> pCopiedFunctionEntryExit) {
    // exit not reachable, e.g., due to while(1) without break, return statement
    if (pCopiedFunctionEntryExit.getSecond() == null) {
      return;
    }

    // remove edges from dummy graph according to third rule
    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();

    boolean ruleApplicable;
    CFANode currentNode;
    CFAEdge removedEdge;

    DomTree<CFANode> inverseDomTree =
        DomTree.forGraph(
            CFAUtils::allSuccessorsOf,
            CFAUtils::allPredecessorsOf,
            pCopiedFunctionEntryExit.getSecond());

    ImmutableSet<Pair<CFANode, CFANode>> pathsWithInput =
        determinePathsWithRequiredInputs(pCopiedFunctionEntryExit.getFirst());

    waitlist.add(pCopiedFunctionEntryExit.getFirst());
    visitedNodes.add(pCopiedFunctionEntryExit.getFirst());
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();
      ruleApplicable = currentNode.getNumEnteringEdges() > 0;
      removedEdge = null;
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentNode)) {
        if (!inverseDomTree.isAncestorOf(leavingEdge.getSuccessor(), currentNode)
            || pathsWithInput.contains(Pair.of(currentNode, leavingEdge.getSuccessor()))) {
          if (removedEdge == null) {
            removedEdge = leavingEdge;
            if (entersProgramStart(removedEdge, pCopiedFunctionEntryExit.getFirst())
                || entersProgramEnd(removedEdge, pCopiedFunctionEntryExit.getSecond())
                || isSelfLoop(removedEdge)
                || (removedEdge instanceof DummyInputCFAEdge
                    && ((DummyInputCFAEdge) removedEdge).providesInput())) {
              // make sure we don't merge anything into the root node
              ruleApplicable = false;
              break;
            }
          } else {
            ruleApplicable = false;
            break;
          }
        }
      }

      if (ruleApplicable && removedEdge != null && !isSelfLoop(removedEdge)) {
        removeEdge(removedEdge, copiedEdgeToTestTargetsMap, pTestTargets, true);
        waitlist.remove(removedEdge.getSuccessor());
        // read current node to the queue since we merge something into it and the new merged node
        // may satisfy the condition again
        waitlist.add(currentNode);
      }

      if (!waitlist.contains(currentNode)) {
        for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentNode)) {
          if (visitedNodes.add(leavingEdge.getSuccessor())) {
            waitlist.add(leavingEdge.getSuccessor());
          }
        }
      }
    }
  }

  /** Remove edges from dummy graph according to fourth rule. */
  private void applyRule4(
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final CFANode copiedFunctionEntry) {

    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    CFANode currentNode;
    boolean ruleApplicable;
    CFAEdge removedEdge;
    // create domination relationship on the reduced graph
    DomTree<CFANode> domTree =
        DomTree.forGraph(
            CFAUtils::allPredecessorsOf, CFAUtils::allSuccessorsOf, copiedFunctionEntry);
    // start at entry node because why not?
    waitlist.add(copiedFunctionEntry);
    visitedNodes.add(copiedFunctionEntry);
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();

      ruleApplicable = currentNode.getNumLeavingEdges() > 0;
      removedEdge = null;
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(currentNode)) {
        if (!domTree.isAncestorOf(currentNode, enteringEdge.getPredecessor())) {
          if (removedEdge == null) {
            removedEdge = enteringEdge;
            if (entersProgramStart(removedEdge, copiedFunctionEntry) || isSelfLoop(removedEdge)) {
              // make sure we don't merge anything into the root node
              ruleApplicable = false;
              break;
            }
          } else {
            ruleApplicable = false;
            break;
          }
        }
      }

      if (ruleApplicable && removedEdge != null && !isSelfLoop(removedEdge)) {
        removeEdge(removedEdge, copiedEdgeToTestTargetsMap, pTestTargets, false);
        waitlist.remove(removedEdge.getSuccessor());
      }

      // add successors of the current node to the queue
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentNode)) {
        if (visitedNodes.add(leavingEdge.getSuccessor())) {
          waitlist.add(leavingEdge.getSuccessor());
        }
      }
    }
  }
}
