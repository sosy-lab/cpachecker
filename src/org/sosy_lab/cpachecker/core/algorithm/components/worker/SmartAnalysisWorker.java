// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public class SmartAnalysisWorker extends AnalysisWorker {

  private final BlockingQueue<Message> smartQueue;

  SmartAnalysisWorker(
      String pId,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      SSAMap pTypeMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(pId, pBlock, pLogger, pCFA, pSpecification, pConfiguration, pShutdownManager, pTypeMap);
    smartQueue = new PriorityBlockingQueue<>();
  }

  @Override
  public Message nextMessage() throws InterruptedException {
    Optional<Message> foundPrecondition = Optional.empty();
    Optional<Message> endMessage = Optional.empty();
    if (connection.isEmpty()) {
      return super.nextMessage();
    }
    while (!connection.isEmpty()) {
      Message m = super.nextMessage();
      if (m.getType() == MessageType.BLOCK_POSTCONDITION) {
        Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
            .filter(node -> node.getNodeNumber() == m.getTargetNodeNumber()).findAny();
        CFANode node = optionalCFANode.orElseThrow();
        if (node.equals(block.getStartNode())) {
          receivedPreConditions.put(m.getUniqueBlockId(), m);
          foundPrecondition = Optional.of(m);
        }
      } else if (m.getType() == MessageType.FOUND_RESULT || m.getType() == MessageType.ERROR) {
        endMessage = Optional.of(m);
      } else {
        smartQueue.add(m);
      }
    }
    // worker shuts down
    if (endMessage.isPresent()) {
      return endMessage.orElseThrow();
    }

    if (foundPrecondition.isPresent()) {
      smartQueue.add(foundPrecondition.orElseThrow());
    }
    if (smartQueue.isEmpty()) {
      return nextMessage();
    }
    return smartQueue.take();
  }
}
