// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.java_smt.api.SolverException;

public interface ProceedOperator {

  /**
   * Decide whether to start a forward analysis based on the contents of the {@link
   * BlockSummaryPostConditionMessage}.
   *
   * @param pMessage Incoming message
   * @return A potentially empty set of responses to {@code pMessage}
   * @throws InterruptedException thrown if program is interrupted unexpectedly.
   */
  BlockSummaryMessageProcessing proceedForward(BlockSummaryPostConditionMessage pMessage)
      throws InterruptedException;

  /**
   * Decide whether to start a backward analysis based on the contents of the {@link
   * BlockSummaryPostConditionMessage}.
   *
   * @param pMessage Incoming message
   * @return A potentially empty set of responses to {@code pMessage}
   * @throws InterruptedException thrown if program is interrupted unexpectedly.
   * @throws SolverException thrown if backwards analysis is infeasible
   */
  BlockSummaryMessageProcessing proceedBackward(BlockSummaryErrorConditionMessage pMessage)
      throws InterruptedException, SolverException;

  /**
   * Decide whether to respond to the incoming message {@code pMessage}
   *
   * @param pMessage Incoming message
   * @return A potentially empty set of responses to {@code pMessage}
   * @throws InterruptedException thrown if program is interrupted unexpectedly.
   * @throws SolverException thrown if backwards analysis is infeasible
   */
  BlockSummaryMessageProcessing proceed(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException;

  /**
   * Synchronize the knowledge of the forward analysis with the knowledge of the backward analysis
   * for later infeasible checks.
   *
   * @param pAnalysis Synchronize the knowledge of {@code pAnalysis} with this proceed operator
   */
  void synchronizeKnowledge(DistributedConfigurableProgramAnalysis pAnalysis)
      throws InterruptedException;

  /**
   * Set the latest own {@link BlockSummaryPostConditionMessage}
   *
   * @param pLatestOwnPreconditionMessage latest {@link BlockSummaryPostConditionMessage}
   */
  void update(BlockSummaryPostConditionMessage pLatestOwnPreconditionMessage)
      throws InterruptedException;
}
