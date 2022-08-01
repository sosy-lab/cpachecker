// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ActorMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.java_smt.api.SolverException;

public class AlwaysProceed implements ProceedOperator {
  @Override
  public ActorMessageProcessing proceedForward(BlockSummaryPostConditionMessage pMessage)
      throws InterruptedException {
    return proceed(pMessage);
  }

  @Override
  public ActorMessageProcessing proceedBackward(BlockSummaryErrorConditionMessage pMessage)
      throws InterruptedException, SolverException {
    return proceed(pMessage);
  }

  @Override
  public ActorMessageProcessing proceed(BlockSummaryMessage pMessage) {
    return ActorMessageProcessing.proceed();
  }

  @Override
  public void synchronizeKnowledge(DistributedConfigurableProgramAnalysis pAnalysis) {}

  @Override
  public void update(BlockSummaryPostConditionMessage pLatestOwnPreconditionMessage) {}
}
