// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.SolverException;

public interface ProceedOperator {

  /**
   * Processes the given state for a forward-analysis. The returned {@link DssMessageProcessing}
   * contains information about whether to start a forward analysis based on the given {@link
   * AbstractState}.
   *
   * @param pState Incoming state
   * @return a {@link DssMessageProcessing} that contains a decision whether to proceed and
   *     potential messages that were generated during processing
   * @throws InterruptedException thrown if program is interrupted unexpectedly.
   */
  DssMessageProcessing processForward(AbstractState pState)
      throws InterruptedException, SolverException;

  /**
   * Processes the given state for a backward-analysis. The returned {@link DssMessageProcessing}
   * contains information about whether to start a backward analysis based on the given {@link
   * AbstractState}.
   *
   * @param pState Incoming state
   * @return a {@link DssMessageProcessing} that contains a decision whether to proceed and
   *     potential messages that were generated during processing
   * @throws InterruptedException thrown if program is interrupted unexpectedly.
   * @throws SolverException thrown if backwards analysis is infeasible
   */
  DssMessageProcessing processBackward(AbstractState pState)
      throws InterruptedException, SolverException;

  static ProceedOperator always() {
    return new AlwaysProceed();
  }

  static ProceedOperator never() {
    return new NeverProceed();
  }
}
