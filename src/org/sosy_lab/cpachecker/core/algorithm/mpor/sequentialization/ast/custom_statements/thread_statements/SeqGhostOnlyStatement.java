// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a thread simulation statement that contains only a {@code pc} update, and optionally
 * its injected statements.
 *
 * <p>E.g. {@code case m: pc[thread_id] = n; continue;}
 */
public final class SeqGhostOnlyStatement extends CSeqThreadStatement {

  /** Use this if the target pc is an {@code int}. */
  public SeqGhostOnlyStatement(
      ReductionOrder pReductionOrder, CLeftHandSide pPcLeftHandSide, int pTargetPc) {
    super(pReductionOrder, ImmutableSet.of(), pPcLeftHandSide, pTargetPc);
  }

  private SeqGhostOnlyStatement(
      ReductionOrder pReductionOrder,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(
        pReductionOrder,
        ImmutableSet.of(),
        pPcLeftHandSide,
        pTargetPc,
        Optional.empty(),
        pInjectedStatements);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return SeqThreadStatementUtil.buildInjectedStatementsString(
        reductionOrder, pcLeftHandSide, targetPc, Optional.empty(), injectedStatements);
  }

  @Override
  public SeqGhostOnlyStatement withTargetPc(int pTargetPc) {
    return new SeqGhostOnlyStatement(
        reductionOrder, pcLeftHandSide, Optional.of(pTargetPc), injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    throw new UnsupportedOperationException(this.getClass().getName() + " do not have target goto");
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqGhostOnlyStatement(reductionOrder, pcLeftHandSide, targetPc, pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return false;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }
}
