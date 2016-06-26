/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.collect.Iterables.addAll;
import static org.sosy_lab.cpachecker.util.CFAUtils.predecessorsOf;
import static org.sosy_lab.cpachecker.util.CFAUtils.successorsOf;

import com.google.common.base.Predicates;

import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class CFASimplifier {

  /**
   * This method takes a cfa as input and simplifies it, in the way, that
   * Assume Edges which are not needed (p.e. because there are no edges besides
   * BlankEdges in the subtree of an AssumeEdge) are deleted and replaced by a
   * single BlankEdge.
   *
   * @param cfa The cfa which should be simplified
   */
  public static void simplifyCFA(MutableCFA cfa) {
    for (CFANode root : cfa.getAllFunctionHeads()) {
      simplifyFunction(root, cfa);
    }
  }

  /**
   * This method makes the simplification step for a single function, the
   * root node is the node where the search for possible simplifications starts.
   *
   * @param root start node for simplification
   * @param cfa The cfa where the simplifications should be applied
   */
  private static void simplifyFunction(final CFANode root, final MutableCFA cfa) {
    // We want to eliminate branching with two empty branches (only blank edges).
    // Inner branches need to be eliminated first.

    // The list of all branching points in this function.
    final Deque<CFANode> branchingPoints = findBranchingPoints(root, cfa);
    assert branchingPoints.size() == new HashSet<>(branchingPoints).size()
        : "branchingPoints contains duplicate CFANode " + branchingPoints;

    // We need to simplify inner branches first, thus we iterate backwards through the queue.
    while (!branchingPoints.isEmpty()) {
      final CFANode branchingPoint = branchingPoints.pollLast();

      simplifyBranching(branchingPoint, cfa);
    }
  }

  /**
   * Search all branching points in a CFA in post order
   * (any (transitive) predecessor of a node comes before that node in the result).
   * @param root The entry point of the CFA.
   * @return A queue of CFANodes that are branching points, in post order.
   */
  private static Deque<CFANode> findBranchingPoints(final CFANode root, MutableCFA cfa) {

    // at first we check if there is at least one branching with following blank
    // edges, if not we can immediately return an empty list as it is not possible
    // to reduce anything then, later on (when we have found at least one such case)
    // it is enough to have an AssumeEdge or a BlankEdge following, as due to
    // changing one part of the code other part might then also be changeable

    boolean foundAtLeastOneBlankEdgeAssume = false;
    for (CFANode node : cfa.getFunctionNodes(root.getFunctionName())) {
      if (node.getNumLeavingEdges() == 2) {
        CFAEdge edge1 = node.getLeavingEdge(0);
        CFAEdge edge2 = node.getLeavingEdge(1);
        if (edge1 instanceof AssumeEdge && edge2 instanceof AssumeEdge) {
          CFANode succ1 = edge1.getSuccessor();
          CFANode succ2 = edge2.getSuccessor();
          foundAtLeastOneBlankEdgeAssume =
              succ1.getNumLeavingEdges() == 1
                  && succ1.getLeavingEdge(0) instanceof BlankEdge
                  && succ2.getNumLeavingEdges() == 1
                  && succ2.getLeavingEdge(0) instanceof BlankEdge;
        }
      }

      // shortcut, no further search needed
      if (foundAtLeastOneBlankEdgeAssume) {
        break;
      }
    }

    if (!foundAtLeastOneBlankEdgeAssume) {
      return new ArrayDeque<>();
    }

    // The order is important: branching points at the beginning need to come first,
    // (similar to reverse post order).
    // Thus we iterate through the CFA and visit each sucessor only
    // after all its predecessors have been handled.

    final Deque<CFANode> branchingPoints = new ArrayDeque<>();
    final Set<CFANode> visitedNodes = new HashSet<>();
    final Deque<CFANode> waitlist = new ArrayDeque<>();
    waitlist.push(root);

    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.pollLast();
      if (visitedNodes.contains(current)) {
        // loop in CFA
        continue;
      }

      if (visitedNodes.containsAll(predecessorsOf(current).toList())) {
        visitedNodes.add(current);

        if (current.getNumLeavingEdges() > 1) {
          if (current.getNumLeavingEdges() > 2) {
            throw new AssertionError("More than 2 leaving edges on node " + current + " in function " + current.getFunctionName());
          }
          assert CFAUtils.allLeavingEdges(current).allMatch(Predicates.instanceOf(AssumeEdge.class));

          boolean firstNodeQualifies =
              nodeQualifiesForPossibleRemoval(current.getLeavingEdge(0).getSuccessor());
          boolean secondNodeQualifies =
              nodeQualifiesForPossibleRemoval(current.getLeavingEdge(1).getSuccessor());
          if (firstNodeQualifies && secondNodeQualifies) {
            branchingPoints.addLast(current);
          }
        }

        addAll(waitlist, successorsOf(current));
      }
    }
    return branchingPoints;
  }

  private static boolean nodeQualifiesForPossibleRemoval(CFANode node) {
    int numLeavingEdges = node.getNumLeavingEdges();

    if (numLeavingEdges == 1) {
      return node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.BlankEdge;
    } else if (numLeavingEdges == 2) {
      return node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge
          && node.getLeavingEdge(1).getEdgeType() == CFAEdgeType.AssumeEdge;
    }

    return false;
  }

  /**
   * Simplify one branching in the CFA at the given node (if possible).
   * @param branchingPoint The root of the branching (needs to have 2 outgoing AssumeEdges).
   * @param cfa the cfa which should be simplified
   */
  private static void simplifyBranching(final CFANode branchingPoint, final MutableCFA cfa) {
    CFANode leftEndpoint  = findEndOfBlankEdgeChain(branchingPoint.getLeavingEdge(0).getSuccessor());
    CFANode rightEndpoint = findEndOfBlankEdgeChain(branchingPoint.getLeavingEdge(1).getSuccessor());

    if (leftEndpoint.equals(rightEndpoint)) {
      final CFANode endpoint = leftEndpoint;
      final List<FileLocation> removedFileLocations = new ArrayList<>();

      final CFAEdge leftEdge = branchingPoint.getLeavingEdge(0);
      final CFAEdge rightEdge = branchingPoint.getLeavingEdge(1);

      {
        branchingPoint.removeLeavingEdge(leftEdge);
        assert leftEdge instanceof AssumeEdge;
        removedFileLocations.add(leftEdge.getFileLocation());
        CFANode toRemove = leftEdge.getSuccessor();
        toRemove.removeEnteringEdge(leftEdge);
        removeChainOfNodes(toRemove, endpoint, cfa, removedFileLocations);
      }
      {
        branchingPoint.removeLeavingEdge(rightEdge);
        assert rightEdge instanceof AssumeEdge;
        removedFileLocations.add(rightEdge.getFileLocation());
        CFANode toRemove = rightEdge.getSuccessor();
        toRemove.removeEnteringEdge(rightEdge);
        removeChainOfNodes(toRemove, endpoint, cfa, removedFileLocations);
      }

      // Maybe there are more outgoing blank edges from the endpoint,
      // also remove them.
      final CFANode endpoint2 = findEndOfBlankEdgeChain(endpoint);
      removeChainOfNodes(endpoint, endpoint2, cfa, removedFileLocations);

      CFAEdge blankEdge = new BlankEdge("skipped unnecessary edges",
          FileLocation.merge(removedFileLocations), branchingPoint, endpoint2, "skipped unnecessary edges");
      CFACreationUtils.addEdgeUnconditionallyToCFA(blankEdge);
    }
  }

  private static CFANode findEndOfBlankEdgeChain(CFANode current) {
    Set<CFANode> visitedNodes = new HashSet<>();

    while (current.getNumLeavingEdges() == 1
        && current.getLeavingEdge(0) instanceof BlankEdge) {

      if (!visitedNodes.add(current)) {
        return current;
      }

      current = current.getLeavingEdge(0).getSuccessor();
    }
    return current;
  }

  private static void removeChainOfNodes(final CFANode start, final CFANode endpoint,
      final MutableCFA cfa, final List<FileLocation> removedFileLocations) {
    CFANode toRemove = start;

    while (!toRemove.equals(endpoint)) {
      if (toRemove.getNumEnteringEdges() > 0) {
        return;
      }
      assert toRemove.getNumEnteringEdges() == 0;
      assert toRemove.getNumLeavingEdges() == 1;

      CFAEdge leavingEdge = toRemove.getLeavingEdge(0);
      toRemove.removeLeavingEdge(leavingEdge);
      cfa.removeNode(toRemove);

      CFANode nextNode = leavingEdge.getSuccessor();
      nextNode.removeEnteringEdge(leavingEdge);

      assert leavingEdge instanceof BlankEdge;
      removedFileLocations.add(leavingEdge.getFileLocation());

      toRemove = nextNode;
    }
  }
}