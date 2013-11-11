/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;


public class CFATransformations {

  /******************************************************************+
   * CFASimplification
   *
   * Using simplifyCFA all Nodes and Edges in Branches (subtree of AssumeEdges)
   * which do not have any effects on the program are deleted.
   */


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
        removeNodesBetween(root, root, left, cfa);
        CFAEdge blankEdge = new BlankEdge("skipped uneccesary edges", root.getLineNumber(), root, endNode, "skipped uneccesary edges");
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
  private static void removeNodesBetween(CFANode from, CFANode actualFrom, CFANode to, MutableCFA cfa) {

    // if actualFrom and From are the same, this is the first call of removeNodesBetween
    // so we know that the from node has two leaving (Assume)Edges
    if (actualFrom == from) {
      CFAEdge left = actualFrom.getLeavingEdge(0);
      CFAEdge right = actualFrom.getLeavingEdge(1);

      actualFrom.removeLeavingEdge(left);
      actualFrom.removeLeavingEdge(right);

      actualFrom = left.getSuccessor();
      actualFrom.removeEnteringEdge(left);
      if (actualFrom != to) {
        cfa.removeNode(actualFrom);
        removeNodesBetween(from, actualFrom, to, cfa);
      }

      actualFrom = right.getSuccessor();
      actualFrom.removeEnteringEdge(right);
      if (actualFrom != to) {
        cfa.removeNode(actualFrom);
        removeNodesBetween(from, actualFrom, to, cfa);
      }

    // if actualFrom and from are not equal, there should actually be exactly
    // one leaving edge, this is handled here
    } else if (actualFrom.getNumLeavingEdges() == 1) {
      CFAEdge edge = actualFrom.getLeavingEdge(0);
      actualFrom.removeLeavingEdge(edge);
      actualFrom = edge.getSuccessor();
      actualFrom.removeEnteringEdge(edge);

      if (actualFrom != to) {
        cfa.removeNode(actualFrom);
        removeNodesBetween(from, actualFrom, to, cfa);
      }

    // more or less than one leaving edge, this should never happen (if the method
    // is called at the right places)
    } else {
      throw new AssertionError(actualFrom.getNumLeavingEdges() + " leaving Edge"
                               + (actualFrom.getNumLeavingEdges() < 1 ? "" : "s")
                               + " where exactly one should be");
    }
  }

  /******************************************************************+
   * NullPointerDetection
   *
   * Using detectNullPointers, before every occurence of *p we insert a test on
   * p == 0 in order to detect null pointers.
   */

  public static void detectNullPointers(MutableCFA cfa, LogManager logger) {

    CBinaryExpressionBuilder binBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    Collection<CFANode> allNodes = cfa.getAllNodes();
    List<CFANode> copiedNodes = new LinkedList<>(allNodes);

    for (CFANode node : copiedNodes) {
      switch (node.getNumLeavingEdges()) {
      case 0:
        break;
      case 1:
        handleEdge(node.getLeavingEdge(0), cfa, binBuilder);
        break;
      case 2:
        handleEdge(node.getLeavingEdge(0), cfa, binBuilder);
        handleEdge(node.getLeavingEdge(1), cfa, binBuilder);
        break;
      default:
        throw new CFAGenerationRuntimeException("Too much leaving Edges on CFANode");
      }
    }
  }

  private static void handleEdge(CFAEdge edge, MutableCFA cfa, CBinaryExpressionBuilder builder) {
    List<CExpression> expList = new ArrayList<>();
    if (edge instanceof CReturnStatementEdge) {
      CExpression returnExp = ((CReturnStatementEdge)edge).getExpression();
      if (returnExp != null) {
        expList = returnExp.accept(new ContainsPointerVisitor());
      }
    } else if (edge instanceof CStatementEdge) {
      CStatement stmt = ((CStatementEdge)edge).getStatement();
      if (stmt instanceof CFunctionCallStatement) {
        expList = ((CFunctionCallStatement)stmt).getFunctionCallExpression().accept(new ContainsPointerVisitor());
      } else if (stmt instanceof CFunctionCallAssignmentStatement) {
        expList = ((CFunctionCallAssignmentStatement)stmt).getFunctionCallExpression().accept(new ContainsPointerVisitor());
      } else if (stmt instanceof CExpressionStatement) {
        expList = ((CExpressionStatement)stmt).getExpression().accept(new ContainsPointerVisitor());
      } else if (stmt instanceof CExpressionAssignmentStatement) {
        expList = ((CExpressionAssignmentStatement)stmt).getRightHandSide().accept(new ContainsPointerVisitor());
        expList.addAll(((CExpressionAssignmentStatement)stmt).getLeftHandSide().accept(new ContainsPointerVisitor()));
      }
    }

    for (CExpression exp : expList) {
      edge = insertNullPointerCheck(edge, exp, cfa, builder);
    }
  }

