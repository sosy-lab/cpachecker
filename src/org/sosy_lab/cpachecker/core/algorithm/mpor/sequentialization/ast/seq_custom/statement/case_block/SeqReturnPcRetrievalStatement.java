// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;

/**
 * Represents a {@code return_pc} retrieval, i.e. assigning the {@code return_pc} to the current
 * threads {@code pc}.
 *
 * <p>E.g. {@code pc[{thread_id}] = __return_pc_{thread_id}_{func_name};}
 */
public class SeqReturnPcRetrievalStatement implements SeqCaseBlockStatement {

  public final int threadId;

  public final CIdExpression returnPcVar;

  public SeqReturnPcRetrievalStatement(int pThreadId, CIdExpression pReturnPcVar) {
    threadId = pThreadId;
    returnPcVar = pReturnPcVar;
  }

  @Override
  public String toASTString() {
    CLeftHandSide pc = SeqExpressions.getPcExpression(threadId);
    CExpressionAssignmentStatement assignment = SeqExpressions.buildExprAssignStmt(pc, returnPcVar);
    return assignment.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.empty();
  }

  @NonNull
  @Override
  public SeqReturnPcRetrievalStatement cloneWithTargetPc(int pTargetPc) {
    throw new UnsupportedOperationException("SeqReturnPcRetrievalStatement do not have targetPcs");
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
