// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
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

  /**
   * Returns the set of {@code int} target pc in {@code pCaseClause}. Not factored in are {@link
   * CIdExpression}s, e.g. {@code RETURN_PC} variables. Note that it is possible after POR that a
   * {@link SeqCaseClause} targets the same {@code pc} multiple times in different branches.
   */
  public static ImmutableSet<Integer> collectIntegerTargetPc(SeqCaseClause pCaseClause) {
    ImmutableSet.Builder<Integer> rIntTargetPc = ImmutableSet.builder();
    for (SeqCaseBlockStatement statement : pCaseClause.block.statements) {
      Optional<Integer> targetPc = tryExtractIntTargetPc(statement);
      if (targetPc.isPresent()) {
        rIntTargetPc.add(targetPc.orElseThrow());
      }
    }
    return rIntTargetPc.build();
  }

  /**
   * Tries to extract the {@code int} target {@code pc} from {@code pStatement}. If the target
   * {@code pc} is a {@link CIdExpression} ({@code RETURN_PC}), returns {@link Optional#empty()}.
   */
  public static Optional<Integer> tryExtractIntTargetPc(SeqCaseBlockStatement pStatement) {
    Optional<CExpression> targetPcExpression = pStatement.getTargetPcExpression();
    if (targetPcExpression.isPresent()) {
      if (targetPcExpression.orElseThrow() instanceof CIntegerLiteralExpression intExpression) {
        return Optional.of(intExpression.getValue().intValue());
      }
    } else if (pStatement.getTargetPc().isPresent()) {
      return Optional.of(pStatement.getTargetPc().orElseThrow());
    }
    Verify.verify(
        targetPcExpression.orElseThrow() instanceof CIdExpression,
        "if no int is found, then target pc expression must be CIdExpression");
    return Optional.empty();
  }
}
