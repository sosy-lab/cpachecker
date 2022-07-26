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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class SmartAnalysisBlockSummaryWorker extends AnalysisBlockSummaryWorker {

  private final BlockingQueue<ActorMessage> smartQueue;
  private final BlockNode block;

  SmartAnalysisBlockSummaryWorker(
      String pId,
      UpdatedTypeMap pTypeMap,
      AnalysisOptions pOptions,
      Connection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(
        "smart-worker-" + pId,
        pTypeMap,
        pOptions,
        pConnection,
        pBlock,
        pCFA,
        pSpecification,
        pShutdownManager);
    smartQueue = new PriorityBlockingQueue<>();
    block = pBlock;
  }

  @Override
  public ActorMessage nextMessage() throws InterruptedException, SolverException {
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
              getForwardAnalysis().getDistributedCPA().getProceedOperator().proceedForward(m);
          if (!mp.end()) {
            Payload payload = m.getPayload();
            // proceedForward stores already processed messages and won't continue with a plain
            // copy of this message. We add "smart": "true" to it to avoid equality.
            payload = Payload.builder().putAll(payload).addEntry(Payload.SMART, "true").build();
            postcondMessage = ActorMessage.replacePayload(m, payload);
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
