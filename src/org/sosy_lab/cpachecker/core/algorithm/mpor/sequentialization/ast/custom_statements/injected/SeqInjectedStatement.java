// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;

public abstract sealed class SeqInjectedStatement implements SeqStatement
    permits SeqBitVectorAssignmentStatement,
        SeqBitVectorEvaluationStatement,
        SeqCountUpdateStatement,
        SeqIgnoreSleepReductionStatement,
        SeqLastBitVectorUpdateStatement,
        SeqLastThreadOrderStatement,
        SeqRoundGotoStatement,
        SeqSyncUpdateStatement {

  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return Optional.empty();
  }

  public SeqInjectedStatement withLabelNumber(int pTargetNumber) {
    throw new UnsupportedOperationException(
        this.getClass().getName() + " does not have a target goto, cloning not possible.");
  }
}
