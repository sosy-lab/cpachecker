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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

public class TestTargetMinimizerEssential {

  public TestTargetMinimizerEssential() {

  }

  public Set<CFAEdge> reduceTargets(Set<CFAEdge> testTargets, final CFA pCfa) {

    // create a copy of the cfa graph that can be minimized using the essential Branch rules

    // maps a node in the original cfa to one in the new dummy graph
    Map<CFANode, CFANode> nodesMapping = new HashMap<>();
    // maps a dummy edge to the testTarget that can be removed if its dominated by another
    // testTarget
    Map<CFAEdge, CFAEdge> dummyTestTargetsMapping = new IdentityHashMap<>();


    // a set of nodes that has already been created to prevent duplicates
    Set<CFANode> addedNodes = new HashSet<>();

    Queue<CFANode> nodeQueue = new ArrayDeque<>();
    // start with function entry point
    CFANode dummyStartNode = CFANode.newDummyCFANode("");
    nodesMapping.put(pCfa.getMainFunction(), dummyStartNode);
    nodeQueue.add(pCfa.getMainFunction());
    addedNodes.add(pCfa.getMainFunction());
    while (!nodeQueue.isEmpty()) {
      // get next node in the queue
      CFANode currentNode = nodeQueue.poll();

      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        // create dummies of all outgoing edges and the nodes they go into if they dont yet exist
        CFAEdge currentEdge = currentNode.getLeavingEdge(i);
        // create new dummyNode if the node hasn't yet been created
        CFANode dummySuccessorNode = CFANode.newDummyCFANode("");
        if (addedNodes.contains(currentEdge.getSuccessor())) {
          // node the edge goes to has been added already so we retrieve the mapped dummy node to
          // create the new edge
          dummySuccessorNode = nodesMapping.get(currentEdge.getSuccessor());
        } else {
          // node the edge goes to hasnt been added yet so we add the new dummy node
          nodesMapping.put(currentEdge.getSuccessor(), dummySuccessorNode);
          nodeQueue.add(currentEdge.getSuccessor());
          addedNodes.add(currentEdge.getSuccessor());
        }
        // create the new Edge and add it to its predecessor and successor nodes aswell as mapping
        // the original to it
        CFAEdge newDummyEdge =
            new BlankEdge(
                "not null",
                FileLocation.DUMMY,
                nodesMapping.get(currentNode),
                dummySuccessorNode,
                "not null");
        nodesMapping.get(currentNode).addLeavingEdge(newDummyEdge);
        dummySuccessorNode.addEnteringEdge(newDummyEdge);

        // if the original edge is part of the test targets we have to map the new dummy edge to
        // the test target that may be removed if its being dominated
        if (testTargets.contains(currentEdge)) {
          dummyTestTargetsMapping.put(newDummyEdge, currentEdge);
        }
      }
    }
    // complete dummy graph has been created

