// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class BlockSummaryResultWorker extends BlockSummaryWorker {

  private final Map<String, BlockNode> nodeMap;
  private final Set<String> messageReceived;
  private final Map<String, Integer> expectAnswer;
  private final int numWorkers;
  private final BlockSummaryConnection connection;
  private boolean shutdown;

  BlockSummaryResultWorker(
      Collection<BlockNode> pNodes,
      BlockSummaryConnection pConnection,
      BlockSummaryAnalysisOptions pOptions) {
    super("result-worker", pOptions);
    nodeMap = new HashMap<>();
    connection = pConnection;
    pNodes.forEach(node -> nodeMap.put(node.getId(), node));
    messageReceived = new LinkedHashSet<>();
    expectAnswer = new ConcurrentHashMap<>();
    nodeMap.keySet().forEach(nodeId -> expectAnswer.put(nodeId, 0));
    numWorkers = pNodes.size();
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
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
        boolean newPostCondition = ((BlockSummaryErrorConditionMessage) pMessage).isFirst();
        if (newPostCondition) {
          // we need a block to first send an own error condition or the first BLOCKPOSTCONDITION
          expectAnswer.merge(senderId, 1, Integer::sum);
        } else {
          expectAnswer.merge(senderId, -1, Integer::sum);
          nodeMap
              .get(senderId)
              .getPredecessors()
              .forEach(b -> expectAnswer.merge(b.getId(), 1, Integer::sum));
        }
        return response(pMessage);
      case ERROR_CONDITION_UNREACHABLE:
        expectAnswer.merge(senderId, -1, Integer::sum);
        return response(pMessage);
      case FOUND_RESULT:
        // fall through
      case ERROR:
        shutdown = true;
        return ImmutableSet.of();
      case BLOCK_POSTCONDITION:
        // we need a block to first send an own error condition or the first BLOCKPOSTCONDITION
        return response(pMessage);
      default:
        throw new AssertionError(type + " does not exist");
    }
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  private Collection<BlockSummaryMessage> response(BlockSummaryMessage pMessage) {
    // negative values can occur as it is not guaranteed
    // that messages are processed in the same way on all workers
    // that's why we use allMatch
    // to ensure we do not forget an error location, we need every worker to send an initial message
    if (getLogger().wouldBeLogged(Level.ALL)) {
      getLogger()
          .logf(
              Level.ALL,
              "Waiting for answers: %s (%d/%d)",
              Maps.filterValues(expectAnswer, v -> v != 0),
              messageReceived.size(),
              numWorkers);
    }
    if (messageReceived.size() == numWorkers
        && expectAnswer.values().stream().allMatch(i -> i == 0)) {
      shutdown = true;
      Set<String> visited = ImmutableSet.of();
      if (pMessage.getType() == MessageType.BLOCK_POSTCONDITION) {
        visited = ((BlockSummaryPostConditionMessage) pMessage).visitedBlockIds();
      } else if (pMessage.getType() == MessageType.ERROR_CONDITION) {
        visited = ((BlockSummaryErrorConditionMessage) pMessage).visitedBlockIds();
      }
      return ImmutableSet.of(
          BlockSummaryMessage.newResultMessage(
              pMessage.getUniqueBlockId(), 0, Result.TRUE, visited));
    }
    return ImmutableSet.of();
  }
}
