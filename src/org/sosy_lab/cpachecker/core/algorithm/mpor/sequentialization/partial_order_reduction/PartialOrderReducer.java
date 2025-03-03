// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;

@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class PartialOrderReducer {

  public static SeqCaseClause concatenateClauses(SeqCaseClause pPrefix, SeqCaseClause pSuffix) {
    for (SeqCaseBlockStatement statement : pPrefix.block.statements) {
      Optional<Integer> targetPc = SeqCaseClauseUtil.tryExtractIntTargetPc(statement);
      if (targetPc.isPresent()) {
        int pc = targetPc.orElseThrow();
        if (pc == pSuffix.label.value) {
          return cloneWithPcWriteReplacement(pPrefix, pSuffix, pc);
        }
      }
    }
    throw new AssertionError(
        "could not concatenate "
            + pPrefix.toASTString()
            + " and "
            + pSuffix.toASTString()
            + ". does pPrefix target pSuffix?");
  }

  /** Replaces the {@code pc} write to {@code pPc} with the {@link String} representation of */
  private static SeqCaseClause cloneWithPcWriteReplacement(
      SeqCaseClause pPrefix, SeqCaseClause pReplacement, int pPc) {
    // TODO make use of statement cloning here
    if (pPc == 0) {
      return pPrefix;
    } else {
      return pReplacement;
    }
  }

  /** Ensures that {@code pPrefix} and {@code pSuffix} can be concatenated. */
  private static boolean validPrefixAndSuffix(SeqCaseClause pPrefix, SeqCaseClause pSuffix) {
    // TODO what about .alwaysWritesPc() here?
    // TODO make some tests with function entries / exits. though it should be fine to clone them
    if (pPrefix.equals(pSuffix)) {
      return false;
    } else if (pSuffix.isLoopStart) {
      // never concatenate loop starts so that they remain directly reachable
      return false;
    }
    // prefix must have a direct target pc to suffix label pc
    return SeqCaseClauseUtil.collectIntegerTargetPc(pPrefix).contains(pSuffix.label.value);
  }
}
