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
package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.CFAGenerationRuntimeException;


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
      simplifyFunction(root, cfa, new HashSet<CFANode>());
    }
  }

  /**
   * Helper Method which only gets called by #simplifyFunction in several places
   */
  private static CFANode simplifyFunctionHelp(CFANode root, Set<CFANode> visitedNodes) {
    while (root.getNumLeavingEdges() == 1
        && (root.getLeavingEdge(0) instanceof BlankEdge)
        && root.getLeavingEdge(0).getSuccessor().getNumEnteringEdges() == 1) {

       if (!visitedNodes.add(root)) {
         return null;
       }

       root = root.getLeavingEdge(0).getSuccessor();
    }
    return root;
  }

  /**
   * This method makes the simplification step for a single function, the
   * root node is the node where the search for possible simplifications starts.
   *
   * @param root
   * @param cfa The cfa where the simplifications should be applied
   * @param visitedNodes a set of all visitedNodes, so we do not revisit them
   *                     in case of loops / recursion
   * @return The return value is only needed for the recursion, the returned
   *          CFANode is the node where the search for simplifications should
   *          continue after visiting AssumeEdges
   */
  private static CFANode simplifyFunction(CFANode root, MutableCFA cfa, Set<CFANode> visitedNodes) {

    if (root == null) {
      return null;
    }

    // differentiate by the number of leaving edges, when there is none, we
    // arrived at a point where the method ends
    // if there is one, we can progress normally
    // if there are two leaving edges this means we got to AssumeEdges, here
    // we have to evaluate the two subtrees and then decide if something can
    // get simplified
    switch (root.getNumLeavingEdges()) {

    // no further edges, nothing more to do
    case 0:
      return null;

    // one leaving edge, restart simplification from this node
    case 1:
      if (root.getNumEnteringEdges() > 1) {
        return root;
      }

      // simplify all blankedges iteratively
      root = simplifyFunctionHelp(root, visitedNodes);

      // no next node, so we can skip simplification here
      if (root == null) {
        return null;
      }

      // more than one leaving edge => simplify this first
      if (root.getNumLeavingEdges() != 1) {
        return simplifyFunction(root, cfa, visitedNodes);
      }

      // from here we know theres only one leaving edge
      // if this edge is a blankedge, we can simplificate the successor of this edge further
      // if it is not, we have to restart simplification from the successor, before there
      // are no simplifications possible
      if (root.getLeavingEdge(0) instanceof BlankEdge) {

        // if the node was already visited, we are in a loop and skip simplification
        if (visitedNodes.add(root)) {
          return root.getLeavingEdge(0).getSuccessor();
        } else {
          return null;
        }

        // no blankEdge, so we have to restart the simplification
      } else {
        if (!visitedNodes.add(root)) {
          return null;
        }

        // some improvements in order to make the method not too recursive
        root = root.getLeavingEdge(0).getSuccessor();
        if (root.getNumLeavingEdges() == 1 && root.getNumEnteringEdges() == 1) {
          root = simplifyFunctionHelp(root, visitedNodes);
        }
        while (root.getNumLeavingEdges() == 1 && root.getNumEnteringEdges() == 1 && !(root.getLeavingEdge(0) instanceof BlankEdge)) {

          // if the node was already visited, we are in a loop and skip simplification
          if (visitedNodes.add(root)) {
            root = root.getLeavingEdge(0).getSuccessor();
          } else {
            return null;
          }
        }

        // restarting the simplification is done from here
        simplifyFunction(root, cfa, visitedNodes);

        // we cannot simplify the cfa above the root node, so return null
        return null;
      }

    // these are assume Edges which can eventually be simplified
    case 2:

      // if the node was already visited, we are in a loop and skip simplification
      if (!visitedNodes.add(root)) {
        return null;
      }

      // simplify the left subtree
      CFANode left = root.getLeavingEdge(0).getSuccessor();
      if (left.getNumLeavingEdges() == 1 && left.getNumEnteringEdges() == 1) {
        left = simplifyFunctionHelp(left, visitedNodes);
      }
      left = simplifyFunction(left, cfa, visitedNodes);

      // simplify the right subtree
      CFANode right = root.getLeavingEdge(1).getSuccessor();
      if (right.getNumLeavingEdges() == 1 && right.getNumEnteringEdges() == 1) {
        right = simplifyFunctionHelp(right, visitedNodes);
      }
      right = simplifyFunction(right, cfa, visitedNodes);

      // if left and right are equal, and not null the cfa can be simplificated
      // so the edges and nodes between the root node and left/right are removed
      // and a new blankedge is introduced
      if (left == right && left != null) {
        CFANode endNode = left;
        List<FileLocation> removedFileLocations = new ArrayList<>();
        removeNodesBetween(root, root, left, cfa, removedFileLocations);
        CFAEdge blankEdge = new BlankEdge("skipped uneccesary edges",
            FileLocation.merge(removedFileLocations), root, endNode, "skipped uneccesary edges");
        root.addLeavingEdge(blankEdge);
        endNode.addEnteringEdge(blankEdge);

        if (endNode.getNumLeavingEdges() == 1 && endNode.getNumEnteringEdges() == 1) {
          endNode = simplifyFunctionHelp(endNode, visitedNodes);
        }

        // furter simplifications are applied
        return simplifyFunction(endNode, cfa, visitedNodes);
      }

      // left and right are not equal, so we cannot simplificate anything, return null
      return null;

    // more than 2 leaving edges should not occur
    default:
      throw new CFAGenerationRuntimeException("More than 2 leaving edges on node " + root + " in function " + root.getFunctionName());
    }
  }

  /**
   * This method removes all nodes and edges between the from and the to node, and also
   * deletes them from the cfa.
   *
   * @param from the CFANode where the deletion of further nodes should start
   * @param actualFrom the same node as the from node!
   * @param to the CFANode where the deletion should stop
   * @param cfa The cfa where the nodes have to be deleted, too
   */
  private static void removeNodesBetween(CFANode from, CFANode actualFrom, CFANode to,
      MutableCFA cfa, List<FileLocation> removedFileLocations) {

    // if actualFrom and From are the same, this is the first call of removeNodesBetween
    // so we know that the from node has two leaving (Assume)Edges
    if (actualFrom == from) {
      CFAEdge left = actualFrom.getLeavingEdge(0);
      CFAEdge right = actualFrom.getLeavingEdge(1);

      actualFrom.removeLeavingEdge(left);
      actualFrom.removeLeavingEdge(right);
      removedFileLocations.add(left.getFileLocation());
      removedFileLocations.add(right.getFileLocation());

      actualFrom = left.getSuccessor();
      actualFrom.removeEnteringEdge(left);
      if (actualFrom != to) {
        if (actualFrom.getNumEnteringEdges() > 0) {
          for (int i = 0;  i < actualFrom.getNumEnteringEdges(); i++) {
            moveEdgeToOtherSuccessor(actualFrom.getEnteringEdge(i), to);
          }
        }
        cfa.removeNode(actualFrom);
        removeNodesBetween(from, actualFrom, to, cfa, removedFileLocations);
      }

      actualFrom = right.getSuccessor();
      actualFrom.removeEnteringEdge(right);
      if (actualFrom != to && actualFrom.getNumEnteringEdges() == 0) {
        if (actualFrom.getNumEnteringEdges() > 0) {
          for (int i = 0;  i < actualFrom.getNumEnteringEdges(); i++) {
            moveEdgeToOtherSuccessor(actualFrom.getEnteringEdge(i), to);
          }
        }
        cfa.removeNode(actualFrom);
        removeNodesBetween(from, actualFrom, to, cfa, removedFileLocations);
      }

    // if actualFrom and from are not equal, there should actually be exactly
    // one leaving edge, this is handled here
    } else if (actualFrom.getNumLeavingEdges() == 1) {
      CFAEdge edge = actualFrom.getLeavingEdge(0);
      actualFrom.removeLeavingEdge(edge);
      actualFrom = edge.getSuccessor();
      actualFrom.removeEnteringEdge(edge);
      removedFileLocations.add(edge.getFileLocation());

      if (actualFrom != to) {
        cfa.removeNode(actualFrom);
        removeNodesBetween(from, actualFrom, to, cfa, removedFileLocations);
      }

    // more or less than one leaving edge, this should never happen (if the method
    // is called at the right places)
    } else {
      throw new AssertionError(actualFrom.getNumLeavingEdges() + " leaving Edge"
                               + (actualFrom.getNumLeavingEdges() < 1 ? "" : "s")
                               + " where exactly one should be");
    }
  }

  private static CFAEdge moveEdgeToOtherSuccessor(CFAEdge edge, CFANode succ) {
    CFANode pred = edge.getPredecessor();
    pred.removeLeavingEdge(edge);
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      edge = new CAssumeEdge(((CAssumeEdge)edge).getRawStatement(),
                             edge.getFileLocation(),
                             pred,
                             succ,
                             ((CAssumeEdge)edge).getExpression(),
                             ((CAssumeEdge)edge).getTruthAssumption());
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return edge;
    case BlankEdge:
      edge = new BlankEdge(((BlankEdge)edge).getRawStatement(),
                            edge.getFileLocation(),
                            pred,
                            succ,
                            ((BlankEdge)edge).getDescription());
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return edge;
    case DeclarationEdge:
      edge = new CDeclarationEdge(((CDeclarationEdge)edge).getRawStatement(),
                                  edge.getFileLocation(),
                                  pred,
                                  succ,
                                  ((CDeclarationEdge)edge).getDeclaration());
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return edge;
    case ReturnStatementEdge:
      edge = new CReturnStatementEdge(((CReturnStatementEdge)edge).getRawStatement(),
                                      ((CReturnStatementEdge)edge).getRawAST().orNull(),
                                      edge.getFileLocation(),
                                      pred,
                                      (FunctionExitNode) succ);
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return edge;
    case StatementEdge:
      edge = new CStatementEdge(((CStatementEdge)edge).getRawStatement(),
                                ((CStatementEdge)edge).getStatement(),
                                edge.getFileLocation(),
                                pred,
                                succ);
      pred.addLeavingEdge(edge);
      succ.addEnteringEdge(edge);
      return edge;
    case CallToReturnEdge:
    case FunctionReturnEdge:
    case MultiEdge:
    default:
      throw new AssertionError("should never happen");
    }
  }
}