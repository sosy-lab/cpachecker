// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
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
    super(pLogger);
    nodeMap = new HashMap<>();
    pNodes.forEach(node -> nodeMap.put(node.getId(), node));
    messageReceived = new HashSet<>();
    expectAnswer = new HashMap<>();
    nodeMap.keySet().forEach(id -> expectAnswer.put(id, 0));
    numWorkers = pNodes.size();
    violationOrigins = new HashSet<>();
  }

  @Override
  public Optional<Message> processMessage(Message pMessage)
      throws InterruptedException, CPAException, IOException, SolverException {
    String senderId = pMessage.getUniqueBlockId();
    MessageType type = pMessage.getType();
    if (!nodeMap.containsKey(senderId)) {
      return noResponse;
    }
    int numViolationsBefore = violationOrigins.size();
    messageReceived.add(senderId);
    switch (type) {
      case POSTCONDITION:
        boolean newPostCondition = Boolean.parseBoolean(pMessage.getAdditionalInformation());
        if (newPostCondition) {
          expectAnswer.merge(senderId, 1, Integer::sum);
          violationOrigins.add(senderId);
        } else {
          expectAnswer.merge(senderId, -1, Integer::sum);
          nodeMap.get(senderId).getPredecessors()
              .forEach(b -> expectAnswer.merge(b.getId(), 1, Integer::sum));
        }
        return response(numViolationsBefore);
      case POSTCONDITION_UNREACHABLE:
        expectAnswer.merge(senderId, -1, Integer::sum);
        return response(numViolationsBefore);
      case FOUND_RESULT:
      case ERROR:
        shutdown();
      case PRECONDITION:
        return noResponse;
      default:
        throw new AssertionError(type + " does not exist");
    }
  }

  private Optional<Message> response(int numViolationsBefore) {
    boolean onlyOriginViolations = true;
    for (Entry<String, Integer> stringIntegerEntry : expectAnswer.entrySet()) {
      if (violationOrigins.contains(stringIntegerEntry.getKey())) {
        onlyOriginViolations &= stringIntegerEntry.getValue() == 1;
      } else {
        onlyOriginViolations &= stringIntegerEntry.getValue() == 0;
      }
    }
    finished =
        messageReceived.size() == numWorkers && numViolationsBefore == violationOrigins.size()
            && (expectAnswer.values().stream().mapToInt(i -> i).sum() == 0
            || onlyOriginViolations);
    if (finished) {
      return answer(Message.newResultMessage("main", 0, Result.TRUE));
    }
    return noResponse;
  }

}
