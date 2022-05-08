// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.collect.FluentIterable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

public class TestTargetMinimizerEssential {

  // only works correctly if DummyCFAEdge uses Object equals
  public Set<CFAEdge> reduceTargets(
      final Set<CFAEdge> testTargets, final CFA pCfa, final boolean fullCFACopy) {
    // maps a copied edge to the testTarget that can be removed if its dominated by another
    // testTarget
    Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap = new HashMap<>();

    // create a copy of the cfa graph that can be minimized using the essential Branch rules
    Pair<CFANode, CFANode> copiedFunctionEntryExit;
    if (fullCFACopy) {
      copiedFunctionEntryExit =
          copyCFA(testTargets, copiedEdgeToTestTargetsMap, pCfa.getMainFunction());
    } else {
      copiedFunctionEntryExit =
          TestTargetReductionUtils.buildTestGoalGraph(
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

      // create copies of all outgoing edges and the nodes they go into if they dont yet exist
      for (CFAEdge currentEdge : CFAUtils.leavingEdges(currentNode)) {
        CFANode copiedSuccessorNode;
        if (origNodesCopied.contains(currentEdge.getSuccessor())) {
          // node the edge goes to has been added already so we retrieve the mapped dummy node to
          // create the new edge
          copiedSuccessorNode = origCFANodeToCopyMap.get(currentEdge.getSuccessor());
        } else {
          // node the edge goes to hasnt been added yet so we add the a new copied node
          copiedSuccessorNode = CFANode.newDummyCFANode("");
          origCFANodeToCopyMap.put(currentEdge.getSuccessor(), copiedSuccessorNode);
          waitlist.add(currentEdge.getSuccessor());
          origNodesCopied.add(currentEdge.getSuccessor());
        }
        // create the new Edge and add it to its predecessor and successor nodes aswell as mapping
        // the original to it
        CFAEdge copyEdge =
            new DummyCFAEdge(origCFANodeToCopyMap.get(currentNode), copiedSuccessorNode);
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
    return Pair.of(
        origCFANodeToCopyMap.get(pEntryNode), origCFANodeToCopyMap.get(pEntryNode.getExitNode()));
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
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap) {
    /*
     * redirect edges (construct a new edge and replace because predecessors and successors are
     * immutable
     */
    CFAEdge redirectedEdge;
    if (isSelfLoop(edgeToRedirect)) {
      redirectedEdge = new DummyCFAEdge(newPredecessor, newPredecessor);
    } else {
      redirectedEdge = new DummyCFAEdge(newPredecessor, edgeToRedirect.getSuccessor());
    }
    edgeToRedirect.getSuccessor().removeEnteringEdge(edgeToRedirect);
    redirectedEdge.getSuccessor().addEnteringEdge(redirectedEdge);
    redirectedEdge.getPredecessor().addLeavingEdge(redirectedEdge);

    copyTestTargetProperty(edgeToRedirect, redirectedEdge, copiedEdgeToTestTargetsMap);
  }

  private void redirectEdgeToNewSuccessor(
      CFAEdge edgeToRedirect,
      CFANode newSuccessor,
      Map<CFAEdge, CFAEdge> pCopiedEdgeToTestTargetsMap) {
    // create a new edge from current Edges start to the successor of its end
    CFAEdge redirectedEdge;
    if (isSelfLoop(edgeToRedirect)) {
      redirectedEdge = new DummyCFAEdge(newSuccessor, newSuccessor);
    } else {
      redirectedEdge = new DummyCFAEdge(edgeToRedirect.getPredecessor(), newSuccessor);
    }
    edgeToRedirect.getPredecessor().removeLeavingEdge(edgeToRedirect);
    redirectedEdge.getPredecessor().addLeavingEdge(redirectedEdge);
    redirectedEdge.getSuccessor().addEnteringEdge(redirectedEdge);
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

  private void removeLeavingEdge(
      final CFAEdge toRemove,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final Set<CFAEdge> pTestTargets) {
    CFANode pred = toRemove.getPredecessor();
    // remove the current nodes leaving edge from its successor and from the current edge
    pred.removeLeavingEdge(toRemove);
    toRemove.getSuccessor().removeEnteringEdge(toRemove);
    // add the exiting edges from the successor to the predecessor to keep the graph intact
    CFANode successorNode = toRemove.getSuccessor();
    for (CFAEdge twoStepDescendantEdge : CFAUtils.leavingEdges(successorNode)) {
      redirectEdgeToNewPredecessor(twoStepDescendantEdge, pred, copiedEdgeToTestTargetsMap);
    }

    // copy the incoming edges to the previous successor to the current Node
    for (CFAEdge enteringRemovedNode : CFAUtils.enteringEdges(successorNode)) {
      if (enteringRemovedNode == toRemove) {
        continue;
      }
      redirectEdgeToNewSuccessor(enteringRemovedNode, pred, copiedEdgeToTestTargetsMap);
    }

    updateTestGoalMappingAfterRemoval(
        toRemove, CFAUtils.enteringEdges(pred), copiedEdgeToTestTargetsMap, pTestTargets);
  }

  private void removeEnteringEdge(
      final CFAEdge toRemove,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final Set<CFAEdge> pTestTargets,
      final boolean mayBeLoopHead) {
    CFANode succ = toRemove.getSuccessor();
    toRemove.getPredecessor().removeLeavingEdge(toRemove);
    succ.removeEnteringEdge(toRemove);

    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(succ)) {
      redirectEdgeToNewPredecessor(
          leavingEdge, toRemove.getPredecessor(), copiedEdgeToTestTargetsMap);
    }

    if (mayBeLoopHead) {
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(succ)) {
        if (toRemove == enteringEdge) {
          continue;
        }
        redirectEdgeToNewSuccessor(
            enteringEdge, toRemove.getPredecessor(), copiedEdgeToTestTargetsMap);
      }
    }

    updateTestGoalMappingAfterRemoval(
        toRemove, CFAUtils.leavingEdges(succ), copiedEdgeToTestTargetsMap, pTestTargets);
  }

  private void applyRule1( // remove edges from copied graph according to first rule
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final Pair<CFANode, CFANode> pCopiedFunctionEntryExit) {

    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    CFAEdge removedEdge;
    CFANode currentNode;

    waitlist.add(pCopiedFunctionEntryExit.getFirst());
    visitedNodes.add(pCopiedFunctionEntryExit.getFirst());
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();

      // Read the node to the queue to ensure longer paths arent overlooked
      // shrink graph if node has only one outgoing edge by removing the successor from the graph
      // unless successor is the root node.
      while (currentNode.getNumLeavingEdges() == 1) {
        if (entersProgramStart(currentNode.getLeavingEdge(0), pCopiedFunctionEntryExit.getFirst())
            || entersProgramEnd(currentNode.getLeavingEdge(0), pCopiedFunctionEntryExit.getSecond())
            || isSelfLoop(currentNode.getLeavingEdge(0))) {
          break;
        }

        removedEdge = currentNode.getLeavingEdge(0);
        removeLeavingEdge(removedEdge, copiedEdgeToTestTargetsMap, pTestTargets);

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
        removeEnteringEdge(
            currentNode.getEnteringEdge(0), copiedEdgeToTestTargetsMap, pTestTargets, false);

        waitlist.remove(currentNode);
      }
      // add nodes to the queue that havent been added yet
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentNode)) {
        if (vistedNodes.add(leavingEdge.getSuccessor())) {
          waitlist.add(leavingEdge.getSuccessor());
        }
      }
    }
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

    waitlist.add(pCopiedFunctionEntryExit.getFirst());
    visitedNodes.add(pCopiedFunctionEntryExit.getFirst());
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();
      ruleApplicable = currentNode.getNumEnteringEdges() > 0;
      removedEdge = null;
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentNode)) {
        if (!inverseDomTree.isAncestorOf(leavingEdge.getSuccessor(), currentNode)) {
          if (removedEdge == null) {
            removedEdge = leavingEdge;
            if (entersProgramStart(removedEdge, pCopiedFunctionEntryExit.getFirst())
                || entersProgramEnd(removedEdge, pCopiedFunctionEntryExit.getSecond())
                || isSelfLoop(removedEdge)) {
              // make sure we dont merge anything into the root node
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
        removeLeavingEdge(removedEdge, copiedEdgeToTestTargetsMap, pTestTargets);
        waitlist.remove(removedEdge.getSuccessor());
        // readd current node to the queue since we merge something into it and the new merged node
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

  private void applyRule4( // remove edges from dummy graph according to fourth rule
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
              // make sure we dont merge anything into the root node
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
        removeEnteringEdge(removedEdge, copiedEdgeToTestTargetsMap, pTestTargets, true);
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
