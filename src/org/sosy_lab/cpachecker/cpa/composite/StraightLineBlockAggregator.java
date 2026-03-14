// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibStatementEdge;

class StraightLineBlockAggregator implements BasicBlockAggregator {

  private final CFA cfa;

  StraightLineBlockAggregator(CFA pCfa) {
    cfa = pCfa;
  }

  @Override
  public boolean isValidMultiEdgeStart(CFANode node) {
    return node.getNumLeavingEdges() == 1 // linear chain of edges
        && node.getLeavingSummaryEdge() == null // without a functioncall
        && node.getNumEnteringEdges() > 0; // without a functionstart
  }

  @Override
  public boolean isValidMultiEdgeComponent(CFANode startNode, CFAEdge edge) {
    boolean result =
        edge.getEdgeType() == CFAEdgeType.BlankEdge
            || edge.getEdgeType() == CFAEdgeType.DeclarationEdge
            || edge.getEdgeType() == CFAEdgeType.StatementEdge
            || edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge;

    CFANode nodeAfterEdge = edge.getSuccessor();

    result =
        result
            && nodeAfterEdge.getNumEnteringEdges() == 1
            && nodeAfterEdge.getClass() == CFANode.class;

    return result && !containsFunctionCall(edge);
  }

  /**
   * This method checks, if the given (statement) edge contains a function call directly or via a
   * function pointer.
   *
   * @param edge the edge to inspect
   * @return whether this edge contains a function call or not.
   */
  private boolean containsFunctionCall(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      if (edge instanceof CStatementEdge statementEdge) {
        if ((statementEdge.getStatement() instanceof CFunctionCall call)) {
          CSimpleDeclaration declaration = call.getFunctionCallExpression().getDeclaration();

          // declaration == null -> functionPointer
          // functionName exists in CFA -> functioncall with CFA for called function
          // otherwise: call of non-existent function, example: nondet_int() -> ignore this case
          return declaration == null
              || cfa.getAllFunctionNames().contains(declaration.getQualifiedName());
        }
        return (statementEdge.getStatement() instanceof CFunctionCall);
      } else if (edge instanceof SvLibStatementEdge pSvLibStatementEdge) {
        return pSvLibStatementEdge.getStatement() instanceof SvLibFunctionCallAssignmentStatement;
      } else {
        throw new UnsupportedOperationException("Unknown statement edge type: " + edge.getClass());
      }
    }
    return false;
  }
}
