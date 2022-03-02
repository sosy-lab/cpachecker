// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class ResultWorker extends Worker {

  private final Map<String, BlockNode> nodeMap;
  private final Set<String> messageReceived;
  private final Map<String, Integer> expectAnswer;
  private final int numWorkers;

  ResultWorker(
      LogManager pLogger,
      Collection<BlockNode> pNodes,
      AnalysisOptions pOptions
  ) {
    super("result-worker", pLogger, pOptions);
    nodeMap = new HashMap<>();
    pNodes.forEach(node -> nodeMap.put(node.getId(), node));
    messageReceived = new HashSet<>();
    expectAnswer = new ConcurrentHashMap<>();
    nodeMap.keySet().forEach(nodeId -> expectAnswer.put(nodeId, 0));
    numWorkers = pNodes.size();
  }

  @Override
  public Collection<Message> processMessage(Message pMessage)
      throws InterruptedException, CPAException, IOException, SolverException {
    String senderId = pMessage.getUniqueBlockId();
    MessageType type = pMessage.getType();

    // not an analysis-worker
    if (!nodeMap.containsKey(senderId)) {
      return ImmutableSet.of();
    }

    messageReceived.add(senderId);

    switch (type) {
      case ERROR_CONDITION:
        boolean newPostCondition = Boolean.parseBoolean(pMessage.getPayload().get("first"));
        if (newPostCondition) {
          // we need a block to first send an own error condition or the first BLOCKPOSTCONDITION
          expectAnswer.merge(senderId, 1, Integer::sum);
        } else {
          expectAnswer.merge(senderId, -1, Integer::sum);
          nodeMap.get(senderId).getPredecessors()
              .forEach(b -> expectAnswer.merge(b.getId(), 1, Integer::sum));
        }
        return response(pMessage);
      case ERROR_CONDITION_UNREACHABLE:
        expectAnswer.merge(senderId, -1, Integer::sum);
        return response(pMessage);
      case FOUND_RESULT:
        // fall through
      case ERROR:
        shutdown();
        return ImmutableSet.of();
      case BLOCK_POSTCONDITION:
        // we need a block to first send an own error condition or the first BLOCKPOSTCONDITION
        return ImmutableSet.of();
      default:
        throw new AssertionError(type + " does not exist");
    }
  }

  private Collection<Message> response(Message pMessage) {
    // negative values can occur as it is not guaranteed
    // that messages are processed in the same way on all workers
    // that's why we use allMatch
    // to ensure we do not forget an error location, we need every worker to send an initial message
    logger.log(Level.ALL,"Waiting for answers: ", expectAnswer.entrySet().stream().filter(e -> e.getValue() != 0).collect(
        ImmutableMap.toImmutableMap(e -> e.getKey(), e-> e.getValue())) + "-" + messageReceived.size() + "/" + numWorkers);
    finished =
        messageReceived.size() == numWorkers
            && expectAnswer.values().stream().allMatch(i -> i == 0);
    if (finished) {
      return ImmutableSet.of(Message.newResultMessage(pMessage.getUniqueBlockId(), 0, Result.TRUE,
          new HashSet<>(Splitter.on(",")
              .splitToList(pMessage.getPayload().getOrDefault(Payload.VISITED, "")))));
    }
    return ImmutableSet.of();
  }

}
