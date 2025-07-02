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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

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
        pNode.isRoot()
            && pNode.isEmpty()
            && pNode.getFinalLocation().equals(pNode.getInitialLocation()),
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
      case VIOLATION_CONDITION -> {
        if (root.getSuccessorIds().contains(pMessage.getSenderId())) {
          yield ImmutableSet.of(messageFactory.createDssResultMessage(root.getId(), Result.FALSE));
        }
        yield ImmutableSet.of();
      }
      case RESULT, EXCEPTION -> {
        shutdown = true;
        yield ImmutableSet.of();
      }
      case STATISTIC, PRECONDITION -> ImmutableSet.of();
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
