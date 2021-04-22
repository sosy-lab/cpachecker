// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

public class TestTargetMinimizerEssential {
  // TODO check that dummyTestTargetsMapping/copiedEdgeToTestTargetsMap keys are only CFADummyEdges,
  // and only those are used to
  // access them

  // only works correctly if DummyCFAEdge uses Object equals
  public Set<CFAEdge> reduceTargets(final Set<CFAEdge> testTargets, final CFA pCfa) {
    // maps a node in the original cfa to one in the new copied graph
    Map<CFANode, CFANode> origCFANodeToCopyMap = new HashMap<>();
    // maps a copied edge to the testTarget that can be removed if its dominated by another
    // testTarget
    Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap = new HashMap<>();

    // create a copy of the cfa graph that can be minimized using the essential Branch rules
    copyCFA(testTargets, origCFANodeToCopyMap, copiedEdgeToTestTargetsMap, pCfa.getMainFunction());

    CFANode copiedFunctionEntry = origCFANodeToCopyMap.get(pCfa.getMainFunction());
    // handle all cases of nodes with a single outgoing edge
    applyRule1(
        testTargets,
        copiedEdgeToTestTargetsMap,
        copiedFunctionEntry);

    applyRule2(
        testTargets,
        copiedEdgeToTestTargetsMap,
        copiedFunctionEntry);

    applyRule3(
        testTargets,
        origCFANodeToCopyMap,
        copiedEdgeToTestTargetsMap,
        pCfa.getMainFunction());

    applyRule4(
        testTargets,
        origCFANodeToCopyMap,
        copiedEdgeToTestTargetsMap,
        pCfa.getMainFunction());

    return testTargets;
  }

