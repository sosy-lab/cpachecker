// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class SeqThreadStatementUtil {

  /**
   * Returns {@code true} if {@code pCurrentStatement} starts inside an atomic block, but does not
   * actually start it.
   */
  public static boolean startsInAtomicBlock(SeqThreadStatement pStatement) {
    for (SubstituteEdge substituteEdge : pStatement.getSubstituteEdges()) {
      ThreadEdge threadEdge = substituteEdge.threadEdge;
      // use the predecessor, since we require information about this statement
      if (threadEdge.getPredecessor().isInAtomicBlock) {
        return true;
      }
    }
    return false;
  }

  public static boolean anySynchronizesThreads(ImmutableList<SeqThreadStatement> pStatements) {
    for (SeqThreadStatement statement : pStatements) {
      if (statement.synchronizesThreads()) {
        return true;
      }
    }
    return false;
  }

  public static boolean allHaveTargetGoto(ImmutableList<SeqThreadStatement> pStatements) {
    for (SeqThreadStatement statement : pStatements) {
      if (statement.getTargetGoto().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public static boolean anyContainsEmptyBitVectorEvaluationExpression(
      ImmutableList<SeqThreadStatement> pStatements) {

    for (SeqThreadStatement statement : pStatements) {
      if (containsEmptyBitVectorEvaluationExpression(statement.getInjectedStatements())) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsEmptyBitVectorEvaluationExpression(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement evaluationStatement) {
        if (evaluationStatement.getEvaluationExpression().isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  // Helper ========================================================================================

  static ImmutableList<SeqInjectedStatement> appendInjectedStatements(
      SeqThreadStatement pStatement,
      ImmutableList<SeqInjectedStatement> pAppendedInjectedStatements) {

    return ImmutableList.<SeqInjectedStatement>builder()
        .addAll(pStatement.getInjectedStatements())
        .addAll(pAppendedInjectedStatements)
        .build();
  }
}
