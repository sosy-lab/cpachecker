// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class SeqThreadStatementUtil {

  /**
   * Returns {@code true} if {@code pCurrentStatement} is inside an atomic block. Does not search
   * concatenated statements.
   */
  public static boolean isInAtomicBlock(SeqThreadStatement pCurrentStatement) {
    for (SubstituteEdge substituteEdge : pCurrentStatement.getSubstituteEdges()) {
      ThreadEdge threadEdge = substituteEdge.threadEdge;
      // use the predecessor, since we require information about this statement
      if (threadEdge.getPredecessor().isInAtomicBlock) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code pCurrentStatement} is inside an atomic block. Does not search
   * concatenated statements.
   */
  private static boolean isTargetInAtomicBlock(SeqThreadStatement pCurrentStatement) {
    for (SubstituteEdge substituteEdge : pCurrentStatement.getSubstituteEdges()) {
      ThreadEdge threadEdge = substituteEdge.threadEdge;
      // use the successor, since we require information about the target statement
      if (threadEdge.getSuccessor().isInAtomicBlock) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code pCurrentStatement} targets at least one statement that is an
   * atomic block.
   */
  public static boolean targetsAtomicBlock(SeqThreadStatement pCurrentStatement) {
    // first, recursively search concatenated statements
    if (pCurrentStatement.isConcatenable()) {
      ImmutableList<SeqThreadStatement> concatenatedStatements =
          pCurrentStatement.getConcatenatedStatements();
      if (!concatenatedStatements.isEmpty()) {
        for (SeqThreadStatement concatenatedStatement : concatenatedStatements) {
          return targetsAtomicBlock(concatenatedStatement);
        }
      }
    }
    // no concatenations -> search this statement and return
    return isTargetInAtomicBlock(pCurrentStatement);
  }
}
