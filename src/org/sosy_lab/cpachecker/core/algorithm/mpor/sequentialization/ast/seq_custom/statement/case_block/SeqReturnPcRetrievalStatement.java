// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;

/**
 * Represents a {@code return_pc} retrieval, i.e. assigning the {@code return_pc} to the current
 * threads {@code pc}.
 *
 * <p>E.g. {@code pc[{thread_id}] = __return_pc_{thread_id}_{func_name};}
 */
public class SeqReturnPcRetrievalStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement assign;

  public SeqReturnPcRetrievalStatement(CExpressionAssignmentStatement pAssign) {
    assign = pAssign;
  }

  @Override
  public String toASTString() {
    return assign.toASTString();
  }
}