    // remove edges from dummy graph according to first rule
    nodeQueue = new ArrayDeque<>();
    addedNodes = new HashSet<>();
    // start at entry node because why not?
    nodeQueue.add(nodesMapping.get(pCfa.getMainFunction()));
    addedNodes.add(nodesMapping.get(pCfa.getMainFunction()));
    while (!nodeQueue.isEmpty()) {
      CFANode currentNode = nodeQueue.poll();

      // Read the node to the queue to ensure longer paths arent overlooked
      // shrink graph if node has only one outgoing edge by removing the successor from the graph
      // unless successor is the root node.
      if (currentNode.getNumLeavingEdges() == 1) {
        if (currentNode.getLeavingEdge(0).getSuccessor() == nodesMapping
            .get(pCfa.getMainFunction())) {
          // TODO merge current Node into its successor instead?
          continue;
        }
        nodeQueue.add(currentNode);
        // remove the current nodes leaving edge from its successor and from the current edge
        CFAEdge removedEdge = currentNode.getLeavingEdge(0);
        currentNode.removeLeavingEdge(removedEdge);
        // due to branching the node we remove might already be in the queue of nodes to look at.
        // In that case we need to remove it from the queue
        if (nodeQueue.contains(removedEdge.getSuccessor())) {
          nodeQueue.remove(removedEdge.getSuccessor());
        }



        if (dummyTestTargetsMapping.containsKey(removedEdge)) {

          // first check if any test targets dominate the removed edges testtarget
          for (int i = 0; i < currentNode.getNumEnteringEdges(); i++) {
            // if an edge is mapped to a different test target
            if (dummyTestTargetsMapping.containsKey(currentNode.getEnteringEdge(i))
                && dummyTestTargetsMapping.get(currentNode.getEnteringEdge(i))
                    .equals(dummyTestTargetsMapping.get(removedEdge))) {
              // removed edge is getting dominated by this edge so remove the removed edges
              // testtarget from our list of testtargets
              CFAEdge dominatedTestTarget = dummyTestTargetsMapping.get(removedEdge);
              testTargets.remove(dominatedTestTarget);
              // need to remove all occurences of the same value that has been dominated to clean up
              // the mapping and prevent
              // accidental removal of test targets based on an already removed dominator due to the
              // order of nodes
              dummyTestTargetsMapping.values()
                  .removeAll(Collections.singleton(dominatedTestTarget));

              break;
            }
          }
        }

        if (dummyTestTargetsMapping.containsKey(removedEdge)) {
          // inherit the testTarget information to all the incoming edges as if they get dominated
          // we can exclude the testtarget
          // and remove the removed edge from the mapping as it is now unnecessary
          for (int i = 0; i < currentNode.getNumEnteringEdges(); i++) {
            dummyTestTargetsMapping
                .put(currentNode.getEnteringEdge(i), dummyTestTargetsMapping.get(removedEdge));

          }
          dummyTestTargetsMapping.remove(removedEdge);
        }
        // add the exiting edges from the successor to the predecessor to keep the graph intact
        CFANode successorNode = removedEdge.getSuccessor();
        for (int i = 0; i < successorNode.getNumLeavingEdges(); i++) {
          // create a new edge from current Edges start to the successor of its end
          CFAEdge changedEdge = successorNode.getLeavingEdge(i);
          CFAEdge newDummyEdge =
              new BlankEdge(
                  "",
                  FileLocation.DUMMY,
                  currentNode,
                  changedEdge.getSuccessor(),
                  "");

          if (dummyTestTargetsMapping.containsKey(changedEdge)) {
            // copy potential testTarget associations to new edge

            dummyTestTargetsMapping
                .put(newDummyEdge, dummyTestTargetsMapping.get(changedEdge));
            dummyTestTargetsMapping.remove(changedEdge);

          }
          // remove the edge from its successor and add a new edge from current Node to said
          // successor
          successorNode.getLeavingEdge(i)
              .getSuccessor()
              .removeEnteringEdge(successorNode.getLeavingEdge(i));
          successorNode.getLeavingEdge(i).getSuccessor().addEnteringEdge(newDummyEdge);
          currentNode.addLeavingEdge(newDummyEdge);
        }
        // copy the incoming edges to the previous successor to the current Node
        for (int i = 0; i < successorNode.getNumEnteringEdges(); i++) {
          if (successorNode.getEnteringEdge(i).equals(removedEdge)) {
            continue;
          }
          CFAEdge changedEdge = successorNode.getEnteringEdge(i);
          // create a new edge from current Edges start to the successor of its end
          CFAEdge newDummyEdge =
              new BlankEdge(
                  "",
                  FileLocation.DUMMY,
                  changedEdge.getPredecessor(),
                  currentNode,
                  "");

          // remove the edge from its successor and add a new edge from current Node to said
          // successor
          if (dummyTestTargetsMapping.containsKey(changedEdge)) {
            // copy potential testTarget associations to new edge

            dummyTestTargetsMapping
                .put(newDummyEdge, dummyTestTargetsMapping.get(changedEdge));
            dummyTestTargetsMapping.remove(changedEdge);

          }
          changedEdge.getPredecessor().removeLeavingEdge(changedEdge);
          changedEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          currentNode.addEnteringEdge(newDummyEdge);
        }

      }
      // add nodes to the queue that havent been added yet unless current node has been readded.
      if (!nodeQueue.contains(currentNode)) {

        for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {


          if (!addedNodes.contains(currentNode.getLeavingEdge(i).getSuccessor())) {
            nodeQueue.add(currentNode.getLeavingEdge(i).getSuccessor());
            addedNodes.add(currentNode.getLeavingEdge(i).getSuccessor());
          }
        }
        }
    }



