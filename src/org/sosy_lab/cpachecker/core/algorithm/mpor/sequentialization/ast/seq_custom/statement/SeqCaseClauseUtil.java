// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;

public class SeqCaseClauseUtil {

  /**
   * From {@code pCaseClauses} extracts all statements that are instances of {@code
   * pStatementClass}.
   */
  public static <T extends SeqCaseBlockStatement> ImmutableSet<T> getStatementsByClass(
      ImmutableList<SeqCaseClause> pCaseClauses, Class<T> pStatementClass) {

    ImmutableSet.Builder<T> rStatements = ImmutableSet.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        if (pStatementClass.isInstance(statement)) {
          rStatements.add(pStatementClass.cast(statement));
        }
      }
    }
    return rStatements.build();
  }
}