  private void copyCFA(
      final Set<CFAEdge> pTestTargets,
      final Map<CFANode, CFANode> origCFANodeToCopyMap,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final CFANode pEntryNode)
  {
    // a set of nodes that has already been created to prevent duplicates
    Set<CFANode> origNodesCopied = new HashSet<>();

    Queue<CFANode> waitlist = new ArrayDeque<>();
    // start with function entry point
    origCFANodeToCopyMap.put(pEntryNode, CFANode.newDummyCFANode(""));
    waitlist.add(pEntryNode);
    origNodesCopied.add(pEntryNode);
    while (!waitlist.isEmpty()) {
      // get next node in the queue
      CFANode currentNode = waitlist.poll();

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
  }

  private boolean isSelfLoop(final CFAEdge pEdge) {
    return pEdge.getPredecessor() == pEdge.getSuccessor();
  }

  private boolean entersProgramStart(final CFAEdge pEdge, final CFANode pFunctionEntryNode) {
    return pEdge.getSuccessor() == pFunctionEntryNode;
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

  private void applyRule1( // remove edges from copied graph according to first rule
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final CFANode copiedFunctionEntry) {

    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    CFAEdge removedEdge, testTargetToBeDominated;
    CFANode currentNode;

    // start at entry node because why not?
    waitlist.add(copiedFunctionEntry);
    visitedNodes.add(copiedFunctionEntry);
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();

      // Read the node to the queue to ensure longer paths arent overlooked
      // shrink graph if node has only one outgoing edge by removing the successor from the graph
      // unless successor is the root node.
      while (currentNode.getNumLeavingEdges() == 1) {
        if (entersProgramStart(currentNode.getLeavingEdge(0), copiedFunctionEntry)
            || isSelfLoop(currentNode.getLeavingEdge(0))) {
          // TODO merge current Node into its successor instead?
          break;
        }

        removedEdge = currentNode.getLeavingEdge(0);
        // remove the current nodes leaving edge from its successor and from the current edge
        currentNode.removeLeavingEdge(removedEdge);
        // add the exiting edges from the successor to the predecessor to keep the graph intact
        CFANode successorNode = removedEdge.getSuccessor();
        for (CFAEdge twoStepDescendantEdge : CFAUtils.leavingEdges(successorNode)) {
          redirectEdgeToNewPredecessor(
              twoStepDescendantEdge,
              currentNode,
              copiedEdgeToTestTargetsMap);
        }

        // copy the incoming edges to the previous successor to the current Node
        for (CFAEdge enteringRemovedNode : CFAUtils.enteringEdges(successorNode)) {
          if (enteringRemovedNode.equals(removedEdge)) {
            continue;
          }
          redirectEdgeToNewSuccessor(enteringRemovedNode, currentNode, copiedEdgeToTestTargetsMap);
        }

        if (copiedEdgeToTestTargetsMap.containsKey(removedEdge)) {
          testTargetToBeDominated = copiedEdgeToTestTargetsMap.get(removedEdge);
          // first check if any test targets dominate the removed edges testtarget
          for (CFAEdge enterEdge : CFAUtils.enteringEdges(currentNode)) {
            // if an edge is mapped to a different test target
            if (copiedEdgeToTestTargetsMap.containsKey(enterEdge)
                && !copiedEdgeToTestTargetsMap.get(enterEdge)
                    .equals(testTargetToBeDominated)) {
              // removed edge is getting dominated by this edge so remove the removed edges
              // testtarget from our list of testtargets
              pTestTargets.remove(testTargetToBeDominated);
              // need to remove all occurrences of the same value that has been dominated to clean
              // up
              // the mapping and prevent
              // accidental removal of test targets based on an already removed dominator due to the
              // order of nodes
              copiedEdgeToTestTargetsMap.values()
                  .removeAll(Collections.singleton(testTargetToBeDominated));

              break;
            }
            copiedEdgeToTestTargetsMap.put(enterEdge, testTargetToBeDominated);
          }
          copiedEdgeToTestTargetsMap.remove(removedEdge);
        }

        // due to branching the node we remove might already be in the queue of nodes to look at.
        // In that case we need to remove it from the queue
        if (waitlist.contains(removedEdge.getSuccessor())) {
          waitlist.remove(removedEdge.getSuccessor());
        }
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
      final CFANode copiedFunctionEntry) {
    Set<CFANode> vistedNodes = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    CFANode currentNode;
    CFAEdge removedEdge, testTargetToBeDominated;

    // remove edges from dummy graph according to second rule
    // start at entry node because why not?
    waitlist.add(copiedFunctionEntry);
    vistedNodes.add(copiedFunctionEntry);
    while (!waitlist.isEmpty()) {
      currentNode = waitlist.poll();

      // shrink graph if node has only one outgoing edge by removing the successor from the graph.
      // TODO if current Node is starting node shrink it by removing the predecessor instead of
      // skipping it
      if (currentNode.getNumEnteringEdges() == 1
          && currentNode != copiedFunctionEntry
          && !(isSelfLoop(currentNode.getEnteringEdge(0)))) {

        // remove the current nodes entering edge from its predecessor and from the current node
        removedEdge = currentNode.getEnteringEdge(0);
        currentNode.removeEnteringEdge(removedEdge);
        removedEdge.getPredecessor().removeLeavingEdge(removedEdge);
        if (waitlist.contains(currentNode)) {
          waitlist.remove(currentNode);
        }

        // add the exiting edges from the successor to the predecessor to keep the graph intact
        for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentNode)) {
          // create a new edge from current Edges start to the successor of its end
          redirectEdgeToNewPredecessor(
              leavingEdge,
              removedEdge.getPredecessor(),
              copiedEdgeToTestTargetsMap);
        }

        if (copiedEdgeToTestTargetsMap.containsKey(removedEdge)) {
          testTargetToBeDominated = copiedEdgeToTestTargetsMap.get(removedEdge);
          // inherit the testTarget information to all the outgoing edges as if they get dominated
          // we can exclude the testtarget
          // and remove the removed edge from the mapping as it is now unnecessary
          for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentNode)) {

            if (copiedEdgeToTestTargetsMap.containsKey(leavingEdge)
                && !copiedEdgeToTestTargetsMap.get(leavingEdge).equals(testTargetToBeDominated)) {
              // removed edge is getting dominated by this edge so remove the removed edges
              // testtarget from our list of testtargets
              pTestTargets.remove(testTargetToBeDominated);

              copiedEdgeToTestTargetsMap.values()
                  .removeAll(Collections.singleton(testTargetToBeDominated));
              break;
            }
            copiedEdgeToTestTargetsMap
                .put(leavingEdge, testTargetToBeDominated);
          }
          copiedEdgeToTestTargetsMap.remove(removedEdge);
        }

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
      final Map<CFANode, CFANode> origCFANodeToCopyMap,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final FunctionEntryNode pFunctionEntryNode) {
    // remove edges from dummy graph according to third rule
    Set<CFANode> addedNodes = new HashSet<>();
    Queue<CFANode> nodeQueue = new ArrayDeque<>();

    CFANode entryNode = origCFANodeToCopyMap.get(pFunctionEntryNode);
    DomTree<CFANode> domTree =
        Dominance.createDomTree(
            entryNode,
            TestTargetMinimizerEssential::iteratePredecessors,
            TestTargetMinimizerEssential::iterateSuccessors);
    // start at entry node because why not?
    nodeQueue.add(origCFANodeToCopyMap.get(pFunctionEntryNode));
    addedNodes.add(origCFANodeToCopyMap.get(pFunctionEntryNode));
    while (!nodeQueue.isEmpty()) {
      CFANode currentNode = nodeQueue.poll();
      boolean ruleConditionIsViolated = false;
      CFAEdge firstEdge = null;
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        // TODO figure out dominator tree mechanics so that
        // currentNode.getLeavingEdge(i).getPredecessor dominates
        // currentNode.getLeavingEdge(i).getSuccessor();
        if(domTree.isAncestorOf(domTree.getId(currentNode.getLeavingEdge(i).getPredecessor()),
            domTree.getId(currentNode.getLeavingEdge(i).getSuccessor()))) {
          if(firstEdge==null) {
            firstEdge= currentNode.getLeavingEdge(i);
            if (firstEdge.getPredecessor() == pFunctionEntryNode
                || firstEdge.getPredecessor() == firstEdge.getSuccessor()) {
              // make sure we dont merge anything into the root node
              ruleConditionIsViolated = true;
              break;
            }
          }
          else {
            ruleConditionIsViolated = true;
            break;
          }
        }
      }
      // add successors of the current node to the queue
      // for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
      // if (addedNodes.add(currentNode.getLeavingEdge(i).getSuccessor())) {
      // nodeQueue.add(currentNode.getLeavingEdge(i).getSuccessor());
      // }
      // }

      // remove "first edge" from the graph if ruleCondition hasnt been violated.
      if (!ruleConditionIsViolated
          && firstEdge != null
          && firstEdge.getPredecessor() != firstEdge.getSuccessor()) {
        // readd current node to the queue since we merge something into it and the new merged node
        // may satisfy the condition again
        nodeQueue.add(currentNode);

        // copy testtarget associations to edges incoming to the predecessor of first edge and
        // remove first edge from the list as it will get removed
        if (copiedEdgeToTestTargetsMap.containsKey(firstEdge)) {
          for (int i = 0; i < firstEdge.getPredecessor().getNumEnteringEdges(); i++) {
            if (copiedEdgeToTestTargetsMap
                .containsKey(firstEdge.getPredecessor().getEnteringEdge(i))) {
              // remove test target associated with first edge from our set of testtargets since it
              // is covered by an incoming edge
              pTestTargets.remove(copiedEdgeToTestTargetsMap.get(firstEdge));
              break;
            }
            copiedEdgeToTestTargetsMap.put(
                firstEdge.getPredecessor().getEnteringEdge(i),
                copiedEdgeToTestTargetsMap.get(firstEdge));
          }
          copiedEdgeToTestTargetsMap.remove(firstEdge);
        }
        if (nodeQueue.contains(firstEdge.getSuccessor())) {
          nodeQueue.remove(firstEdge.getSuccessor());
        }

        // copy entering and leaving edges to the Predecessor

        // copy leaving edges
        for (int i = 0; i < firstEdge.getSuccessor().getNumLeavingEdges(); i++) {
          CFAEdge changedEdge = firstEdge.getSuccessor().getLeavingEdge(i);
          CFAEdge newDummyEdge =
              new DummyCFAEdge(firstEdge.getPredecessor(), changedEdge.getSuccessor());
          if (changedEdge.getPredecessor() == changedEdge.getSuccessor()) {
            newDummyEdge = new DummyCFAEdge(firstEdge.getPredecessor(), firstEdge.getPredecessor());
          }
          if (copiedEdgeToTestTargetsMap.containsKey(changedEdge)) {
            copiedEdgeToTestTargetsMap.put(
                newDummyEdge,
                copiedEdgeToTestTargetsMap.get(changedEdge));
            copiedEdgeToTestTargetsMap.remove(changedEdge);
          }
          newDummyEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          newDummyEdge.getSuccessor().addEnteringEdge(newDummyEdge);
          changedEdge.getSuccessor().removeEnteringEdge(changedEdge);
        }
        // copy entering edges except first edge
        for (int i = 0; i < firstEdge.getSuccessor().getNumEnteringEdges(); i++) {
          CFAEdge changedEdge = firstEdge.getSuccessor().getEnteringEdge(i);
          if (firstEdge == changedEdge) {
            continue;
          }
          CFAEdge newDummyEdge =
              new DummyCFAEdge(changedEdge.getPredecessor(), firstEdge.getPredecessor());
          if (changedEdge.getPredecessor() == changedEdge.getSuccessor()) {
            newDummyEdge = new DummyCFAEdge(firstEdge.getPredecessor(), firstEdge.getPredecessor());
          }
          if (copiedEdgeToTestTargetsMap.containsKey(changedEdge)) {
            copiedEdgeToTestTargetsMap.put(
                newDummyEdge,
                copiedEdgeToTestTargetsMap.get(changedEdge));
            copiedEdgeToTestTargetsMap.remove(changedEdge);
          }
          newDummyEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          newDummyEdge.getSuccessor().addEnteringEdge(newDummyEdge);
          changedEdge.getPredecessor().removeLeavingEdge(changedEdge);
        }

        firstEdge.getPredecessor().removeLeavingEdge(firstEdge);
      }
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        if (!addedNodes.contains(currentNode.getLeavingEdge(i).getSuccessor())) {
          nodeQueue.add(currentNode.getLeavingEdge(i).getSuccessor());
          addedNodes.add(currentNode.getLeavingEdge(i).getSuccessor());
        }
      }

    }

  }

  private void applyRule4( // remove edges from dummy graph according to fourth rule
      final Set<CFAEdge> pTestTargets,
      final Map<CFANode, CFANode> origCFANodeToCopyMap,
      final Map<CFAEdge, CFAEdge> copiedEdgeToTestTargetsMap,
      final FunctionEntryNode pFunctionEntryNode) {

    Set<CFANode> addedNodes = new HashSet<>();
    Queue<CFANode> nodeQueue = new ArrayDeque<>();

    // create domination relationship on the reduced graph
    CFANode entryNode = origCFANodeToCopyMap.get(pFunctionEntryNode);
    DomTree<CFANode> domTree =
        Dominance.createDomTree(
            entryNode,
            TestTargetMinimizerEssential::iteratePredecessors,
            TestTargetMinimizerEssential::iterateSuccessors);
    // start at entry node because why not?
    nodeQueue.add(origCFANodeToCopyMap.get(pFunctionEntryNode));
    addedNodes.add(origCFANodeToCopyMap.get(pFunctionEntryNode));
    while (!nodeQueue.isEmpty()) {
      CFANode currentNode = nodeQueue.poll();
      boolean ruleConditionIsViolated = false;
      CFAEdge firstEdge = null;
      for (int i = 0; i < currentNode.getNumEnteringEdges(); i++) {
        // TODO figure out dominator tree mechanics so that
        // currentNode.getEnteringEdge(i).getPredecessor is dominated by
        // currentNode.getEnteringEdge(i).getSuccessor();
        if(!domTree.isAncestorOf(domTree.getId(currentNode), domTree.getId(currentNode.getEnteringEdge(i).getPredecessor()))) {
          if(firstEdge==null) {
            firstEdge= currentNode.getEnteringEdge(i);
            if (firstEdge.getPredecessor() == pFunctionEntryNode
                || firstEdge.getPredecessor() == firstEdge.getSuccessor()) {
              // make sure we dont merge anything into the root node
              ruleConditionIsViolated = true;
              break;
            }
          }
          else {
            ruleConditionIsViolated = true;
            break;
          }
        }

      }
      // add successors of the current node to the queue
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        if (addedNodes.add(currentNode.getLeavingEdge(i).getSuccessor())) {
          nodeQueue.add(currentNode.getLeavingEdge(i).getSuccessor());
        }
      }
      //remove "first edge" from the graph if isruleConditionviolated =false.
      if (!ruleConditionIsViolated
          && firstEdge != null
          && firstEdge.getPredecessor() != firstEdge.getSuccessor()) {
        //copy testtarget associations to edges leaving the Successor
        if(copiedEdgeToTestTargetsMap.containsKey(firstEdge)) {
          for (int i=0;i<firstEdge.getSuccessor().getNumLeavingEdges();i++) {
            if(copiedEdgeToTestTargetsMap.containsKey(firstEdge.getSuccessor().getLeavingEdge(i))) {
              //remove test target associated with first edge from our set of testtargets
              pTestTargets.remove(copiedEdgeToTestTargetsMap.get(firstEdge));
              break;
            }
            copiedEdgeToTestTargetsMap.put(firstEdge.getSuccessor().getLeavingEdge(i), copiedEdgeToTestTargetsMap.get(firstEdge));


          }
          copiedEdgeToTestTargetsMap.remove(firstEdge);
        }
        if (nodeQueue.contains(firstEdge.getSuccessor())) {
          nodeQueue.remove(firstEdge.getSuccessor());
        }

        //copy entering and leaving edges to the Predecessor

        //copy leaving edges
        for (int i=0;i<firstEdge.getSuccessor().getNumLeavingEdges();i++) {
          CFAEdge changedEdge = firstEdge.getSuccessor().getLeavingEdge(i);
          CFAEdge newDummyEdge =
              new DummyCFAEdge(firstEdge.getPredecessor(), changedEdge.getSuccessor());

          if (changedEdge.getPredecessor() == changedEdge.getSuccessor()) {
            newDummyEdge = new DummyCFAEdge(firstEdge.getPredecessor(), firstEdge.getPredecessor());
          }
          if (copiedEdgeToTestTargetsMap.containsKey(changedEdge)) {
            copiedEdgeToTestTargetsMap.put(newDummyEdge, copiedEdgeToTestTargetsMap.get(changedEdge));
            copiedEdgeToTestTargetsMap.remove(changedEdge);
          }
          newDummyEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          newDummyEdge.getSuccessor().addEnteringEdge(newDummyEdge);
          changedEdge.getSuccessor().removeEnteringEdge(changedEdge);
        }
        //copy entering edges except first edge
        for(int i = 0; i<firstEdge.getSuccessor().getNumEnteringEdges();i++) {
          if(firstEdge==firstEdge.getSuccessor().getEnteringEdge(i)) {
            continue;
          }
          CFAEdge changedEdge = firstEdge.getSuccessor().getEnteringEdge(i);
          CFAEdge newDummyEdge =
              new DummyCFAEdge(changedEdge.getPredecessor(), firstEdge.getPredecessor());

          if (changedEdge.getPredecessor() == changedEdge.getSuccessor()) {
            newDummyEdge = new DummyCFAEdge(firstEdge.getPredecessor(), firstEdge.getPredecessor());
          }
          if (copiedEdgeToTestTargetsMap.containsKey(changedEdge)) {
            copiedEdgeToTestTargetsMap.put(newDummyEdge, copiedEdgeToTestTargetsMap.get(changedEdge));
            copiedEdgeToTestTargetsMap.remove(changedEdge);
          }
          newDummyEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          newDummyEdge.getSuccessor().addEnteringEdge(newDummyEdge);
          changedEdge.getPredecessor().removeLeavingEdge(changedEdge);
          changedEdge.getSuccessor().removeEnteringEdge(changedEdge);
        }

        firstEdge.getPredecessor().removeLeavingEdge(firstEdge);
        firstEdge.getSuccessor().removeEnteringEdge(firstEdge);
      }

      // add successors of the current node to the queue
      if (!nodeQueue.contains(currentNode)) {
        for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
          if (addedNodes.add(currentNode.getLeavingEdge(i).getSuccessor())) {
            nodeQueue.add(currentNode.getLeavingEdge(i).getSuccessor());
          }
        }
      }

    }

  }

  private static Iterable<CFANode> iteratePredecessors(CFANode pNode) {

    return createNodeIterable(
        pNode,
        false,
        node -> node instanceof FunctionEntryNode,
        node -> !(node instanceof FunctionExitNode));
  }

  private static Iterable<CFANode> iterateSuccessors(CFANode pNode) {

    return createNodeIterable(
        pNode,
        true,
        node -> node instanceof FunctionExitNode,
        node -> !(node instanceof FunctionEntryNode));
  }

  private static Iterable<CFANode> createNodeIterable(
      CFANode pNode,
      boolean pForward,
      Predicate<CFANode> pStop,
      Predicate<CFANode> pFilter) {

    if (pStop.test(pNode)) {
      return Collections::emptyIterator;
    }

    Iterator<CFANode> iterator =
        (pForward ? CFAUtils.allSuccessorsOf(pNode) : CFAUtils.allPredecessorsOf(pNode)).iterator();

    return () -> Iterators.filter(iterator, pFilter);
  }

}