  private static CFAEdge insertNullPointerCheck(CFAEdge edge, CExpression exp, MutableCFA cfa, CBinaryExpressionBuilder binBuilder) {
    CFANode predecessor = edge.getPredecessor();
    CFANode successor = edge.getSuccessor();
    predecessor.removeLeavingEdge(edge);
    successor.removeEnteringEdge(edge);

    CFANode trueNode = new CFANode(edge.getLineNumber(), predecessor.getFunctionName());
    CFANode falseNode = new CFANode(edge.getLineNumber(), predecessor.getFunctionName());
    AssumeEdge trueEdge = new CAssumeEdge(edge.getRawStatement(),
                                         edge.getLineNumber(),
                                         predecessor, trueNode,
                                         binBuilder.buildBinaryExpression(exp, new CIntegerLiteralExpression(exp.getFileLocation(), CNumericTypes.INT,BigInteger.valueOf(0)), BinaryOperator.EQUALS),
                                         true);
    AssumeEdge falseEdge = new CAssumeEdge(edge.getRawStatement(),
                                           edge.getLineNumber(),
                                           predecessor, falseNode,
                                           binBuilder.buildBinaryExpression(exp, new CIntegerLiteralExpression(exp.getFileLocation(), CNumericTypes.INT,BigInteger.valueOf(0)), BinaryOperator.NOT_EQUALS),
                                           false);
    predecessor.addLeavingEdge(trueEdge);
    predecessor.addLeavingEdge(falseEdge);
    trueNode.addEnteringEdge(trueEdge);
    falseNode.addEnteringEdge(falseEdge);

    CFAEdge newEdge = createOldEdgeWithNewNodes(falseNode, successor, edge);
    falseNode.addLeavingEdge(newEdge);
    successor.addEnteringEdge(newEdge);

    CFANode endNode = new CFANode(edge.getLineNumber(), predecessor.getFunctionName());
    BlankEdge endEdge = new BlankEdge("null-deref", edge.getLineNumber(), trueNode, endNode, "null-deref");
    trueNode.addLeavingEdge(endEdge);
    endNode.addEnteringEdge(endEdge);

    cfa.addNode(trueNode);
    cfa.addNode(falseNode);
    cfa.addNode(endNode);

    return newEdge;
  }

  private static CFAEdge createOldEdgeWithNewNodes(CFANode predecessor, CFANode successor, CFAEdge edge) {
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      return new CAssumeEdge(edge.getRawStatement(), edge.getLineNumber(),
                             predecessor, successor, ((CAssumeEdge)edge).getExpression(),
                             ((CAssumeEdge)edge).getTruthAssumption());
    case CallToReturnEdge:
      assert(false);
      break;
    case ReturnStatementEdge:
      return new CReturnStatementEdge(edge.getRawStatement(),
                                      ((CReturnStatementEdge)edge).getRawAST().get(),
                                      edge.getLineNumber(), predecessor,
                                      ((CReturnStatementEdge)edge).getSuccessor());
    case StatementEdge:
      return new CStatementEdge(edge.getRawStatement(), ((CStatementEdge)edge).getStatement(),
                                edge.getLineNumber(), predecessor, successor);
    }
    throw new CFAGenerationRuntimeException("more edge types valid than expected, more work to do here");
  }


  /**
   * This visitor returns all Expressions where a Pointer is included
   */
  static class ContainsPointerVisitor extends DefaultCExpressionVisitor<List<CExpression>, CFAGenerationRuntimeException>
                                      implements CRightHandSideVisitor<List<CExpression>, CFAGenerationRuntimeException> {

    @Override
    public List<CExpression> visit(CFunctionCallExpression pIastFunctionCallExpression) {
      return new ArrayList<>();
    }

    @Override
    public List<CExpression> visit(CArraySubscriptExpression e) {
      List<CExpression> exps = e.getArrayExpression().accept(this);
      exps.addAll(e.getSubscriptExpression().accept(this));
      return exps;
    }
    @Override
    public List<CExpression> visit(CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public List<CExpression> visit(CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public List<CExpression> visit(CFieldReference e) {
      List<CExpression> exps = new ArrayList<>();
      if (e.isPointerDereference()) {
        exps.add(e.getFieldOwner());
      }
      return exps;
    }

    @Override
    public List<CExpression> visit(CUnaryExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public List<CExpression> visit(CPointerExpression e) {
      List<CExpression> exps = new ArrayList<>();
      exps.add(e.getOperand());
      exps.addAll(e.getOperand().accept(this));
      return exps;
    }

    @Override
    public List<CExpression> visit(CBinaryExpression pIastbBinaryExpression) {
      List<CExpression> exps = pIastbBinaryExpression.getOperand1().accept(this);
      exps.addAll(pIastbBinaryExpression.getOperand2().accept(this));
      return exps;
    }

    @Override
    protected List<CExpression> visitDefault(CExpression pExp) throws CFAGenerationRuntimeException {
      return new ArrayList<>();
    }

  }
}