// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

public abstract sealed class CLoopStatement implements CExportStatement
    permits CForLoopStatement, CWhileLoopStatement {

  /**
   * The condition for the control flow to enter the loop. E.g., {@code i < N} in {@code for (int i
   * = 0; i < N; i++)} or {@code 1} in {@code while (1)}.
   */
  private final CExportExpression condition;

  /**
   * The list of statements inside the loop. Can be empty, e.g., to simulate an infinite loop that
   * does nothing.
   */
  private final CCompoundStatement body;

  CLoopStatement(CExportExpression pCondition, CCompoundStatement pBody) {
    condition = pCondition;
    body = pBody;
  }

  public CExportExpression getCondition() {
    return condition;
  }

  public CCompoundStatement getBody() {
    return body;
  }
}
