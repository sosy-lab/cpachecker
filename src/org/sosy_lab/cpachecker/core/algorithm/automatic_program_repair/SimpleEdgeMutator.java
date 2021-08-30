// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

public class SimpleEdgeMutator extends EdgeMutator {
  CFAEdge edgeToMutate;

  public SimpleEdgeMutator(CFA cfa, CFAEdge pOriginalEdge) {
    super(cfa);
    edgeToMutate = CorrespondingEdgeProvider.findCorrespondingEdge(pOriginalEdge, getClonedCFA());
  }

  /**
   * Returns a new assume edge with a different expression.
   */
  public CAssumeEdge replaceExpressionInAssumeEdge(CExpression newExpression) {
    CAssumeEdge originalAssumeEdge = (CAssumeEdge) edgeToMutate;
    return new CAssumeEdge(
        newExpression.toASTString(),
        originalAssumeEdge.getFileLocation(),
        originalAssumeEdge.getPredecessor(),
        originalAssumeEdge.getSuccessor(),
        newExpression,
        originalAssumeEdge.getTruthAssumption());
  }

  /**
   * Returns a new statement edge with a different expression.
   */
  public CStatementEdge replaceStatementInStatementEdge(
      CStatement newStatement) {
    CStatementEdge originalStatementEdge = (CStatementEdge) edgeToMutate;
    return new CStatementEdge(
        newStatement.toASTString(),
        newStatement,
        originalStatementEdge.getFileLocation(),
        originalStatementEdge.getPredecessor(),
        originalStatementEdge.getSuccessor());
  }


  /**
   * Returns a new return statement edge with a different return expression.
   */
  public CReturnStatementEdge replaceReturnExpressionInReturnStatementEdge(
      CReturnStatement originalReturnStatement,
      CAssignment assignment) {
    CReturnStatementEdge originalReturnStatementEdge = (CReturnStatementEdge) edgeToMutate;

    return new CReturnStatementEdge(
        originalReturnStatementEdge.getRawStatement(),
        StatementMutator.replaceAssignment(originalReturnStatement, assignment),
        originalReturnStatementEdge.getFileLocation(),
        originalReturnStatementEdge.getPredecessor(),
        originalReturnStatementEdge.getSuccessor());
  }
}
