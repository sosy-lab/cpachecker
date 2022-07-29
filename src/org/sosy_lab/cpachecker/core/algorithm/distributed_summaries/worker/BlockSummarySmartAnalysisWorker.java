// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockPostConditionActorMessage;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockSummarySmartAnalysisWorker extends BlockSummaryAnalysisWorker {

  private final BlockingQueue<ActorMessage> smartQueue;
  private final BlockNode block;

  BlockSummarySmartAnalysisWorker(
      String pId,
      LogManager pLogManager,
      AnalysisOptions pOptions,
      Connection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(
        "smart-worker-" + pId,
        pOptions,
        pConnection,
        pBlock,
        pCFA,
        pSpecification,
        pShutdownManager,
        pLogManager);
    smartQueue = new PriorityBlockingQueue<>();
    block = pBlock;
  }

  @Override
  public ActorMessage nextMessage() throws InterruptedException {
    final Connection connection = getConnection();
    if (!smartQueue.isEmpty()) {
      return smartQueue.take();
    }
    if (connection.isEmpty()) {
      return connection.read();
    }
    Set<ActorMessage> newMessages = new LinkedHashSet<>();
    while (!connection.isEmpty()) {
      newMessages.add(connection.read());
    }
    ActorMessage postcondMessage = null;
    for (ActorMessage m : newMessages) {
      if (m.getType() == MessageType.BLOCK_POSTCONDITION) {
        if (m.getTargetNodeNumber() == block.getStartNode().getNodeNumber()) {
          MessageProcessing mp =
              getForwardAnalysis()
                  .getDistributedCPA()
                  .getProceedOperator()
                  .proceedForward((BlockPostConditionActorMessage) m);
          if (!mp.end()) {
            postcondMessage = ActorMessage.addEntry(m, Payload.SMART, "true");
          }
        }
      } else {
        smartQueue.add(m);
      }
    }
    if (postcondMessage != null) {
      smartQueue.add(postcondMessage);
    }
    return nextMessage();
  }
}