    // all cases of nodes with a single outgoing edge should be eliminated

    // remove edges from dummy graph according to second rule
    nodeQueue = new ArrayDeque<>();
    addedNodes = new HashSet<>();
    // start at entry node because why not?
    nodeQueue.add(nodesMapping.get(pCfa.getMainFunction()));
    addedNodes.add(nodesMapping.get(pCfa.getMainFunction()));
    while (!nodeQueue.isEmpty()) {
      CFANode currentNode = nodeQueue.poll();

      // shrink graph if node has only one outgoing edge by removing the successor from the graph.
      // TODO if current Node is starting node shrink it by removing the predecessor instead of
      // skipping it
      if (currentNode.getNumEnteringEdges() == 1
          && !(currentNode == nodesMapping.get(pCfa.getMainFunction()))) {

        // remove the current nodes entering edge from its predecessor and from the current node
        CFAEdge removedEdge = currentNode.getEnteringEdge(0);
        currentNode.removeEnteringEdge(removedEdge);
        removedEdge.getPredecessor().removeLeavingEdge(removedEdge);
        if (nodeQueue.contains(removedEdge.getSuccessor())) {
          nodeQueue.remove(removedEdge.getSuccessor());
        }

        if (dummyTestTargetsMapping.containsKey(removedEdge)) {
          // inherit the testTarget information to all the outgoing edges as if they get dominated
          // we can exclude the testtarget
          // and remove the removed edge from the mapping as it is now unnecessary
          for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {

            if (dummyTestTargetsMapping.containsKey(currentNode.getLeavingEdge(i))) {
              // removed edge is getting dominated by this edge so remove the removed edges
              // testtarget from our list of testtargets
              // TODO do we need to remove all other edges with this value from the mapping aswell
              // to prevent wrong eliminations of test targets?
              testTargets.remove(dummyTestTargetsMapping.get(removedEdge));
              break;
            }
            dummyTestTargetsMapping
                .put(currentNode.getLeavingEdge(i), dummyTestTargetsMapping.get(removedEdge));

          }
          dummyTestTargetsMapping.remove(removedEdge);
        }
        // add the exiting edges from the successor to the predecessor to keep the graph intact
        CFANode successorNode = removedEdge.getSuccessor();
        for (int i = 0; i < successorNode.getNumLeavingEdges(); i++) {
          // create a new edge from current Edges start to the successor of its end
          CFAEdge newDummyEdge =
              new BlankEdge(
                  "",
                  FileLocation.DUMMY,
                  removedEdge.getPredecessor(),
                  successorNode.getLeavingEdge(i).getSuccessor(),
                  "");

          if (dummyTestTargetsMapping.containsKey(successorNode.getLeavingEdge(i))) {
            // copy potential testTarget associations to new edge
            dummyTestTargetsMapping
                .put(newDummyEdge, dummyTestTargetsMapping.get(successorNode.getLeavingEdge(i)));
            dummyTestTargetsMapping.remove(successorNode.getLeavingEdge(i));
          }
          // remove the edge from its successor and add a new edge from current Node to said
          // successor
          successorNode.getLeavingEdge(i)
              .getSuccessor()
              .removeEnteringEdge(successorNode.getLeavingEdge(i));
          successorNode.getLeavingEdge(i).getSuccessor().addEnteringEdge(newDummyEdge);
          removedEdge.getPredecessor().addLeavingEdge(newDummyEdge);
        }

      }
      // add nodes to the queue that havent been added yet
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        if (!addedNodes.contains(currentNode.getLeavingEdge(i).getSuccessor())) {
          nodeQueue.add(currentNode.getLeavingEdge(i).getSuccessor());
          addedNodes.add(currentNode.getLeavingEdge(i).getSuccessor());
        }
      }


    }
    // remove edges from dummy graph according to third rule
    CFANode entryNode = nodesMapping.get(pCfa.getMainFunction());
    DomTree<CFANode> domTree =
        Dominance.createDomTree(
            entryNode,
            TestTargetMinimizerEssential::iteratePredecessors,
            TestTargetMinimizerEssential::iterateSuccessors);
    nodeQueue = new ArrayDeque<>();
    addedNodes = new HashSet<>();
    // start at entry node because why not?
    nodeQueue.add(nodesMapping.get(pCfa.getMainFunction()));
    addedNodes.add(nodesMapping.get(pCfa.getMainFunction()));
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
            if (firstEdge.getPredecessor() == pCfa.getMainFunction()) {
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
      if (!ruleConditionIsViolated && firstEdge != null) {
        // readd current node to the queue since we merge something into it and the new merged node
        // may satisfy the condition again
        nodeQueue.add(currentNode);

        // copy testtarget associations to edges incoming to the predecessor of first edge and
        // remove first edge from the list as it will get removed
        if (dummyTestTargetsMapping.containsKey(firstEdge)) {
          for (int i = 0; i < firstEdge.getPredecessor().getNumEnteringEdges(); i++) {
            if (dummyTestTargetsMapping
                .containsKey(firstEdge.getPredecessor().getEnteringEdge(i))) {
              // remove test target associated with first edge from our set of testtargets since it
              // is covered by an incoming edge
              testTargets.remove(dummyTestTargetsMapping.get(firstEdge));
              break;
            }
            dummyTestTargetsMapping.put(
                firstEdge.getPredecessor().getEnteringEdge(i),
                dummyTestTargetsMapping.get(firstEdge));
          }
          dummyTestTargetsMapping.remove(firstEdge);
        }
        if (nodeQueue.contains(firstEdge.getSuccessor())) {
          nodeQueue.remove(firstEdge.getSuccessor());
        }

        // copy entering and leaving edges to the Predecessor

        // copy leaving edges
        for (int i = 0; i < firstEdge.getSuccessor().getNumLeavingEdges(); i++) {
          CFAEdge newDummyEdge =
              new BlankEdge(
                  "",
                  FileLocation.DUMMY,
                  firstEdge.getPredecessor(),
                  firstEdge.getSuccessor().getLeavingEdge(i).getSuccessor(),
                  "");
          if (dummyTestTargetsMapping.containsKey(firstEdge.getSuccessor().getLeavingEdge(i))) {
            dummyTestTargetsMapping.put(
                newDummyEdge,
                dummyTestTargetsMapping.get(firstEdge.getSuccessor().getLeavingEdge(i)));
            dummyTestTargetsMapping.remove(firstEdge.getSuccessor().getLeavingEdge(i));
          }
          newDummyEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          newDummyEdge.getSuccessor().addEnteringEdge(newDummyEdge);
          newDummyEdge.getSuccessor()
              .removeEnteringEdge(firstEdge.getSuccessor().getLeavingEdge(i));
        }
        // copy entering edges except first edge
        for (int i = 0; i < firstEdge.getSuccessor().getNumEnteringEdges(); i++) {
          if (firstEdge == firstEdge.getSuccessor().getEnteringEdge(i)) {
            continue;
          }
          CFAEdge newDummyEdge =
              new BlankEdge(
                  "",
                  FileLocation.DUMMY,
                  firstEdge.getSuccessor().getEnteringEdge(i).getPredecessor(),
                  firstEdge.getPredecessor(),
                  "");
          if (dummyTestTargetsMapping.containsKey(firstEdge.getSuccessor().getEnteringEdge(i))) {
            dummyTestTargetsMapping.put(
                newDummyEdge,
                dummyTestTargetsMapping.get(firstEdge.getSuccessor().getEnteringEdge(i)));
            dummyTestTargetsMapping.remove(firstEdge.getSuccessor().getEnteringEdge(i));
          }
          newDummyEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          newDummyEdge.getSuccessor().addEnteringEdge(newDummyEdge);
          firstEdge.getSuccessor()
              .getEnteringEdge(i)
              .getPredecessor()
              .removeLeavingEdge(firstEdge.getSuccessor().getEnteringEdge(i));
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




    // remove edges from dummy graph according to fourth rule
    // create domination relationship on the reduced graph
    entryNode = nodesMapping.get(pCfa.getMainFunction());
    domTree =
        Dominance.createDomTree(
            entryNode,
            TestTargetMinimizerEssential::iteratePredecessors,
            TestTargetMinimizerEssential::iterateSuccessors);
    nodeQueue = new ArrayDeque<>();
    addedNodes = new HashSet<>();
    // start at entry node because why not?
    nodeQueue.add(nodesMapping.get(pCfa.getMainFunction()));
    addedNodes.add(nodesMapping.get(pCfa.getMainFunction()));
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
            if (firstEdge.getPredecessor() == pCfa.getMainFunction()) {
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
      if(!ruleConditionIsViolated&&firstEdge!=null) {
        //copy testtarget associations to edges leaving the Successor
        if(dummyTestTargetsMapping.containsKey(firstEdge)) {
          for (int i=0;i<firstEdge.getSuccessor().getNumLeavingEdges();i++) {
            if(dummyTestTargetsMapping.containsKey(firstEdge.getSuccessor().getLeavingEdge(i))) {
              //remove test target associated with first edge from our set of testtargets
              testTargets.remove(dummyTestTargetsMapping.get(firstEdge));
              break;
            }
            dummyTestTargetsMapping.put(firstEdge.getSuccessor().getLeavingEdge(i), dummyTestTargetsMapping.get(firstEdge));


          }
          dummyTestTargetsMapping.remove(firstEdge);
        }
        if (nodeQueue.contains(firstEdge.getSuccessor())) {
          nodeQueue.remove(firstEdge.getSuccessor());
        }

        //copy entering and leaving edges to the Predecessor

        //copy leaving edges
        for (int i=0;i<firstEdge.getSuccessor().getNumLeavingEdges();i++) {
          CFAEdge newDummyEdge =
              new BlankEdge(
                  "",
                  FileLocation.DUMMY,
                  firstEdge.getPredecessor(),
                  firstEdge.getSuccessor().getLeavingEdge(i).getSuccessor(),
                  "");
          if(dummyTestTargetsMapping.containsKey(firstEdge.getSuccessor().getLeavingEdge(i))){
            dummyTestTargetsMapping.put(newDummyEdge,dummyTestTargetsMapping.get(firstEdge.getSuccessor().getLeavingEdge(i)));
            dummyTestTargetsMapping.remove(firstEdge.getSuccessor().getLeavingEdge(i));
          }
          newDummyEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          newDummyEdge.getSuccessor().addEnteringEdge(newDummyEdge);
          newDummyEdge.getSuccessor().removeEnteringEdge(firstEdge.getSuccessor().getLeavingEdge(i));
        }
        //copy entering edges except first edge
        for(int i = 0; i<firstEdge.getSuccessor().getNumEnteringEdges();i++) {
          if(firstEdge==firstEdge.getSuccessor().getEnteringEdge(i)) {
            continue;
          }
          CFAEdge newDummyEdge =
              new BlankEdge(
                  "",
                  FileLocation.DUMMY,
                  firstEdge.getSuccessor().getEnteringEdge(i).getPredecessor(),
                  firstEdge.getPredecessor(),
                  "");
          if(dummyTestTargetsMapping.containsKey(firstEdge.getSuccessor().getEnteringEdge(i))) {
            dummyTestTargetsMapping.put(newDummyEdge, dummyTestTargetsMapping.get(firstEdge.getSuccessor().getEnteringEdge(i)));
            dummyTestTargetsMapping.remove(firstEdge.getSuccessor().getEnteringEdge(i));
          }
          newDummyEdge.getPredecessor().addLeavingEdge(newDummyEdge);
          newDummyEdge.getSuccessor().addEnteringEdge(newDummyEdge);
          firstEdge.getSuccessor().getEnteringEdge(i).getPredecessor().removeLeavingEdge(firstEdge.getSuccessor().getEnteringEdge(i));
          firstEdge.getSuccessor().getEnteringEdge(i).getSuccessor().removeEnteringEdge(firstEdge.getSuccessor().getEnteringEdge(i));
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


    return testTargets;
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
