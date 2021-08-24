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

/**
 * This class provides functions to alter a given edge by one parameter. The functions return a new
 * instance of the edge that is equal to the once given except for the replaced parameter.
 */
public class EdgeMutator {

  /**
   * Returns a new assume edge with a different expression.
   */
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

  /**
   * Returns a new statement edge with a different expression.
   */
  public static CStatementEdge replaceExpression(
      CStatementEdge originalStatementEdge, CStatement newStatement) {
    return new CStatementEdge(
        newStatement.toASTString(),
        newStatement,
        originalStatementEdge.getFileLocation(),
        originalStatementEdge.getPredecessor(),
        originalStatementEdge.getSuccessor());
  }

  /**
   * Returns a new function call edge with a different function call.
   */
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

  /**
   * Returns a new function summary edge with a different function call.
   */
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

  /**
   * Returns a new function return edge with a different function call.
   */
  public static CFunctionReturnEdge replaceFunctionCall(
      CFunctionReturnEdge functionReturnEdge, CFunctionSummaryEdge functionSummaryEdge) {
    return new CFunctionReturnEdge(
        functionReturnEdge.getFileLocation(),
        functionReturnEdge.getPredecessor(),
        functionReturnEdge.getSuccessor(),
        functionSummaryEdge);
  }

  /**
   * Returns a new return statement edge with a different return expression.
   */
  public static CReturnStatementEdge replaceReturnExpression(
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
