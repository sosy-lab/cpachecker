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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockSummarySmartAnalysisWorker extends BlockSummaryAnalysisWorker {

  private final BlockingQueue<BlockSummaryMessage> smartQueue;
  private final BlockNode block;

  BlockSummarySmartAnalysisWorker(
      String pId,
      BlockSummaryAnalysisOptions pOptions,
      BlockSummaryConnection pConnection,
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
        pShutdownManager);
    smartQueue = new PriorityBlockingQueue<>();
    block = pBlock;
  }

  @Override
  public BlockSummaryMessage nextMessage() throws InterruptedException {
    final BlockSummaryConnection connection = getConnection();
    if (!smartQueue.isEmpty()) {
      return smartQueue.take();
    }
    if (!connection.hasPendingMessages()) {
      return connection.read();
    }
    Set<BlockSummaryMessage> newMessages = new LinkedHashSet<>();
    while (connection.hasPendingMessages()) {
      newMessages.add(connection.read());
    }
    BlockSummaryMessage postcondMessage = null;
    for (BlockSummaryMessage m : newMessages) {
      if (m.getType() == MessageType.BLOCK_POSTCONDITION) {
        if (m.getTargetNodeNumber() == block.getStartNode().getNodeNumber()) {
          BlockSummaryMessageProcessing mp =
              getForwardAnalysis()
                  .getDistributedCPA()
                  .getProceedOperator()
                  .proceedForward((BlockSummaryPostConditionMessage) m);
          if (!mp.end()) {
            postcondMessage =
                BlockSummaryMessage.addEntry(m, BlockSummaryMessagePayload.SMART, "true");
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
