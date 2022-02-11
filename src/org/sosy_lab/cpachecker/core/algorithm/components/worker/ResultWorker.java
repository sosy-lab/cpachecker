// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
  private final Set<String> violationOrigins;
  private final int numWorkers;

  ResultWorker(
      LogManager pLogger,
      Collection<BlockNode> pNodes
  ) {
    super("result-worker", pLogger);
    nodeMap = new HashMap<>();
    pNodes.forEach(node -> nodeMap.put(node.getId(), node));
    messageReceived = new HashSet<>();
    expectAnswer = new ConcurrentHashMap<>();
    nodeMap.keySet().forEach(id -> expectAnswer.put(id, 0));
    numWorkers = pNodes.size();
    violationOrigins = new HashSet<>();
  }

  @Override
  public Collection<Message> processMessage(Message pMessage)
      throws InterruptedException, CPAException, IOException, SolverException {
    String senderId = pMessage.getUniqueBlockId();
    MessageType type = pMessage.getType();
    if (!nodeMap.containsKey(senderId)) {
      return ImmutableSet.of();
    }
    int numViolationsBefore = violationOrigins.size();
    messageReceived.add(senderId);
    switch (type) {
      case ERROR_CONDITION:
        boolean newPostCondition = Boolean.parseBoolean(pMessage.getPayload().get("first"));
        if (newPostCondition) {
          expectAnswer.merge(senderId, 1, Integer::sum);
          violationOrigins.add(senderId);
        } else {
          expectAnswer.merge(senderId, -1, Integer::sum);
          nodeMap.get(senderId).getPredecessors()
              .forEach(b -> expectAnswer.merge(b.getId(), 1, Integer::sum));
        }
        return response(numViolationsBefore, pMessage);
      case ERROR_CONDITION_UNREACHABLE:
        expectAnswer.merge(senderId, -1, Integer::sum);
        return response(numViolationsBefore, pMessage);
      case FOUND_RESULT:
      case ERROR:
        shutdown();
      case BLOCK_POSTCONDITION:
        return ImmutableSet.of();
      default:
        throw new AssertionError(type + " does not exist");
    }
  }

  private Collection<Message> response(int numViolationsBefore, Message pMessage) {
    logger.log(Level.INFO, pMessage);
    boolean onlyOriginViolations = true;
    for (Entry<String, Integer> stringIntegerEntry : expectAnswer.entrySet()) {
      if (violationOrigins.contains(stringIntegerEntry.getKey())) {
        onlyOriginViolations &= stringIntegerEntry.getValue() == 1;
      } else {
        onlyOriginViolations &= stringIntegerEntry.getValue() == 0;
      }
    }
    // negative values can occur as it is not guaranteed that messages are processed in the same way on all workers
    finished =
        messageReceived.size() == numWorkers && numViolationsBefore == violationOrigins.size()
            // that's why we use allMatch
            && (expectAnswer.values().stream().allMatch(i -> i == 0)
            || onlyOriginViolations);
    if (finished) {
      return ImmutableSet.of(Message.newResultMessage(pMessage.getUniqueBlockId(), 0, Result.TRUE,
          new HashSet<>(Splitter.on(",")
              .splitToList(pMessage.getPayload().getOrDefault(Payload.VISITED, "")))));
    }
    return ImmutableSet.of();
  }

}
