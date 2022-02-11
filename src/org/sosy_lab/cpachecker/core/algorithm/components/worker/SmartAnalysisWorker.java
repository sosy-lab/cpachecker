// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class SmartAnalysisWorker extends AnalysisWorker {

  private final BlockingQueue<Message> smartQueue;

  SmartAnalysisWorker(
      String pId,
      AnalysisOptions pOptions,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      UpdatedTypeMap pTypeMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(pId, pOptions, pBlock, pLogger, pCFA, pSpecification, pConfiguration, pShutdownManager,
        pTypeMap);
    smartQueue = new PriorityBlockingQueue<>();
  }

  @Override
  public Message nextMessage() throws InterruptedException, SolverException {
    if (!smartQueue.isEmpty()) {
      return smartQueue.take();
    }
    if (connection.isEmpty()) {
      return connection.read();
    }
    Set<Message> newMessages = new LinkedHashSet<>();
    while (!connection.isEmpty()) {
      newMessages.add(connection.read());
    }
    Message postcondMessage = null;
    for (Message m : newMessages) {
      if (m.getType() == MessageType.BLOCK_POSTCONDITION) {
        if (m.getTargetNodeNumber() == block.getStartNode().getNodeNumber()) {
          MessageProcessing mp = forwardAnalysis.getDistributedCPA().proceedForward(m);
          if (!mp.end()) {
            Payload payload = m.getPayload();
            // proceedForward stores already processed messages and won't continue with a plain
            // copy of this message. We add "smart": "true" to it to avoid equality.
            payload = Payload.builder().putAll(payload).addEntry(Payload.SMART, "true").build();
            postcondMessage = Message.replacePayload(m, payload);
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
