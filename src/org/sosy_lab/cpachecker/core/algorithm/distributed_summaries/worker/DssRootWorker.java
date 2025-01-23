// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;

public class DssRootWorker extends DssWorker {

  private final BlockNode root;
  private final DssConnection connection;
  private final DssMessageFactory messageFactory;

  private boolean shutdown;

  DssRootWorker(
      String pId,
      DssConnection pConnection,
      BlockNode pNode,
      DssMessageFactory pMessageFactory,
      LogManager pLogger) {
    super(pId, pMessageFactory, pLogger);
    checkArgument(
        pNode.isRoot() && pNode.isEmpty() && pNode.getLast().equals(pNode.getFirst()),
        "Root node must be empty and cannot have predecessors: " + "%s",
        pNode);

    connection = pConnection;
    shutdown = false;
    root = pNode;
    messageFactory = pMessageFactory;
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage pMessage) {
    return switch (pMessage.getType()) {
      case ERROR_CONDITION -> {
        if (pMessage.getTargetNodeNumber() == root.getLast().getNodeNumber()) {
          yield ImmutableSet.of(
              messageFactory.newResultMessage(
                  root.getId(), root.getLast().getNodeNumber(), Result.FALSE));
        }
        yield ImmutableSet.of();
      }
      case FOUND_RESULT, ERROR -> {
        shutdown = true;
        yield ImmutableSet.of();
      }
      case STATISTICS, BLOCK_POSTCONDITION -> ImmutableSet.of();
    };
  }

  @Override
  public DssConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }
}
