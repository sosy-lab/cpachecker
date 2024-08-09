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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage;

public class DSSRootWorker extends DSSWorker {

  private final BlockNode root;
  private final DSSConnection connection;
  private boolean shutdown;

  DSSRootWorker(String pId, DSSConnection pConnection, BlockNode pNode, LogManager pLogger) {
    super(pId, pLogger);
    checkArgument(
        pNode.isRoot() && pNode.isEmpty() && pNode.getLast().equals(pNode.getFirst()),
        "Root node must be empty and cannot have predecessors: " + "%s",
        pNode);

    connection = pConnection;
    shutdown = false;
    root = pNode;
  }

  @Override
  public Collection<DSSMessage> processMessage(DSSMessage pMessage) {
    return switch (pMessage.getType()) {
      case ERROR_CONDITION -> {
        if (pMessage.getTargetNodeNumber() == root.getLast().getNodeNumber()) {
          yield ImmutableSet.of(
              DSSMessage.newResultMessage(
                  root.getId(), root.getLast().getNodeNumber(), Result.FALSE));
        }
        yield ImmutableSet.of();
      }
      case FOUND_RESULT, ERROR -> {
        shutdown = true;
        yield ImmutableSet.of();
      }
      case STATISTICS, BLOCK_POSTCONDITION, ERROR_CONDITION_UNREACHABLE -> ImmutableSet.of();
    };
  }

  @Override
  public DSSConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }
}
