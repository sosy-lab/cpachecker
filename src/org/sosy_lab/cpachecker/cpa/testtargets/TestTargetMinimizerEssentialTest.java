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
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;
public class TestTargetMinimizerEssentialTest {

  @Test
  public void rule3Test() {
    Set<CFAEdge> testTargets = new HashSet<>();
    CFANode u = CFANode.newDummyCFANode("u");
    CFANode v = CFANode.newDummyCFANode("v");
    CFANode w = CFANode.newDummyCFANode("w");
    CFANode x = CFANode.newDummyCFANode("x");
    CFANode y = CFANode.newDummyCFANode("y");
    CFANode z = CFANode.newDummyCFANode("z");
    CFAEdge a = new BlankEdge("ux", FileLocation.DUMMY, u, x, "ux");
    CFAEdge b = new BlankEdge("xy", FileLocation.DUMMY, x, y, "vx");
    CFAEdge c = new BlankEdge("yz", FileLocation.DUMMY, y, z, "c");
    CFAEdge d = new BlankEdge("yz2", FileLocation.DUMMY, y, z, "d");
    CFAEdge e = new BlankEdge("xw", FileLocation.DUMMY, x, w, "e");
    CFAEdge f = new BlankEdge("xw2", FileLocation.DUMMY, x, w, "f");
    CFAEdge g = new BlankEdge("wx", FileLocation.DUMMY, w, x, "e");
    CFAEdge h = new BlankEdge("wx2", FileLocation.DUMMY, w, x, "f");

    u.addLeavingEdge(a);
    x.addEnteringEdge(a);
    x.addLeavingEdge(b);
    y.addEnteringEdge(b);
    y.addLeavingEdge(c);
    z.addEnteringEdge(c);
    y.addLeavingEdge(d);
    z.addEnteringEdge(d);
    x.addLeavingEdge(e);
    w.addEnteringEdge(e);
    x.addLeavingEdge(f);
    w.addEnteringEdge(f);
    w.addLeavingEdge(g);
    x.addEnteringEdge(g);
    w.addLeavingEdge(h);
    x.addEnteringEdge(h);

    Map<CFANode, CFANode> nodesMapping = new HashMap<>();
    // maps a dummy edge to the testTarget that can be removed if its dominated by another
    // testTarget
    Map<CFAEdge, CFAEdge> dummyTestTargetsMapping = new IdentityHashMap<>();

    CFANode entryNode = u;
    DomTree<CFANode> domTree =
        Dominance.createDomTree(
            entryNode,
            TestTargetMinimizerEssentialTest::iteratePredecessors,
            TestTargetMinimizerEssentialTest::iterateSuccessors);
    Queue<CFANode> nodeQueue = new ArrayDeque<>();
    Set<CFANode> addedNodes = new HashSet<>();
    // start at entry node because why not?
    nodeQueue.add(u);
    addedNodes.add(u);
    while (!nodeQueue.isEmpty()) {
      CFANode currentNode = nodeQueue.poll();
      boolean ruleConditionIsViolated = false;
      CFAEdge firstEdge = null;
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        // TODO figure out dominator tree mechanics so that
        // currentNode.getLeavingEdge(i).getPredecessor dominates
        // currentNode.getLeavingEdge(i).getSuccessor();
        if (domTree.isAncestorOf(
            domTree.getId(currentNode.getLeavingEdge(i).getPredecessor()),
            domTree.getId(currentNode.getLeavingEdge(i).getSuccessor()))) {
          if (firstEdge == null) {
            firstEdge = currentNode.getLeavingEdge(i);
            if (firstEdge.getPredecessor() == u) {
              // make sure we dont merge anything into the root node
              ruleConditionIsViolated = true;
              break;
            }
          } else {
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
    String tets = addedNodes.toString();
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
