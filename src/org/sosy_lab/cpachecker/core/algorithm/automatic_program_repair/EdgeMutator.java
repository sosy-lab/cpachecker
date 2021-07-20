// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

public class EdgeMutator {

  public static CAssumeEdge replaceExpression(
      CAssumeEdge originalAssumeEdge, CExpression newExpression) {
    return new CAssumeEdge(
        newExpression.toASTString(),
        originalAssumeEdge.getFileLocation(),
        originalAssumeEdge.getPredecessor(),
        originalAssumeEdge.getSuccessor(),
        newExpression,
        originalAssumeEdge.getTruthAssumption());
  }

  public static CStatementEdge replaceExpression(
      CStatementEdge originalStatementEdge, CStatement newStatement) {
    return new CStatementEdge(
        newStatement.toASTString(),
        newStatement,
        originalStatementEdge.getFileLocation(),
        originalStatementEdge.getPredecessor(),
        originalStatementEdge.getSuccessor());
  }

  public static CFunctionCallEdge replaceFunctionCall(
      CFunctionCallEdge functionCallEdge,
      CFunctionSummaryEdge summaryEdge,
      CFunctionCall newFunctionCall) {
    return new CFunctionCallEdge(
        functionCallEdge.getRawStatement(),
        functionCallEdge.getFileLocation(),
        functionCallEdge.getPredecessor(),
        functionCallEdge.getSuccessor(),
        newFunctionCall,
        summaryEdge);
  }

  public static CFunctionSummaryEdge replaceFunctionCall(
      CFunctionSummaryEdge summaryEdge, CFunctionCall newFunctionCall) {
    return new CFunctionSummaryEdge(
        newFunctionCall.toASTString(),
        summaryEdge.getFileLocation(),
        summaryEdge.getPredecessor(),
        summaryEdge.getSuccessor(),
        newFunctionCall,
        summaryEdge.getFunctionEntry());
  }

  public static CFunctionReturnEdge replaceFunctionCall(
      CFunctionReturnEdge functionReturnEdge, CFunctionSummaryEdge functionSummaryEdge) {
    return new CFunctionReturnEdge(
        functionReturnEdge.getFileLocation(),
        functionReturnEdge.getPredecessor(),
        functionReturnEdge.getSuccessor(),
        functionSummaryEdge);
  }

  public static CReturnStatementEdge replaceAssignment(
      CReturnStatementEdge originalReturnStatementEdge,
      CReturnStatement originalReturnStatement,
      CAssignment assignment) {
    return new CReturnStatementEdge(
        originalReturnStatementEdge.getRawStatement(),
        StatementMutator.replaceAssignment(originalReturnStatement, assignment),
        originalReturnStatementEdge.getFileLocation(),
        originalReturnStatementEdge.getPredecessor(),
        originalReturnStatementEdge.getSuccessor());
  }
}
