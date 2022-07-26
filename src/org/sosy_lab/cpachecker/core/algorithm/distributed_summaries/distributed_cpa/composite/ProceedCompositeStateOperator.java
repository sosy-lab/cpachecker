// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ErrorConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedCompositeStateOperator implements ProceedOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final AnalysisDirection direction;

  private BlockPostConditionMessage latestOwnPostConditionMessage;

  public ProceedCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      AnalysisDirection pDirection) {
    direction = pDirection;
    registered = pRegistered;
  }

  @Override
  public MessageProcessing proceedForward(BlockPostConditionMessage pMessage)
      throws InterruptedException {
    MessageProcessing processing = MessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing = processing.merge(value.getProceedOperator().proceedForward(pMessage), true);
    }
    return processing;
  }

  @Override
  public MessageProcessing proceedBackward(ErrorConditionMessage pMessage)
      throws InterruptedException, SolverException {
    MessageProcessing processing = MessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing = processing.merge(value.getProceedOperator().proceedBackward(pMessage), true);
    }
    return processing;
  }

  @Override
  public MessageProcessing proceed(ActorMessage pMessage)
      throws InterruptedException, SolverException {
    return direction == AnalysisDirection.FORWARD
        ? proceedForward((BlockPostConditionMessage) pMessage)
        : proceedBackward((ErrorConditionMessage) pMessage);
  }

  @Override
  public void synchronizeKnowledge(DistributedConfigurableProgramAnalysis pAnalysis) {
    ProceedCompositeStateOperator distributed =
        (ProceedCompositeStateOperator) pAnalysis.getProceedOperator();
    if (direction == AnalysisDirection.BACKWARD) {
      latestOwnPostConditionMessage = distributed.latestOwnPostConditionMessage;
    }
    for (Entry<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
        entry : registered.entrySet()) {
      if (distributed.registered.containsKey(entry.getKey())) {
        entry
            .getValue()
            .getProceedOperator()
            .synchronizeKnowledge(distributed.registered.get(entry.getKey()));
      }
    }
  }

  @Override
  public void update(BlockPostConditionMessage pLatestOwnPreconditionMessage) {
    latestOwnPostConditionMessage = pLatestOwnPreconditionMessage;
    registered
        .values()
        .forEach(analysis -> analysis.getProceedOperator().update(pLatestOwnPreconditionMessage));
  }
}
