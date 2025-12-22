// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;

public sealed interface SeqInjectedStatement extends SeqStatement
    permits SeqBitVectorAssignmentStatement,
        SeqCountUpdateStatement,
        SeqInjectedStatementWithTargetGoto,
        SeqLastBitVectorUpdateStatement,
        SeqLastThreadOrderStatement,
        SeqSyncUpdateStatement {

  /**
   * Whether this {@link SeqInjectedStatement} can be pruned from its owning {@link
   * CSeqThreadStatement} if it contains a target {@code goto} instead of a target {@code pc}.
   */
  boolean isPrunedWithTargetGoto();

  /**
   * Whether this {@link SeqInjectedStatement} can be pruned from its owning {@link
   * CSeqThreadStatement} when at least one {@link SeqInjectedStatement} contains an empty bit
   * vector evaluation expression.
   */
  boolean isPrunedWithEmptyBitVectorEvaluation();
}
