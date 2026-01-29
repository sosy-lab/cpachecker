// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;

public sealed interface SeqInjectedStatement extends CExportStatement
    permits SeqBitVectorAssignmentStatement,
        SeqCountUpdateStatement,
        SeqInjectedStatementWithTargetGoto,
        SeqLastBitVectorUpdateStatement,
        SeqSyncUpdateStatement {

  /**
   * Whether this {@link SeqInjectedStatement} can be pruned from its owning {@link
   * CSeqThreadStatement} if it contains a target {@code goto} instead of a target {@code pc}.
   *
   * <p>If a target {@code goto} is present, then the simulation stays in the same thread. Some
   * {@link SeqInjectedStatement}s update e.g. ghost variables that are utilized by other threads.
   * But if no context-switch occurs due to the {@code goto}, then the ghost variable updates are
   * unnecessary and can be pruned.
   */
  boolean isPrunedWithTargetGoto();

  /**
   * Whether this {@link SeqInjectedStatement} can be pruned from its owning {@link
   * CSeqThreadStatement} when at least one {@link SeqInjectedStatement} contains an empty bit
   * vector evaluation expression.
   */
  boolean isPrunedWithEmptyBitVectorEvaluation();
}
