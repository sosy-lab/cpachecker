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

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

/**
 * Class which tries to find all sequences of simple edges in the CFA and
 * replaces them by {@link MultiEdge}s.
 */
class MultiEdgeCreator extends DefaultCFAVisitor {

  static void createMultiEdges(MutableCFA cfa) {
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(cfa.getMainFunction(),
                                                         new MultiEdgeCreator(cfa));
  }

  private final MutableCFA cfa;

  private MultiEdgeCreator(MutableCFA pCfa) {
    cfa = pCfa;
  }

  @Override
  public TraversalProcess visitNode(final CFANode pNode) {

    if (nodeQualifiesAsStartNode(pNode)) {
      List<CFAEdge> edges = new ArrayList<>();
      Set<CFANode> nodes = new HashSet<>();

      CFANode node = pNode;
      do {
        CFAEdge edge = node.getLeavingEdge(0);

        if (!edgeQualifies(edge)) {
          break;
        }

        edges.add(edge);

        nodes.add(edge.getPredecessor());
        nodes.add(edge.getSuccessor());

        node = edge.getSuccessor();
      } while (nodeQualifies(node));

      if (edges.size() > 1) {
        CFAEdge firstEdge = edges.get(0);
        CFANode firstNode = firstEdge.getPredecessor();
        assert firstNode == pNode;
        CFAEdge lastEdge = edges.get(edges.size()-1);
        CFANode lastNode = lastEdge.getSuccessor();

        // remove old edges
        firstNode.removeLeavingEdge(firstEdge);
        lastNode.removeEnteringEdge(lastEdge);

        // add new edges
        MultiEdge newEdge = new MultiEdge(firstNode, lastNode, edges);
        firstNode.addLeavingEdge(newEdge);
        lastNode.addEnteringEdge(newEdge);

        // remove now unreachable nodes
        nodes.remove(firstNode);
        nodes.remove(lastNode);
        assert !nodes.isEmpty();
        for (CFANode middleNode : nodes) {
          cfa.removeNode(middleNode);
        }
      }
    }

    return TraversalProcess.CONTINUE;
  }

  private boolean nodeQualifiesAsStartNode(CFANode node) {
    return node.getNumLeavingEdges() == 1
        && node.getLeavingSummaryEdge() == null;
  }

  private boolean nodeQualifies(CFANode node) {
    return node.getNumLeavingEdges() == 1
        && node.getNumEnteringEdges() == 1
        && node.getLeavingSummaryEdge() == null
        && !node.isLoopStart()
        && node.getClass() == CFANode.class;
  }

  private boolean edgeQualifies(CFAEdge edge) {
    boolean result = edge.getEdgeType() == CFAEdgeType.BlankEdge
        || edge.getEdgeType() == CFAEdgeType.DeclarationEdge
        || edge.getEdgeType() == CFAEdgeType.StatementEdge
        || edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge;

    return result && !containsFunctionPointerCall(edge);
  }

  /**
   * This method checks, if the given (statement) edge contains a function call via a function pointer.
   *
   * @param edge the edge to inspect
   * @return whether or not this edge contains a function call or not.
   */
  private boolean containsFunctionPointerCall(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge)edge;

      if ((statementEdge.getStatement() instanceof CFunctionCall)) {
        CFunctionCall call = ((CFunctionCall)statementEdge.getStatement());
        CSimpleDeclaration declaration = call.getFunctionCallExpression().getDeclaration();

        if (declaration == null) {
          return true;
        }
      }
      // simplest (close-to-optimal) solution, that would split at any function call
      //return (statementEdge.getStatement() instanceof CFunctionCall);
    }
    return false;
  }
}
