// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockPostConditionActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ErrorConditionActorMessage;
import org.sosy_lab.java_smt.api.SolverException;

public class NeverProceed implements ProceedOperator {

  @Override
  public MessageProcessing proceedForward(BlockPostConditionActorMessage pMessage)
      throws InterruptedException {
    return proceed(pMessage);
  }

  @Override
  public MessageProcessing proceedBackward(ErrorConditionActorMessage pMessage)
      throws InterruptedException, SolverException {
    return proceed(pMessage);
  }

  @Override
  public MessageProcessing proceed(ActorMessage pMessage) {
    return MessageProcessing.stop();
  }

  @Override
  public void synchronizeKnowledge(DistributedConfigurableProgramAnalysis pAnalysis) {}

  @Override
  public void update(BlockPostConditionActorMessage pLatestOwnPreconditionMessage) {}
}
