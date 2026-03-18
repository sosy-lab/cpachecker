// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

/**
 * Class for static functions that create or copy CFAEdges for program transformations.
 */
public class ProgramTransformationCFAEdgeCreator {

  /**
   * Copy a CFAEdge with new predecessor and successor nodes. Only for CCFAEdges.
   *
   * @param pCFAEdge the CFAEdge to be copied
   * @param pNewPredecessor the new predecessor CFANode
   * @param pNewSuccessor the new successor CFANode
   * @return CFAEdge
   */
  public static CFAEdge copyCFAEdge(CFAEdge pCFAEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {
    if (! (pCFAEdge instanceof CCfaEdge)) {
      return null;
    }
    return switch (pCFAEdge) {
      case BlankEdge edge ->
          new BlankEdge(
              edge.getRawStatement(),
              FileLocation.DUMMY,
              pNewPredecessor,
              pNewSuccessor,
              edge.getDescription());
      case CAssumeEdge edge ->
          new CAssumeEdge(
              edge.getRawStatement(),
              FileLocation.DUMMY,
              pNewPredecessor,
              pNewSuccessor,
              edge.getExpression(),
              edge.getTruthAssumption());
      case CDeclarationEdge edge ->
          new CDeclarationEdge(
              edge.getRawStatement(),
              FileLocation.DUMMY,
              pNewPredecessor,
              pNewSuccessor,
              edge.getDeclaration());
      case CFunctionCallEdge edge ->
          new CFunctionCallEdge(
              edge.getRawStatement(),
              FileLocation.DUMMY,
              pNewPredecessor,
              edge.getSuccessor(),
              edge.getFunctionCall(),
              edge.getSummaryEdge());
      //case CFunctionReturnEdge edge -> null;
      case CFunctionSummaryEdge edge ->
          new CFunctionSummaryEdge(
              edge.getRawStatement(),
              FileLocation.DUMMY,
              pNewPredecessor,
              pNewSuccessor,
              edge.getExpression(),
              edge.getFunctionEntry());
      case CReturnStatementEdge edge ->
          new CReturnStatementEdge(
              edge.getRawStatement(),
              edge.getReturnStatement(),
              FileLocation.DUMMY,
              pNewPredecessor,
              (FunctionExitNode) pNewSuccessor);
      case CStatementEdge edge ->
          new CStatementEdge(
              edge.getRawStatement(), edge.getStatement(), null, pNewPredecessor, pNewSuccessor);
      default -> null;
    };
  }
}
