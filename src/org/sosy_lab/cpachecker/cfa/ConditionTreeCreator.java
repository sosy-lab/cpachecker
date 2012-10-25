/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import com.google.common.collect.ImmutableSet;

/**
 * Class which tries to find assumptions in the CFA and
 * replaces them by conditionTrees.
 */
class ConditionTreeCreator extends DefaultCFAVisitor {

  static void createConditionTrees(MutableCFA cfa) {
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(cfa.getMainFunction(),
        new ConditionTreeCreator(cfa));
  }

  private final MutableCFA cfa;

  private ConditionTreeCreator(MutableCFA pCfa) {
    cfa = pCfa;
  }

  @Override
  public TraversalProcess visitNode(final CFANode pNode) {

    if (isAssumptionRoot(pNode)) {

      final CFANode rootNode = pNode;
      CExpression condition = ((CAssumeEdge) rootNode.getLeavingEdge(0)).getExpression();

      // get 2 branches, the then- and the else-branch
      final CFAEdge thenBranch;
      final CFAEdge elseBranch;
      if (((CAssumeEdge) rootNode.getLeavingEdge(0)).getTruthAssumption()) {
        thenBranch = rootNode.getLeavingEdge(0);
        elseBranch = rootNode.getLeavingEdge(1);
      } else {
        thenBranch = rootNode.getLeavingEdge(1);
        elseBranch = rootNode.getLeavingEdge(0);
      }

      final CFANode thenNode = thenBranch.getSuccessor();
      final CFANode elseNode = elseBranch.getSuccessor();

      // remove old edges
      rootNode.removeLeavingEdge(thenBranch);
      rootNode.removeLeavingEdge(elseBranch);

      thenNode.removeEnteringEdge(thenBranch);
      elseNode.removeEnteringEdge(elseBranch);

      // build tree
      buildConditionTree(condition, rootNode, thenNode, elseNode, thenNode, elseNode, true, true);
    }

    return TraversalProcess.CONTINUE;
  }

  private boolean isAssumptionRoot(final CFANode node) {
    boolean result = (node.getNumLeavingEdges() == 2)
        && (node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge);

    if (result) {
      assert node.getLeavingEdge(1).getEdgeType() == CFAEdgeType.AssumeEdge;
      assert node.getLeavingSummaryEdge() == null;
    }

    return result;
  }

  private void buildConditionTree(final CExpression condition,
      final CFANode rootNode, final CFANode thenNode, final CFANode elseNode,
      CFANode thenNodeForLastThen, CFANode elseNodeForLastElse,
      final boolean furtherThenComputation, final boolean furtherElseComputation) {

    // !a, this block avoids nested NOTs like !!a and allows to handle !(a&&b)
    if (condition instanceof CUnaryExpression
        && ((CUnaryExpression) condition).getOperator() == UnaryOperator.NOT) {
      buildConditionTree(((CUnaryExpression) condition).getOperand(), rootNode, elseNode, thenNode,
          elseNodeForLastElse, thenNodeForLastThen, true, true);

      // a && b
    } else if (condition instanceof CBinaryExpression
        && ((CBinaryExpression) condition).getOperator() == BinaryOperator.LOGICAL_AND) {
      final CFANode innerNode = new CFANode(rootNode.getLineNumber(), rootNode.getFunctionName());
      cfa.addNode(innerNode);
      buildConditionTree(((CBinaryExpression) condition).getOperand1(), rootNode, innerNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, false);
      buildConditionTree(((CBinaryExpression) condition).getOperand2(), innerNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);

      // a || b
    } else if (condition instanceof CBinaryExpression
        && ((CBinaryExpression) condition).getOperator() == BinaryOperator.LOGICAL_OR) {
      final CFANode innerNode = new CFANode(rootNode.getLineNumber(), rootNode.getFunctionName());
      cfa.addNode(innerNode);
      buildConditionTree(((CBinaryExpression) condition).getOperand1(), rootNode, thenNode, innerNode,
          thenNodeForLastThen, elseNodeForLastElse, false, true);
      buildConditionTree(((CBinaryExpression) condition).getOperand2(), innerNode, thenNode, elseNode,
          thenNodeForLastThen, elseNodeForLastElse, true, true);

    } else {
      if (furtherThenComputation) {
        thenNodeForLastThen = thenNode;
      }
      if (furtherElseComputation) {
        elseNodeForLastElse = elseNode;
      }

      final CExpression boolCondition = convertToBooleanExpression(condition);
      addConditionEdges(boolCondition, rootNode, thenNodeForLastThen, elseNodeForLastElse);
    }
  }

  /** This method adds 2 edges to the cfa:
   * 1. trueEdge from rootNode to thenNode and
   * 2. falseEdge from rootNode to elseNode. */
  private void addConditionEdges(CExpression condition, CFANode rootNode, CFANode thenNode, CFANode elseNode) {
    // edge connecting condition with thenNode
    final CAssumeEdge trueEdge = new CAssumeEdge(condition.toASTString(),
        rootNode.getLineNumber(), rootNode, thenNode, condition, true);

    rootNode.addLeavingEdge(trueEdge);
    thenNode.addEnteringEdge(trueEdge);

    // edge connecting condition with elseNode
    final CAssumeEdge falseEdge = new CAssumeEdge("!(" + condition.toASTString() + ")",
        rootNode.getLineNumber(), rootNode, elseNode, condition, false);

    rootNode.addLeavingEdge(falseEdge);
    elseNode.addEnteringEdge(falseEdge);
  }

  private static final Set<BinaryOperator> BOOLEAN_BINARY_OPERATORS = ImmutableSet.of(
      BinaryOperator.EQUALS,
      BinaryOperator.NOT_EQUALS,
      BinaryOperator.GREATER_EQUAL,
      BinaryOperator.GREATER_THAN,
      BinaryOperator.LESS_EQUAL,
      BinaryOperator.LESS_THAN,
      BinaryOperator.LOGICAL_AND,
      BinaryOperator.LOGICAL_OR);

  private boolean isBooleanExpression(CExpression exp) {
    if (exp instanceof CBinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(((CBinaryExpression) exp).getOperator());

    } else if (exp instanceof CUnaryExpression &&
        ((CUnaryExpression) exp).getOperator() == UnaryOperator.NOT) {
      throw new AssertionError("UnaryOperator.NOT should not appear here, it should be unwrapped");

    } else {
      return false;
    }
  }

  /** If exp is boolean, exp is returned, else (exp != 0). */
  private CExpression convertToBooleanExpression(CExpression exp) {
    if (isBooleanExpression(exp)) {
      return exp;

    } else {
      final CExpression zero = new CIntegerLiteralExpression(
          exp.getFileLocation(), exp.getExpressionType(), BigInteger.ZERO);
      return new CBinaryExpression(exp.getFileLocation(), exp.getExpressionType(),
          exp, zero, BinaryOperator.NOT_EQUALS);
    }
  }
}
