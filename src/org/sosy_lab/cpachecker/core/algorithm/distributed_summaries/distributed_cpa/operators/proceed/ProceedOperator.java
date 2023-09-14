// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.SolverException;

public interface ProceedOperator {

  /**
   * Decide whether to start a forward analysis based on the contents of the {@link AbstractState}.
   *
   * @param pState Incoming state
   * @return A potentially empty set of responses to {@code pMessage}
   * @throws InterruptedException thrown if program is interrupted unexpectedly.
   */
  BlockSummaryMessageProcessing proceedForward(AbstractState pState)
      throws InterruptedException, SolverException;

  /**
   * Decide whether to start a backward analysis based on the contents of the {@link AbstractState}.
   *
   * @param pState Incoming state
   * @return A potentially empty set of responses to {@code pMessage}
   * @throws InterruptedException thrown if program is interrupted unexpectedly.
   * @throws SolverException thrown if backwards analysis is infeasible
   */
  BlockSummaryMessageProcessing proceedBackward(AbstractState pState)
      throws InterruptedException, SolverException;

  static ProceedOperator always() {
    return new AlwaysProceed();
  }

  static ProceedOperator never() {
    return new NeverProceed();
  }
}
